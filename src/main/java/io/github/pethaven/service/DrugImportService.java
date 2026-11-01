package io.github.pethaven.service;

import io.github.pethaven.dto.response.DrugImportSummaryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Importación masiva de medicamentos desde un archivo Excel.
 */
public interface DrugImportService {

    /**
     * Importa medicamentos desde un archivo multipart.
     *
     * @param file archivo Excel (.xlsx/.xls)
     * @return resumen de la importación
     */
    DrugImportSummaryResponse importFromExcel(MultipartFile file);

    /**
     * Importa medicamentos desde un stream. El llamador conserva la responsabilidad de cerrarlo.
     *
     * @param inputStream contenido del archivo Excel
     * @param fileName    nombre lógico del archivo (se usa en el resumen y en los mensajes)
     * @return resumen de la importación
     */
    DrugImportSummaryResponse importFromExcel(InputStream inputStream, String fileName);
}
