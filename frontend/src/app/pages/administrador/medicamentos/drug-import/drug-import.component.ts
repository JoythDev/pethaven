import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { EMPTY, Observable, Subject, catchError, exhaustMap, finalize, tap } from 'rxjs';
import { DrugImportSummary } from '../../../../models/drug.model';
import { DrugService } from '../../../../services/drug.service';
import { getErrorMessage } from '../../../../core/http/http-error';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { ADMIN_DRUG_AREA } from '../../../shared/drugs/drug-area';
import { formatUnits } from '../../../shared/drugs/drug-stock';

// Mismo límite que spring.servlet.multipart.max-file-size (10 MB).
export const MAX_IMPORT_FILE_BYTES = 10 * 1024 * 1024;

// Extensiones que acepta el API (DrugImportServiceImpl valida la extensión y la firma del archivo).
const ACCEPTED_EXTENSIONS = ['.xlsx', '.xls'];

// Plantilla estática servida desde public/: solo encabezados en la primera hoja.
export const DRUG_IMPORT_TEMPLATE_URL = '/templates/plantilla-medicamentos.xlsx';

@Component({
  selector: 'app-drug-import',
  imports: [RouterLink, ButtonModule, MessageModule],
  templateUrl: './drug-import.component.html',
})
export class DrugImportComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly drugService = inject(DrugService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly basePath = ADMIN_DRUG_AREA.basePath;
  readonly templateUrl = DRUG_IMPORT_TEMPLATE_URL;
  readonly acceptAttribute = ACCEPTED_EXTENSIONS.join(',');
  readonly formatUnits = formatUnits;

  readonly selectedFile = signal<File | null>(null);
  // Error de validación local del archivo (extensión o tamaño), antes de llamar al API.
  readonly fileError = signal<string | null>(null);
  // Mensaje del API cuando la importación falla (400, 409, 413, 415...).
  readonly importError = signal<string | null>(null);
  readonly result = signal<DrugImportSummary | null>(null);
  readonly uploading = signal(false);
  readonly dragging = signal(false);

  // Cada emisión es un archivo validado que debe enviarse al API.
  private readonly upload$ = new Subject<File>();

  ngOnInit(): void {
    // exhaustMap ignora nuevos envíos mientras una importación sigue en curso.
    this.upload$
      .pipe(
        exhaustMap((file) => this.upload(file)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((summary) => {
        this.result.set(summary);
        this.notifications.success(
          'Importación completada',
          `Se importaron ${formatUnits(summary.importedCount)} medicamentos.`,
        );
      });
  }

  onFileInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectFile(input.files?.item(0) ?? null);
    // Permite volver a elegir el mismo archivo después de corregirlo.
    input.value = '';
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  onDragLeave(): void {
    this.dragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    this.selectFile(event.dataTransfer?.files.item(0) ?? null);
  }

  quitarArchivo(): void {
    this.selectedFile.set(null);
    this.fileError.set(null);
    this.importError.set(null);
  }

  importar(): void {
    const file = this.selectedFile();
    if (file && !this.fileError()) {
      this.upload$.next(file);
    }
  }

  importarOtro(): void {
    this.result.set(null);
    this.quitarArchivo();
  }

  verMedicamentos(): void {
    this.router.navigate([this.basePath]);
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} B`;
    }
    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} KB`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  // Guarda el archivo elegido y valida extensión y tamaño para no enviar algo que el API rechazará.
  private selectFile(file: File | null): void {
    this.importError.set(null);
    this.result.set(null);
    this.selectedFile.set(file);
    this.fileError.set(file ? this.validateFile(file) : null);
  }

  private validateFile(file: File): string | null {
    const name = file.name.toLowerCase();
    if (!ACCEPTED_EXTENSIONS.some((extension) => name.endsWith(extension))) {
      return 'El archivo debe tener extensión .xlsx o .xls.';
    }
    if (file.size === 0) {
      return 'El archivo está vacío.';
    }
    if (file.size > MAX_IMPORT_FILE_BYTES) {
      return 'El archivo supera el tamaño máximo permitido de 10 MB.';
    }
    return null;
  }

  // Envía el archivo. Si el API lo rechaza muestra su mensaje (indica fila y columna del problema)
  // y termina sin emitir, para que el usuario corrija el archivo y lo vuelva a cargar.
  private upload(file: File): Observable<DrugImportSummary> {
    this.uploading.set(true);
    this.importError.set(null);

    return this.drugService.importExcel(file).pipe(
      tap(() => this.selectedFile.set(null)),
      catchError((error: unknown) => {
        this.importError.set(getErrorMessage(error, 'No se pudo importar el archivo.'));
        return EMPTY;
      }),
      finalize(() => this.uploading.set(false)),
    );
  }
}
