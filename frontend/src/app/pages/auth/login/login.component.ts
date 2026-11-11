import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { applyServerErrors, getErrorMessage } from '../../../core/http/http-error';
import { AuthUser } from '../../../models/auth.model';
import { AuthService } from '../../../services/auth.service';

// Pantalla de inicio de sesión única: al entrar, cada usuario va al portal de su rol.
@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  private readonly route = inject(ActivatedRoute);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
    password: ['', [Validators.required]],
  });

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly noticeMessage = signal<string | null>(
    this.route.snapshot.queryParamMap.get('expired') ? 'Tu sesión expiró. Inicia sesión de nuevo.' : null
  );
  readonly showPassword = signal(false);

  togglePassword(): void {
    this.showPassword.update((visible) => !visible);
  }

  submit(): void {
    this.errorMessage.set(null);
    this.noticeMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    const credentials = this.form.getRawValue();
    this.auth.login(credentials).subscribe({
      next: (user) => {
        this.submitting.set(false);
        void this.router.navigateByUrl(this.destinationFor(user));
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

  // Vuelve a la ruta pedida antes del login (solo rutas internas, para evitar redirecciones abiertas);
  // si no hay, al inicio del portal de su rol. Si la ruta es de otro portal, el guard lo redirige.
  private destinationFor(user: AuthUser): string {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    // Ruta interna: empieza por una sola "/" (no "//" ni "/\", que el navegador trata como otro host).
    if (returnUrl && /^\/(?![\/\\])/.test(returnUrl)) {
      return returnUrl;
    }
    return this.auth.homeUrl(user);
  }
}
