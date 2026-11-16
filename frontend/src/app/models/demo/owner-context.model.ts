// Lo que un dueño puede conocer con el API actual: sus mascotas, los tratamientos de estas
// y, de ahí, los veterinarios que las han atendido.
import { ClienteTreatment } from '../treatment-cliente.model';
import { Pet } from '../pet.model';
import { VetRef } from './appointment.model';

export interface OwnerContext {
  pets: Pet[];
  petTreatments: { pet: Pet; treatments: ClienteTreatment[] }[];
  vets: VetRef[];
}
