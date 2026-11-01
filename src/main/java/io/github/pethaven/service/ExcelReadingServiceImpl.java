package io.github.pethaven.service;

import io.github.pethaven.dto.request.DrugRequest;
import io.github.pethaven.exception.InvalidExcelFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExcelReadingServiceImpl implements ExcelReadingService {

    // Índice de la columna A: NOMBRE
    private static final int NAME_COLUMN = 0;
    // Índice de la columna B: PRECIO VENTA
    private static final int SALE_PRICE_COLUMN = 1;
    // Índice de la columna C: PRECIO COMPRA
    private static final int PURCHASE_PRICE_COLUMN = 2;
    // Índice de la columna D: UNIDADES DISPONIBLES
    private static final int UNITS_AVAILABLE_COLUMN = 3;
    // Índice de la columna E: UNIDADES VENDIDAS (se ignora, pero se usa para detectar filas vacías)
    private static final int UNITS_SOLD_COLUMN = 4;
    // Última columna que se inspecciona en los encabezados (A-D)
    private static final int LAST_HEADER_COLUMN = UNITS_AVAILABLE_COLUMN;
    // Primer índice de fila con datos (la fila 0 contiene los encabezados)
    private static final int FIRST_DATA_ROW = 1;

    private final Validator validator;

    @Autowired
    public ExcelReadingServiceImpl(Validator validator) {
        this.validator = validator;
    }

    @Override
    public ExcelReadingResult readDrugsFromExcel(InputStream inputStream) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new InvalidExcelFormatException("El archivo Excel no contiene ninguna hoja.");
            }

            Sheet sheet = workbook.getSheetAt(0);
            validateHeaderRow(sheet);

            List<DrugRequest> requests = new ArrayList<>();
            int skippedBlankRows = 0;

            for (int i = FIRST_DATA_ROW; i <= sheet.getLastRowNum(); i++) {
                int excelRow = i + 1;
                Row row = sheet.getRow(i);

                // Las filas con formato pero sin contenido se ignoran.
                if (isBlankRow(row)) {
                    skippedBlankRows++;
                    continue;
                }

                String name = requireCell(row, NAME_COLUMN, CellType.STRING, "el nombre, como texto", excelRow)
                        .getStringCellValue();
                BigDecimal salePrice = BigDecimal.valueOf(
                        requireCell(row, SALE_PRICE_COLUMN, CellType.NUMERIC, "el precio de venta, como número", excelRow)
                                .getNumericCellValue());
                BigDecimal purchasePrice = BigDecimal.valueOf(
                        requireCell(row, PURCHASE_PRICE_COLUMN, CellType.NUMERIC, "el precio de compra, como número", excelRow)
                                .getNumericCellValue());
                Integer unitsAvailable = readUnitsAvailable(row, excelRow);

                DrugRequest request = new DrugRequest(name, purchasePrice, salePrice, unitsAvailable);

                // Validación de las reglas de negocio del DTO.
                Set<ConstraintViolation<DrugRequest>> violations = validator.validate(request);
                if (!violations.isEmpty()) {
                    String details = violations.stream()
                            .map(ConstraintViolation::getMessage)
                            .collect(Collectors.joining("; "));
                    throw InvalidExcelFormatException.invalidRow(excelRow, details);
                }

                requests.add(request);
            }

            if (requests.isEmpty()) {
                throw InvalidExcelFormatException.noDataRows(sheet.getSheetName());
            }

            return new ExcelReadingResult(sheet.getSheetName(), requests, skippedBlankRows);
        }
    }

    /**
     * Rechaza una primera fila con datos de medicamentos en lugar de encabezados.
     * No valida los textos ni el orden de los encabezados.
     */
    private void validateHeaderRow(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            return;
        }

        for (int column = NAME_COLUMN; column <= LAST_HEADER_COLUMN; column++) {
            Cell cell = headerRow.getCell(column);
            if (cell != null && !isHeaderCell(cell)) {
                throw new InvalidExcelFormatException(
                        "La primera fila debe contener los encabezados (texto), no datos de medicamentos.");
            }
        }
    }

    private boolean isHeaderCell(Cell cell) {
        CellType type = cell.getCellType();
        return switch (type) {
            case STRING, BLANK, _NONE -> true;
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.STRING;
            default -> false;
        };
    }

    /**
     * Una fila es completamente vacía cuando no hay contenido en las columnas A-E.
     */
    private boolean isBlankRow(Row row) {
        if (row == null) {
            return true;
        }
        for (int column = NAME_COLUMN; column <= UNITS_SOLD_COLUMN; column++) {
            if (!isBlankCell(row.getCell(column))) {
                return false;
            }
        }
        return true;
    }

    private boolean isBlankCell(Cell cell) {
        if (cell == null) {
            return true;
        }

        CellType type = cell.getCellType();
        if (type == CellType.BLANK || type == CellType._NONE) {
            return true;
        }
        if (type == CellType.STRING) {
            return cell.getStringCellValue().isBlank();
        }
        if (type == CellType.FORMULA) {
            CellType cachedType = cell.getCachedFormulaResultType();
            if (cachedType == CellType.BLANK || cachedType == CellType._NONE) {
                return true;
            }
            if (cachedType == CellType.STRING) {
                return cell.getStringCellValue().isBlank();
            }
        }
        return false;
    }

    private Integer readUnitsAvailable(Row row, int excelRow) {
        Cell cell = requireCell(row, UNITS_AVAILABLE_COLUMN, CellType.NUMERIC,
                "las unidades disponibles, como número entero", excelRow);
        BigDecimal value = BigDecimal.valueOf(cell.getNumericCellValue());
        try {
            return value.intValueExact();
        } catch (ArithmeticException e) {
            throw InvalidExcelFormatException.invalidValue(excelRow, 'D', "un número entero", value.toPlainString());
        }
    }

    private Cell requireCell(Row row, int columnIndex, CellType expectedType, String expectedDescription, int excelRow) {
        char letter = (char) ('A' + columnIndex);
        Cell cell = row.getCell(columnIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            throw InvalidExcelFormatException.emptyCell(excelRow, letter, expectedDescription);
        }
        if (cell.getCellType() != expectedType) {
            throw InvalidExcelFormatException.wrongType(excelRow, letter, expectedDescription, cell.getCellType());
        }
        return cell;
    }

}
