import { DrugRequest } from '../../models/drug.model';

// Límites de DrugRequest en el API: nombre de 120 caracteres, precios con 10 enteros y 2 decimales
// y unidades como Integer de Java.
export const DRUG_LIMITS = {
  nameMaxLength: 120,
  priceMax: 9_999_999_999.99,
  unitsMax: 2_147_483_647,
} as const;

export type DrugFormField = 'name' | 'purchasePrice' | 'salePrice' | 'unitsAvailable';

/** Valor del formulario: los números empiezan en null para que el campo se vea vacío. */
export interface DrugFormValue {
  name: string;
  purchasePrice: number | null;
  salePrice: number | null;
  unitsAvailable: number | null;
}

type ErrorMap = Record<string, unknown> | null | undefined;

/** Cuerpo del API a partir del formulario: nombre recortado; un número vacío no llega (el formulario lo bloquea). */
export function toDrugRequest(value: DrugFormValue): DrugRequest {
  return {
    name: value.name.trim(),
    purchasePrice: value.purchasePrice ?? 0,
    salePrice: value.salePrice ?? 0,
    unitsAvailable: value.unitsAvailable ?? 0,
  };
}

/** Mensaje de error de un campo: qué pasa y cómo corregirlo (11.4). Null si no hay error. */
export function drugFieldError(field: DrugFormField, errors: ErrorMap): string | null {
  if (!errors || Object.keys(errors).length === 0) {
    return null;
  }
  if (typeof errors['server'] === 'string') {
    return errors['server'];
  }
  const has = (key: string): boolean => key in errors;

  switch (field) {
    case 'name':
      return has('maxlength')
        ? `Usa ${DRUG_LIMITS.nameMaxLength} caracteres o menos en el nombre.`
        : 'Escribe el nombre del medicamento, por ejemplo Amoxicilina 250 mg.';
    case 'purchasePrice':
      return priceError('de compra', errors);
    case 'salePrice':
      return priceError('de venta', errors);
    case 'unitsAvailable':
      if (has('max')) return 'Escribe un número menor a 2.147.483.647.';
      if (has('min')) return 'Las unidades no pueden ser negativas. Escribe 0 o más.';
      return 'Escribe las unidades disponibles, por ejemplo 40. Usa 0 si no hay.';
  }
}

function priceError(kind: 'de compra' | 'de venta', errors: Record<string, unknown>): string {
  if ('max' in errors) return `El precio ${kind} es demasiado alto. Usa máximo 10 dígitos enteros.`;
  if ('min' in errors) return `El precio ${kind} no puede ser negativo. Escribe 0 o más.`;
  return `Escribe el precio ${kind}, por ejemplo 8500.`;
}

const PRICE_WARNING =
  'El precio de venta es menor que el de compra: cada unidad vendida dejará pérdida. Puedes guardarlo así.';

/** Aviso (no bloquea) cuando se vendería por debajo del costo. Sin aviso si falta un precio o alguno es inválido. */
export function priceWarning(purchase: number | null, sale: number | null): string | null {
  const valid = (n: number | null): n is number => n !== null && Number.isFinite(n) && n >= 0;
  return valid(purchase) && valid(sale) && sale < purchase ? PRICE_WARNING : null;
}
