package io.github.pethaven.service;

import io.github.pethaven.dto.request.DrugRequest;
import io.github.pethaven.dto.response.DrugResponse;
import io.github.pethaven.exception.InvalidExcelFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
    // Primer índice de fila con datos (la fila 0 contiene los encabezados)
    private static final int FIRST_DATA_ROW = 1;

    private final DrugService drugService;
    private final Validator validator;

    public ExcelReadingServiceImpl(DrugService drugService, Validator validator) {
        this.drugService = drugService;
        this.validator = validator;
    }

    @Override
    public List<DrugResponse> loadDrugsFromExcel(InputStream inputStream) throws IOException {
        List<DrugRequest> requests = new ArrayList<>();

        // Primera pasada: leer y validar todas las filas antes de persistir nada.
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = FIRST_DATA_ROW; i <= sheet.getLastRowNum(); i++) {
                int excelRow = i + 1;
                Row row = sheet.getRow(i);
                if (row == null) {
                    throw InvalidExcelFormatException.emptyRow(excelRow);
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
        }

        // Segunda pasada: persistir solo cuando todas las filas son válidas.
        List<DrugResponse> loadedDrugs = new ArrayList<>();
        for (DrugRequest request : requests) {
            loadedDrugs.add(drugService.createDrug(request));
        }
        return loadedDrugs;
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
