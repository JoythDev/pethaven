// Roles que devuelve el API de autenticación.
export type AuthRole = 'OWNER' | 'VETERINARIAN' | 'ADMIN';

// Cuerpo de POST /auth/login (LoginRequest del backend).
export interface LoginRequest {
  email: string;
  password: string;
}

// Respuesta de POST /auth/login y de POST /auth/resend-otp (OtpChallengeResponse del backend): la contraseña
// es correcta y se envió un código al correo. Todavía no hay sesión.
export interface OtpChallenge {
  challengeId: string;
  maskedEmail: string; // correo al que se envió el código, parcialmente oculto
  expiresIn: number; // segundos de vigencia del código
  resendAvailableIn: number; // segundos que hay que esperar antes de pedir otro código
}

// Cuerpo de POST /auth/verify-otp (VerifyOtpRequest del backend).
export interface VerifyOtpRequest {
  challengeId: string;
  code: string; // 6 dígitos
}

// Respuesta de POST /auth/verify-otp (AuthResponse del backend).
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
