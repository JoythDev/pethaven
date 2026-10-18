package io.github.pethaven;

import io.github.pethaven.dto.response.DrugImportSummaryResponse;
import io.github.pethaven.exception.InvalidExcelFormatException;
import io.github.pethaven.service.DrugImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Importa los medicamentos del archivo Excel incluido en el classpath al arrancar la aplicación.
 * Se ejecuta después de {@link DataLoader} para no compartir su transacción de datos de ejemplo.
 */
@Component
@Order(2)
public class ExcelDrugImportRunner implements CommandLineRunner {

    private static final String EXCEL_RESOURCE_PATH = "data/medicamentos-veterinaria.xlsx";

    private static final Logger log = LoggerFactory.getLogger(ExcelDrugImportRunner.class);

    private final DrugImportService drugImportService;

    @Autowired
    public ExcelDrugImportRunner(DrugImportService drugImportService) {
        this.drugImportService = drugImportService;
    }

    @Override
    public void run(String... args) throws Exception {
        try (InputStream inputStream = new ClassPathResource(EXCEL_RESOURCE_PATH).getInputStream()) {
            DrugImportSummaryResponse summary = drugImportService.importFromExcel(inputStream, EXCEL_RESOURCE_PATH);
            log.info("Importación de medicamentos completada: {} medicamentos desde '{}' ({} filas vacías ignoradas).",
                    summary.importedCount(), summary.fileName(), summary.skippedBlankRows());
        } catch (InvalidExcelFormatException e) {
            log.error("El archivo Excel de medicamentos no es válido. No se cargó ningún medicamento.", e);
        } catch (Exception e) {
            log.error("Ocurrió un error inesperado al importar el archivo Excel de medicamentos.", e);
        }
    }
}
