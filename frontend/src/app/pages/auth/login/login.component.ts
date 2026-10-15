import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { applyServerErrors, getErrorMessage } from '../../../core/http/http-error';
import { AuthService } from '../../../services/auth.service';

type LoginKind = 'owner' | 'vet';

// Textos, imagen y destino de cada portal. El mismo componente sirve para los dos.
interface LoginConfig {
  title: string;
  subtitle: string;
  image: string;
  icon: string;
  successRoute: string;
  otherLabel: string;
  otherLink: string;
  otherCta: string;
}

const CONFIGS: Record<LoginKind, LoginConfig> = {
  owner: {
    title: 'Bienvenido de nuevo',
    subtitle: 'Ingresa para ver el estado y el historial de tus mascotas.',
    image: '/images/instalacion_recepcion.jpg',
    icon: '/images/icons/paw-print-icon.svg',
    successRoute: '/cliente',
    otherLabel: '¿Eres veterinario?',
    otherLink: '/vet/login',
    otherCta: 'Ingresa aquí',
  },
  vet: {
    title: 'Portal Veterinario',
    subtitle: 'Accede a tus pacientes, tratamientos y reportes clínicos.',
    image: '/images/instalacion_quirofano.jpg',
    icon: '/images/icons/stethoscope-icon.svg',
    successRoute: '/veterinario',
    otherLabel: '¿Eres dueño de una mascota?',
    otherLink: '/login',
    otherCta: 'Inicia sesión aquí',
  },
};

// Pantalla de inicio de sesión. Qué portal es (dueño o veterinario)
// se decide con data.kind en app.routes.ts.
@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  private readonly kind: LoginKind =
    inject(ActivatedRoute).snapshot.data['kind'] === 'vet' ? 'vet' : 'owner';

  readonly config = CONFIGS[this.kind];

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
    password: ['', [Validators.required]],
  });

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly showPassword = signal(false);

  togglePassword(): void {
    this.showPassword.update((visible) => !visible);
  }

  submit(): void {
    this.errorMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    const credentials = this.form.getRawValue();
    const request$ =
      this.kind === 'vet'
        ? this.auth.loginVeterinarian(credentials)
        : this.auth.loginOwner(credentials);

    request$.subscribe({
      next: () => {
        this.submitting.set(false);
        this.router.navigateByUrl(this.config.successRoute);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        // Si el API marcó campos concretos (400) se muestran en el campo;
        // si no, se muestra el mensaje general (401, 403, sin conexión...).
        if (!applyServerErrors(this.form, error)) {
          this.errorMessage.set(getErrorMessage(error, 'No se pudo iniciar sesión.'));
        }
      },
    });
  }
}
