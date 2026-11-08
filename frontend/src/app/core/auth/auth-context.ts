import { HttpContextToken } from '@angular/common/http';

// Marca una petición para que un 401 no dispare el cierre de sesión con redirección
// (la usa la restauración de la sesión al arrancar, que decide por su cuenta).
export const SKIP_SESSION_EXPIRED_REDIRECT = new HttpContextToken<boolean>(() => false);
