import { Signal, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  LucideBell,
  LucideCalendar,
  LucideClipboardCheck,
  LucideFileUp,
  LucideIcon,
  LucidePackageMinus,
  LucidePackageX,
  LucidePill,
  LucideSyringe,
  LucideTrendingDown,
  LucideTrendingUp,
  LucideTriangleAlert,
  LucideUserX,
} from '@lucide/angular';
import { map, timer } from 'rxjs';

import { DEMO_NOW } from '../../../services/demo/demo-latency';

// Los iconos que usan los generadores de notificaciones (core/demo/notifications.ts), por nombre.
const ICONS: Record<string, LucideIcon> = {
  calendar: LucideCalendar,
  pill: LucidePill,
  syringe: LucideSyringe,
  'triangle-alert': LucideTriangleAlert,
  'package-x': LucidePackageX,
  'package-minus': LucidePackageMinus,
  'clipboard-check': LucideClipboardCheck,
  'trending-up': LucideTrendingUp,
  'trending-down': LucideTrendingDown,
  'user-x': LucideUserX,
  'file-up': LucideFileUp,
};

/** Icono de una notificación; un nombre desconocido cae en la campana. */
export function notificationIcon(name: string): LucideIcon {
  return ICONS[name] ?? LucideBell;
}

/** Reloj que avanza cada minuto para que «Hace 20 minutos» no se quede congelado. */
export function injectNotificationClock(): Signal<Date> {
  const now = inject(DEMO_NOW);
  return toSignal(timer(60_000, 60_000).pipe(map(() => now())), { initialValue: now() });
}
