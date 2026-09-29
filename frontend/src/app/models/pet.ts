import type { Owner } from './owner';
import type { Species } from './species';
import type { Treatment } from './treatment';

export interface Pet {
  id: number;
  name: string;
  owner: Owner;
  treatments: Treatment[];
  species: Species;
  breed: string;
  age: number;
  weight: number;
  disease: string | null;
  photoUrl: string | null;
  active: boolean;
}
