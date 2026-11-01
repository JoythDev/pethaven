import { Injectable, inject } from '@angular/core';
import { MessageService } from 'primeng/api';
import { getErrorMessage } from '../http/http-error';

// Centraliza los toasts para que la severidad y el mensaje de error se armen igual en todas las vistas.
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly messageService = inject(MessageService);

  success(summary: string, detail: string): void {
    this.messageService.add({ severity: 'success', summary, detail });
  }

  warn(summary: string, detail: string): void {
    this.messageService.add({ severity: 'warn', summary, detail });
  }

  error(summary: string, error: unknown): void {
    this.messageService.add({ severity: 'error', summary, detail: getErrorMessage(error) });
  }
}
