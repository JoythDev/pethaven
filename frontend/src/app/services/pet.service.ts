import { Injectable, signal } from '@angular/core';
import { Pet, Species } from '../models/pet.model';

@Injectable({
  providedIn: 'root'
})
export class PetService {

  // Nuestra "base de datos" por ahora: un arreglo en memoria, con datos
  // quemados calcados del DataLoader.java del backend (mismos nombres,
  // razas, fotos y enfermedades) para que la migración a Angular se
  // sienta consistente con la app original mientras no hay backend real.
  // Usamos un signal para que cualquier componente que lo lea se
  // actualice solo cuando agreguemos, editemos o borremos algo.
  private pets = signal<Pet[]>([
    { id: 1, name: 'Buddy', species: Species.DOG, breed: 'Golden Retriever', age: 3, weight: 15.0, disease: null, photoUrl: 'https://images.unsplash.com/photo-1633722715463-d30f4f325e24?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 1, active: true },
    { id: 2, name: 'Whiskers', species: Species.CAT, breed: 'Persian', age: 2, weight: 4.5, disease: null, photoUrl: 'https://eu-central-1.graphassets.com/AnwjgMYRvQfWK3bRPjoq3z/resize=height:778,width:1080/output=format:webp/GftmE5Qtm0AtcNcRWRA1', ownerId: 2, active: true },
    { id: 3, name: 'Max', species: Species.DOG, breed: 'German Shepherd', age: 4, weight: 20.0, disease: null, photoUrl: 'https://ask.woodgreen.org.uk/media/pages/images/5979b7d0bc-1727379943/german-shepherd-900x900-crop-52-5-28-8.jpg', ownerId: 3, active: true },
    { id: 4, name: 'Luna', species: Species.CAT, breed: 'Siamese', age: 1, weight: 3.0, disease: null, photoUrl: 'https://assets.elanco.com/8e0bf1c2-1ae4-001f-9257-f2be3c683fb1/fca42f04-2474-4302-a238-990c8aebfe8c/Siamese_cat_1110x740.jpg', ownerId: 4, active: true },
    { id: 5, name: 'Charlie', species: Species.DOG, breed: 'Beagle', age: 5, weight: 10.0, disease: null, photoUrl: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQRWp3zpN9nyTOC-i1UVNYwutRtjTHDpc40wIIE1BSUTn0kMqAk6ztLwffh&s=10', ownerId: 5, active: true },
    { id: 6, name: 'Rocky', species: Species.CAT, breed: 'Sphynx', age: 6, weight: 4.0, disease: null, photoUrl: 'https://images.unsplash.com/photo-1574158622682-e40e69881006?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 6, active: false },
    { id: 7, name: 'Toby', species: Species.DOG, breed: 'Golden Retriever', age: 3, weight: 14.5, disease: null, photoUrl: 'https://placedog.net/500/400?id=1', ownerId: 7, active: true },
    { id: 8, name: 'Thor', species: Species.DOG, breed: 'Pastor Alemán', age: 5, weight: 32.0, disease: null, photoUrl: 'https://placedog.net/500/400?id=2', ownerId: 8, active: true },
    { id: 9, name: 'Kira', species: Species.CAT, breed: 'Bengalí', age: 2, weight: 3.8, disease: null, photoUrl: 'https://loremflickr.com/500/400/kitten?lock=1', ownerId: 9, active: true },
    { id: 10, name: 'Nina', species: Species.CAT, breed: 'Europeo Común', age: 4, weight: 4.2, disease: 'Conjuntivitis', photoUrl: 'https://loremflickr.com/500/400/kitten?lock=2', ownerId: 10, active: true },
    { id: 11, name: 'Zeus', species: Species.DOG, breed: 'Dóberman', age: 4, weight: 35.0, disease: null, photoUrl: 'https://placedog.net/500/400?id=3', ownerId: 1, active: true },
    { id: 12, name: 'Apolo', species: Species.DOG, breed: 'Labrador Retriever', age: 2, weight: 12.0, disease: null, photoUrl: 'https://placedog.net/500/400?id=4', ownerId: 2, active: true },
    { id: 13, name: 'Misha', species: Species.CAT, breed: 'Persa', age: 6, weight: 5.1, disease: null, photoUrl: 'https://loremflickr.com/500/400/kitten?lock=3', ownerId: 3, active: true },
    { id: 14, name: 'Laia', species: Species.CAT, breed: 'Abisinio', age: 3, weight: 3.5, disease: 'Asma felino', photoUrl: 'https://loremflickr.com/500/400/kitten?lock=4', ownerId: 4, active: false },
    { id: 15, name: 'Rayo', species: Species.DOG, breed: 'Galgo Español', age: 7, weight: 27.5, disease: null, photoUrl: 'https://placedog.net/500/400?id=5', ownerId: 5, active: true },
    { id: 16, name: 'Bruno', species: Species.DOG, breed: 'Boxer', age: 3, weight: 28.0, disease: 'Displasia de cadera', photoUrl: 'https://placedog.net/500/400?id=6', ownerId: 6, active: true },
    { id: 17, name: 'Mia', species: Species.CAT, breed: 'Siamés', age: 1, weight: 2.9, disease: null, photoUrl: 'https://loremflickr.com/500/400/kitten?lock=5', ownerId: 7, active: true },
    { id: 18, name: 'Nube', species: Species.CAT, breed: 'Angora Turco', age: 5, weight: 4.4, disease: null, photoUrl: 'https://loremflickr.com/500/400/kitten?lock=6', ownerId: 8, active: true },
    { id: 19, name: 'Diesel', species: Species.DOG, breed: 'Rottweiler', age: 6, weight: 41.0, disease: 'Artritis', photoUrl: 'https://placedog.net/500/400?id=7', ownerId: 9, active: true },
    { id: 20, name: 'Titán', species: Species.DOG, breed: 'Dogo Argentino', age: 5, weight: 38.5, disease: null, photoUrl: 'https://placedog.net/500/400?id=8', ownerId: 10, active: true },
    { id: 21, name: 'India', species: Species.CAT, breed: 'Bosque de Noruega', age: 4, weight: 5.6, disease: null, photoUrl: 'https://loremflickr.com/500/400/kitten?lock=7', ownerId: 1, active: true },
    { id: 22, name: 'Nala', species: Species.CAT, breed: 'Maine Coon', age: 3, weight: 6.2, disease: null, photoUrl: 'https://loremflickr.com/500/400/kitten?lock=8', ownerId: 2, active: false },
    { id: 23, name: 'Duna', species: Species.DOG, breed: 'Mestizo', age: 2, weight: 9.8, disease: 'Leishmaniasis', photoUrl: 'https://placedog.net/500/400?id=9', ownerId: 3, active: true },
    { id: 24, name: 'Canela', species: Species.DOG, breed: 'Cocker Spaniel', age: 8, weight: 13.2, disease: 'Otitis externa', photoUrl: 'https://placedog.net/500/400?id=10', ownerId: 4, active: true },
    { id: 25, name: 'Gala', species: Species.CAT, breed: 'Ragdoll', age: 2, weight: 4.0, disease: null, photoUrl: 'https://loremflickr.com/500/400/kitten?lock=9', ownerId: 5, active: true },
    { id: 26, name: 'Frida', species: Species.CAT, breed: 'Azul Ruso', age: 10, weight: 5.0, disease: 'Leucemia felina', photoUrl: 'https://loremflickr.com/500/400/kitten?lock=10', ownerId: 6, active: true },
    { id: 27, name: 'Rocco', species: Species.DOG, breed: 'Pastor Belga', age: 4, weight: 30.0, disease: null, photoUrl: 'https://placedog.net/500/400?id=11', ownerId: 7, active: true },
    { id: 28, name: 'Nero', species: Species.DOG, breed: 'Schnauzer', age: 9, weight: 8.5, disease: 'Cataratas', photoUrl: 'https://placedog.net/500/400?id=12', ownerId: 8, active: true },
    { id: 29, name: 'Dalí', species: Species.CAT, breed: 'Europeo Común', age: 10, weight: 5.0, disease: null, photoUrl: 'https://loremflickr.com/500/400/kitten?lock=11', ownerId: 9, active: true },
    { id: 30, name: 'Gaudí', species: Species.CAT, breed: 'Siamés', age: 4, weight: 3.9, disease: 'Dermatitis alérgica', photoUrl: 'https://loremflickr.com/500/400/kitten?lock=12', ownerId: 10, active: true },
    { id: 31, name: 'Rudy', species: Species.DOG, breed: 'Beagle', age: 5, weight: 11.0, disease: 'Tos de las perreras', photoUrl: 'https://placedog.net/500/400?id=13', ownerId: 1, active: true },
    { id: 32, name: 'Cometa', species: Species.DOG, breed: 'Border Collie', age: 3, weight: 16.5, disease: null, photoUrl: 'https://placedog.net/500/400?id=14', ownerId: 2, active: true },
    { id: 33, name: 'Tomasa', species: Species.CAT, breed: 'Europeo Común', age: 12, weight: 4.6, disease: null, photoUrl: 'https://loremflickr.com/500/400/kitten?lock=13', ownerId: 3, active: true },
    { id: 34, name: 'Matilda', species: Species.CAT, breed: 'Persa', age: 7, weight: 4.8, disease: 'Estreñimiento', photoUrl: 'https://loremflickr.com/500/400/kitten?lock=14', ownerId: 4, active: true },
    { id: 35, name: 'Simba', species: Species.DOG, breed: 'Shar Pei', age: 2, weight: 19.5, disease: 'Obesidad', photoUrl: 'https://placedog.net/500/400?id=15', ownerId: 5, active: true },
    { id: 36, name: 'Otto', species: Species.DOG, breed: 'Teckel', age: 6, weight: 8.2, disease: 'Hernia discal', photoUrl: 'https://placedog.net/500/400?id=16', ownerId: 6, active: true },
    { id: 37, name: 'Cleo', species: Species.CAT, breed: 'Bengalí', age: 3, weight: 4.1, disease: null, photoUrl: 'https://loremflickr.com/500/400/kitten?lock=15', ownerId: 7, active: true },
    { id: 38, name: 'Casimira', species: Species.CAT, breed: 'Himalayo', age: 8, weight: 4.4, disease: 'Panleucopenia felina', photoUrl: 'https://loremflickr.com/500/400/kitten?lock=16', ownerId: 8, active: false },
    { id: 39, name: 'Firulais', species: Species.DOG, breed: 'Mestizo', age: 4, weight: 12.4, disease: null, photoUrl: 'https://placedog.net/500/400?id=17', ownerId: 9, active: true },
    { id: 40, name: 'Aisha', species: Species.CAT, breed: 'Abisinio', age: 2, weight: 3.2, disease: null, photoUrl: 'https://loremflickr.com/500/400/kitten?lock=17', ownerId: 10, active: true }
  ]);

  // El id que le tocará a la próxima mascota que se registre
  private nextId = 41;

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
