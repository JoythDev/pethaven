package io.github.pethaven.exception;

import org.apache.poi.ss.usermodel.CellType;

public class InvalidExcelFormatException extends RuntimeException {

    public InvalidExcelFormatException(String message) {
        super(message);
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
