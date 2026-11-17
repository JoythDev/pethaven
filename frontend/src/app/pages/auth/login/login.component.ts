import { DOCUMENT } from '@angular/common';
import { Component, DestroyRef, ElementRef, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  LucideArrowLeft,
  LucideClock,
  LucideEye,
  LucideEyeOff,
  LucideHeart,
  LucideInfo,
  LucidePawPrint,
  LucideShield,
  LucideTriangleAlert,
  provideLucideConfig,
} from '@lucide/angular';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { applyServerErrors, getErrorMessage } from '../../../core/http/http-error';
import { AuthUser } from '../../../models/auth.model';
import { AuthService } from '../../../services/auth.service';
import { AuthArtComponent } from '../auth-art/auth-art.component';

// Ancho desde el que se muestra el bloque ilustrado; por debajo, solo el formulario (9.11).
const ART_MIN_WIDTH = '(min-width: 960px)';

// Pantalla de inicio de sesión única: al entrar, cada usuario va al portal de su rol.
@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    InputTextModule,
    TooltipModule,
    AuthArtComponent,
    LucideArrowLeft,
    LucideClock,
    LucideEye,
    LucideEyeOff,
    LucideHeart,
    LucideInfo,
    LucidePawPrint,
    LucideShield,
    LucideTriangleAlert,
  ],
  providers: [provideLucideConfig({ size: 18, strokeWidth: 1.8 })],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly document = inject(DOCUMENT);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly destroyRef = inject(DestroyRef);

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
  // El bloque ilustrado solo existe mientras cabe: así no corre su animación oculto.
  readonly showArt = signal(false);

  ngOnInit(): void {
    // Scrollbars propios mientras haya acceso en pantalla (4.5).
    this.document.documentElement.classList.add('ph-scrollbars');

    const query = this.document.defaultView?.matchMedia(ART_MIN_WIDTH);
    if (query) {
      this.showArt.set(query.matches);
      const onChange = (event: MediaQueryListEvent) => this.showArt.set(event.matches);
      query.addEventListener('change', onChange);
      this.destroyRef.onDestroy(() => query.removeEventListener('change', onChange));
    }
  }

  ngOnDestroy(): void {
    this.document.documentElement.classList.remove('ph-scrollbars');
  }

  togglePassword(): void {
    this.showPassword.update((visible) => !visible);
  }

  submit(): void {
    this.errorMessage.set(null);
    this.noticeMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.host.nativeElement.querySelector<HTMLInputElement>('input.ng-invalid')?.focus();
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
