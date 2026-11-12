// Configuración del entorno de producción. Angular la usa en lugar de
// environment.ts cuando se compila con la configuración "production".
// apiBaseUrl debe coincidir con la URL pública del backend en Render (el nombre
// del servicio es el subdominio) seguida de /api.
export const environment = {
  apiBaseUrl: 'https://pethaven-backend.onrender.com/api',
} as const;
