import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import {
  LucideCircleCheck,
  LucideDownload,
  LucideFileSpreadsheet,
  LucideTriangleAlert,
  LucideUpload,
  LucideX,
} from '@lucide/angular';
import { ButtonModule } from 'primeng/button';
import { EMPTY, Observable, Subject, catchError, exhaustMap, finalize, tap } from 'rxjs';

import {
  DRUG_IMPORT_TEMPLATE_NAME,
  DRUG_IMPORT_TEMPLATE_URL,
  IMPORT_ACCEPT,
  IMPORT_COLUMNS,
  IMPORT_EXAMPLE_ROWS,
  MAX_IMPORT_FILE_BYTES,
  formatFileSize,
  importSummaryItems,
  parseImportError,
  validateImportFile,
} from '../../../../core/drugs/drug-import';
import { getErrorMessage } from '../../../../core/http/http-error';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { DrugImportSummary } from '../../../../models/drug.model';
import { DrugService } from '../../../../services/drug.service';
import { BlockComponent } from '../../../shared/block/block.component';
import { CardComponent } from '../../../shared/card/card.component';
import { DefinitionListComponent } from '../../../shared/definition-list/definition-list.component';
import { ADMIN_DRUG_AREA } from '../../../shared/drugs/drug-area';
import { formatUnits } from '../../../shared/drugs/drug-stock';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { StickerComponent } from '../../../shared/sticker/sticker.component';

const NO_FILE_MESSAGE = 'Elige un archivo de Excel para importar.';

/**
 * Importación masiva de medicamentos desde Excel (11.14): tres pasos (preparar, subir, resultado) y
 * una columna lateral con los requisitos. El formato, los límites y los mensajes de error salen del
 * API (`core/drugs/drug-import`); la importación es todo o nada.
 */
@Component({
  selector: 'app-drug-import',
  imports: [
    RouterLink,
    ButtonModule,
    LucideCircleCheck,
    LucideDownload,
    LucideFileSpreadsheet,
    LucideTriangleAlert,
    LucideUpload,
    LucideX,
    BlockComponent,
    CardComponent,
    DefinitionListComponent,
    PageHeaderComponent,
    StickerComponent,
  ],
  templateUrl: './drug-import.component.html',
})
export class DrugImportComponent implements OnInit {
  private readonly drugService = inject(DrugService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly basePath = ADMIN_DRUG_AREA.basePath;
  protected readonly templateUrl = DRUG_IMPORT_TEMPLATE_URL;
  protected readonly templateName = DRUG_IMPORT_TEMPLATE_NAME;
  protected readonly accept = IMPORT_ACCEPT;
  protected readonly columns = IMPORT_COLUMNS;
  protected readonly exampleRows = IMPORT_EXAMPLE_ROWS;
  protected readonly maxMegabytes = MAX_IMPORT_FILE_BYTES / (1024 * 1024);
  protected readonly formatFileSize = formatFileSize;

  protected readonly selectedFile = signal<File | null>(null);
  // Mensaje de campo del archivo (tipo, tamaño o falta de archivo), antes de llamar al API.
  protected readonly fileError = signal<string | null>(null);
  // Mensaje del API cuando la importación falla (400, 409, 413, 415...).
  protected readonly importError = signal<string | null>(null);
  protected readonly result = signal<DrugImportSummary | null>(null);
  protected readonly uploading = signal(false);
  protected readonly dragging = signal(false);

  protected readonly errorDetail = computed(() => {
    const message = this.importError();
    return message ? parseImportError(message) : null;
  });
  protected readonly summaryItems = computed(() => {
    const summary = this.result();
    return summary ? importSummaryItems(summary) : [];
  });

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

  protected onFileInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectFile(input.files?.item(0) ?? null);
    // Permite volver a elegir el mismo archivo después de corregirlo.
    input.value = '';
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  protected onDragLeave(): void {
    this.dragging.set(false);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    this.selectFile(event.dataTransfer?.files.item(0) ?? null);
  }

  protected quitarArchivo(): void {
    this.selectedFile.set(null);
    this.fileError.set(null);
    this.importError.set(null);
  }

  protected importar(): void {
    const file = this.selectedFile();
    if (!file) {
      this.fileError.set(NO_FILE_MESSAGE);
      return;
    }
    if (!this.fileError()) {
      this.upload$.next(file);
    }
  }

  protected importarOtro(): void {
    this.result.set(null);
    this.quitarArchivo();
  }

  // Guarda el archivo elegido y lo valida para no enviar algo que el API rechazará.
  private selectFile(file: File | null): void {
    this.importError.set(null);
    this.result.set(null);
    this.selectedFile.set(file);
    this.fileError.set(file ? validateImportFile(file) : null);
  }

  // Envía el archivo. Si el API lo rechaza muestra su mensaje (indica fila y columna del problema)
  // y termina sin emitir, para que la persona corrija el archivo y lo vuelva a cargar.
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
