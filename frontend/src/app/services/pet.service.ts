import { Injectable, signal } from '@angular/core';
import { Pet, Species } from '../models/pet.model';

@Injectable({
  providedIn: 'root'
})
export class PetService {

  // Nuestra "base de datos" por ahora: un arreglo en memoria, con datos
  // quemados calcados del DataLoader.java del backend (mismos nombres,
  // razas, fotos y enfermedades, con los dueños asignados siguiendo la misma secuencia Random(42) del backend) para que la migración a Angular se
  // sienta consistente con la app original mientras no hay backend real.
  // Las 85 mascotas adicionales (ids 41-125) son una extensión de la demo para que todos los dueños tengan al menos una.
  // Usamos un signal para que cualquier componente que lo lea se
  // actualice solo cuando agreguemos, editemos o borremos algo.
  private pets = signal<Pet[]>([
    { id: 1, name: 'Buddy', species: Species.DOG, breed: 'Golden Retriever', age: 3, weight: 15.0, disease: null, photoUrl: 'https://images.unsplash.com/photo-1633722715463-d30f4f325e24?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 31, active: true },
    { id: 2, name: 'Whiskers', species: Species.CAT, breed: 'Persian', age: 2, weight: 4.5, disease: null, photoUrl: 'https://eu-central-1.graphassets.com/AnwjgMYRvQfWK3bRPjoq3z/resize=height:778,width:1080/output=format:webp/GftmE5Qtm0AtcNcRWRA1', ownerId: 64, active: true },
    { id: 3, name: 'Max', species: Species.DOG, breed: 'German Shepherd', age: 4, weight: 20.0, disease: null, photoUrl: 'https://ask.woodgreen.org.uk/media/pages/images/5979b7d0bc-1727379943/german-shepherd-900x900-crop-52-5-28-8.jpg', ownerId: 49, active: true },
    { id: 4, name: 'Luna', species: Species.CAT, breed: 'Siamese', age: 1, weight: 3.0, disease: null, photoUrl: 'https://assets.elanco.com/8e0bf1c2-1ae4-001f-9257-f2be3c683fb1/fca42f04-2474-4302-a238-990c8aebfe8c/Siamese_cat_1110x740.jpg', ownerId: 85, active: true },
    { id: 5, name: 'Charlie', species: Species.DOG, breed: 'Beagle', age: 5, weight: 10.0, disease: null, photoUrl: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQRWp3zpN9nyTOC-i1UVNYwutRtjTHDpc40wIIE1BSUTn0kMqAk6ztLwffh&s=10', ownerId: 71, active: true },
    { id: 6, name: 'Rocky', species: Species.CAT, breed: 'Sphynx', age: 6, weight: 4.0, disease: null, photoUrl: 'https://images.unsplash.com/photo-1574158622682-e40e69881006?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 26, active: false },
    { id: 7, name: 'Toby', species: Species.DOG, breed: 'Golden Retriever', age: 3, weight: 14.5, disease: null, photoUrl: 'https://placedog.net/500/400?id=1', ownerId: 6, active: true },
    { id: 8, name: 'Thor', species: Species.DOG, breed: 'Pastor Alemán', age: 5, weight: 32.0, disease: null, photoUrl: 'https://placedog.net/500/400?id=2', ownerId: 19, active: true },
    { id: 9, name: 'Kira', species: Species.CAT, breed: 'Bengalí', age: 2, weight: 3.8, disease: null, photoUrl: 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 20, active: true },
    { id: 10, name: 'Nina', species: Species.CAT, breed: 'Europeo Común', age: 4, weight: 4.2, disease: 'Conjuntivitis', photoUrl: 'https://images.unsplash.com/photo-1519052537078-e6302a4968d4?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 94, active: true },
    { id: 11, name: 'Zeus', species: Species.DOG, breed: 'Dóberman', age: 4, weight: 35.0, disease: null, photoUrl: 'https://placedog.net/500/400?id=3', ownerId: 83, active: true },
    { id: 12, name: 'Apolo', species: Species.DOG, breed: 'Labrador Retriever', age: 2, weight: 12.0, disease: null, photoUrl: 'https://placedog.net/500/400?id=4', ownerId: 3, active: true },
    { id: 13, name: 'Misha', species: Species.CAT, breed: 'Persa', age: 6, weight: 5.1, disease: null, photoUrl: 'https://images.unsplash.com/photo-1495360010541-f48722b34f7d?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 77, active: true },
    { id: 14, name: 'Laia', species: Species.CAT, breed: 'Abisinio', age: 3, weight: 3.5, disease: 'Asma felino', photoUrl: 'https://images.unsplash.com/photo-1533738363-b7f9aef128ce?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 93, active: false },
    { id: 15, name: 'Rayo', species: Species.DOG, breed: 'Galgo Español', age: 7, weight: 27.5, disease: null, photoUrl: 'https://placedog.net/500/400?id=5', ownerId: 77, active: true },
    { id: 16, name: 'Bruno', species: Species.DOG, breed: 'Boxer', age: 3, weight: 28.0, disease: 'Displasia de cadera', photoUrl: 'https://placedog.net/500/400?id=6', ownerId: 33, active: true },
    { id: 17, name: 'Mia', species: Species.CAT, breed: 'Siamés', age: 1, weight: 2.9, disease: null, photoUrl: 'https://images.unsplash.com/photo-1592194996308-7b43878e84a6?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 57, active: true },
    { id: 18, name: 'Nube', species: Species.CAT, breed: 'Angora Turco', age: 5, weight: 4.4, disease: null, photoUrl: 'https://images.unsplash.com/photo-1543852786-1cf6624b9987?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 71, active: true },
    { id: 19, name: 'Diesel', species: Species.DOG, breed: 'Rottweiler', age: 6, weight: 41.0, disease: 'Artritis', photoUrl: 'https://placedog.net/500/400?id=7', ownerId: 44, active: true },
    { id: 20, name: 'Titán', species: Species.DOG, breed: 'Dogo Argentino', age: 5, weight: 38.5, disease: null, photoUrl: 'https://placedog.net/500/400?id=8', ownerId: 10, active: true },
    { id: 21, name: 'India', species: Species.CAT, breed: 'Bosque de Noruega', age: 4, weight: 5.6, disease: null, photoUrl: 'https://images.unsplash.com/photo-1518791841217-8f162f1e1131?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 1, active: true },
    { id: 22, name: 'Nala', species: Species.CAT, breed: 'Maine Coon', age: 3, weight: 6.2, disease: null, photoUrl: 'https://images.unsplash.com/photo-1529778873920-4da4926a72c2?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 64, active: false },
    { id: 23, name: 'Duna', species: Species.DOG, breed: 'Mestizo', age: 2, weight: 9.8, disease: 'Leishmaniasis', photoUrl: 'https://placedog.net/500/400?id=9', ownerId: 27, active: true },
    { id: 24, name: 'Canela', species: Species.DOG, breed: 'Cocker Spaniel', age: 8, weight: 13.2, disease: 'Otitis externa', photoUrl: 'https://placedog.net/500/400?id=10', ownerId: 14, active: true },
    { id: 25, name: 'Gala', species: Species.CAT, breed: 'Ragdoll', age: 2, weight: 4.0, disease: null, photoUrl: 'https://images.unsplash.com/photo-1596854407944-bf87f6fdd49e?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 44, active: true },
    { id: 26, name: 'Frida', species: Species.CAT, breed: 'Azul Ruso', age: 10, weight: 5.0, disease: 'Leucemia felina', photoUrl: 'https://images.unsplash.com/photo-1573865526739-10659fec78a5?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 42, active: true },
    { id: 27, name: 'Rocco', species: Species.DOG, breed: 'Pastor Belga', age: 4, weight: 30.0, disease: null, photoUrl: 'https://placedog.net/500/400?id=11', ownerId: 31, active: true },
    { id: 28, name: 'Nero', species: Species.DOG, breed: 'Schnauzer', age: 9, weight: 8.5, disease: 'Cataratas', photoUrl: 'https://placedog.net/500/400?id=12', ownerId: 59, active: true },
    { id: 29, name: 'Dalí', species: Species.CAT, breed: 'Europeo Común', age: 10, weight: 5.0, disease: null, photoUrl: 'https://images.unsplash.com/photo-1494256997604-768d1f608cac?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 88, active: true },
    { id: 30, name: 'Gaudí', species: Species.CAT, breed: 'Siamés', age: 4, weight: 3.9, disease: 'Dermatitis alérgica', photoUrl: 'https://images.unsplash.com/photo-1513360371669-4adf3dd7dff8?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 47, active: true },
    { id: 31, name: 'Rudy', species: Species.DOG, breed: 'Beagle', age: 5, weight: 11.0, disease: 'Tos de las perreras', photoUrl: 'https://placedog.net/500/400?id=13', ownerId: 31, active: true },
    { id: 32, name: 'Cometa', species: Species.DOG, breed: 'Border Collie', age: 3, weight: 16.5, disease: null, photoUrl: 'https://placedog.net/500/400?id=14', ownerId: 57, active: true },
    { id: 33, name: 'Tomasa', species: Species.CAT, breed: 'Europeo Común', age: 12, weight: 4.6, disease: null, photoUrl: 'https://images.unsplash.com/photo-1548247416-ec66f4900b2e?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 31, active: true },
    { id: 34, name: 'Matilda', species: Species.CAT, breed: 'Persa', age: 7, weight: 4.8, disease: 'Estreñimiento', photoUrl: 'https://images.unsplash.com/photo-1571566882372-1598d88abd90?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 86, active: true },
    { id: 35, name: 'Simba', species: Species.DOG, breed: 'Shar Pei', age: 2, weight: 19.5, disease: 'Obesidad', photoUrl: 'https://placedog.net/500/400?id=15', ownerId: 18, active: true },
    { id: 36, name: 'Otto', species: Species.DOG, breed: 'Teckel', age: 6, weight: 8.2, disease: 'Hernia discal', photoUrl: 'https://placedog.net/500/400?id=16', ownerId: 28, active: true },
    { id: 37, name: 'Cleo', species: Species.CAT, breed: 'Bengalí', age: 3, weight: 4.1, disease: null, photoUrl: 'https://images.unsplash.com/photo-1583795128727-6ec3642408f8?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 13, active: true },
    { id: 38, name: 'Casimira', species: Species.CAT, breed: 'Himalayo', age: 8, weight: 4.4, disease: 'Panleucopenia felina', photoUrl: 'https://images.unsplash.com/photo-1526336024174-e58f5cdd8e13?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 94, active: false },
    { id: 39, name: 'Firulais', species: Species.DOG, breed: 'Mestizo', age: 4, weight: 12.4, disease: null, photoUrl: 'https://placedog.net/500/400?id=17', ownerId: 14, active: true },
    { id: 40, name: 'Aisha', species: Species.CAT, breed: 'Abisinio', age: 2, weight: 3.2, disease: null, photoUrl: 'https://images.unsplash.com/photo-1561948955-570b270e7c36?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 65, active: true },
    { id: 41, name: 'Michi', species: Species.CAT, breed: 'Siamés', age: 4, weight: 2.5, disease: 'Conjuntivitis', photoUrl: 'https://images.unsplash.com/photo-1478098711619-5ab0b478d6e6?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 2, active: true },
    { id: 42, name: 'Miau', species: Species.CAT, breed: 'Persa', age: 9, weight: 3.2, disease: 'Asma felino', photoUrl: 'https://images.unsplash.com/photo-1595433707802-6b2626ef1c91?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 2, active: true },
    { id: 43, name: 'Garfield', species: Species.CAT, breed: 'Bengalí', age: 2, weight: 3.9, disease: 'Leucemia felina', photoUrl: 'https://images.unsplash.com/photo-1591871937573-74dbba515c4c?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 4, active: true },
    { id: 44, name: 'Tom', species: Species.CAT, breed: 'Maine Coon', age: 7, weight: 4.6, disease: 'Panleucopenia felina', photoUrl: 'https://images.unsplash.com/photo-1548802673-380ab8ebc7b7?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 5, active: true },
    { id: 45, name: 'Silvestre', species: Species.CAT, breed: 'Azul Ruso', age: 12, weight: 5.3, disease: 'Estreñimiento', photoUrl: 'https://images.unsplash.com/photo-1570824104453-508955ab713e?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 7, active: true },
    { id: 46, name: 'Bigotes', species: Species.CAT, breed: 'Abisinio', age: 5, weight: 6, disease: 'Dermatitis alérgica', photoUrl: 'https://images.unsplash.com/photo-1577023311546-cdc07a8454d9?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 8, active: true },
    { id: 47, name: 'Coco', species: Species.DOG, breed: 'Golden Retriever', age: 10, weight: 11.6, disease: null, photoUrl: 'https://placedog.net/500/400?id=18', ownerId: 8, active: true },
    { id: 48, name: 'Simón', species: Species.DOG, breed: 'Beagle', age: 3, weight: 12.7, disease: null, photoUrl: 'https://placedog.net/500/400?id=19', ownerId: 9, active: true },
    { id: 49, name: 'Lola', species: Species.DOG, breed: 'Pastor Alemán', age: 8, weight: 13.8, disease: null, photoUrl: 'https://placedog.net/500/400?id=20', ownerId: 11, active: true },
    { id: 50, name: 'Duke', species: Species.DOG, breed: 'Labrador Retriever', age: 1, weight: 14.9, disease: null, photoUrl: 'https://placedog.net/500/400?id=21', ownerId: 12, active: false },
    { id: 51, name: 'Bella', species: Species.DOG, breed: 'Border Collie', age: 6, weight: 16, disease: 'Leishmaniasis', photoUrl: 'https://placedog.net/500/400?id=22', ownerId: 15, active: true },
    { id: 52, name: 'Rex', species: Species.DOG, breed: 'Boxer', age: 11, weight: 17.1, disease: 'Cataratas', photoUrl: 'https://placedog.net/500/400?id=23', ownerId: 15, active: true },
    { id: 53, name: 'Milo', species: Species.DOG, breed: 'Cocker Spaniel', age: 4, weight: 18.2, disease: 'Dermatitis alérgica', photoUrl: 'https://placedog.net/500/400?id=24', ownerId: 16, active: true },
    { id: 54, name: 'Sasha', species: Species.DOG, breed: 'Schnauzer', age: 9, weight: 19.3, disease: 'Alergia alimentaria', photoUrl: 'https://placedog.net/500/400?id=25', ownerId: 17, active: true },
    { id: 55, name: 'Chispa', species: Species.DOG, breed: 'Mestizo', age: 2, weight: 20.4, disease: 'Gastroenteritis', photoUrl: 'https://placedog.net/500/400?id=26', ownerId: 21, active: true },
    { id: 56, name: 'Bongo', species: Species.DOG, breed: 'Teckel', age: 7, weight: 21.5, disease: 'Parvovirus', photoUrl: 'https://placedog.net/500/400?id=27', ownerId: 22, active: true },
    { id: 57, name: 'Tambor', species: Species.DOG, breed: 'Shar Pei', age: 12, weight: 22.6, disease: null, photoUrl: 'https://placedog.net/500/400?id=28', ownerId: 22, active: true },
    { id: 58, name: 'Tigre', species: Species.CAT, breed: 'Europeo Común', age: 5, weight: 5.4, disease: null, photoUrl: 'https://images.unsplash.com/photo-1574144611937-0df059b5ef3e?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 23, active: true },
    { id: 59, name: 'Tigrillo', species: Species.CAT, breed: 'Angora Turco', age: 10, weight: 6.1, disease: null, photoUrl: 'https://images.unsplash.com/photo-1615789591457-74a63395c990?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 24, active: true },
    { id: 60, name: 'Azafrán', species: Species.CAT, breed: 'Ragdoll', age: 3, weight: 6.8, disease: null, photoUrl: 'https://images.unsplash.com/photo-1594149929911-78975a43d4f5?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 25, active: false },
    { id: 61, name: 'Miel', species: Species.CAT, breed: 'Sphynx', age: 8, weight: 3, disease: 'Conjuntivitis', photoUrl: 'https://images.unsplash.com/photo-1606214174585-fe31582dc6ee?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 29, active: true },
    { id: 62, name: 'Cacao', species: Species.CAT, breed: 'Himalayo', age: 1, weight: 3.7, disease: 'Asma felino', photoUrl: 'https://images.unsplash.com/photo-1560114928-40f1f1eb26a0?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 29, active: true },
    { id: 63, name: 'Nutella', species: Species.CAT, breed: 'Bosque de Noruega', age: 6, weight: 4.4, disease: 'Leucemia felina', photoUrl: 'https://images.unsplash.com/photo-1570018144715-43110363d70a?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 30, active: true },
    { id: 64, name: 'Rufo', species: Species.DOG, breed: 'Rottweiler', age: 11, weight: 30.3, disease: 'Insuficiencia cardíaca', photoUrl: 'https://placedog.net/500/400?id=29', ownerId: 32, active: true },
    { id: 65, name: 'Trufa', species: Species.DOG, breed: 'Galgo Español', age: 4, weight: 31.4, disease: 'Displasia de cadera', photoUrl: 'https://placedog.net/500/400?id=30', ownerId: 34, active: true },
    { id: 66, name: 'Pelusa', species: Species.DOG, breed: 'Dogo Argentino', age: 9, weight: 32.5, disease: 'Otitis externa', photoUrl: 'https://placedog.net/500/400?id=31', ownerId: 35, active: true },
    { id: 67, name: 'Manchas', species: Species.DOG, breed: 'Pastor Belga', age: 2, weight: 33.6, disease: null, photoUrl: 'https://placedog.net/500/400?id=32', ownerId: 35, active: true },
    { id: 68, name: 'Bombón', species: Species.DOG, breed: 'Husky Siberiano', age: 7, weight: 34.7, disease: null, photoUrl: 'https://placedog.net/500/400?id=33', ownerId: 36, active: true },
    { id: 69, name: 'Kiwi', species: Species.DOG, breed: 'Samoyedo', age: 12, weight: 35.8, disease: null, photoUrl: 'https://placedog.net/500/400?id=34', ownerId: 37, active: true },
    { id: 70, name: 'Mango', species: Species.DOG, breed: 'Dálmata', age: 5, weight: 36.9, disease: null, photoUrl: 'https://placedog.net/500/400?id=35', ownerId: 38, active: false },
    { id: 71, name: 'Pepe', species: Species.DOG, breed: 'Bulldog Francés', age: 10, weight: 6, disease: 'Tos de las perreras', photoUrl: 'https://placedog.net/500/400?id=36', ownerId: 39, active: true },
    { id: 72, name: 'Tommy', species: Species.DOG, breed: 'Poodle', age: 3, weight: 7.1, disease: 'Obesidad', photoUrl: 'https://placedog.net/500/400?id=37', ownerId: 39, active: true },
    { id: 73, name: 'Chulo', species: Species.DOG, breed: 'Pomerania', age: 8, weight: 8.2, disease: 'Hernia discal', photoUrl: 'https://placedog.net/500/400?id=38', ownerId: 40, active: true },
    { id: 74, name: 'Dandy', species: Species.DOG, breed: 'Yorkshire Terrier', age: 1, weight: 9.3, disease: 'Artritis', photoUrl: 'https://placedog.net/500/400?id=39', ownerId: 41, active: true },
    { id: 75, name: 'Galleta', species: Species.CAT, breed: 'Devon Rex', age: 6, weight: 3.8, disease: 'Insuficiencia renal', photoUrl: 'https://images.unsplash.com/photo-1575550959106-5a7defe28b56?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 43, active: true },
    { id: 76, name: 'Copito', species: Species.CAT, breed: 'Sagrado de Birmania', age: 11, weight: 4.5, disease: 'Diabetes felina', photoUrl: 'https://images.unsplash.com/photo-1541781774459-bb2af2f05b55?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 45, active: true },
    { id: 77, name: 'Perlita', species: Species.CAT, breed: 'American Shorthair', age: 4, weight: 5.2, disease: null, photoUrl: 'https://images.unsplash.com/photo-1583512603805-3cc6b41f3edb?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 45, active: true },
    { id: 78, name: 'Estrella', species: Species.CAT, breed: 'Scottish Fold', age: 9, weight: 5.9, disease: null, photoUrl: 'https://images.unsplash.com/photo-1511044568932-338cba0ad803?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 46, active: true },
    { id: 79, name: 'Rita', species: Species.CAT, breed: 'Siamés', age: 2, weight: 6.6, disease: null, photoUrl: 'https://images.unsplash.com/photo-1533743983669-94fa5c4338ec?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 48, active: true },
    { id: 80, name: 'Pantera', species: Species.CAT, breed: 'Persa', age: 7, weight: 2.8, disease: null, photoUrl: 'https://images.unsplash.com/photo-1525253013412-55c1a69a5738?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 50, active: false },
    { id: 81, name: 'Princesa', species: Species.DOG, breed: 'San Bernardo', age: 12, weight: 17, disease: 'Dermatitis alérgica', photoUrl: 'https://placedog.net/500/400?id=40', ownerId: 51, active: true },
    { id: 82, name: 'Teo', species: Species.DOG, breed: 'Mastín Español', age: 5, weight: 18.1, disease: 'Alergia alimentaria', photoUrl: 'https://placedog.net/500/400?id=41', ownerId: 51, active: true },
    { id: 83, name: 'Rufus', species: Species.DOG, breed: 'Criollo', age: 10, weight: 19.2, disease: 'Gastroenteritis', photoUrl: 'https://placedog.net/500/400?id=42', ownerId: 52, active: true },
    { id: 84, name: 'Odin', species: Species.DOG, breed: 'Golden Retriever', age: 3, weight: 20.3, disease: 'Parvovirus', photoUrl: 'https://placedog.net/500/400?id=43', ownerId: 53, active: true },
    { id: 85, name: 'Balto', species: Species.DOG, breed: 'Beagle', age: 8, weight: 21.4, disease: 'Moquillo', photoUrl: 'https://placedog.net/500/400?id=44', ownerId: 54, active: true },
    { id: 86, name: 'Argos', species: Species.DOG, breed: 'Pastor Alemán', age: 1, weight: 22.5, disease: 'Convalecencia post-quirúrgica', photoUrl: 'https://placedog.net/500/400?id=45', ownerId: 55, active: true },
    { id: 87, name: 'Hera', species: Species.DOG, breed: 'Labrador Retriever', age: 6, weight: 23.6, disease: null, photoUrl: 'https://placedog.net/500/400?id=46', ownerId: 55, active: true },
    { id: 88, name: 'Athena', species: Species.DOG, breed: 'Border Collie', age: 11, weight: 24.7, disease: null, photoUrl: 'https://placedog.net/500/400?id=47', ownerId: 56, active: true },
    { id: 89, name: 'Nerón', species: Species.DOG, breed: 'Boxer', age: 4, weight: 25.8, disease: null, photoUrl: 'https://placedog.net/500/400?id=48', ownerId: 58, active: true },
    { id: 90, name: 'Espartaco', species: Species.DOG, breed: 'Cocker Spaniel', age: 9, weight: 26.9, disease: null, photoUrl: 'https://placedog.net/500/400?id=49', ownerId: 60, active: false },
    { id: 91, name: 'Rambo', species: Species.DOG, breed: 'Schnauzer', age: 2, weight: 28, disease: 'Parásitos intestinales', photoUrl: 'https://placedog.net/500/400?id=50', ownerId: 61, active: true },
    { id: 92, name: 'Duquesa', species: Species.CAT, breed: 'Bengalí', age: 7, weight: 6.7, disease: 'Gingivitis', photoUrl: 'https://images.unsplash.com/photo-1583083527882-4bee9aba2eea?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 61, active: true },
    { id: 93, name: 'Morita', species: Species.CAT, breed: 'Maine Coon', age: 12, weight: 2.9, disease: 'Parásitos intestinales', photoUrl: 'https://images.unsplash.com/photo-1516663713099-37eb6d60c825?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 62, active: true },
    { id: 94, name: 'Salem', species: Species.CAT, breed: 'Azul Ruso', age: 5, weight: 3.6, disease: 'Otitis externa', photoUrl: 'https://images.unsplash.com/photo-1516280440614-37939bbacd81?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 63, active: true },
    { id: 95, name: 'Merlín', species: Species.CAT, breed: 'Abisinio', age: 10, weight: 4.3, disease: 'Insuficiencia renal', photoUrl: 'https://images.unsplash.com/photo-1583324113626-70df0f4deaab?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 66, active: true },
    { id: 96, name: 'Índigo', species: Species.CAT, breed: 'Europeo Común', age: 3, weight: 5, disease: 'Diabetes felina', photoUrl: 'https://images.unsplash.com/photo-1596492784531-6e6eb5ea9993?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 67, active: true },
    { id: 97, name: 'Topacio', species: Species.CAT, breed: 'Angora Turco', age: 8, weight: 5.7, disease: null, photoUrl: 'https://images.unsplash.com/photo-1589656966895-2f33e7653819?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 67, active: true },
    { id: 98, name: 'Falco', species: Species.DOG, breed: 'Mestizo', age: 1, weight: 35.7, disease: null, photoUrl: 'https://placedog.net/500/400?id=51', ownerId: 68, active: true },
    { id: 99, name: 'Draco', species: Species.DOG, breed: 'Teckel', age: 6, weight: 36.8, disease: null, photoUrl: 'https://placedog.net/500/400?id=52', ownerId: 69, active: true },
    { id: 100, name: 'Kaiser', species: Species.DOG, breed: 'Shar Pei', age: 11, weight: 5.9, disease: null, photoUrl: 'https://placedog.net/500/400?id=53', ownerId: 70, active: false },
    { id: 101, name: 'Lupo', species: Species.DOG, breed: 'Rottweiler', age: 4, weight: 7, disease: 'Hernia discal', photoUrl: 'https://placedog.net/500/400?id=54', ownerId: 72, active: true },
    { id: 102, name: 'Perla', species: Species.DOG, breed: 'Galgo Español', age: 9, weight: 8.1, disease: 'Artritis', photoUrl: 'https://placedog.net/500/400?id=55', ownerId: 72, active: true },
    { id: 103, name: 'Toffee', species: Species.DOG, breed: 'Dogo Argentino', age: 2, weight: 9.2, disease: 'Leishmaniasis', photoUrl: 'https://placedog.net/500/400?id=56', ownerId: 73, active: true },
    { id: 104, name: 'Waffle', species: Species.DOG, breed: 'Pastor Belga', age: 7, weight: 10.3, disease: 'Cataratas', photoUrl: 'https://placedog.net/500/400?id=57', ownerId: 74, active: true },
    { id: 105, name: 'Brownie', species: Species.DOG, breed: 'Husky Siberiano', age: 12, weight: 11.4, disease: 'Dermatitis alérgica', photoUrl: 'https://placedog.net/500/400?id=58', ownerId: 75, active: true },
    { id: 106, name: 'Cookie', species: Species.DOG, breed: 'Samoyedo', age: 5, weight: 12.5, disease: 'Alergia alimentaria', photoUrl: 'https://placedog.net/500/400?id=59', ownerId: 76, active: true },
    { id: 107, name: 'Muffin', species: Species.DOG, breed: 'Dálmata', age: 10, weight: 13.6, disease: null, photoUrl: 'https://placedog.net/500/400?id=60', ownerId: 76, active: true },
    { id: 108, name: 'Nacho', species: Species.DOG, breed: 'Bulldog Francés', age: 3, weight: 14.7, disease: null, photoUrl: 'https://placedog.net/500/400?id=61', ownerId: 78, active: true },
    { id: 109, name: 'Ónix', species: Species.CAT, breed: 'Ragdoll', age: 8, weight: 5.1, disease: null, photoUrl: 'https://images.unsplash.com/photo-1615751072497-5f5169febe17?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 79, active: true },
    { id: 110, name: 'Ámbar', species: Species.CAT, breed: 'Sphynx', age: 1, weight: 5.8, disease: null, photoUrl: 'https://images.unsplash.com/photo-1571875257727-256c39da42af?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 80, active: false },
    { id: 111, name: 'Caramelo', species: Species.CAT, breed: 'Himalayo', age: 6, weight: 6.5, disease: 'Hipertiroidismo felino', photoUrl: 'https://images.unsplash.com/photo-1560807707-8cc77767d783?fm=jpg&q=60&w=800&auto=format&fit=crop', ownerId: 81, active: true },
    { id: 112, name: 'Flan', species: Species.CAT, breed: 'Bosque de Noruega', age: 11, weight: 2.7, disease: 'Gingivitis', photoUrl: 'https://placecats.com/neo/500/400', ownerId: 81, active: true },
    { id: 113, name: 'Sirena', species: Species.CAT, breed: 'Devon Rex', age: 4, weight: 3.4, disease: 'Parásitos intestinales', photoUrl: 'https://placecats.com/millie/500/400', ownerId: 82, active: true },
    { id: 114, name: 'Dama', species: Species.CAT, breed: 'Sagrado de Birmania', age: 9, weight: 4.1, disease: 'Otitis externa', photoUrl: 'https://placecats.com/500/400', ownerId: 84, active: true },
    { id: 115, name: 'Churro', species: Species.DOG, breed: 'Poodle', age: 2, weight: 22.4, disease: 'Parásitos intestinales', photoUrl: 'https://placedog.net/500/400?id=62', ownerId: 87, active: true },
    { id: 116, name: 'Canelo', species: Species.DOG, breed: 'Pomerania', age: 7, weight: 23.5, disease: 'Insuficiencia cardíaca', photoUrl: 'https://placedog.net/500/400?id=63', ownerId: 89, active: true },
    { id: 117, name: 'Sombra', species: Species.DOG, breed: 'Yorkshire Terrier', age: 12, weight: 24.6, disease: null, photoUrl: 'https://placedog.net/500/400?id=64', ownerId: 90, active: true },
    { id: 118, name: 'Centella', species: Species.DOG, breed: 'San Bernardo', age: 5, weight: 25.7, disease: null, photoUrl: 'https://placedog.net/500/400?id=65', ownerId: 91, active: true },
    { id: 119, name: 'Relámpago', species: Species.DOG, breed: 'Mastín Español', age: 10, weight: 26.8, disease: null, photoUrl: 'https://placedog.net/500/400?id=66', ownerId: 92, active: true },
    { id: 120, name: 'Trueno', species: Species.DOG, breed: 'Criollo', age: 3, weight: 27.9, disease: null, photoUrl: 'https://placedog.net/500/400?id=67', ownerId: 95, active: false },
    { id: 121, name: 'Ares', species: Species.DOG, breed: 'Golden Retriever', age: 8, weight: 29, disease: 'Displasia de cadera', photoUrl: 'https://placedog.net/500/400?id=68', ownerId: 96, active: true },
    { id: 122, name: 'Héctor', species: Species.DOG, breed: 'Beagle', age: 1, weight: 30.1, disease: 'Otitis externa', photoUrl: 'https://placedog.net/500/400?id=69', ownerId: 97, active: true },
    { id: 123, name: 'Hércules', species: Species.DOG, breed: 'Pastor Alemán', age: 6, weight: 31.2, disease: 'Tos de las perreras', photoUrl: 'https://placedog.net/500/400?id=70', ownerId: 98, active: true },
    { id: 124, name: 'Maximus', species: Species.DOG, breed: 'Labrador Retriever', age: 11, weight: 32.3, disease: 'Obesidad', photoUrl: 'https://placedog.net/500/400?id=71', ownerId: 99, active: true },
    { id: 125, name: 'Esparta', species: Species.DOG, breed: 'Border Collie', age: 4, weight: 33.4, disease: 'Hernia discal', photoUrl: 'https://placedog.net/500/400?id=72', ownerId: 100, active: true },
  ]);

  // El id que le tocará a la próxima mascota que se registre
  private nextId = 126;

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

  // Mascotas de un dueño (como PetService.getPetsByOwnerId del backend)
  getByOwnerId(ownerId: number): Pet[] {
    return this.pets().filter(pet => pet.ownerId === ownerId);
  }

  // Cascada al eliminar un dueño (igual que la cascada física Owner -> Pet del backend)
  deleteByOwner(ownerId: number): void {
    this.pets.update(current => current.filter(pet => pet.ownerId !== ownerId));
  }
}
