// Configuración del entorno de producción. Angular la usa en lugar de
// environment.ts cuando se compila con la configuración "production".
// apiBaseUrl debe coincidir con la URL pública del servicio del backend en Render,
// seguida de /api.
export const environment = {
  apiBaseUrl: 'https://pethaven-backend-bl6h.onrender.com/api',
} as const;
