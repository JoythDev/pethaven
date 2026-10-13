// Cuerpo de error estándar que devuelve el API (ApiErrorResponse del backend).
// "errors" trae un mensaje por campo cuando la falla es de validación (400).
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  errors: Record<string, string>;
}
