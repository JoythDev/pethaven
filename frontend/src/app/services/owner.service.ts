import { Injectable, inject, signal } from '@angular/core';
import { Owner } from '../models/owner.model';
import { PetService } from './pet.service';

@Injectable({
  providedIn: 'root'
})
export class OwnerService {

  private readonly petService = inject(PetService);

  // Los mismos 100 dueños del DataLoader.java.
  private owners = signal<Owner[]>([
    { id: 1, name: 'John Doe', document: '123456789', phone: '1234567890', email: 'john.doe@example.com', password: 'password123' },
    { id: 2, name: 'Jane Doe', document: '987654321', phone: '0987654321', email: 'jane.doe@example.com', password: 'password123' },
    { id: 3, name: 'Alice Smith', document: '456789123', phone: '4567891230', email: 'alice.smith@example.com', password: 'password123' },
    { id: 4, name: 'Bob Johnson', document: '789123456', phone: '7891234560', email: 'bob.johnson@example.com', password: 'password123' },
    { id: 5, name: 'Charlie Brown', document: '321654987', phone: '3216549870', email: 'charlie.brown@example.com', password: 'password123' },
    { id: 6, name: 'María García', document: '145230786', phone: '612345678', email: 'maria.garcia@example.com', password: 'password123' },
    { id: 7, name: 'José Rodríguez', document: '287134590', phone: '623456789', email: 'jose.rodriguez@example.com', password: 'clave123' },
    { id: 8, name: 'Ana Martínez', document: '376495821', phone: '634567891', email: 'ana.martinez@example.com', password: 'mascota123' },
    { id: 9, name: 'Carlos López', document: '461728935', phone: '645678912', email: 'carlos.lopez@example.com', password: 'pass1234' },
    { id: 10, name: 'Laura Sánchez', document: '592816374', phone: '645678923', email: 'laura.sanchez@example.com', password: 'ejemplo123' },
    { id: 11, name: 'David Pérez', document: '675834129', phone: '656789012', email: 'david.perez@example.com', password: 'password123' },
    { id: 12, name: 'Carmen Fernández', document: '712948365', phone: '667890145', email: 'carmen.fernandez@example.com', password: 'clave123' },
    { id: 13, name: 'Javier Gómez', document: '829415763', phone: '678901256', email: 'javier.gomez@example.com', password: 'mascota123' },
    { id: 14, name: 'Elena Ruiz', document: '846173920', phone: '689012367', email: 'elena.ruiz@example.com', password: 'pass1234' },
    { id: 15, name: 'Miguel Torres', document: '954268137', phone: '691234578', email: 'miguel.torres@example.com', password: 'ejemplo123' },
    { id: 16, name: 'Pilar Ramírez', document: '162584739', phone: '602345689', email: 'pilar.ramirez@example.com', password: 'password123' },
    { id: 17, name: 'Antonio Navarro', document: '273859164', phone: '613456791', email: 'antonio.navarro@example.com', password: 'clave123' },
    { id: 18, name: 'Lucía Díaz', document: '384925716', phone: '624567812', email: 'lucia.diaz@example.com', password: 'mascota123' },
    { id: 19, name: 'Sergio Morales', document: '495136827', phone: '635678923', email: 'sergio.morales@example.com', password: 'pass1234' },
    { id: 20, name: 'Isabel Ortega', document: '516247938', phone: '646789014', email: 'isabel.ortega@example.com', password: 'ejemplo123' },
    { id: 21, name: 'Pedro Vargas', document: '627358149', phone: '657890125', email: 'pedro.vargas@example.com', password: 'password123' },
    { id: 22, name: 'Marta Herrera', document: '738469251', phone: '668901236', email: 'marta.herrera@example.com', password: 'clave123' },
    { id: 23, name: 'Roberto Castillo', document: '849571362', phone: '679012347', email: 'roberto.castillo@example.com', password: 'mascota123' },
    { id: 24, name: 'Rosa Jiménez', document: '849572473', phone: '681234458', email: 'rosa.jimenez@example.com', password: 'pass1234' },
    { id: 25, name: 'Alberto Mendoza', document: '951683584', phone: '692345569', email: 'alberto.mendoza@example.com', password: 'ejemplo123' },
    { id: 26, name: 'Cristina Vega', document: '162794695', phone: '603456671', email: 'cristina.vega@example.com', password: 'password123' },
    { id: 27, name: 'Andrés Rubio', document: '273815716', phone: '614567782', email: 'andres.rubio@example.com', password: 'clave123' },
    { id: 28, name: 'Paula Castro', document: '384926827', phone: '625678893', email: 'paula.castro@example.com', password: 'mascota123' },
    { id: 29, name: 'Francisco Molina', document: '495137938', phone: '636789914', email: 'francisco.molina@example.com', password: 'pass1234' },
    { id: 30, name: 'Raquel Delgado', document: '516248149', phone: '647890125', email: 'raquel.delgado@example.com', password: 'ejemplo123' },
    { id: 31, name: 'Diego Aguilar', document: '527359251', phone: '658901236', email: 'diego.aguilar@example.com', password: 'password123' },
    { id: 32, name: 'Nuria Campos', document: '638461362', phone: '669012347', email: 'nuria.campos@example.com', password: 'clave123' },
    { id: 33, name: 'Álvaro Rivera', document: '749572473', phone: '671234458', email: 'alvaro.rivera@example.com', password: 'mascota123' },
    { id: 34, name: 'Beatriz Reyes', document: '851683584', phone: '682345569', email: 'beatriz.reyes@example.com', password: 'pass1234' },
    { id: 35, name: 'Manuel Cabrera', document: '962794695', phone: '693456671', email: 'manuel.cabrera@example.com', password: 'ejemplo123' },
    { id: 36, name: 'Silvia Montes', document: '173815716', phone: '604567782', email: 'silvia.montes@example.com', password: 'password123' },
    { id: 37, name: 'Jorge Flores', document: '284926827', phone: '615678893', email: 'jorge.flores@example.com', password: 'clave123' },
    { id: 38, name: 'Adriana Salas', document: '395137938', phone: '626789914', email: 'adriana.salas@example.com', password: 'mascota123' },
    { id: 39, name: 'Óscar Ibáñez', document: '416248149', phone: '637890125', email: 'oscar.ibanez@example.com', password: 'pass1234' },
    { id: 40, name: 'Natalia Vidal', document: '538470362', phone: '648901236', email: 'natalia.vidal@example.com', password: 'ejemplo123' },
    { id: 41, name: 'Ricardo Marín', document: '648372915', phone: '659012347', email: 'ricardo.marin@example.com', password: 'password123' },
    { id: 42, name: 'Gloria Sanz', document: '759683026', phone: '661234458', email: 'gloria.sanz@example.com', password: 'clave123' },
    { id: 43, name: 'Enrique Espinosa', document: '861794137', phone: '672345569', email: 'enrique.espinosa@example.com', password: 'mascota123' },
    { id: 44, name: 'Teresa Rojas', document: '972815248', phone: '683456671', email: 'teresa.rojas@example.com', password: 'pass1234' },
    { id: 45, name: 'Gabriel Cruz', document: '173815717', phone: '694567782', email: 'gabriel.cruz@example.com', password: 'ejemplo123' },
    { id: 46, name: 'Ángela Prieto', document: '284926828', phone: '605678893', email: 'angela.prieto@example.com', password: 'password123' },
    { id: 47, name: 'Fernando Cano', document: '395137939', phone: '616789914', email: 'fernando.cano@example.com', password: 'clave123' },
    { id: 48, name: 'Mónica Serrano', document: '416248150', phone: '627890125', email: 'monica.serrano@example.com', password: 'mascota123' },
    { id: 49, name: 'Rubén Lozano', document: '527359252', phone: '638901236', email: 'ruben.lozano@example.com', password: 'pass1234' },
    { id: 50, name: 'Irene Blanco', document: '638461363', phone: '649012347', email: 'irene.blanco@example.com', password: 'ejemplo123' },
    { id: 51, name: 'Pablo Gallego', document: '749572474', phone: '651234458', email: 'pablo.gallego@example.com', password: 'password123' },
    { id: 52, name: 'Susana Ferrer', document: '851683585', phone: '662345569', email: 'susana.ferrer@example.com', password: 'clave123' },
    { id: 53, name: 'Adrián Salas', document: '962794696', phone: '673456671', email: 'adrian.salas@example.com', password: 'mascota123' },
    { id: 54, name: 'Verónica Soto', document: '173815718', phone: '684567782', email: 'veronica.soto@example.com', password: 'pass1234' },
    { id: 55, name: 'Raúl Carmona', document: '284926829', phone: '695678893', email: 'raul.carmona@example.com', password: 'ejemplo123' },
    { id: 56, name: 'Andrea Miranda', document: '395137940', phone: '606789914', email: 'andrea.miranda@example.com', password: 'password123' },
    { id: 57, name: 'Santiago Guerrero', document: '416248151', phone: '617890125', email: 'santiago.guerrero@example.com', password: 'clave123' },
    { id: 58, name: 'Noelia Aguilar', document: '527359253', phone: '628901236', email: 'noelia.aguilar@example.com', password: 'mascota123' },
    { id: 59, name: 'Iván Pardo', document: '638461364', phone: '639012347', email: 'ivan.pardo@example.com', password: 'pass1234' },
    { id: 60, name: 'Eva Soler', document: '749572475', phone: '641234458', email: 'eva.soler@example.com', password: 'ejemplo123' },
    { id: 61, name: 'Héctor Vargas', document: '851683586', phone: '652345569', email: 'hector.vargas@example.com', password: 'password123' },
    { id: 62, name: 'Claudia Franco', document: '962794697', phone: '663456671', email: 'claudia.franco@example.com', password: 'clave123' },
    { id: 63, name: 'Gustavo Peña', document: '173815719', phone: '674567782', email: 'gustavo.pena@example.com', password: 'mascota123' },
    { id: 64, name: 'Alba Domínguez', document: '284926830', phone: '685678893', email: 'alba.dominguez@example.com', password: 'pass1234' },
    { id: 65, name: 'Mario Bravo', document: '395137941', phone: '696789914', email: 'mario.bravo@example.com', password: 'ejemplo123' },
    { id: 66, name: 'Sara Villar', document: '416248152', phone: '607890125', email: 'sara.villar@example.com', password: 'password123' },
    { id: 67, name: 'Emilio Fuentes', document: '527359254', phone: '618901236', email: 'emilio.fuentes@example.com', password: 'clave123' },
    { id: 68, name: 'Rocío Gallardo', document: '638461365', phone: '629012347', email: 'rocio.gallardo@example.com', password: 'mascota123' },
    { id: 69, name: 'Tomás Escobar', document: '749572476', phone: '631234458', email: 'tomas.escobar@example.com', password: 'pass1234' },
    { id: 70, name: 'Lidia Valdés', document: '851683587', phone: '642345569', email: 'lidia.valdes@example.com', password: 'ejemplo123' },
    { id: 71, name: 'Bernardo Ríos', document: '962794698', phone: '653456671', email: 'bernardo.rios@example.com', password: 'password123' },
    { id: 72, name: 'Sonia Acosta', document: '173815720', phone: '664567782', email: 'sonia.acosta@example.com', password: 'clave123' },
    { id: 73, name: 'Marcos Vidal', document: '284926831', phone: '675678893', email: 'marcos.vidal@example.com', password: 'mascota123' },
    { id: 74, name: 'Bruno Márquez', document: '395137942', phone: '686789914', email: 'bruno.marquez@example.com', password: 'pass1234' },
    { id: 75, name: 'Daniela Cortés', document: '416248153', phone: '697890125', email: 'daniela.cortes@example.com', password: 'ejemplo123' },
    { id: 76, name: 'Félix Sandoval', document: '527359255', phone: '608901236', email: 'felix.sandoval@example.com', password: 'password123' },
    { id: 77, name: 'Elvira Méndez', document: '638461366', phone: '619012347', email: 'elvira.mendez@example.com', password: 'clave123' },
    { id: 78, name: 'Iván Peña', document: '749572477', phone: '621234458', email: 'ivan.pena@example.com', password: 'mascota123' },
    { id: 79, name: 'Sonia Gil', document: '851683588', phone: '632345569', email: 'sonia.gil@example.com', password: 'pass1234' },
    { id: 80, name: 'Ramón Expósito', document: '962794699', phone: '643456671', email: 'ramon.exposito@example.com', password: 'ejemplo123' },
    { id: 81, name: 'Carla Redondo', document: '173815721', phone: '654567782', email: 'carla.redondo@example.com', password: 'password123' },
    { id: 82, name: 'Julio Navas', document: '284926832', phone: '665678893', email: 'julio.navas@example.com', password: 'clave123' },
    { id: 83, name: 'Marisol Vega', document: '395137943', phone: '676789914', email: 'marisol.vega@example.com', password: 'mascota123' },
    { id: 84, name: 'Teo Camacho', document: '416248154', phone: '687890125', email: 'teo.camacho@example.com', password: 'pass1234' },
    { id: 85, name: 'Julieta Pons', document: '527359256', phone: '698901236', email: 'julieta.pons@example.com', password: 'ejemplo123' },
    { id: 86, name: 'Abel Salgado', document: '638461367', phone: '609012347', email: 'abel.salgado@example.com', password: 'password123' },
    { id: 87, name: 'Estrella Cabrera', document: '749572478', phone: '611234458', email: 'estrella.cabrera@example.com', password: 'clave123' },
    { id: 88, name: 'Imanol Rivas', document: '851683589', phone: '622345569', email: 'imanol.rivas@example.com', password: 'mascota123' },
    { id: 89, name: 'Aroa Sáez', document: '962794700', phone: '633456671', email: 'aroa.saez@example.com', password: 'pass1234' },
    { id: 90, name: 'Germán Peralta', document: '173815722', phone: '644567782', email: 'german.peralta@example.com', password: 'ejemplo123' },
    { id: 91, name: 'Celia Bermejo', document: '284926833', phone: '655678893', email: 'celia.bermejo@example.com', password: 'password123' },
    { id: 92, name: 'Igor Sanz', document: '395137944', phone: '666789914', email: 'igor.sanz@example.com', password: 'clave123' },
    { id: 93, name: 'Leire Ortiz', document: '416248155', phone: '677890125', email: 'leire.ortiz@example.com', password: 'mascota123' },
    { id: 94, name: 'Ramiro Estévez', document: '527359257', phone: '688901236', email: 'ramiro.estevez@example.com', password: 'pass1234' },
    { id: 95, name: 'Yolanda Cuevas', document: '638461368', phone: '699901236', email: 'yolanda.cuevas@example.com', password: 'ejemplo123' },
    { id: 96, name: 'Ismael Arias', document: '749572479', phone: '610234567', email: 'ismael.arias@example.com', password: 'password123' },
    { id: 97, name: 'Aitor Lasa', document: '851683590', phone: '620345678', email: 'aitor.lasa@example.com', password: 'clave123' },
    { id: 98, name: 'Berta Anaya', document: '962794701', phone: '630456789', email: 'berta.anaya@example.com', password: 'mascota123' },
    { id: 99, name: 'Guillermo Fuster', document: '173815723', phone: '640567891', email: 'guillermo.fuster@example.com', password: 'pass1234' },
    { id: 100, name: 'Rodrigo Alcántara', document: '284926834', phone: '650567892', email: 'rodrigo.alcantara@example.com', password: 'ejemplo123' },
  ]);

  // El id que le tocará al próximo dueño que se registre
  private nextId = 101;

  // Versión de solo lectura del signal: los componentes pueden leerla,
  // pero todo cambio real pasa por los métodos de aquí abajo
  readonly ownersList = this.owners.asReadonly();

  getAll(): Owner[] {
    return this.owners();
  }

  getById(id: number): Owner | undefined {
    return this.owners().find(owner => owner.id === id);
  }

  add(owner: Omit<Owner, 'id'>): Owner {
    const newOwner: Owner = { ...owner, id: this.nextId++ };
    this.owners.update(current => [...current, newOwner]);
    return newOwner;
  }

  update(id: number, changes: Omit<Owner, 'id'>): void {
    this.owners.update(current =>
      current.map(owner => (owner.id === id ? { ...changes, id } : owner))
    );
  }

  // Igual a la cascada física del backend (Owner -> Pet): eliminar un dueño
  // elimina también sus mascotas asociadas.
  delete(id: number): void {
    this.petService.deleteByOwner(id);
    this.owners.update(current => current.filter(owner => owner.id !== id));
  }
}
