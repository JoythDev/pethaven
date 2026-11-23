import { InjectionToken } from '@angular/core';

// Reloj de la aplicación. La lógica pura recibe la fecha; las páginas la leen de aquí.
export const CLOCK = new InjectionToken<() => Date>('CLOCK', {
  providedIn: 'root',
  factory: () => () => new Date(),
});
