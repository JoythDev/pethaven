/** Ítem de navegación lateral de un área (veterinario, cliente, ...). */
export interface AreaNavItem {
  label: string;
  /** Ruta de un SVG ('/images/...') o clase de PrimeIcons ('pi pi-x'). */
  icon: string;
  link?: string;
  disabled?: boolean;
  badge?: string;
  /**
   * Acción del ítem cuando no navega (p. ej. 'Cerrar sesión').
   * Mismo patrón que los MenuItem de PrimeNG.
   */
  command?: () => void;
}
