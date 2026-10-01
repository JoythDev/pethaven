import type { Treatment } from './treatment';

export interface Veterinarian {
  id: number;
  document: string;
  name: string;
  email: string;
  password: string;
  specialty: string;
  photoUrl: string | null;
  treatments: Treatment[];
  attentions: number;
  active: boolean;
}
