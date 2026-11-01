package io.github.pethaven.service;

import io.github.pethaven.dto.request.DrugRequest;

import java.util.List;

/**
 * Resultado de la lectura de un archivo Excel de medicamentos.
 *
 * @param sheetName       nombre de la hoja leída
 * @param drugs           medicamentos válidos encontrados en la hoja
 * @param skippedBlankRows cantidad de filas completamente vacías ignoradas
 */
public record ExcelReadingResult(
        String sheetName,
        List<DrugRequest> drugs,
        int skippedBlankRows
) { }
