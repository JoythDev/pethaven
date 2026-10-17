package io.github.pethaven.exception;

import org.apache.poi.ss.usermodel.CellType;

public class InvalidExcelFormatException extends RuntimeException {

    public InvalidExcelFormatException(String message) {
        super(message);
    }

    public InvalidExcelFormatException(int row, char column, CellType type, String value) {
        this(String.format(
                "Formato del archivo Excel inválido. La columna %c debe ser de tipo %s. El valor de la celda %c%d es inválido: '%s'",
                column, type, column, row, value
        ));
    }

}
