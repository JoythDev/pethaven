package io.github.pethaven.service;

import io.github.pethaven.dto.request.DrugRequest;
import io.github.pethaven.exception.InvalidExcelFormatException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

// Pruebas del lector de Excel con libros generados en memoria por Apache POI: sin mocks y sin contexto de Spring.
public class ExcelReadingServiceTest {

    private static final String SHEET_NAME = "Inventario";

    // Encabezados reales del archivo: A-D se validan como texto y E se ignora.
    private static final Object[] HEADER_ROW = {
            "NOMBRE", "PRECIO VENTA", "PRECIO COMPRA", "UNIDADES DISPONIBLES", "UNIDADES VENDIDAS"
    };

    // Marcador para crear una celda explícitamente vacía (CellType.BLANK) dentro de una fila.
    private static final Object BLANK_CELL = new Object();

    private ValidatorFactory validatorFactory;
    private ExcelReadingService excelReadingService;

    @BeforeEach
    public void setUp() {
        // El servicio usa el validador real de Bean Validation para aplicar las reglas del DTO.
        validatorFactory = Validation.buildDefaultValidatorFactory();
        excelReadingService = new ExcelReadingServiceImpl(validatorFactory.getValidator());
    }

    @AfterEach
    public void tearDown() {
        validatorFactory.close();
    }

    // ---------- Helpers ----------

    private Object[] row(Object... cells) {
        return cells;
    }

    private byte[] xlsxBytes(String sheetName, int startRowIndex, Object[]... rows) throws IOException {
        return workbookBytes(new XSSFWorkbook(), sheetName, startRowIndex, rows);
    }

    private byte[] xlsBytes(String sheetName, int startRowIndex, Object[]... rows) throws IOException {
        return workbookBytes(new HSSFWorkbook(), sheetName, startRowIndex, rows);
    }

    private byte[] workbookBytes(Workbook workbook, String sheetName, int startRowIndex, Object[]... rows)
            throws IOException {
        fillSheet(workbook, sheetName, startRowIndex, rows);
        return writeWorkbook(workbook);
    }

    private void fillSheet(Workbook workbook, String sheetName, int startRowIndex, Object[]... rows) {
        Sheet sheet = workbook.createSheet(sheetName);
        for (int offset = 0; offset < rows.length; offset++) {
            // Un elemento null representa una fila que no existe en el libro (hueco real).
            if (rows[offset] != null) {
                writeRow(sheet.createRow(startRowIndex + offset), rows[offset]);
            }
        }
    }

    private void writeRow(Row row, Object[] values) {
        for (int column = 0; column < values.length; column++) {
            Object value = values[column];
            if (value == null) {
                // Sin celda creada: el lector debe reportarla como una celda vacía.
                continue;
            }
            if (value == BLANK_CELL) {
                row.createCell(column, CellType.BLANK);
                continue;
            }
            Cell cell = row.createCell(column);
            if (value instanceof String text) {
                cell.setCellValue(text);
            } else if (value instanceof Number number) {
                cell.setCellValue(number.doubleValue());
            } else {
                throw new IllegalArgumentException("Valor de celda no soportado: " + value);
            }
        }
    }

    private byte[] writeWorkbook(Workbook workbook) throws IOException {
        try (workbook; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void assertDrug(DrugRequest drug, String name, String salePrice, String purchasePrice, int unitsAvailable) {
        // El DTO declara (name, purchasePrice, salePrice, unitsAvailable); aquí se comprueba el mapeo de columnas.
        Assertions.assertThat(drug.name()).isEqualTo(name);
        Assertions.assertThat(drug.salePrice()).isEqualByComparingTo(salePrice);
        Assertions.assertThat(drug.purchasePrice()).isEqualByComparingTo(purchasePrice);
        Assertions.assertThat(drug.unitsAvailable()).isEqualTo(unitsAvailable);
    }

    // ---------- Lectura correcta ----------

    @Test
    public void excelReadingService_readDrugsFromExcel_ThreeValidXlsxRows_MapsColumnsInRowOrder() throws IOException {
        // Arrange
        // B (precio de venta) y C (precio de compra) llevan valores distintos para detectar un intercambio.
        byte[] content = xlsxBytes(SHEET_NAME, 0,
                HEADER_ROW,
                row("Amoxicilina 250mg", 15000, 8000, 200, 12),
                row("Ibuprofeno 400mg", 22000, 12000, 75, 30),
                row("Meloxicam 5mg", 9000, 5500, 0, 3));

        // Act
        ExcelReadingResult result = excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content));

        // Assert
        Assertions.assertThat(result.sheetName()).isEqualTo(SHEET_NAME);
        Assertions.assertThat(result.skippedBlankRows()).isZero();
        Assertions.assertThat(result.drugs()).hasSize(3);

        // Los precios son BigDecimal de escala 1 porque el lector usa BigDecimal.valueOf(double).
        assertDrug(result.drugs().get(0), "Amoxicilina 250mg", "15000.0", "8000.0", 200);
        assertDrug(result.drugs().get(1), "Ibuprofeno 400mg", "22000.0", "12000.0", 75);
        assertDrug(result.drugs().get(2), "Meloxicam 5mg", "9000.0", "5500.0", 0);
    }

    @Test
    public void excelReadingService_readDrugsFromExcel_ValidXlsRow_MapsColumns() throws IOException {
        // Arrange
        byte[] content = xlsBytes(SHEET_NAME, 0,
                HEADER_ROW,
                row("Ivermectina 1%", 12000, 4500, 18, 2));

        // Act
        ExcelReadingResult result = excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content));

        // Assert
        Assertions.assertThat(result.sheetName()).isEqualTo(SHEET_NAME);
        Assertions.assertThat(result.skippedBlankRows()).isZero();
        Assertions.assertThat(result.drugs()).hasSize(1);
        assertDrug(result.drugs().get(0), "Ivermectina 1%", "12000.0", "4500.0", 18);
    }

    @Test
    public void excelReadingService_readDrugsFromExcel_TwoSheets_ReadsOnlyFirstSheet() throws IOException {
        // Arrange
        Workbook workbook = new XSSFWorkbook();
        fillSheet(workbook, "Inventario", 0,
                HEADER_ROW,
                row("Amoxicilina 250mg", 15000, 8000, 200, 12));
        // La segunda hoja tiene datos distintos: el lector solo debe inspeccionar la hoja 0.
        fillSheet(workbook, "Historico", 0,
                HEADER_ROW,
                row("Meloxicam 5mg", 9900, 4400, 7, 3));
        byte[] content = writeWorkbook(workbook);

        // Act
        ExcelReadingResult result = excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content));

        // Assert
        Assertions.assertThat(result.sheetName()).isEqualTo("Inventario");
        Assertions.assertThat(result.skippedBlankRows()).isZero();
        Assertions.assertThat(result.drugs()).hasSize(1);
        assertDrug(result.drugs().get(0), "Amoxicilina 250mg", "15000.0", "8000.0", 200);
    }

    // ---------- Filas vacías ----------

    @Test
    public void excelReadingService_readDrugsFromExcel_GapBlankStringAndBlankCellRows_SkipsAndCountsThem()
            throws IOException {
        // Arrange
        // Índice 2: la fila no existe; índice 3: celdas A-E con espacios; índice 5: celdas A-E vacías (BLANK).
        byte[] content = xlsxBytes(SHEET_NAME, 0,
                HEADER_ROW,
                row("Amoxicilina 250mg", 15000, 8000, 200, 12),
                null,
                row("   ", "   ", "   ", "   ", "   "),
                row("Ibuprofeno 400mg", 22000, 12000, 75, 30),
                row(BLANK_CELL, BLANK_CELL, BLANK_CELL, BLANK_CELL, BLANK_CELL),
                row("Meloxicam 5mg", 9000, 5500, 0, 3));

        // Act
        ExcelReadingResult result = excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content));

        // Assert
        Assertions.assertThat(result.sheetName()).isEqualTo(SHEET_NAME);
        Assertions.assertThat(result.skippedBlankRows()).isEqualTo(3);
        Assertions.assertThat(result.drugs()).hasSize(3);
        assertDrug(result.drugs().get(0), "Amoxicilina 250mg", "15000.0", "8000.0", 200);
        assertDrug(result.drugs().get(1), "Ibuprofeno 400mg", "22000.0", "12000.0", 75);
        assertDrug(result.drugs().get(2), "Meloxicam 5mg", "9000.0", "5500.0", 0);
    }

    @Test
    public void excelReadingService_readDrugsFromExcel_OnlyUnitsSoldCell_ThrowsEmptyNameCell() throws IOException {
        // Arrange
        // La columna E no se lee, pero su contenido impide que la fila se considere vacía.
        byte[] content = xlsxBytes(SHEET_NAME, 0,
                HEADER_ROW,
                row(null, null, null, null, 4));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content)))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("Formato del archivo Excel inválido. La celda A2 (fila 2, columna A) está vacía. "
                        + "Se esperaba: el nombre, como texto.");
    }

    // ---------- Validación de encabezados ----------

    @Test
    public void excelReadingService_readDrugsFromExcel_NumericHeaderCell_ThrowsInvalidHeader() throws IOException {
        // Arrange
        // El número está en D, la última columna que valida el encabezado.
        byte[] content = xlsxBytes(SHEET_NAME, 0,
                row("NOMBRE", "PRECIO VENTA", "PRECIO COMPRA", 200),
                row("Amoxicilina 250mg", 15000, 8000, 200, 12));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content)))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("La primera fila debe contener los encabezados (texto), no datos de medicamentos.");
    }

    @Test
    public void excelReadingService_readDrugsFromExcel_NoHeaderRow_ReadsDataFromRowIndexOne() throws IOException {
        // Arrange
        // No existe la fila 0: al no haber encabezado que validar, los datos empiezan en el índice 1.
        byte[] content = xlsxBytes(SHEET_NAME, 1,
                row("Amoxicilina 250mg", 15000, 8000, 200, 12));

        // Act
        ExcelReadingResult result = excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content));

        // Assert
        Assertions.assertThat(result.sheetName()).isEqualTo(SHEET_NAME);
        Assertions.assertThat(result.skippedBlankRows()).isZero();
        Assertions.assertThat(result.drugs()).hasSize(1);
        assertDrug(result.drugs().get(0), "Amoxicilina 250mg", "15000.0", "8000.0", 200);
    }

    // ---------- Celdas obligatorias ----------

    @Test
    public void excelReadingService_readDrugsFromExcel_MissingNameCell_ThrowsEmptyCell() throws IOException {
        // Arrange
        byte[] content = xlsxBytes(SHEET_NAME, 0,
                HEADER_ROW,
                row(null, 15000, 8000, 200));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content)))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("Formato del archivo Excel inválido. La celda A2 (fila 2, columna A) está vacía. "
                        + "Se esperaba: el nombre, como texto.");
    }

    @Test
    public void excelReadingService_readDrugsFromExcel_MissingSalePriceCell_ThrowsEmptyCell() throws IOException {
        // Arrange
        byte[] content = xlsxBytes(SHEET_NAME, 0,
                HEADER_ROW,
                row("Amoxicilina 250mg", null, 8000, 200));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content)))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("Formato del archivo Excel inválido. La celda B2 (fila 2, columna B) está vacía. "
                        + "Se esperaba: el precio de venta, como número.");
    }

    @Test
    public void excelReadingService_readDrugsFromExcel_MissingPurchasePriceCell_ThrowsEmptyCell() throws IOException {
        // Arrange
        byte[] content = xlsxBytes(SHEET_NAME, 0,
                HEADER_ROW,
                row("Amoxicilina 250mg", 15000, null, 200));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content)))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("Formato del archivo Excel inválido. La celda C2 (fila 2, columna C) está vacía. "
                        + "Se esperaba: el precio de compra, como número.");
    }

    @Test
    public void excelReadingService_readDrugsFromExcel_MissingUnitsAvailableCell_ThrowsEmptyCell() throws IOException {
        // Arrange
        byte[] content = xlsxBytes(SHEET_NAME, 0,
                HEADER_ROW,
                row("Amoxicilina 250mg", 15000, 8000));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content)))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("Formato del archivo Excel inválido. La celda D2 (fila 2, columna D) está vacía. "
                        + "Se esperaba: las unidades disponibles, como número entero.");
    }

    @Test
    public void excelReadingService_readDrugsFromExcel_BlankSalePriceCell_ThrowsEmptyCell() throws IOException {
        // Arrange
        // Una celda B creada pero vacía (BLANK) se trata igual que una celda inexistente.
        byte[] content = xlsxBytes(SHEET_NAME, 0,
                HEADER_ROW,
                row("Amoxicilina 250mg", BLANK_CELL, 8000, 200));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content)))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("Formato del archivo Excel inválido. La celda B2 (fila 2, columna B) está vacía. "
                        + "Se esperaba: el precio de venta, como número.");
    }

    // ---------- Tipos de celda ----------

    @Test
    public void excelReadingService_readDrugsFromExcel_TextSalePriceCell_ThrowsWrongType() throws IOException {
        // Arrange
        byte[] content = xlsxBytes(SHEET_NAME, 0,
                HEADER_ROW,
                row("Amoxicilina 250mg", "15000", 8000, 200));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content)))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("Formato del archivo Excel inválido. La celda B2 (fila 2, columna B) debe contener "
                        + "el precio de venta, como número, pero su tipo es STRING.");
    }

    @Test
    public void excelReadingService_readDrugsFromExcel_NumericNameCell_ThrowsWrongType() throws IOException {
        // Arrange
        byte[] content = xlsxBytes(SHEET_NAME, 0,
                HEADER_ROW,
                row(42, 15000, 8000, 200));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content)))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("Formato del archivo Excel inválido. La celda A2 (fila 2, columna A) debe contener "
                        + "el nombre, como texto, pero su tipo es NUMERIC.");
    }

    @Test
    public void excelReadingService_readDrugsFromExcel_DecimalUnitsAvailable_ThrowsInvalidValue() throws IOException {
        // Arrange
        byte[] content = xlsxBytes(SHEET_NAME, 0,
                HEADER_ROW,
                row("Amoxicilina 250mg", 15000, 8000, 3.5));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content)))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("Formato del archivo Excel inválido. La celda D2 (fila 2, columna D) tiene un valor "
                        + "inválido: '3.5'. Se esperaba: un número entero.");
    }

    // ---------- Reglas de negocio del DTO ----------

    @Test
    public void excelReadingService_readDrugsFromExcel_NegativeSalePrice_ThrowsInvalidRow() throws IOException {
        // Arrange
        // Solo se rompe @PositiveOrZero del precio de venta: el mensaje de la violación es determinista.
        byte[] content = xlsxBytes(SHEET_NAME, 0,
                HEADER_ROW,
                row("Amoxicilina 250mg", -15000, 8000, 200));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content)))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("Formato del archivo Excel inválido. La fila 2 tiene datos inválidos: "
                        + "El precio de venta no puede ser negativo.");
    }

    @Test
    public void excelReadingService_readDrugsFromExcel_NameLongerThan120Characters_ThrowsInvalidRow()
            throws IOException {
        // Arrange
        // Solo se rompe @Size del nombre; el resto de campos es válido.
        String longName = "A".repeat(121);
        byte[] content = xlsxBytes(SHEET_NAME, 0,
                HEADER_ROW,
                row(longName, 15000, 8000, 200));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content)))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("Formato del archivo Excel inválido. La fila 2 tiene datos inválidos: "
                        + "El nombre no puede superar los 120 caracteres.");
    }

    // ---------- Número de fila en los mensajes ----------

    @Test
    public void excelReadingService_readDrugsFromExcel_InvalidRowAtIndexThree_ReportsOneBasedExcelRow()
            throws IOException {
        // Arrange
        // El índice 3 es la cuarta fila de Excel: el mensaje debe usar "fila 4" y la referencia "B4".
        byte[] content = xlsxBytes(SHEET_NAME, 0,
                HEADER_ROW,
                row("Amoxicilina 250mg", 15000, 8000, 200, 12),
                row("Ibuprofeno 400mg", 22000, 12000, 75, 30),
                row("Meloxicam 5mg", null, 5500, 0));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content)))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("Formato del archivo Excel inválido. La celda B4 (fila 4, columna B) está vacía. "
                        + "Se esperaba: el precio de venta, como número.");
    }

    // ---------- Hojas sin datos ----------

    @Test
    public void excelReadingService_readDrugsFromExcel_HeaderOnly_ThrowsNoDataRows() throws IOException {
        // Arrange
        byte[] content = xlsxBytes(SHEET_NAME, 0, HEADER_ROW);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content)))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("El archivo Excel no contiene filas de datos en la hoja 'Inventario'.");
    }

    @Test
    public void excelReadingService_readDrugsFromExcel_HeaderAndBlankRowsOnly_ThrowsNoDataRows() throws IOException {
        // Arrange
        // El nombre de la hoja se toma del libro, no de una constante del servicio.
        byte[] content = xlsxBytes("Hoja vacía", 0,
                HEADER_ROW,
                row("   ", "   ", "   ", "   ", "   "),
                row(BLANK_CELL, BLANK_CELL, BLANK_CELL, BLANK_CELL, BLANK_CELL));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content)))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("El archivo Excel no contiene filas de datos en la hoja 'Hoja vacía'.");
    }

    // ---------- Libro y contenido inválidos ----------

    @Test
    public void excelReadingService_readDrugsFromExcel_WorkbookWithoutSheets_ThrowsNoSheets() throws IOException {
        // Arrange
        // XSSFWorkbook permite escribir un libro válido sin hojas; no se simula el escenario.
        byte[] content = writeWorkbook(new XSSFWorkbook());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content)))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("El archivo Excel no contiene ninguna hoja.");
    }

    @Test
    public void excelReadingService_readDrugsFromExcel_NonExcelContent_PropagatesIOException() {
        // Arrange
        // WorkbookFactory.create rechaza el contenido con IOException antes de que el lector pueda tipificarlo;
        // DrugImportServiceImpl captura esa IOException y la traduce a "no se pudo leer el archivo Excel".
        byte[] content = "hola".getBytes(StandardCharsets.UTF_8);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> excelReadingService.readDrugsFromExcel(new ByteArrayInputStream(content)))
                .isInstanceOf(IOException.class)
                .isNotInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("Can't open workbook - unsupported file type: UNKNOWN");
    }
}
