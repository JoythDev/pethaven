import { DOCUMENT } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, ElementRef, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  LucideArrowLeft,
  LucideClock,
  LucideEye,
  LucideEyeOff,
  LucideHeart,
  LucideInfo,
  LucideShield,
  LucideTriangleAlert,
  provideLucideConfig,
} from '@lucide/angular';
import { interval } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { applyServerErrors, getErrorMessage } from '../../../core/http/http-error';
import { AuthUser, OtpChallenge } from '../../../models/auth.model';
import { AuthService } from '../../../services/auth.service';
import { AuthArtComponent } from '../auth-art/auth-art.component';
import { BrandLogoComponent } from '../../shared/brand-logo/brand-logo.component';

// Ancho desde el que se muestra el bloque ilustrado; por debajo, solo el formulario (9.11).
const ART_MIN_WIDTH = '(min-width: 960px)';

// El API responde 401 con este inicio de mensaje cuando el código no coincide pero quedan intentos: en ese
// caso se queda en el segundo paso; cualquier otro rechazo (venció, intentos agotados) obliga a empezar de nuevo.
const WRONG_CODE_PREFIX = 'El código es incorrecto';

// Pantalla de inicio de sesión única, en dos pasos: contraseña y luego el código que llega al correo.
// Al entrar, cada usuario va al portal de su rol.
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
    BrandLogoComponent,
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

  // Segundo paso: desafío abierto tras la contraseña (null mientras se piden las credenciales).
  readonly challenge = signal<OtpChallenge | null>(null);
  readonly codeForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });
  // Instante (ms) desde el que se puede pedir otro código; con `now` da la cuenta regresiva del reenvío.
  private readonly resendAt = signal(0);
  private readonly now = signal(Date.now());
  readonly resendSeconds = computed(() => Math.max(0, Math.ceil((this.resendAt() - this.now()) / 1000)));
  readonly resending = signal(false);

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

    // Reloj de la cuenta regresiva del reenvío; se libera con el componente.
    interval(1000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.now.set(Date.now()));

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
      next: (challenge) => {
        this.submitting.set(false);
        this.openChallenge(challenge);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        // Si el API marcó campos concretos (400) se muestran en el campo;
        // si no, se muestra el mensaje general (401, 403, 503, sin conexión...).
        if (!applyServerErrors(this.form, error)) {
          this.errorMessage.set(getErrorMessage(error, 'No se pudo iniciar sesión.'));
        }
      },
    });
  }

  // Segundo paso: envía el código recibido por correo. Con el código correcto abre la sesión.
  verifyCode(): void {
    const challenge = this.challenge();
    this.errorMessage.set(null);

    if (!challenge || this.submitting()) {
      return;
    }
    if (this.codeForm.invalid) {
      this.codeForm.markAllAsTouched();
      this.host.nativeElement.querySelector<HTMLInputElement>('#otpCode')?.focus();
      return;
    }

    this.submitting.set(true);
    this.auth.verifyOtp({ challengeId: challenge.challengeId, code: this.codeForm.getRawValue().code }).subscribe({
      next: (user) => {
        this.submitting.set(false);
        void this.router.navigateByUrl(this.destinationFor(user));
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        const message = getErrorMessage(error, 'No se pudo verificar el código.');
        if (this.canRetryCode(error, message)) {
          this.codeForm.reset({ code: '' });
          this.errorMessage.set(message);
          this.host.nativeElement.querySelector<HTMLInputElement>('#otpCode')?.focus();
        } else {
          this.backToCredentials(message);
        }
      },
    });
  }

  // Pide un código nuevo para el mismo desafío. Un desafío vencido (401) obliga a empezar de nuevo; la espera
  // o el tope de reenvíos (429) y un fallo del correo (503) dejan al usuario en el mismo paso.
  resendCode(): void {
    const challenge = this.challenge();
    if (!challenge || this.resending() || this.resendSeconds() > 0) {
      return;
    }

    this.errorMessage.set(null);
    this.resending.set(true);
    this.auth.resendOtp(challenge.challengeId).subscribe({
      next: (renewed) => {
        this.resending.set(false);
        this.openChallenge(renewed);
      },
      error: (error: unknown) => {
        this.resending.set(false);
        const message = getErrorMessage(error, 'No se pudo reenviar el código.');
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.backToCredentials(message);
        } else {
          this.errorMessage.set(message);
        }
      },
    });
  }

  // Vuelve al primer paso (por ejemplo, para usar otra cuenta).
  backToCredentials(message: string | null = null): void {
    this.challenge.set(null);
    this.codeForm.reset({ code: '' });
    this.form.controls.password.reset('');
    this.showPassword.set(false);
    this.errorMessage.set(message);
  }

  // Solo se admiten dígitos en el campo del código.
  onCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const digits = input.value.replace(/\D/g, '').slice(0, 6);
    if (digits !== input.value) {
      input.value = digits;
    }
    this.codeForm.controls.code.setValue(digits);
  }

  // Muestra el segundo paso y arranca la cuenta regresiva del reenvío.
  private openChallenge(challenge: OtpChallenge): void {
    this.challenge.set(challenge);
    this.codeForm.reset({ code: '' });
    this.now.set(Date.now());
    this.resendAt.set(Date.now() + challenge.resendAvailableIn * 1000);
    setTimeout(() => this.host.nativeElement.querySelector<HTMLInputElement>('#otpCode')?.focus());
  }

  private canRetryCode(error: unknown, message: string): boolean {
    return error instanceof HttpErrorResponse && error.status === 401 && message.startsWith(WRONG_CODE_PREFIX);
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
