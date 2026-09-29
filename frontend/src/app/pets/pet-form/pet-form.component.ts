import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule, NgForm } from '@angular/forms';
import { PetService } from '../pet.service';
import { Pet, Species } from '../pet.model';

@Component({
  selector: 'app-pet-form',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './pet-form.component.html'
})
export class PetFormComponent implements OnInit {

  Species = Species; // para poder usar el enum directo en el HTML

  // Los datos del formulario. Si estamos editando, se rellenan solos
  // con los datos de la mascota existente (ver ngOnInit)
  pet: Omit<Pet, 'id'> = {
    name: '',
    species: Species.DOG,
    breed: '',
    age: 0,
    weight: 0,
    disease: null,
    photoUrl: '',
    ownerId: 1,
    active: true
  };

  // Si esto tiene un valor, estamos editando esa mascota.
  // Si es null, estamos creando una nueva.
  editandoId: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private petService: PetService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editandoId = Number(idParam);
      const existente = this.petService.getById(this.editandoId);
      if (existente) {
        const { id, ...resto } = existente;
        this.pet = resto;
      }
    }
  }

  guardar(form: NgForm): void {
    if (form.invalid) {
      return;
    }

    if (this.editandoId !== null) {
      this.petService.update(this.editandoId, this.pet);
      this.router.navigate(['/pets', this.editandoId]);
    } else {
      const nuevaMascota = this.petService.add(this.pet);
      this.router.navigate(['/pets', nuevaMascota.id]);
    }
  }
}
