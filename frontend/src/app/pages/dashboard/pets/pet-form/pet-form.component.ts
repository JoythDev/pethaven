import { Component, OnInit, computed, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { Pet, Species } from '../../../../models/pet.model';
import { OwnerService } from '../../../../services/owner.service';
import { PetService } from '../../../../services/pet.service';

interface SpeciesOption {
  label: string;
  value: Species;
}

@Component({
  selector: 'app-pets-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    InputTextModule,
    InputNumberModule,
    SelectModule,
    ToggleSwitchModule,
    MessageModule,
  ],
  templateUrl: './pet-form.component.html',
})
export class PetsFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly petService = inject(PetService);
  private readonly ownerService = inject(OwnerService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly messageService = inject(MessageService);

  readonly speciesOptions: SpeciesOption[] = [
    { label: 'Perro', value: Species.DOG },
    { label: 'Gato', value: Species.CAT },
  ];

  readonly ownerOptions = computed(() =>
    this.ownerService.ownersList().map((owner) => ({
      label: `${owner.name} · ${owner.document}`,
      value: owner.id,
    }))
  );

  // Si tiene valor, estamos editando esa mascota; si es null, creamos una nueva.
  editandoId: number | null = null;

  readonly form = this.formBuilder.group({
    name: ['', Validators.required],
    species: [Species.DOG, Validators.required],
    breed: ['', Validators.required],
    age: [0, [Validators.required, Validators.min(0)]],
    weight: [0, [Validators.required, Validators.min(0)]],
    disease: null as string | null,
    photoUrl: ['', [Validators.required, Validators.pattern(/^https?:\/\/.+/)]],
    ownerId: [null as number | null, Validators.required],
    active: [true],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editandoId = Number(idParam);
      const existente = this.petService.getById(this.editandoId);
      if (existente) {
        const { id, ...resto } = existente;
        this.form.patchValue(resto);
      } else {
        this.messageService.add({
          severity: 'error',
          summary: 'Mascota no encontrada',
          detail: 'La mascota que intentas editar no existe.',
        });
        this.router.navigate(['/dashboard/pets']);
      }
    }
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const pet = this.form.getRawValue() as Omit<Pet, 'id'>;

    if (this.editandoId !== null) {
      this.petService.update(this.editandoId, pet);
      this.messageService.add({
        severity: 'success',
        summary: 'Mascota actualizada',
        detail: `${pet.name} se actualizó correctamente.`,
      });
      this.router.navigate(['/dashboard/pets', this.editandoId]);
    } else {
      const nueva = this.petService.add(pet);
      this.messageService.add({
        severity: 'success',
        summary: 'Mascota registrada',
        detail: `${nueva.name} se registró correctamente.`,
      });
      this.router.navigate(['/dashboard/pets', nueva.id]);
    }
  }

  cancelar(): void {
    this.router.navigate(['/dashboard/pets']);
  }
}
