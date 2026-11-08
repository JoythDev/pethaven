// Roles que devuelve el API de autenticación.
export type AuthRole = 'OWNER' | 'VETERINARIAN' | 'ADMIN';

// Cuerpo de POST /auth/login (LoginRequest del backend).
export interface LoginRequest {
  email: string;
  password: string;
}

// Respuesta de POST /auth/login (AuthResponse del backend).
export interface AuthResponse {
  token: string;
  expiresIn: number; // segundos de vigencia del token
  profileId: number;
  name: string;
  email: string;
  role: AuthRole;
}

// Respuesta de GET /auth/me (AuthProfileResponse del backend).
export interface AuthProfile {
  profileId: number;
  name: string;
  email: string;
  role: AuthRole;
}

// Usuario autenticado tal como lo usa la app. `id` es el id del perfil
// (dueño, veterinario o administrador según `role`). Nunca trae la contraseña.
export interface AuthUser {
  id: number;
  name: string;
  email: string;
  role: AuthRole;
}

// Sesión guardada en localStorage: token, momento de expiración (epoch ms) y usuario.
export interface AuthSession {
  token: string;
  expiresAt: number;
  user: AuthUser;
}
