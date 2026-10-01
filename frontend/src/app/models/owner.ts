import type { Pet } from './pet';

export interface Owner {
  id: number;
  document: string;
  name: string;
  email: string;
  password: string;
  phone: string;
  pets: Pet[];
}
