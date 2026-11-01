package io.github.pethaven.service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Lectura de medicamentos desde un archivo Excel.
 */
public interface ExcelReadingService {

    /**
     * Lee y valida las filas de medicamentos de la primera hoja del archivo.
     * No persiste ningún dato.
     *
     * @param inputStream contenido del archivo Excel
     * @return nombre de la hoja, medicamentos leídos y filas vacías ignoradas
     * @throws IOException si el archivo no se puede leer
     */
    ExcelReadingResult readDrugsFromExcel(InputStream inputStream) throws IOException;
}
