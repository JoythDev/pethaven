import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PetService } from '../../../services/pet.service';
import { Pet, Species } from '../../../models/pet.model';

@Component({
  selector: 'app-pet-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './pet-form.component.html'
})
export class PetFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly petService = inject(PetService);
  private readonly formBuilder = inject(FormBuilder);

  readonly Species = Species; // para poder usar el enum directo en el HTML

  // Si esto tiene un valor, estamos editando esa mascota.
  // Si es null, estamos creando una nueva. El mismo formulario sirve
  // para los dos casos: solo cambia si el :id llegó o no por la URL.
  editandoId: number | null = null;

  readonly form = this.formBuilder.group({
    name: ['', Validators.required],
    species: [Species.DOG, Validators.required],
    breed: ['', Validators.required],
    age: [0, [Validators.required, Validators.min(0)]],
    weight: [0, [Validators.required, Validators.min(0)]],
    disease: null as string | null,
    photoUrl: ['', [Validators.required, Validators.pattern(/^https?:\/\/.+/)]],
    ownerId: [1, Validators.required],
    active: [true]
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editandoId = Number(idParam);
      const existente = this.petService.getById(this.editandoId);
      if (existente) {
        const { id, ...resto } = existente;
        this.form.patchValue(resto);
      }
    }
  }

  guardar(): void {
    if (this.form.invalid) {
      return;
    }

    const pet = this.form.getRawValue() as Omit<Pet, 'id'>;

    if (this.editandoId !== null) {
      this.petService.update(this.editandoId, pet);
      this.router.navigate(['/pets', this.editandoId]);
    } else {
      const nuevaMascota = this.petService.add(pet);
      this.router.navigate(['/pets', nuevaMascota.id]);
    }
  }
}
