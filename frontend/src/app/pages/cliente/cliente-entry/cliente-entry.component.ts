import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

// Puente entre el login y el dashboard del dueño: el login navega a '/cliente' sin id
// y aquí se resuelve el id del usuario autenticado para entrar a sus mascotas.
// Sin sesión (o con rol de veterinario) vuelve al login.
@Component({
  selector: 'app-cliente-entry',
  template: '',
})
export class ClienteEntryComponent {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  constructor() {
    const user = this.auth.user();

    if (user && user.role === 'OWNER') {
      void this.router.navigate(['/cliente', user.id, 'mascotas']);
      return;
    }

    void this.router.navigate(['/login']);
  }
}
