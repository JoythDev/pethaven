package io.github.pethaven.service;

import io.github.pethaven.dto.response.DrugImportSummaryResponse;
import io.github.pethaven.entity.Drug;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.InvalidExcelFormatException;
import io.github.pethaven.repository.DrugRepository;
import jakarta.persistence.EntityManager;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Pruebas de integración de la importación de Excel: servicio, lector, repositorios y base H2 reales (sin mocks).
// Cada prueba corre en una transacción que se revierte al terminar, así que la base queda como estaba.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DrugImportServiceIntegrationTest {

    // Nombre de la hoja y encabezados que espera el lector real.
    private static final String SHEET_NAME = "Medicamentos";
    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final Object[] HEADER_ROW = {
            "NOMBRE", "PRECIO VENTA", "PRECIO COMPRA", "UNIDADES DISPONIBLES", "UNIDADES VENDIDAS"
    };

    // Firma ZIP de un archivo XLSX.
    private static final byte[] ZIP_SIGNATURE = {0x50, 0x4B, 0x03, 0x04};

    // Marcador para crear una celda explícitamente vacía (CellType.BLANK) dentro de una fila.
    private static final Object BLANK_CELL = new Object();

    private final DrugImportService drugImportService;
    private final DrugRepository drugRepository;
    private final EntityManager entityManager;

    @Autowired
    public DrugImportServiceIntegrationTest(
            DrugImportService drugImportService,
            DrugRepository drugRepository,
            EntityManager entityManager
    ) {
        this.drugImportService = drugImportService;
        this.drugRepository = drugRepository;
        this.entityManager = entityManager;
    }

    // ---------- Helpers ----------

    private Object[] row(Object... cells) {
        return cells;
    }

    private MockMultipartFile multipart(String originalFilename, byte[] content) {
        return new MockMultipartFile("file", originalFilename, CONTENT_TYPE, content);
    }

    private byte[] xlsx(Object[]... rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            for (int index = 0; index < rows.length; index++) {
                // Un elemento null representa una fila que no existe en el libro (hueco real).
                if (rows[index] != null) {
                    writeRow(sheet.createRow(index), rows[index]);
                }
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void writeRow(Row row, Object[] values) {
        for (int column = 0; column < values.length; column++) {
            Object value = values[column];
            if (value == null) {
                // Sin celda creada: el lector la trata como vacía.
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

    private byte[] corruptZipBytes() {
        // La firma ZIP pasa la validación binaria, pero el resto no forma un libro de Excel.
        byte[] text = "esto no es un libro de Excel".getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[ZIP_SIGNATURE.length + text.length];
        System.arraycopy(ZIP_SIGNATURE, 0, content, 0, ZIP_SIGNATURE.length);
        System.arraycopy(text, 0, content, ZIP_SIGNATURE.length, text.length);
        return content;
    }

    // ---------- Archivo real del classpath ----------

    @Test
    public void drugImportService_importFromExcel_RealClasspathFile_ImportsEveryRowConsistently() throws IOException {
        // Arrange
        // El archivo real puede cambiar: solo se validan invariantes, no filas ni nombres concretos.
        ClassPathResource resource = new ClassPathResource("data/medicamentos-veterinaria.xlsx");
        long countBefore = drugRepository.count();
        Set<Long> idsBefore = drugRepository.findAll().stream().map(Drug::getId).collect(Collectors.toSet());

        // Act
        DrugImportSummaryResponse summary;
        try (InputStream inputStream = resource.getInputStream()) {
            summary = drugImportService.importFromExcel(inputStream, "medicamentos-veterinaria.xlsx");
        }
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(summary.fileName()).isEqualTo("medicamentos-veterinaria.xlsx");
        Assertions.assertThat(summary.importedCount()).isPositive();
        Assertions.assertThat(summary.importedCount()).isEqualTo(summary.totalRows());
        Assertions.assertThat(drugRepository.count()).isEqualTo(countBefore + summary.importedCount());

        List<Drug> importedDrugs = drugRepository.findAll().stream()
                .filter(drug -> !idsBefore.contains(drug.getId()))
                .toList();
        Assertions.assertThat(importedDrugs).hasSize(summary.importedCount());

        List<String> importedNames = importedDrugs.stream().map(Drug::getName).toList();
        Assertions.assertThat(importedNames).doesNotHaveDuplicates();
        Assertions.assertThat(importedNames).allMatch(name -> !name.isBlank());

        Assertions.assertThat(importedDrugs).allSatisfy(drug -> {
            Assertions.assertThat(drug.getSalePrice()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            Assertions.assertThat(drug.getPurchasePrice()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            Assertions.assertThat(drug.getUnitsAvailable()).isGreaterThanOrEqualTo(0);
            Assertions.assertThat(drug.getUnitsSold()).isZero();
        });
    }

    // ---------- Importación completa ----------

    @Test
    public void drugImportService_importFromExcel_ThreeRowsWithUnitsSoldColumn_MapsColumnsAndReturnsSummary()
            throws IOException {
        // Arrange
        // B y C llevan valores distintos para detectar un intercambio; E se llena para probar que se ignora.
        byte[] content = xlsx(HEADER_ROW,
                row("Amoxicilina 250mg", 15000, 8000, 200, 12),
                row("Ibuprofeno 400mg", 22000, 12000, 75, 30),
                row("Meloxicam 5mg", 9000, 5500, 0, 3));

        // Act
        DrugImportSummaryResponse summary = drugImportService.importFromExcel(
                multipart("medicamentos.xlsx", content));
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(summary)
                .isEqualTo(new DrugImportSummaryResponse("medicamentos.xlsx", SHEET_NAME, 3, 3, 0));

        Drug amoxicilina = drugRepository.findByName("Amoxicilina 250mg").orElseThrow();
        Assertions.assertThat(amoxicilina.getSalePrice()).isEqualByComparingTo("15000");
        Assertions.assertThat(amoxicilina.getPurchasePrice()).isEqualByComparingTo("8000");
        Assertions.assertThat(amoxicilina.getUnitsAvailable()).isEqualTo(200);
        Assertions.assertThat(amoxicilina.getUnitsSold()).isZero();

        Drug ibuprofeno = drugRepository.findByName("Ibuprofeno 400mg").orElseThrow();
        Assertions.assertThat(ibuprofeno.getSalePrice()).isEqualByComparingTo("22000");
        Assertions.assertThat(ibuprofeno.getPurchasePrice()).isEqualByComparingTo("12000");
        Assertions.assertThat(ibuprofeno.getUnitsAvailable()).isEqualTo(75);
        Assertions.assertThat(ibuprofeno.getUnitsSold()).isZero();

        Drug meloxicam = drugRepository.findByName("Meloxicam 5mg").orElseThrow();
        Assertions.assertThat(meloxicam.getSalePrice()).isEqualByComparingTo("9000");
        Assertions.assertThat(meloxicam.getPurchasePrice()).isEqualByComparingTo("5500");
        Assertions.assertThat(meloxicam.getUnitsAvailable()).isZero();
        Assertions.assertThat(meloxicam.getUnitsSold()).isZero();
    }

    // ---------- Filas vacías ----------

    @Test
    public void drugImportService_importFromExcel_BlankRowsBetweenData_SkipsCountsAndImportsOnlyRealRows()
            throws IOException {
        // Arrange
        long countBefore = drugRepository.count();
        // Fila 3 no existe, fila 4 solo tiene espacios y fila 6 tiene celdas BLANK: las tres se ignoran.
        byte[] content = xlsx(HEADER_ROW,
                row("Amoxicilina 250mg", 15000, 8000, 200, 12),
                (Object[]) null,
                row("   ", "   ", "   ", "   ", "   "),
                row("Ibuprofeno 400mg", 22000, 12000, 75, 30),
                row(BLANK_CELL, BLANK_CELL, BLANK_CELL, BLANK_CELL, BLANK_CELL),
                row("Meloxicam 5mg", 9000, 5500, 0, 3));

        // Act
        DrugImportSummaryResponse summary = drugImportService.importFromExcel(
                multipart("medicamentos.xlsx", content));
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(summary)
                .isEqualTo(new DrugImportSummaryResponse("medicamentos.xlsx", SHEET_NAME, 3, 3, 3));
        Assertions.assertThat(drugRepository.count()).isEqualTo(countBefore + 3);
        Assertions.assertThat(drugRepository.findByName("Amoxicilina 250mg")).isPresent();
        Assertions.assertThat(drugRepository.findByName("Ibuprofeno 400mg")).isPresent();
        Assertions.assertThat(drugRepository.findByName("Meloxicam 5mg")).isPresent();
    }

    // ---------- Nombres existentes o duplicados ----------

    @Test
    public void drugImportService_importFromExcel_NameAlreadyInDatabase_ThrowsIllegalOperationAndPersistsNothing()
            throws IOException {
        // Arrange
        // El nombre de la segunda fila ya existe con otros precios y unidades: no debe modificarse.
        Drug existing = drugRepository.save(Drug.builder()
                .name("Ibuprofeno 400mg")
                .purchasePrice(new BigDecimal("1000.00"))
                .salePrice(new BigDecimal("2000.00"))
                .unitsAvailable(5)
                .unitsSold(7)
                .build());
        entityManager.flush();
        long countBefore = drugRepository.count();
        byte[] content = xlsx(HEADER_ROW,
                row("Amoxicilina 250mg", 15000, 8000, 200, 12),
                row("Ibuprofeno 400mg", 22000, 12000, 75, 30),
                row("Meloxicam 5mg", 9000, 5500, 0, 3));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugImportService.importFromExcel(
                        multipart("medicamentos.xlsx", content)))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Ya existen medicamentos con estos nombres: 'Ibuprofeno 400mg'. "
                        + "No se importó ningún medicamento.");

        entityManager.flush();
        entityManager.clear();

        // La importación es todo o nada: ni la primera ni la tercera fila se guardan.
        Assertions.assertThat(drugRepository.count()).isEqualTo(countBefore);
        Assertions.assertThat(drugRepository.findByName("Amoxicilina 250mg")).isEmpty();
        Assertions.assertThat(drugRepository.findByName("Meloxicam 5mg")).isEmpty();

        Drug reloaded = drugRepository.findById(existing.getId()).orElseThrow();
        Assertions.assertThat(reloaded.getPurchasePrice()).isEqualByComparingTo("1000.00");
        Assertions.assertThat(reloaded.getSalePrice()).isEqualByComparingTo("2000.00");
        Assertions.assertThat(reloaded.getUnitsAvailable()).isEqualTo(5);
        Assertions.assertThat(reloaded.getUnitsSold()).isEqualTo(7);
    }

    @Test
    public void drugImportService_importFromExcel_DuplicatedNamesInFile_ThrowsIllegalOperationAndPersistsNothing()
            throws IOException {
        // Arrange
        long countBefore = drugRepository.count();
        // La primera y la tercera fila repiten nombre: se rechaza el archivo completo.
        byte[] content = xlsx(HEADER_ROW,
                row("Amoxicilina 250mg", 15000, 8000, 200, 12),
                row("Ibuprofeno 400mg", 22000, 12000, 75, 30),
                row("Amoxicilina 250mg", 16000, 8500, 10, 1));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugImportService.importFromExcel(
                        multipart("medicamentos.xlsx", content)))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("El archivo contiene medicamentos duplicados: 'Amoxicilina 250mg'. "
                        + "No se importó ningún medicamento.");

        entityManager.flush();
        entityManager.clear();

        Assertions.assertThat(drugRepository.count()).isEqualTo(countBefore);
        Assertions.assertThat(drugRepository.findByName("Ibuprofeno 400mg")).isEmpty();
    }

    // ---------- Filas inválidas ----------

    @Test
    public void drugImportService_importFromExcel_TextPriceInThirdRow_ThrowsInvalidExcelFormatAndPersistsNothing()
            throws IOException {
        // Arrange
        long countBefore = drugRepository.count();
        // La fila 4 (índice 3) lleva texto en B: el error se reporta antes de guardar cualquier fila.
        byte[] content = xlsx(HEADER_ROW,
                row("Amoxicilina 250mg", 15000, 8000, 200, 12),
                row("Ibuprofeno 400mg", 22000, 12000, 75, 30),
                row("Meloxicam 5mg", "9000", 5500, 0, 3));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugImportService.importFromExcel(
                        multipart("medicamentos.xlsx", content)))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("Formato del archivo Excel inválido. La celda B4 (fila 4, columna B) debe contener "
                        + "el precio de venta, como número, pero su tipo es STRING.");

        entityManager.flush();
        entityManager.clear();

        // Aunque las dos primeras filas eran válidas, no se persiste ninguna.
        Assertions.assertThat(drugRepository.count()).isEqualTo(countBefore);
        Assertions.assertThat(drugRepository.findByName("Amoxicilina 250mg")).isEmpty();
        Assertions.assertThat(drugRepository.findByName("Ibuprofeno 400mg")).isEmpty();
    }

    // ---------- Archivos que no son Excel ----------

    @Test
    public void drugImportService_importFromExcel_NotAnExcelFile_ThrowsInvalidExcelFormatAndPersistsNothing() {
        // Arrange
        long countBefore = drugRepository.count();
        MockMultipartFile textFile = multipart(
                "medicamentos.xlsx", "contenido de texto".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile corruptZipFile = multipart("medicamentos.xlsx", corruptZipBytes());

        // Act
        Throwable unsupportedFormat = Assertions.catchThrowable(
                () -> drugImportService.importFromExcel(textFile));
        Throwable unreadable = Assertions.catchThrowable(
                () -> drugImportService.importFromExcel(corruptZipFile));

        // Assert
        Assertions.assertThat(unsupportedFormat)
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("El archivo 'medicamentos.xlsx' no es un archivo Excel válido. "
                        + "Use un archivo .xlsx o .xls.");
        Assertions.assertThat(unreadable)
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("No se pudo leer el archivo Excel 'medicamentos.xlsx'. "
                        + "Verifique que no esté dañado ni protegido con contraseña.");
        Assertions.assertThat(unreadable.getCause()).isNotNull();
        Assertions.assertThat(drugRepository.count()).isEqualTo(countBefore);
    }

    // ---------- Doble importación ----------

    @Test
    public void drugImportService_importFromExcel_SameFileImportedTwice_ThrowsIllegalOperationWithAllNames()
            throws IOException {
        // Arrange
        // El orden del archivo no es alfabético: el mensaje debe ordenar los nombres de la base.
        byte[] content = xlsx(HEADER_ROW,
                row("Meloxicam 5mg", 9000, 5500, 0, 3),
                row("Amoxicilina 250mg", 15000, 8000, 200, 12),
                row("Ibuprofeno 400mg", 22000, 12000, 75, 30));
        DrugImportSummaryResponse firstImport = drugImportService.importFromExcel(
                new ByteArrayInputStream(content), "medicamentos.xlsx");
        entityManager.flush();
        entityManager.clear();
        long countAfterFirstImport = drugRepository.count();

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugImportService.importFromExcel(
                        new ByteArrayInputStream(content), "medicamentos.xlsx"))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Ya existen medicamentos con estos nombres: 'Amoxicilina 250mg', 'Ibuprofeno 400mg', "
                        + "'Meloxicam 5mg'. No se importó ningún medicamento.");

        // La primera importación sí se completó y la segunda no agregó ni modificó nada.
        Assertions.assertThat(firstImport)
                .isEqualTo(new DrugImportSummaryResponse("medicamentos.xlsx", SHEET_NAME, 3, 3, 0));
        Assertions.assertThat(drugRepository.count()).isEqualTo(countAfterFirstImport);
    }
}
