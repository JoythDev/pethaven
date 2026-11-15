import { HttpClient, HttpContext, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, firstValueFrom, map, timeout } from 'rxjs';
import { API_BASE_URL } from '../core/api/api-base-url';
import { ANONYMOUS_REQUEST, SKIP_SESSION_EXPIRED_REDIRECT } from '../core/auth/auth-context';
import { AuthProfile, AuthResponse, AuthRole, AuthUser, LoginRequest } from '../models/auth.model';

// Lo único que se guarda en el navegador es el token: el perfil (nombre, rol, id) se pide al API
// con GET /auth/me en cada carga, y la expiración se lee del propio token (claim exp).
const STORAGE_KEY = 'pethaven.token';
// Formatos anteriores, que se migran o eliminan al arrancar: la sesión provisional sin token
// (sessionStorage) y la sesión que además del token guardaba el usuario y la expiración (localStorage).
const LEGACY_USER_KEY = 'pethaven.user';
const LEGACY_SESSION_KEY = 'pethaven.session';
// Espera máxima de GET /auth/me al arrancar; si se agota, se conserva el token (como un error de red).
const RESTORE_TIMEOUT_MS = 5000;
// Mayor espera que admite setTimeout (2^31 - 1 ms, unos 24,8 días).
const MAX_TIMER_DELAY_MS = 2_147_483_647;
const LOGIN_ROUTE = '/login';

// Login de cada portal: a dónde se envía al usuario cuando no tiene sesión o expira.
const LOGIN_ROUTES: Record<AuthRole, string> = {
  OWNER: LOGIN_ROUTE,
  VETERINARIAN: LOGIN_ROUTE,
  ADMIN: LOGIN_ROUTE,
};

// Sesión con Spring Security + JWT: inicia sesión contra el API, guarda solo el token en localStorage,
// mantiene el usuario en memoria (signal), lo pide al API al recargar (GET /auth/me), cierra la sesión
// al expirar el token y la sincroniza entre pestañas.
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiUrl = inject(API_BASE_URL);

  private readonly _token = signal<string | null>(this.readStoredToken());
  // Perfil del usuario según el API; solo vive en memoria.
  private readonly _user = signal<AuthUser | null>(null);
  private expiryTimer: ReturnType<typeof setTimeout> | null = null;

  // Usuario actual; null si nadie ha iniciado sesión (o si el API no pudo confirmar el token al arrancar).
  readonly user = this._user.asReadonly();
  readonly isLoggedIn = computed(() => this._user() !== null);

  constructor() {
    this.removeLegacyUser();
    this.scheduleExpiry();
    this.listenToOtherTabs();
  }

  // Token de acceso vigente, o null si no hay sesión (lo usa el interceptor).
  token(): string | null {
    return this._token();
  }

  // POST /auth/login: 401 si las credenciales no son válidas; 403 si la contraseña es correcta
  // pero la cuenta está desactivada.
  login(request: LoginRequest): Observable<AuthUser> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, request, {
      // Sin el token guardado: un 401 por credenciales incorrectas no debe cerrar la sesión vigente.
      context: new HttpContext().set(ANONYMOUS_REQUEST, true),
    }).pipe(
      map((response) => {
        const user = toUser(response);
        this.setSession(response.token, user);
        return user;
      })
    );
  }

  // Al arrancar la app: si hay un token guardado y vigente, pide el perfil con GET /auth/me.
  // Un 401 (token inválido, cuenta desactivada o eliminada) cierra la sesión. Un error de red o un API
  // que no responde a tiempo conservan el token, pero sin perfil la app trata al usuario como no
  // autenticado (los guards lo llevan al login).
  async restoreSession(): Promise<void> {
    if (!this._token()) {
      return;
    }
    try {
      const profile = await firstValueFrom(
        this.http
          .get<AuthProfile>(`${this.apiUrl}/auth/me`, {
            context: new HttpContext().set(SKIP_SESSION_EXPIRED_REDIRECT, true),
          })
          // El arranque espera esta llamada: un API que no responde no puede dejar la app en blanco.
          .pipe(timeout(RESTORE_TIMEOUT_MS))
      );
      this._user.set(toUser(profile));
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        this.clearSession();
      }
    }
  }

  // Cierra la sesión, borra el token guardado y lleva al login.
  logout(): void {
    this.clearSession();
    void this.router.navigateByUrl(LOGIN_ROUTE);
  }

  // El token ya no sirve (expiró o el API respondió 401): cierra la sesión y lleva al login
  // del portal del usuario con un aviso y la ruta a la que volver.
  expireSession(): void {
    if (!this._token()) {
      return;
    }
    const role = this._user()?.role;
    const returnUrl = this.router.url;
    this.clearSession();
    void this.router.navigate([role ? LOGIN_ROUTES[role] : LOGIN_ROUTE], {
      queryParams: { expired: 1, returnUrl },
    });
  }

  // Página de inicio del portal de cada rol.
  homeUrl(user: AuthUser): string {
    switch (user.role) {
      case 'OWNER':
        return `/cliente/${user.id}/mascotas`;
      case 'VETERINARIAN':
        return '/veterinario';
      case 'ADMIN':
        return '/administrador';
    }
  }

  loginUrl(role: AuthRole): string {
    return LOGIN_ROUTES[role];
  }

  private setSession(token: string, user: AuthUser): void {
    this._token.set(token);
    this._user.set(user);
    try {
      localStorage.setItem(STORAGE_KEY, token);
    } catch {
      // Sin storage la sesión solo vive hasta recargar la página.
    }
    this.scheduleExpiry();
  }

  private clearSession(): void {
    this._token.set(null);
    this._user.set(null);
    this.cancelExpiry();
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // Si el navegador bloquea el storage, igual queda cerrada en memoria.
    }
  }

  // Programa el cierre de la sesión justo cuando expira el token. setTimeout no admite esperas mayores a
  // MAX_TIMER_DELAY_MS (dispararía al instante), así que las largas se reprograman por tramos.
  private scheduleExpiry(): void {
    this.cancelExpiry();
    const token = this._token();
    const expiresAt = token ? tokenExpiry(token) : null;
    if (expiresAt === null) {
      return;
    }
    const remaining = Math.max(0, expiresAt - Date.now());
    this.expiryTimer =
      remaining > MAX_TIMER_DELAY_MS
        ? setTimeout(() => this.scheduleExpiry(), MAX_TIMER_DELAY_MS)
        : setTimeout(() => this.expireSession(), remaining);
  }

  private cancelExpiry(): void {
    if (this.expiryTimer !== null) {
      clearTimeout(this.expiryTimer);
      this.expiryTimer = null;
    }
  }

  // Si otra pestaña cierra la sesión (borra el token), esta también la cierra y vuelve al login.
  // El evento storage solo llega a las demás pestañas, nunca a la que hizo el cambio.
  private listenToOtherTabs(): void {
    window.addEventListener('storage', (event) => {
      const tokenRemoved = event.key === null || (event.key === STORAGE_KEY && event.newValue === null);
      if (tokenRemoved && this._token()) {
        this.logout();
      }
    });
  }

  // Recupera el token guardado (o lo migra del formato anterior); uno vencido, sin fecha de
  // expiración legible o con un formato inesperado se descarta.
  private readStoredToken(): string | null {
    try {
      const token = localStorage.getItem(STORAGE_KEY) ?? this.migrateLegacySession();
      if (!token) {
        return null;
      }
      const expiresAt = tokenExpiry(token);
      if (expiresAt === null || expiresAt <= Date.now()) {
        localStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return token;
    } catch {
      return null;
    }
  }

  // Sesión del formato anterior ({ token, expiresAt, user }): conserva solo el token y borra el resto.
  private migrateLegacySession(): string | null {
    const raw = localStorage.getItem(LEGACY_SESSION_KEY);
    if (raw === null) {
      return null;
    }
    localStorage.removeItem(LEGACY_SESSION_KEY);
    try {
      const token = (JSON.parse(raw) as { token?: unknown })?.token;
      if (typeof token !== 'string' || !token) {
        return null;
      }
      localStorage.setItem(STORAGE_KEY, token);
      return token;
    } catch {
      return null;
    }
  }

  private removeLegacyUser(): void {
    try {
      sessionStorage.removeItem(LEGACY_USER_KEY);
    } catch {
      // Storage bloqueado: no hay nada que limpiar.
    }
  }
}

// Momento de expiración del token (epoch ms) según su claim exp, o null si no se puede leer.
// Solo se decodifica, no se verifica la firma: eso lo hace el API en cada petición.
function tokenExpiry(token: string): number | null {
  const payload = token.split('.')[1];
  if (!payload) {
    return null;
  }
  try {
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
    const exp = (JSON.parse(atob(padded)) as { exp?: unknown }).exp;
    return typeof exp === 'number' ? exp * 1000 : null;
  } catch {
    return null;
  }
}

function toUser(profile: AuthProfile | AuthResponse): AuthUser {
  return { id: profile.profileId, name: profile.name, email: profile.email, role: profile.role };
}
