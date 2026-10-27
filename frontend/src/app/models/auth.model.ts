// Roles que devuelve el API de login (AuthRole del backend).
export type AuthRole = 'OWNER' | 'VETERINARIAN' | 'ADMIN';

// Cuerpo que se envía a los logins (LoginRequest).
export interface LoginRequest {
  email: string;
  password: string;
}

// Usuario autenticado tal como lo devuelve el API (AuthResponse).
// Nunca trae la contraseña, y por ahora tampoco hay token.
export interface AuthUser {
  id: number;
  name: string;
  email: string;
  role: AuthRole;
}
