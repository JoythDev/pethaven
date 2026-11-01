import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { of } from 'rxjs';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { Veterinarian } from '../../../models/veterinarian.model';
import { AuthService } from '../../../services/auth.service';
import { VeterinarianService } from '../../../services/veterinarian.service';

@Component({
  selector: 'app-veterinario-perfil',
  imports: [RouterLink, AvatarModule, ButtonModule, SkeletonModule, TagModule],
  templateUrl: './perfil.component.html',
})
export class VeterinarioPerfilComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly veterinarianService = inject(VeterinarianService);
  private readonly destroyRef = inject(DestroyRef);

  readonly state = signal<ViewState<Veterinarian>>({ status: 'loading' });

  // Pide al API los datos del veterinario que inició sesión. Sin sesión de veterinario
  // termina en error sin llamar al API. El primer HTTP sale de ngOnInit, no del constructor.
  ngOnInit(): void {
    const user = this.auth.user();

    const profile$ =
      user?.role === 'VETERINARIAN'
        ? this.veterinarianService.getById(user.id).pipe(toViewState<Veterinarian>())
        : of<ViewState<Veterinarian>>({
            status: 'error',
            message: 'Inicia sesión como veterinario para ver tu perfil.',
          });

    profile$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((state) => this.state.set(state));
  }

  // Borra la sesión guardada y vuelve al login del veterinario.
  cerrarSesion(): void {
    this.auth.logout();
    this.router.navigate(['/vet/login']);
  }
}
