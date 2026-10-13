// Dueño tal como lo devuelve el API (OwnerResponse): nunca incluye la contraseña.
export interface Owner {
  id: number;
  document: string;
  name: string;
  email: string;
  phone: string;
}

// Cuerpo para registrar un dueño (OwnerRequest): la contraseña es obligatoria.
export interface OwnerRequest {
  document: string;
  name: string;
  email: string;
  password: string;
  phone: string;
}

// Cuerpo para actualizar un dueño (OwnerUpdateRequest): con password en null
// el API conserva la contraseña actual.
export interface OwnerUpdateRequest {
  document: string;
  name: string;
  email: string;
  password: string | null;
  phone: string;
}
