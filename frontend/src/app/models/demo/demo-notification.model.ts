// Notificaciones (demo, S14): se generan con datos reales y demo; funcionalidad pendiente de backend.
export type NotificationGroup = 'Hoy' | 'Esta semana';

export interface DemoNotification {
  id: string;
  // Nombre de un icono de Lucide, por ejemplo 'calendar'.
  icon: string;
  title: string;
  detail: string;
  at: Date;
  group: NotificationGroup;
  unread: boolean;
  // Ruta que abre la notificación; null si no lleva a ninguna parte.
  route: string | null;
}
