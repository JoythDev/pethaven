// Configuración del entorno de desarrollo. El build de producción la reemplaza
// por environment.production.ts (ver fileReplacements en angular.json).
export const environment = {
  apiBaseUrl: 'http://localhost:8080/api',
} as const;
