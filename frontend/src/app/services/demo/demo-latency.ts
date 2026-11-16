import { InjectionToken } from '@angular/core';
import { MonoTypeOperatorFunction, delay } from 'rxjs';

// Espera simulada de los servicios demo, para que las pantallas muestren sus esqueletos.
export const DEMO_LATENCY_MS = 350;

export function demoLatency<T>(): MonoTypeOperatorFunction<T> {
  return delay(DEMO_LATENCY_MS);
}

// Reloj de los servicios demo: los generadores puros reciben la fecha y nunca leen el reloj.
export const DEMO_NOW = new InjectionToken<() => Date>('DEMO_NOW', {
  providedIn: 'root',
  factory: () => () => new Date(),
});
