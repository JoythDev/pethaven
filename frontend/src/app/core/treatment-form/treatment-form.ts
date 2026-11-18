import { ValidationErrors } from '@angular/forms';

import { Drug } from '../../models/drug.model';
import { PET_WEIGHT_MAX, PET_WEIGHT_MIN } from '../../models/pet.model';
import { Evolution } from '../../models/treatment.model';
import { prescriptionTotal } from '../pet-detail/pet-detail';

// Lógica pura del formulario de tratamiento: reglas de evolución, existencias, receta y mensajes.

/** Máximos que valida el API. */
export const DIAGNOSIS_MAX = 200;
export const NOTES_MAX = 1000;

const number = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 1 });
const integer = new Intl.NumberFormat('es-CO');

// ---------- Evolución ----------

export interface EvolutionRule {
  // La mascota no tiene diagnóstico: es su primer registro y no hay con qué comparar.
  admission: boolean;
  evolutionRequired: boolean;
  // «Estable» significa que sigue con el mismo diagnóstico: se fija el actual.
  diagnosisLocked: boolean;
}

export function evolutionRule(currentDiagnosis: string | null, evolution: Evolution | null): EvolutionRule {
  const admission = currentDiagnosis === null;
  return {
    admission,
    evolutionRequired: !admission,
    diagnosisLocked: !admission && evolution === 'STABLE',
  };
}

export const requestEvolution = (rule: EvolutionRule, evolution: Evolution | null): Evolution | null =>
  rule.admission ? null : evolution;

// ---------- Existencias y receta ----------

export interface StockCheck {
  exceeds: boolean;
  // Unidades que quedarían en inventario tras la receta (nunca negativas).
  remaining: number;
  // Unidades que faltan para completar lo pedido.
  shortfall: number;
}

export function stockCheck(available: number, units: number): StockCheck {
  const shortfall = Math.max(0, units - available);
  return { exceeds: shortfall > 0, remaining: Math.max(0, available - units), shortfall };
}

export interface PrescriptionRow {
  drug: Drug;
  units: number;
  unitPrice: number;
  subtotal: number;
  check: StockCheck;
}

export interface PrescriptionLine {
  drugId: number | null;
  units: number | null;
}

export interface Prescription {
  // Una entrada por línea del formulario; null si la línea aún no tiene medicamento.
  rows: (PrescriptionRow | null)[];
  // El veterinario ve precio de venta, nunca el de compra (11.10).
  total: number;
  hasExcess: boolean;
}

export function buildPrescription(lines: PrescriptionLine[], drugs: Map<number, Drug>): Prescription {
  const rows = lines.map((line): PrescriptionRow | null => {
    const drug = line.drugId === null ? undefined : drugs.get(line.drugId);
    if (!drug) {
      return null;
    }
    const units = line.units ?? 0;
    return {
      drug,
      units,
      unitPrice: drug.salePrice,
      subtotal: units * drug.salePrice,
      check: stockCheck(drug.unitsAvailable, units),
    };
  });
  const present = rows.filter((row): row is PrescriptionRow => row !== null);
  return {
    rows,
    total: prescriptionTotal(present.map((row) => ({ units: row.units, unitSalePrice: row.unitPrice }))),
    hasExcess: present.some((row) => row.check.exceeds),
  };
}

// ---------- Mensajes ----------

export type FieldKey = 'weight' | 'diagnosis' | 'evolution' | 'notes' | 'drug' | 'units';

type Errors = ValidationErrors | null | undefined;

const has = (errors: Errors, key: string): boolean => !!errors && key in errors;

const actualLength = (errors: Errors): number =>
  Number((errors?.['maxlength'] as { actualLength?: number } | undefined)?.actualLength ?? 0);

/** Mensaje del primer error del campo (qué pasa y cómo corregirlo, 11.4), o null si no hay errores. */
export function fieldError(field: FieldKey, errors: Errors): string | null {
  if (!errors || Object.keys(errors).length === 0) {
    return null;
  }
  switch (field) {
    case 'weight':
      return `Escribe el peso en kg, entre ${number.format(PET_WEIGHT_MIN)} y ${number.format(PET_WEIGHT_MAX)}. Por ejemplo, 11,2.`;
    case 'diagnosis':
      return has(errors, 'maxlength')
        ? `El diagnóstico tiene ${integer.format(actualLength(errors))} caracteres: resúmelo en ${DIAGNOSIS_MAX} o menos.`
        : 'Escribe el diagnóstico de la consulta, por ejemplo «Otitis externa».';
    case 'evolution':
      return 'Elige cómo sigue la mascota: mejorando, estable o empeorando.';
    case 'notes':
      return `Las observaciones tienen ${integer.format(actualLength(errors))} caracteres: acórtalas a ${integer.format(NOTES_MAX)} o menos.`;
    case 'drug':
      return 'Elige un medicamento de la lista.';
    case 'units': {
      const exceeds = errors['exceedsStock'] as { available: number } | undefined;
      return exceeds
        ? `Solo hay ${integer.format(exceeds.available)} unidades disponibles. Baja las unidades a ${integer.format(exceeds.available)} o menos.`
        : 'Escribe cuántas unidades se administran: un número entero de 1 o más.';
    }
  }
}
