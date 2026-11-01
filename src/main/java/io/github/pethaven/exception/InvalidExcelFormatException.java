package io.github.pethaven.exception;

import org.apache.poi.ss.usermodel.CellType;

public class InvalidExcelFormatException extends RuntimeException {

    public InvalidExcelFormatException(String message) {
        super(message);
    }

    public InvalidExcelFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * El archivo no tiene contenido.
     */
    public static InvalidExcelFormatException emptyFile() {
        return new InvalidExcelFormatException(
                "El archivo Excel está vacío. Seleccione un archivo con datos de medicamentos.");
    }

    /**
     * La extensión del archivo no corresponde a un Excel.
     */
    public static InvalidExcelFormatException unsupportedExtension(String fileName) {
        return new InvalidExcelFormatException(String.format(
                "El archivo '%s' tiene una extensión no soportada. Use un archivo .xlsx o .xls.",
                fileName
        ));
    }

    /**
     * El contenido del archivo no corresponde a un formato Excel (XLSX u XLS).
     */
    public static InvalidExcelFormatException unsupportedFormat(String fileName) {
        return new InvalidExcelFormatException(String.format(
                "El archivo '%s' no es un archivo Excel válido. Use un archivo .xlsx o .xls.",
                fileName
        ));
    }

    /**
     * El archivo no se pudo leer (dañado, protegido o con un formato interno inválido).
     */
    public static InvalidExcelFormatException unreadable(String fileName, Throwable cause) {
        return new InvalidExcelFormatException(String.format(
                "No se pudo leer el archivo Excel '%s'. Verifique que no esté dañado ni protegido con contraseña.",
                fileName
        ), cause);
    }

    /**
     * La hoja no contiene filas de datos.
     */
    public static InvalidExcelFormatException noDataRows(String sheetName) {
        return new InvalidExcelFormatException(String.format(
                "El archivo Excel no contiene filas de datos en la hoja '%s'.",
                sheetName
        ));
    }

    /**
     * La celda esperada está vacía o no existe.
     */
    public static InvalidExcelFormatException emptyCell(int row, char column, String expectedDescription) {
        return new InvalidExcelFormatException(String.format(
                "Formato del archivo Excel inválido. La celda %c%d (fila %d, columna %c) está vacía. Se esperaba: %s.",
                column, row, row, column, expectedDescription
        ));
    }

    /**
     * La celda existe pero su tipo no es el esperado.
     */
    public static InvalidExcelFormatException wrongType(int row, char column, String expectedDescription, CellType actualType) {
        return new InvalidExcelFormatException(String.format(
                "Formato del archivo Excel inválido. La celda %c%d (fila %d, columna %c) debe contener %s, pero su tipo es %s.",
                column, row, row, column, expectedDescription, actualType
        ));
    }

    /**
     * La celda tiene el tipo esperado, pero su valor no es utilizable.
     */
    public static InvalidExcelFormatException invalidValue(int row, char column, String expectedDescription, String value) {
        return new InvalidExcelFormatException(String.format(
                "Formato del archivo Excel inválido. La celda %c%d (fila %d, columna %c) tiene un valor inválido: '%s'. Se esperaba: %s.",
                column, row, row, column, value, expectedDescription
        ));
    }

    /**
     * La fila no existe dentro de la hoja.
     */
    public static InvalidExcelFormatException emptyRow(int row) {
        return new InvalidExcelFormatException(String.format(
                "Formato del archivo Excel inválido. La fila %d está vacía (no contiene celdas).",
                row
        ));
    }

    /**
     * La fila no cumple las reglas de negocio del medicamento.
     */
    public static InvalidExcelFormatException invalidRow(int row, String details) {
        return new InvalidExcelFormatException(String.format(
                "Formato del archivo Excel inválido. La fila %d tiene datos inválidos: %s.",
                row, details
        ));
    }

}
