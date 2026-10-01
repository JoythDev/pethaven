import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { providePrimeNG } from 'primeng/config';

import { routes } from './app.routes';
import { PetHavenPreset } from './theme/pethaven-preset';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: PetHavenPreset,
        options: {
          cssLayer: { name: 'primeng', order: 'theme, base, primeng, utilities' },
          darkModeSelector: '.app-dark'
        }
      }
    })
  ]
};
