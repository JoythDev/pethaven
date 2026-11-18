import { DrugImportSummary } from '../../models/drug.model';

// Mismo límite que spring.servlet.multipart.max-file-size (10 MB).
export const MAX_IMPORT_FILE_BYTES = 10 * 1024 * 1024;

// El API valida la extensión y la firma del archivo (DrugImportServiceImpl): acepta ambas.
export const IMPORT_ACCEPT = '.xlsx,.xls';
const ACCEPTED_EXTENSIONS = IMPORT_ACCEPT.split(',');

// Plantilla estática servida desde public/: encabezados en la primera hoja e instrucciones en la segunda.
export const DRUG_IMPORT_TEMPLATE_URL = '/templates/plantilla-medicamentos.xlsx';
export const DRUG_IMPORT_TEMPLATE_NAME = 'plantilla-medicamentos.xlsx';

/** Columna del archivo, en el orden que lee ExcelReadingServiceImpl (el texto del encabezado no se valida). */
export interface ImportColumn {
  letter: string;
  header: string;
  required: boolean;
  /** Regla que aplica el API a los valores de esa columna. */
  rule: string;
}

export const IMPORT_COLUMNS: readonly ImportColumn[] = [
  { letter: 'A', header: 'NOMBRE', required: true, rule: 'Texto, máximo 120 caracteres. No puede repetirse.' },
  {
    letter: 'B',
    header: 'PRECIO VENTA',
    required: true,
    rule: 'Número no negativo, hasta 10 enteros y 2 decimales.',
  },
  {
    letter: 'C',
    header: 'PRECIO COMPRA',
    required: true,
    rule: 'Número no negativo, hasta 10 enteros y 2 decimales.',
  },
  { letter: 'D', header: 'UNIDADES DISPONIBLES', required: true, rule: 'Número entero, cero o más.' },
  {
    letter: 'E',
    header: 'UNIDADES VENDIDAS',
    required: false,
    rule: 'Opcional: se ignora; las unidades vendidas empiezan en 0.',
  },
];

// Mismas filas que el ejemplo del formato anterior (valores solo ilustrativos).
export const IMPORT_EXAMPLE_ROWS: readonly (readonly string[])[] = [
  ['Amoxicilina 250mg', '15000', '8000', '200', '—'],
  ['Ivermectina 1%', '32000.5', '21500', '45', '—'],
];

/** Mensaje de campo para un archivo que el API rechazaría; null si es válido. */
export function validateImportFile(file: { name: string; size: number }): string | null {
  const name = file.name.toLowerCase();
  if (!ACCEPTED_EXTENSIONS.some((extension) => name.endsWith(extension))) {
    return 'El archivo debe ser un Excel (.xlsx o .xls).';
  }
  if (file.size === 0) {
    return 'El archivo está vacío.';
  }
  if (file.size > MAX_IMPORT_FILE_BYTES) {
    return 'El archivo supera el tamaño máximo de 10 MB.';
  }
  return null;
}

const decimal = new Intl.NumberFormat('es-CO', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const integer = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0 });

/** Tamaño legible: B, KB o MB con coma decimal. */
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${decimal.format(bytes / 1024)} KB`;
  }
  return `${decimal.format(bytes / (1024 * 1024))} MB`;
}

export interface ImportErrorDetail {
  message: string;
  row: number | null;
  column: string | null;
  cell: string | null;
}

// «La celda B5 (fila 5, columna B) …» (InvalidExcelFormatException: celda vacía, tipo o valor).
const CELL_PATTERN = /celda ([A-Z])(\d+) \(fila (\d+), columna ([A-Z])\)/;
// «La fila 12 tiene datos inválidos …» y «La fila 12 está vacía …».
const ROW_PATTERN = /La fila (\d+) /;

/** Separa la fila y la columna que el API menciona en su mensaje; el mensaje completo se conserva. */
export function parseImportError(message: string): ImportErrorDetail {
  const cell = CELL_PATTERN.exec(message);
  if (cell) {
    return { message, row: Number(cell[3]), column: cell[4], cell: `${cell[1]}${cell[2]}` };
  }
  const row = ROW_PATTERN.exec(message);
  return { message, row: row ? Number(row[1]) : null, column: null, cell: null };
}

/** Filas del resultado (paso 3), en el orden del diseño. */
export function importSummaryItems(summary: DrugImportSummary): { term: string; value: string }[] {
  return [
    { term: 'Archivo', value: summary.fileName },
    { term: 'Hoja', value: summary.sheetName },
    { term: 'Filas leídas', value: integer.format(summary.totalRows) },
    { term: 'Medicamentos importados', value: integer.format(summary.importedCount) },
    { term: 'Filas en blanco omitidas', value: integer.format(summary.skippedBlankRows) },
  ];
}
