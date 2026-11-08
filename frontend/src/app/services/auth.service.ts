import { HttpClient, HttpContext, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, firstValueFrom, map, tap, timeout } from 'rxjs';
import { API_BASE_URL } from '../core/api/api-base-url';
import { ANONYMOUS_REQUEST, SKIP_SESSION_EXPIRED_REDIRECT } from '../core/auth/auth-context';
import { AuthProfile, AuthResponse, AuthRole, AuthSession, AuthUser, LoginRequest } from '../models/auth.model';

const STORAGE_KEY = 'pethaven.session';
// Clave de la sesión provisional anterior (sessionStorage, sin token): se elimina al arrancar.
const LEGACY_STORAGE_KEY = 'pethaven.user';
// Espera máxima de GET /auth/me al arrancar; si se agota, se conserva la sesión (como un error de red).
const RESTORE_TIMEOUT_MS = 5000;
// Mayor espera que admite setTimeout (2^31 - 1 ms, unos 24,8 días).
const MAX_TIMER_DELAY_MS = 2_147_483_647;

// Login de cada portal: a dónde se envía al usuario cuando no tiene sesión o expira.
const LOGIN_ROUTES: Record<AuthRole, string> = {
  OWNER: '/login',
  VETERINARIAN: '/vet/login',
  ADMIN: '/admin/login',
};

// Sesión con Spring Security + JWT: inicia sesión contra el API, guarda el token en localStorage,
// lo expone como signals, la restaura al recargar (GET /auth/me) y la cierra al expirar el token.
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiUrl = inject(API_BASE_URL);

  private readonly _session = signal<AuthSession | null>(this.readStoredSession());
  private expiryTimer: ReturnType<typeof setTimeout> | null = null;

  // Usuario actual; null si nadie ha iniciado sesión.
  readonly user = computed<AuthUser | null>(() => this._session()?.user ?? null);
  readonly isLoggedIn = computed(() => this._session() !== null);

  constructor() {
    this.removeLegacySession();
    this.scheduleExpiry();
  }

  // Token de acceso vigente, o null si no hay sesión (lo usa el interceptor).
  token(): string | null {
    return this._session()?.token ?? null;
  }

  // POST /auth/login: 401 si las credenciales no son válidas; 403 si la contraseña es correcta
  // pero la cuenta está desactivada.
  login(request: LoginRequest): Observable<AuthUser> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, request, {
      // Sin el token guardado: un 401 por credenciales incorrectas no debe cerrar la sesión vigente.
      context: new HttpContext().set(ANONYMOUS_REQUEST, true),
    }).pipe(
      map((response) => ({
        token: response.token,
        expiresAt: Date.now() + response.expiresIn * 1000,
        user: toUser(response),
      })),
      tap((session) => this.setSession(session)),
      map((session) => session.user)
    );
  }

  // Al arrancar la app: si hay una sesión guardada y vigente, la confirma con GET /auth/me.
  // Un 401 (token inválido, cuenta desactivada o eliminada) la cierra; un error de red o un API que no
  // responde a tiempo la conservan.
  async restoreSession(): Promise<void> {
    const session = this._session();
    if (!session) {
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
      this.setSession({ ...session, user: toUser(profile) });
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        this.clearSession();
      }
    }
  }

  // Cierra la sesión y borra el token guardado.
  logout(): void {
    this.clearSession();
  }

  // El token ya no sirve (expiró o el API respondió 401): cierra la sesión y lleva al login
  // del portal del usuario con un aviso y la ruta a la que volver.
  expireSession(): void {
    const role = this.user()?.role;
    if (!role) {
      return;
    }
    const returnUrl = this.router.url;
    this.clearSession();
    void this.router.navigate([LOGIN_ROUTES[role]], { queryParams: { expired: 1, returnUrl } });
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

  private setSession(session: AuthSession): void {
    this._session.set(session);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch {
      // Sin storage la sesión solo vive hasta recargar la página.
    }
    this.scheduleExpiry();
  }

  private clearSession(): void {
    this._session.set(null);
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
    const session = this._session();
    if (!session) {
      return;
    }
    const remaining = Math.max(0, session.expiresAt - Date.now());
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

  // Recupera la sesión guardada; una sesión vencida o con un formato inesperado se descarta.
  private readStoredSession(): AuthSession | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return null;
      }
      const session = JSON.parse(raw) as AuthSession;
      if (!session?.token || !session.user || typeof session.expiresAt !== 'number' || session.expiresAt <= Date.now()) {
        localStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return session;
    } catch {
      return null;
    }
  }

  private removeLegacySession(): void {
    try {
      sessionStorage.removeItem(LEGACY_STORAGE_KEY);
    } catch {
      // Storage bloqueado: no hay nada que limpiar.
    }
  }
}

function toUser(profile: AuthProfile | AuthResponse): AuthUser {
  return { id: profile.profileId, name: profile.name, email: profile.email, role: profile.role };
}
