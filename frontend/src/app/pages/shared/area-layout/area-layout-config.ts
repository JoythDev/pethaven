import { MenuItem } from 'primeng/api';
import { AreaNavItem } from './area-nav-item';

/** Navegación lateral de un área. */
export interface AreaSidebarConfig {
  items: AreaNavItem[];
  homeLink: string;
  sectionLabel: string;
}

/** Barra superior de un área. */
export interface AreaTopbarConfig {
  homeItem: MenuItem;
  pageLabels: Record<string, string>;
  subPageLabels: Record<string, Record<string, string>>;
  userName: string;
  userInitial: string;
  userMenuItems: MenuItem[];
  /** Si es true, el breadcrumb ignora un segmento numérico (ids como /cliente/7/...). */
  skipNumericSegment: boolean;
}

/** Configuración visible del shell de un área. */
export interface AreaLayoutConfig {
  sidebar: AreaSidebarConfig;
  topbar: AreaTopbarConfig;
}

/** Ítems por defecto: panel del veterinario. */
export const DEFAULT_NAV_ITEMS: AreaNavItem[] = [
  { label: 'Mascotas', icon: '/images/icons/paw-print-icon.svg', link: '/veterinario/pets' },
  { label: 'Dueños', icon: '/images/icons/user-icon.svg', link: '/veterinario/owners' },
  { label: 'Veterinarios', icon: '/images/icons/stethoscope-icon.svg', disabled: true },
  { label: 'Medicamentos', icon: '/images/icons/pulse-icon.svg', disabled: true },
  { label: 'Tratamientos', icon: '/images/icons/hospital-bed-icon.svg', disabled: true },
];

export const DEFAULT_HOME_ITEM: MenuItem = { icon: 'pi pi-home', routerLink: '/veterinario' };

export const DEFAULT_PAGE_LABELS: Record<string, string> = {
  pets: 'Mascotas',
  owners: 'Dueños',
};

export const DEFAULT_SUB_PAGE_LABELS: Record<string, Record<string, string>> = {
  pets: { add: 'Nueva mascota', update: 'Editar mascota' },
  owners: { add: 'Nuevo dueño', update: 'Editar dueño' },
};

export const DEFAULT_USER_MENU_ITEMS: MenuItem[] = [
  { label: 'Perfil', icon: 'pi pi-user', disabled: true },
  { label: 'Cerrar sesión', icon: 'pi pi-sign-out', disabled: true },
];

/** Config por defecto del shell: panel del veterinario. */
export const DEFAULT_AREA_LAYOUT_CONFIG: AreaLayoutConfig = {
  sidebar: {
    items: DEFAULT_NAV_ITEMS,
    homeLink: '/veterinario',
    sectionLabel: 'Panel',
  },
  topbar: {
    homeItem: DEFAULT_HOME_ITEM,
    pageLabels: DEFAULT_PAGE_LABELS,
    subPageLabels: DEFAULT_SUB_PAGE_LABELS,
    userName: 'Admin',
    userInitial: 'A',
    userMenuItems: DEFAULT_USER_MENU_ITEMS,
    skipNumericSegment: false,
  },
};
