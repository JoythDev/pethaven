export type Theme = 'light' | 'dark';

// Misma clase que darkModeSelector (app.config.ts) y que la variante dark de Tailwind (styles.css).
export const DARK_CLASS = 'app-dark';

// El script de index.html lee esta misma clave antes del primer pintado.
export const THEME_STORAGE_KEY = 'pethaven.theme';

export function parseStoredTheme(value: string | null): Theme | null {
  return value === 'light' || value === 'dark' ? value : null;
}

// La elección guardada manda; sin ella se sigue la preferencia del sistema.
export function resolveTheme(stored: Theme | null, systemPrefersDark: boolean): Theme {
  return stored ?? (systemPrefersDark ? 'dark' : 'light');
}
