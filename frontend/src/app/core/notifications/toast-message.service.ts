import { Injectable } from '@angular/core';
import { MessageService, ToastMessageOptions } from 'primeng/api';

/** Los avisos que confirman una acción se cierran solos a los 4 s (10.x). */
export const TOAST_LIFE_MS = 4000;

/** Aplica la política de avisos: éxito, advertencia e info duran 4 s; los errores esperan a que se cierren. */
export function toastMessage(message: ToastMessageOptions): ToastMessageOptions {
  if (message.severity === 'error') {
    return { closable: true, ...message, sticky: message.sticky ?? true };
  }
  return { life: TOAST_LIFE_MS, ...message, sticky: message.sticky ?? false };
}

/**
 * `MessageService` con la regla «un aviso a la vez»: cada aviso nuevo limpia el actual antes de entrar y
 * recibe la vida y la persistencia que fija `toastMessage`. Sustituye al servicio de PrimeNG en toda la
 * app, así que también cubre a quien llama a `MessageService.add` directamente.
 */
@Injectable()
export class SingleToastMessageService extends MessageService {
  override add(message: ToastMessageOptions): void {
    this.clear();
    super.add(toastMessage(message));
  }

  // Varios avisos a la vez rompen la regla: solo se muestra el último.
  override addAll(messages: ToastMessageOptions[]): void {
    const last = messages[messages.length - 1];
    if (last) {
      this.add(last);
    }
  }
}
