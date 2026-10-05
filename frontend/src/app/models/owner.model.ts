// Entidad plana del Owner de Spring para el CRUD del dashboard:
// la relación con sus mascotas está en Pet.ownerId.
export interface Owner {
  id: number;
  document: string;
  name: string;
  email: string;
  password: string;
  phone: string;
}
