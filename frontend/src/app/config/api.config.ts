import { InjectionToken } from '@angular/core';

// Token con la URL base del API de Spring Boot. Los servicios lo inyectan
// en vez de escribir la URL a mano, así se puede cambiar en un solo lugar.
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => 'http://localhost:8080/api',
});
