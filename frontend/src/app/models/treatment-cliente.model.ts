// Modelos del área del cliente con prefijo propio: el área veterinaria puede
// tener sus propios modelos de tratamiento (Treatment*) sin chocar con estos.

// Renglón de medicamento administrado en un tratamiento (TreatmentDrugResponse).
// OJO: los precios de compra y venta llegan en la respuesta del API, pero NO
// deben mostrarse en la interfaz del cliente.
export interface ClienteTreatmentDrug {
  id: number;
  drugId: number;
  drugName: string;
  units: number;
  unitPurchasePrice: number;
  unitSalePrice: number;
}

// Tratamiento tal como lo devuelve el API (TreatmentResponse): el dueño lo ve
// en el detalle de su mascota.
export interface ClienteTreatment {
  id: number;
  date: string;
  petId: number;
  petName: string;
  veterinarianId: number;
  veterinarianName: string;
  diagnosis: string;
  // 'RECOVERED' marca un alta: la mascota se recuperó y no recibió medicamentos.
  evolution: string | null;
  drugs: ClienteTreatmentDrug[];
}
