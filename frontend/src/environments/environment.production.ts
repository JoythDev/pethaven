// Configuración del entorno de producción. Angular la usa en lugar de
// environment.ts cuando se compila con la configuración "production".
export const environment = {
  apiBaseUrl: 'http://localhost:8080/api',
} as const;
