import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../core/api/api-base-url';
import { AuthUser, LoginRequest } from '../models/auth.model';

const STORAGE_KEY = 'pethaven.user';

// Maneja el inicio de sesión provisional (sin token): llama al API,
// guarda al usuario en sessionStorage y lo expone como signal.
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_BASE_URL);

  // Usuario actual; null si nadie ha iniciado sesión.
  private readonly _user = signal<AuthUser | null>(this.readStoredUser());
  readonly user = this._user.asReadonly();
  readonly isLoggedIn = computed(() => this._user() !== null);

  // POST /auth/owners/login: 401 si las credenciales no son válidas.
  loginOwner(request: LoginRequest): Observable<AuthUser> {
    return this.http
      .post<AuthUser>(`${this.apiUrl}/auth/owners/login`, request)
      .pipe(tap((user) => this.setUser(user)));
  }

  // POST /auth/veterinarians/login: 401 si son inválidas, 403 si la cuenta está desactivada.
  loginVeterinarian(request: LoginRequest): Observable<AuthUser> {
    return this.http
      .post<AuthUser>(`${this.apiUrl}/auth/veterinarians/login`, request)
      .pipe(tap((user) => this.setUser(user)));
  }

  // POST /auth/administrators/login: 401 si las credenciales no son válidas.
  loginAdmin(request: LoginRequest): Observable<AuthUser> {
    return this.http
      .post<AuthUser>(`${this.apiUrl}/auth/administrators/login`, request)
      .pipe(tap((user) => this.setUser(user)));
  }

  // Cierra la sesión y borra al usuario guardado.
  logout(): void {
    this._user.set(null);
    try {
      sessionStorage.removeItem(STORAGE_KEY);
    } catch {
      // Si el navegador bloquea el storage, igual queda cerrada en memoria.
    }
  }

  private setUser(user: AuthUser): void {
    this._user.set(user);
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(user));
    } catch {
      // Sin storage la sesión solo vive hasta recargar la página.
    }
  }

  // Recupera al usuario si ya había iniciado sesión antes de recargar.
  private readStoredUser(): AuthUser | null {
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as AuthUser) : null;
    } catch {
      return null;
    }
  }
}
