import { Species } from '../../../models/pet.model';

// Etiquetas del área del cliente. Se comparten entre el listado de mascotas y el
// detalle para que los dos pinten los mismos textos.

// Especie en lenguaje del cliente.
export const speciesLabel = (species: Species): string => (species === Species.DOG ? 'Perro' : 'Gato');

// Edad en singular cuando es 1 año ("1 año", "3 años").
export const ageLabel = (age: number): string => (age === 1 ? '1 año' : `${age} años`);

// Peso con un decimal solo cuando hace falta ("7 kg", "4.5 kg").
export const weightLabel = (weight: number): string =>
  Number.isInteger(weight) ? `${weight} kg` : `${weight.toFixed(1)} kg`;

// Cantidad de medicamentos de un tratamiento ("1 medicamento", "3 medicamentos").
export const drugCountLabel = (count: number): string =>
  count === 1 ? '1 medicamento' : `${count} medicamentos`;
