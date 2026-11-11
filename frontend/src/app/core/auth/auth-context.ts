import { HttpContextToken } from '@angular/common/http';

// Marca una petición para que un 401 no dispare el cierre de sesión con redirección
// (la usa la restauración de la sesión al arrancar, que decide por su cuenta).
export const SKIP_SESSION_EXPIRED_REDIRECT = new HttpContextToken<boolean>(() => false);

// Petición sin sesión (el login): no lleva el token guardado y su 401 nunca cierra la sesión vigente.
export const ANONYMOUS_REQUEST = new HttpContextToken<boolean>(() => false);
