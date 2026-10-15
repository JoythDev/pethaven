import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { map, of, switchMap } from 'rxjs';
import { Pet } from '../../../../models/pet.model';
import { PetService } from '../../../../services/pet.service';
import { parseRouteId } from '../../../../core/http/route-id';
import { ViewState, toViewState } from '../../../../core/http/view-state';
import { PetStatusTagComponent } from '../components/pet-status-tag/pet-status-tag.component';

@Component({
  selector: 'app-pets-detail',
  imports: [RouterLink, ButtonModule, TagModule, PetStatusTagComponent],
  templateUrl: './pet-detail.component.html',
})
export class PetsDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly petService = inject(PetService);
  private readonly destroyRef = inject(DestroyRef);

  readonly state = signal<ViewState<Pet>>({ status: 'loading' });

  // Lee el :id de la URL y llama a PetService.getById. Un id inválido termina en error sin
  // llamar al API. switchMap descarta la consulta anterior si el id cambia antes de que
  // responda, y toViewState emite "cargando" al empezar cada consulta, luego "listo" o "error".
  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        map((params) => parseRouteId(params.get('id'))),
        switchMap((id) =>
          id === null
            ? of<ViewState<Pet>>({ status: 'error', message: 'La mascota solicitada no existe.' })
            : this.petService.getById(id).pipe(toViewState<Pet>()),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((state) => this.state.set(state));
  }

  editar(id: number): void {
    this.router.navigate(['/veterinario/pets/update', id]);
  }

  volver(): void {
    this.router.navigate(['/veterinario/pets']);
  }
}
