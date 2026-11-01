package io.github.pethaven.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resumen de una importación masiva de medicamentos desde Excel.
 *
 * @param fileName         nombre del archivo importado
 * @param sheetName        nombre de la hoja leída
 * @param totalRows        cantidad de filas de datos válidas procesadas
 * @param importedCount    cantidad de medicamentos importados
 * @param skippedBlankRows cantidad de filas vacías ignoradas
 */
@Schema(description = "Resumen de la importación de medicamentos desde Excel")
public record DrugImportSummaryResponse(
        @Schema(description = "Nombre del archivo importado", example = "medicamentos-veterinaria.xlsx")
        String fileName,

        @Schema(description = "Nombre de la hoja leída", example = "Medicamentos")
        String sheetName,

        @Schema(description = "Cantidad de filas de datos válidas procesadas", example = "500")
        int totalRows,

        @Schema(description = "Cantidad de medicamentos importados", example = "500")
        int importedCount,

        @Schema(description = "Cantidad de filas vacías ignoradas", example = "515")
        int skippedBlankRows
) { }
