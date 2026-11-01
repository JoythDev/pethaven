// Veterinario tal como lo devuelve el API (VeterinarianResponse): nunca incluye la contraseña.
export interface Veterinarian {
  id: number;
  document: string;
  name: string;
  email: string;
  specialty: string;
  photoUrl: string | null;
  attentions: number;
  active: boolean;
}

// Cuerpo para registrar un veterinario (VeterinarianRequest): la contraseña es obligatoria.
export interface VeterinarianRequest {
  document: string;
  name: string;
  email: string;
  password: string;
  specialty: string;
  photoUrl: string | null;
}

// Cuerpo para actualizar un veterinario (VeterinarianUpdateRequest): con password en null
// el API conserva la contraseña actual.
export interface VeterinarianUpdateRequest {
  document: string;
  name: string;
  email: string;
  password: string | null;
  specialty: string;
  photoUrl: string | null;
}
