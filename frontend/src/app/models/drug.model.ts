// Medicamento tal como lo devuelve el API (DrugResponse): unitsSold lo calcula el sistema.
export interface Drug {
  id: number;
  name: string;
  purchasePrice: number;
  salePrice: number;
  unitsAvailable: number;
  unitsSold: number;
}

// Cuerpo para registrar o actualizar un medicamento (DrugRequest): unitsSold no se envía.
export interface DrugRequest {
  name: string;
  purchasePrice: number;
  salePrice: number;
  unitsAvailable: number;
}
