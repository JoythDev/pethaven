import { InjectionToken } from '@angular/core';
import { environment } from '../../../environments/environment';

// Token con la URL base del API de Spring Boot. Los servicios lo inyectan
// en vez de escribir la URL a mano; el valor sale de environment para que
// producción pueda apuntar a otro host sin tocar el código.
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => environment.apiBaseUrl,
});
