import { Injectable, signal } from '@angular/core';
import { Pet, Species } from './pet.model';

@Injectable({
  providedIn: 'root'
})
export class PetService {

  // Nuestra "base de datos" por ahora: un arreglo en memoria.
  // Usamos un signal para que cualquier componente que lo lea se
  // actualice solo cuando agreguemos, editemos o borremos algo.
  private pets = signal<Pet[]>([
    { id: 1, name: 'Buddy', species: Species.DOG, breed: 'Golden Retriever', age: 3, weight: 15.0, disease: null, photoUrl: 'https://images.unsplash.com/photo-1633722715463-d30f4f325e24?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 1, active: true },
    { id: 2, name: 'Whiskers', species: Species.CAT, breed: 'Persian', age: 2, weight: 4.5, disease: null, photoUrl: 'https://eu-central-1.graphassets.com/AnwjgMYRvQfWK3bRPjoq3z/resize=height:778,width:1080/output=format:webp/GftmE5Qtm0AtcNcRWRA1', ownerId: 2, active: true },
    { id: 3, name: 'Max', species: Species.DOG, breed: 'German Shepherd', age: 4, weight: 20.0, disease: null, photoUrl: 'https://ask.woodgreen.org.uk/media/pages/images/5979b7d0bc-1727379943/german-shepherd-900x900-crop-52-5-28-8.jpg', ownerId: 1, active: true },
    { id: 4, name: 'Luna', species: Species.CAT, breed: 'Siamese', age: 1, weight: 3.0, disease: null, photoUrl: 'https://assets.elanco.com/8e0bf1c2-1ae4-001f-9257-f2be3c683fb1/fca42f04-2474-4302-a238-990c8aebfe8c/Siamese_cat_1110x740.jpg', ownerId: 3, active: true },
    { id: 5, name: 'Charlie', species: Species.DOG, breed: 'Beagle', age: 5, weight: 10.0, disease: null, photoUrl: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQRWp3zpN9nyTOC-i1UVNYwutRtjTHDpc40wIIE1BSUTn0kMqAk6ztLwffh&s=10', ownerId: 2, active: true },
    { id: 6, name: 'Rocky', species: Species.CAT, breed: 'Sphynx', age: 6, weight: 4.0, disease: null, photoUrl: 'https://images.unsplash.com/photo-1574158622682-e40e69881006?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 1, active: false },
    { id: 7, name: 'Nina', species: Species.CAT, breed: 'Europeo Común', age: 4, weight: 4.2, disease: 'Conjuntivitis', photoUrl: 'https://loremflickr.com/500/400/kitten?lock=2', ownerId: 3, active: true },
    { id: 8, name: 'Bruno', species: Species.DOG, breed: 'Boxer', age: 3, weight: 28.0, disease: 'Displasia de cadera', photoUrl: 'https://placedog.net/500/400?id=6', ownerId: 2, active: true }
  ]);

  // El id que le tocará a la próxima mascota que se registre
  private nextId = 9;

  // Versión de solo lectura del signal: los componentes pueden leerla,
  // pero todo cambio real pasa por los métodos de aquí abajo
  readonly petsList = this.pets.asReadonly();

  getAll(): Pet[] {
    return this.pets();
  }

  getById(id: number): Pet | undefined {
    return this.pets().find(pet => pet.id === id);
  }

  add(pet: Omit<Pet, 'id'>): Pet {
    const newPet: Pet = { ...pet, id: this.nextId++ };
    this.pets.update(current => [...current, newPet]);
    return newPet;
  }

  update(id: number, changes: Omit<Pet, 'id'>): void {
    this.pets.update(current =>
      current.map(pet => (pet.id === id ? { ...changes, id } : pet))
    );
  }

  delete(id: number): void {
    this.pets.update(current => current.filter(pet => pet.id !== id));
  }
}
