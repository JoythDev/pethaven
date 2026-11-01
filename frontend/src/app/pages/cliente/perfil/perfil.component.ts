import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { Subject, of, switchMap } from 'rxjs';
import { parseRouteId } from '../../../core/http/route-id';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { Owner } from '../../../models/owner.model';
import { OwnerService } from '../../../services/owner.service';
import { ClienteContextStore } from '../cliente-context.store';

@Component({
  selector: 'app-perfil',
  imports: [AvatarModule, ButtonModule, SkeletonModule],
  templateUrl: './perfil.component.html',
})
export class PerfilComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ownerService = inject(OwnerService);
  private readonly context = inject(ClienteContextStore);
  private readonly destroyRef = inject(DestroyRef);

  // :ownerId de la ruta padre; null mientras llega o si el id es inválido.
  private readonly ownerId = signal<number | null>(null);

  // Cada emisión vuelve a pedir el perfil (carga inicial y recargas).
  // El primer HTTP sale de ngOnInit, no del constructor.
  private readonly refresh$ = new Subject<void>();

  // Estado del perfil. La página es dueña de su propio estado (cargando, listo o error);
  // el store del contexto sigue siendo la caché que alimenta la topbar. Un id inválido
  // termina en error sin llamar al API.
  readonly state = toSignal(
    this.refresh$.pipe(
      switchMap(() => {
        const ownerId = this.ownerId();
        return ownerId === null
          ? of<ViewState<Owner>>({ status: 'error', message: 'Dueño no encontrado.' })
          : this.ownerService.getById(ownerId).pipe(toViewState<Owner>());
      }),
    ),
    { initialValue: { status: 'loading' } as ViewState<Owner> },
  );

  ngOnInit(): void {
    // El :ownerId pertenece a la ruta padre: con la estrategia de herencia de params por
    // defecto ("emptyOnly") esta ruta hija no lo hereda, así que se lee del padre.
    // El param puede cambiar sin recrear la página (misma área, otro dueño).
    this.route.parent!.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const ownerId = parseRouteId(params.get('ownerId'));
      this.ownerId.set(ownerId);
      if (ownerId !== null) {
        // Mantiene el nombre de la topbar sincronizado; el store cachea por id.
        this.context.load(ownerId);
      }
      this.refresh$.next();
    });
  }

  // La sesión del cliente no tiene autenticación real: basta con volver al login.
  cerrarSesion(): void {
    this.router.navigate(['/login']);
  }
}
