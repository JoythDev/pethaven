import { HttpErrorResponse } from '@angular/common/http';
import { formatNumber } from '@angular/common';
import { Observable, catchError, map, of, startWith } from 'rxjs';

import { Owner } from '../../models/owner.model';
import { VaccineStatus } from '../../models/demo/vaccine.model';
import { Pet } from '../../models/pet.model';
import { Evolution, Treatment, TreatmentDrug } from '../../models/treatment.model';
import { ViewState } from '../http/view-state';
import { getErrorMessage } from '../http/http-error';
import { TagSeverity } from '../status/pet-status';

// Lógica pura de la ficha de mascota del veterinario: pestañas en la URL, historia clínica en línea
// de tiempo, «Mostrar N más» y dinero de la receta (11.10: el veterinario ve precio de venta, nunca el de compra; el administrador ve además costo y ganancia).

export type DetailTab = 'resumen' | 'historia' | 'vacunas';

export const TAB_PARAM = 'pestana';
export const TREATMENT_PARAM = 'tratamiento';
/** Tratamientos que se ven antes de «Mostrar N más» (S23). */
export const HISTORY_LIMIT = 6;

const TABS: readonly DetailTab[] = ['resumen', 'historia', 'vacunas'];

export function parseTab(value: string | null): DetailTab {
  return TABS.find((tab) => tab === value) ?? 'resumen';
}

/** Valor de `?pestana`: la pestaña por defecto no se escribe en la URL. */
export function tabParam(tab: DetailTab): string | null {
  return tab === 'resumen' ? null : tab;
}

export const lineSubtotal = (drug: Pick<TreatmentDrug, 'units' | 'unitSalePrice'>): number =>
  drug.units * drug.unitSalePrice;

export const prescriptionTotal = (drugs: Pick<TreatmentDrug, 'units' | 'unitSalePrice'>[]): number =>
  drugs.reduce((sum, drug) => sum + lineSubtotal(drug), 0);

// Vista del administrador (11.10): costo de compra de la receta y ganancia sobre el precio de venta.
export const lineCost = (drug: Pick<TreatmentDrug, 'units' | 'unitPurchasePrice'>): number =>
  drug.units * drug.unitPurchasePrice;

export const prescriptionCost = (drugs: Pick<TreatmentDrug, 'units' | 'unitPurchasePrice'>[]): number =>
  drugs.reduce((sum, drug) => sum + lineCost(drug), 0);

export const prescriptionProfit = (
  drugs: Pick<TreatmentDrug, 'units' | 'unitSalePrice' | 'unitPurchasePrice'>[],
): number => prescriptionTotal(drugs) - prescriptionCost(drugs);

/** La más reciente primero; a igual fecha, el id mayor (el registrado después). */
export function sortNewestFirst(treatments: Treatment[]): Treatment[] {
  return [...treatments].sort((a, b) => b.date.localeCompare(a.date) || b.id - a.id);
}

export interface YearGroup {
  year: number;
  items: Treatment[];
}

/** Agrupa por año tratamientos ya ordenados, conservando su orden. */
export function groupByYear(sorted: Treatment[]): YearGroup[] {
  const groups: YearGroup[] = [];
  for (const treatment of sorted) {
    const year = new Date(treatment.date).getFullYear();
    const last = groups[groups.length - 1];
    if (last?.year === year) {
      last.items.push(treatment);
    } else {
      groups.push({ year, items: [treatment] });
    }
  }
  return groups;
}

/** Los seis más recientes o todos; una selección que quedaría oculta también muestra todos. */
export function visibleTreatments(sorted: Treatment[], expanded: boolean, selectedId: number | null): Treatment[] {
  const selectedIndex = selectedId === null ? -1 : sorted.findIndex((t) => t.id === selectedId);
  return expanded || selectedIndex >= HISTORY_LIMIT ? sorted : sorted.slice(0, HISTORY_LIMIT);
}

export const hiddenCount = (sorted: Treatment[]): number => Math.max(0, sorted.length - HISTORY_LIMIT);

export const toggleLabel = (hidden: number, expanded: boolean): string =>
  expanded ? 'Mostrar menos' : `Mostrar ${hidden} más`;

/** El tratamiento pedido en la URL; sin él (o si ya no existe) el más reciente. */
export function resolveSelected(sorted: Treatment[], id: number | null): Treatment | null {
  return sorted.find((t) => t.id === id) ?? sorted[0] ?? null;
}

export interface VisitSummary {
  count: number;
  lastDate: string | null;
}

export const visitSummary = (sorted: Treatment[]): VisitSummary => ({
  count: sorted.length,
  lastDate: sorted[0]?.date ?? null,
});

// Consulta con la variación de peso respecto a la anterior (null en la primera).
export type Visit = Treatment & { weightDelta: number | null };

// Datos de la historia clínica en PDF: mascota, dueño y consultas en orden cronológico.
export interface ClinicalHistory {
  pet: Pet;
  owner: Owner;
  visits: Visit[];
}

// Ordena de la más antigua a la más reciente (fechas ISO) y calcula el cambio de peso.
export const toVisits = (treatments: Treatment[]): Visit[] =>
  [...treatments]
    .sort((a, b) => a.date.localeCompare(b.date))
    .map((t, i, sorted) => ({ ...t, weightDelta: i === 0 ? null : t.weight - sorted[i - 1].weight }));

/** Variante de etiqueta y color del marcador de cada evolución (12.3). */
export function evolutionSeverity(evolution: Evolution | null): TagSeverity {
  switch (evolution) {
    case 'IMPROVING':
      return 'info';
    case 'WORSENING':
      return 'danger';
    case 'RECOVERED':
      return 'success';
    default:
      return 'secondary';
  }
}

/** Variante de la etiqueta de una vacuna (12.3): Vigente, Vence en N días y Vencida. */
export const vaccineSeverity = (status: VaccineStatus): TagSeverity =>
  status === 'ok' ? 'success' : status === 'soon' ? 'warn' : 'danger';

/** «4,3 kg» (12.2: decimal con coma). */
export const formatKg = (value: number): string => `${formatNumber(value, 'es-CO', '1.0-2')} kg`;

/** «4 años»; 0 es una mascota de menos de un año. */
export function ageLabel(age: number): string {
  if (age < 1) {
    return 'Menos de 1 año';
  }
  return age === 1 ? '1 año' : `${age} años`;
}

// La mascota se pide sola para distinguir «no existe» (404) de un fallo que se puede reintentar.
export type PetLoad = ViewState<Pet> | { status: 'not-found' };

export const NOT_FOUND: PetLoad = { status: 'not-found' };

export const toPetLoad = () => (source: Observable<Pet>): Observable<PetLoad> =>
  source.pipe(
    map((data): PetLoad => ({ status: 'ready', data })),
    catchError((error: unknown) =>
      of<PetLoad>(
        error instanceof HttpErrorResponse && error.status === 404
          ? NOT_FOUND
          : { status: 'error', message: getErrorMessage(error) },
      ),
    ),
    startWith<PetLoad>({ status: 'loading' }),
  );
