import { Component, inject } from '@angular/core';
import { InputTextModule } from 'primeng/inputtext';

import { AuthService } from '../../../services/auth.service';
import { CardComponent } from '../../shared/card/card.component';
import { FormFieldComponent } from '../../shared/form-field/form-field.component';
import { ProfileAppearanceCardComponent } from '../../shared/profile/profile-appearance-card.component';
import { ProfileHeaderComponent } from '../../shared/profile/profile-header.component';
import { ProfileLayoutComponent } from '../../shared/profile/profile-layout.component';
import { ProfileSecurityCardComponent } from '../../shared/profile/profile-security-card.component';

/**
 * Perfil del administrador: solo lectura. El API solo ofrece GET /auth/me para esta cuenta, así que
 * nombre y correo vienen de `AuthService.user()` y no se editan; «Apariencia» y «Sesión» sí funcionan.
 * La tarjeta de seguridad no recibe `savePassword`, por eso no ofrece cambiar la contraseña.
 */
@Component({
  selector: 'app-administrador-perfil',
  imports: [
    InputTextModule,
    CardComponent,
    FormFieldComponent,
    ProfileAppearanceCardComponent,
    ProfileHeaderComponent,
    ProfileLayoutComponent,
    ProfileSecurityCardComponent,
  ],
  templateUrl: './perfil.component.html',
})
export class AdministradorPerfilComponent {
  protected readonly user = inject(AuthService).user;
}
