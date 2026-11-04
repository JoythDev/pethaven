package io.github.pethaven.controller;

import io.github.pethaven.dto.request.DrugRequest;
import io.github.pethaven.dto.response.DrugImportSummaryResponse;
import io.github.pethaven.dto.response.DrugResponse;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.InvalidExcelFormatException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.service.DrugImportService;
import io.github.pethaven.service.DrugService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

// Pruebas del controlador de medicamentos con mocks: solo se levanta la capa web (MockMvc), sin base de datos.
@WebMvcTest(DrugController.class)
public class DrugControllerTest {

    // Medicamento válido: el precio de compra tiene dos decimales y las unidades son no negativas.
    private static final String VALID_DRUG_BODY = drugBody("Amoxicilina 250mg", "8500.50", "15000.00", "200");

    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    // Contenido ficticio: el servicio está simulado; solo se comprueba que los bytes lleguen intactos.
    private static final byte[] FAKE_XLSX_CONTENT = "contenido de prueba".getBytes(StandardCharsets.UTF_8);

    // Mensajes de restricciones de DrugRequest, de los servicios,
    // de las excepciones y del handler global.
    private static final String VALIDATION_MESSAGE = "La petición tiene campos inválidos.";
    private static final String NAME_REQUIRED_MESSAGE = "El nombre es obligatorio";
    private static final String NAME_TOO_LONG_MESSAGE = "El nombre no puede superar los 120 caracteres";
    private static final String PURCHASE_PRICE_REQUIRED_MESSAGE = "El precio de compra es obligatorio";
    private static final String PURCHASE_PRICE_NEGATIVE_MESSAGE = "El precio de compra no puede ser negativo";
    private static final String PURCHASE_PRICE_DIGITS_MESSAGE =
            "El precio de compra admite máximo 10 dígitos enteros y 2 decimales";
    private static final String SALE_PRICE_REQUIRED_MESSAGE = "El precio de venta es obligatorio";
    private static final String SALE_PRICE_NEGATIVE_MESSAGE = "El precio de venta no puede ser negativo";
    private static final String SALE_PRICE_DIGITS_MESSAGE =
            "El precio de venta admite máximo 10 dígitos enteros y 2 decimales";
    private static final String UNITS_REQUIRED_MESSAGE = "Las unidades disponibles son obligatorias";
    private static final String UNITS_NEGATIVE_MESSAGE = "Las unidades disponibles no pueden ser negativas";

    private static final String NOT_FOUND_MESSAGE = "El recurso 'Drug' con id: '99' no fue encontrado";
    private static final String DUPLICATED_NAME_MESSAGE =
            "Ya existe un medicamento con el nombre 'Amoxicilina 250mg'.";
    private static final String HAS_TREATMENTS_MESSAGE =
            "No se puede eliminar el medicamento 'Amoxicilina 250mg' porque está asociado a uno o más tratamientos existentes.";

    private static final String ID_TYPE_MESSAGE = "El parámetro 'id' tiene un formato inválido.";
    private static final String AVAILABLE_TYPE_MESSAGE = "El parámetro 'available' tiene un formato inválido.";

    private static final String UNSUPPORTED_EXTENSION_MESSAGE =
            "El archivo 'medicamentos.pdf' tiene una extensión no soportada. Use un archivo .xlsx o .xls.";
    private static final String DUPLICATED_IMPORT_MESSAGE =
            "El archivo contiene medicamentos duplicados: 'Amoxicilina 250mg'. No se importó ningún medicamento.";
    private static final String MISSING_FILE_PART_MESSAGE = "Falta la parte obligatoria 'file' en la petición.";
    private static final String UNSUPPORTED_MEDIA_TYPE_MESSAGE =
            "El tipo de contenido no es compatible. Use multipart/form-data.";

    private final MockMvc mockMvc;

    @MockitoBean
    private DrugService drugService;

    @MockitoBean
    private DrugImportService drugImportService;

    @Autowired
    public DrugControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    // Medicamento de respuesta: precios de escala 2 y unidades distintas de cero para detectar campos intercambiados.
    private static DrugResponse drug(Long id, String name) {
        return new DrugResponse(id, name, new BigDecimal("8500.50"), new BigDecimal("15000.00"), 200, 12);
    }

    // Construye el JSON del medicamento. El nombre va sin comillas (el helper las añade); precios y unidades
    // van como número JSON o como null.
    private static String drugBody(String name, String purchasePrice, String salePrice, String unitsAvailable) {
        return """
                {"name":"%s","purchasePrice":%s,"salePrice":%s,"unitsAvailable":%s}
                """.formatted(name, purchasePrice, salePrice, unitsAvailable);
    }

    // ---------- GET /api/drugs ----------

    @Test
    public void drugController_getAllDrugs_AvailableOmitted_Returns200AndCallsGetAllDrugs() throws Exception {
        // Arrange
        Mockito.when(drugService.getAllDrugs()).thenReturn(List.of(drug(1L, "Amoxicilina 250mg")));

        // Act + Assert
        // Sin el parámetro, available vale false: se llama a getAllDrugs y nunca a getAvailableDrugs.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/drugs"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("Amoxicilina 250mg"));
        Mockito.verify(drugService).getAllDrugs();
        Mockito.verify(drugService, Mockito.never()).getAvailableDrugs();
    }

    @Test
    public void drugController_getAllDrugs_AvailableTrue_Returns200WithAvailableDrugs() throws Exception {
        // Arrange
        Mockito.when(drugService.getAvailableDrugs()).thenReturn(List.of(drug(2L, "Ivermectina")));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/drugs").param("available", "true"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("Ivermectina"));
        Mockito.verify(drugService).getAvailableDrugs();
        Mockito.verify(drugService, Mockito.never()).getAllDrugs();
    }

    @Test
    public void drugController_getAllDrugs_EmptyList_Returns200WithEmptyArray() throws Exception {
        // Arrange
        Mockito.when(drugService.getAllDrugs()).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/drugs"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(0));
    }

    @Test
    public void drugController_getAllDrugs_InvalidAvailable_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // "abc" no se puede convertir a boolean: el handler responde 400 antes de llegar al servicio.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/drugs").param("available", "abc"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(AVAILABLE_TYPE_MESSAGE));
        Mockito.verifyNoInteractions(drugService);
    }

    // ---------- GET /api/drugs/{id} ----------

    @Test
    public void drugController_getDrugById_ExistingId_Returns200WithAllFields() throws Exception {
        // Arrange
        Mockito.when(drugService.getDrugById(1L)).thenReturn(drug(1L, "Amoxicilina 250mg"));

        // Act + Assert
        // Los precios viajan como número JSON; JsonPath los lee como Double, y 8500.50 y 15000.00 son exactos en binario.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/drugs/1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Amoxicilina 250mg"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.purchasePrice").value(8500.50))
                .andExpect(MockMvcResultMatchers.jsonPath("$.salePrice").value(15000.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.unitsAvailable").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("$.unitsSold").value(12));
        Mockito.verify(drugService).getDrugById(1L);
    }

    @Test
    public void drugController_getDrugById_NonExistingId_Returns404WithErrorBody() throws Exception {
        // Arrange
        Mockito.when(drugService.getDrugById(99L)).thenThrow(new ResourceNotFoundException("Drug", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/drugs/99"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(404))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Not Found"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(NOT_FOUND_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/api/drugs/99"));
    }

    @Test
    public void drugController_getDrugById_NonNumericId_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // "abc" no es un Long: la conversión del path variable falla y el handler responde 400.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/drugs/abc"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ID_TYPE_MESSAGE));
        Mockito.verifyNoInteractions(drugService);
    }

    // ---------- POST /api/drugs ----------

    @Test
    public void drugController_createDrug_ValidBody_Returns201WithLocationHeaderAndBody() throws Exception {
        // Arrange
        Mockito.when(drugService.createDrug(ArgumentMatchers.any(DrugRequest.class)))
                .thenReturn(drug(7L, "Amoxicilina 250mg"));

        // Act
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_DRUG_BODY))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(7))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Amoxicilina 250mg"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.purchasePrice").value(8500.50))
                .andExpect(MockMvcResultMatchers.jsonPath("$.salePrice").value(15000.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.unitsAvailable").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("$.unitsSold").value(12))
                .andReturn();

        // Assert
        Assertions.assertThat(result.getResponse().getHeader("Location")).endsWith("/api/drugs/7");

        // El cuerpo llega al servicio tal cual; se comprueban los campos capturados.
        ArgumentCaptor<DrugRequest> captor = ArgumentCaptor.forClass(DrugRequest.class);
        Mockito.verify(drugService).createDrug(captor.capture());
        DrugRequest sent = captor.getValue();
        Assertions.assertThat(sent.name()).isEqualTo("Amoxicilina 250mg");
        Assertions.assertThat(sent.purchasePrice()).isEqualByComparingTo("8500.50");
        Assertions.assertThat(sent.salePrice()).isEqualByComparingTo("15000.00");
        Assertions.assertThat(sent.unitsAvailable()).isEqualTo(200);
    }

    @Test
    public void drugController_createDrug_ZeroPricesAndUnits_Returns201AndCallsService() throws Exception {
        // Arrange
        // Cero cumple @PositiveOrZero en los tres campos numéricos y debe aceptarse.
        Mockito.when(drugService.createDrug(ArgumentMatchers.any(DrugRequest.class)))
                .thenReturn(drug(9L, "Amoxicilina 250mg"));

        // Act
        mockMvc.perform(MockMvcRequestBuilders.post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody("Amoxicilina 250mg", "0.00", "0.00", "0")))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        // Assert
        ArgumentCaptor<DrugRequest> captor = ArgumentCaptor.forClass(DrugRequest.class);
        Mockito.verify(drugService).createDrug(captor.capture());
        DrugRequest sent = captor.getValue();
        Assertions.assertThat(sent.purchasePrice()).isEqualByComparingTo("0.00");
        Assertions.assertThat(sent.salePrice()).isEqualByComparingTo("0.00");
        Assertions.assertThat(sent.unitsAvailable()).isZero();
    }

    @Test
    public void drugController_createDrug_MaximumNameAndPriceDigits_Returns201AndCallsService() throws Exception {
        // Arrange
        // Nombre de 120 caracteres y precios con 10 dígitos enteros y 2 decimales: son límites exactos y deben aceptarse.
        String name = "A".repeat(120);
        Mockito.when(drugService.createDrug(ArgumentMatchers.any(DrugRequest.class)))
                .thenReturn(drug(10L, name));

        // Act
        mockMvc.perform(MockMvcRequestBuilders.post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody(name, "9999999999.99", "9999999999.99", "200")))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        // Assert
        ArgumentCaptor<DrugRequest> captor = ArgumentCaptor.forClass(DrugRequest.class);
        Mockito.verify(drugService).createDrug(captor.capture());
        DrugRequest sent = captor.getValue();
        Assertions.assertThat(sent.name()).hasSize(120);
        Assertions.assertThat(sent.purchasePrice()).isEqualByComparingTo("9999999999.99");
        Assertions.assertThat(sent.salePrice()).isEqualByComparingTo("9999999999.99");
    }

    @Test
    public void drugController_createDrug_BlankName_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // Solo espacios: @NotBlank lo rechaza; @NotEmpty no lo haría.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody("   ", "8500.50", "15000.00", "200")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.name").value(NAME_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(drugService);
    }

    @Test
    public void drugController_createDrug_NameOver120Characters_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // 121 caracteres: solo falla @Size(max = 120).
        mockMvc.perform(MockMvcRequestBuilders.post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody("A".repeat(121), "8500.50", "15000.00", "200")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.name").value(NAME_TOO_LONG_MESSAGE));
        Mockito.verifyNoInteractions(drugService);
    }

    @Test
    public void drugController_createDrug_NullPurchasePrice_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // Sin precio de compra: @NotNull lo rechaza; @PositiveOrZero y @Digits aceptan null.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody("Amoxicilina 250mg", "null", "15000.00", "200")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.purchasePrice")
                        .value(PURCHASE_PRICE_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(drugService);
    }

    @Test
    public void drugController_createDrug_NegativePurchasePrice_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // -1.00: solo falla @PositiveOrZero; @Digits lo acepta.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody("Amoxicilina 250mg", "-1.00", "15000.00", "200")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.purchasePrice")
                        .value(PURCHASE_PRICE_NEGATIVE_MESSAGE));
        Mockito.verifyNoInteractions(drugService);
    }

    @Test
    public void drugController_createDrug_PurchasePriceWithElevenIntegerDigits_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // 11 dígitos enteros: @Digits(integer = 10) lo rechaza; el precio es positivo.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody("Amoxicilina 250mg", "12345678901.00", "15000.00", "200")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.purchasePrice")
                        .value(PURCHASE_PRICE_DIGITS_MESSAGE));
        Mockito.verifyNoInteractions(drugService);
    }

    @Test
    public void drugController_createDrug_NullSalePrice_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody("Amoxicilina 250mg", "8500.50", "null", "200")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.salePrice").value(SALE_PRICE_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(drugService);
    }

    @Test
    public void drugController_createDrug_NegativeSalePrice_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // -1.00: solo falla @PositiveOrZero; @Digits lo acepta.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody("Amoxicilina 250mg", "8500.50", "-1.00", "200")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.salePrice").value(SALE_PRICE_NEGATIVE_MESSAGE));
        Mockito.verifyNoInteractions(drugService);
    }

    @Test
    public void drugController_createDrug_SalePriceWithThreeDecimals_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // 3 decimales: @Digits(fraction = 2) lo rechaza.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody("Amoxicilina 250mg", "8500.50", "15000.005", "200")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.salePrice").value(SALE_PRICE_DIGITS_MESSAGE));
        Mockito.verifyNoInteractions(drugService);
    }

    @Test
    public void drugController_createDrug_NullUnitsAvailable_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // Sin unidades: @NotNull lo rechaza.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody("Amoxicilina 250mg", "8500.50", "15000.00", "null")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.unitsAvailable").value(UNITS_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(drugService);
    }

    @Test
    public void drugController_createDrug_NegativeUnitsAvailable_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // -1 unidad: solo falla @PositiveOrZero.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody("Amoxicilina 250mg", "8500.50", "15000.00", "-1")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.unitsAvailable").value(UNITS_NEGATIVE_MESSAGE));
        Mockito.verifyNoInteractions(drugService);
    }

    @Test
    public void drugController_createDrug_DuplicatedName_Returns409() throws Exception {
        // Arrange
        Mockito.when(drugService.createDrug(ArgumentMatchers.any(DrugRequest.class)))
                .thenThrow(new IllegalOperationException(DUPLICATED_NAME_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_DRUG_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(DUPLICATED_NAME_MESSAGE));
    }

    // ---------- POST /api/drugs/import ----------

    @Test
    public void drugController_importDrugsFromExcel_ValidFile_Returns200WithSummaryAndPassesFile() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "medicamentos.xlsx", XLSX_CONTENT_TYPE,
                FAKE_XLSX_CONTENT);
        Mockito.when(drugImportService.importFromExcel(ArgumentMatchers.any(MultipartFile.class)))
                .thenReturn(new DrugImportSummaryResponse("medicamentos.xlsx", "Medicamentos", 3, 3, 2));

        // Act
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/drugs/import").file(file))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.fileName").value("medicamentos.xlsx"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.sheetName").value("Medicamentos"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalRows").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.importedCount").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.skippedBlankRows").value(2));

        // Assert
        ArgumentCaptor<MultipartFile> captor = ArgumentCaptor.forClass(MultipartFile.class);
        Mockito.verify(drugImportService).importFromExcel(captor.capture());
        Assertions.assertThat(captor.getValue().getOriginalFilename()).isEqualTo("medicamentos.xlsx");
        Assertions.assertThat(captor.getValue().getBytes()).isEqualTo(FAKE_XLSX_CONTENT);
    }

    @Test
    public void drugController_importDrugsFromExcel_UnsupportedExtension_Returns400WithMessage() throws Exception {
        // Arrange
        // El nombre no termina en .xlsx ni .xls: el servicio lanza InvalidExcelFormatException (la validación
        // de extensión ocurre dentro del servicio, así que sí se llama).
        MockMultipartFile file = new MockMultipartFile("file", "medicamentos.pdf", MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4".getBytes(StandardCharsets.UTF_8));
        Mockito.when(drugImportService.importFromExcel(ArgumentMatchers.any(MultipartFile.class)))
                .thenThrow(InvalidExcelFormatException.unsupportedExtension("medicamentos.pdf"));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/drugs/import").file(file))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(UNSUPPORTED_EXTENSION_MESSAGE));
        Mockito.verify(drugImportService).importFromExcel(ArgumentMatchers.any(MultipartFile.class));
    }

    @Test
    public void drugController_importDrugsFromExcel_DuplicatedNames_Returns409() throws Exception {
        // Arrange
        // Nombre repetido dentro del archivo: el servicio lanza IllegalOperationException y el handler responde 409.
        MockMultipartFile file = new MockMultipartFile("file", "medicamentos.xlsx", XLSX_CONTENT_TYPE,
                FAKE_XLSX_CONTENT);
        Mockito.when(drugImportService.importFromExcel(ArgumentMatchers.any(MultipartFile.class)))
                .thenThrow(new IllegalOperationException(DUPLICATED_IMPORT_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/drugs/import").file(file))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(DUPLICATED_IMPORT_MESSAGE));
    }

    @Test
    public void drugController_importDrugsFromExcel_MissingFilePart_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // Multipart sin la parte 'file': @RequestPart falla antes de llegar al servicio.
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/drugs/import"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(MISSING_FILE_PART_MESSAGE));
        Mockito.verifyNoInteractions(drugImportService);
    }

    @Test
    public void drugController_importDrugsFromExcel_JsonContentType_Returns415AndDoesNotCallService() throws Exception {
        // Act + Assert
        // El endpoint solo consume multipart/form-data: un cuerpo JSON no llega al método.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/drugs/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(UNSUPPORTED_MEDIA_TYPE_MESSAGE));
        Mockito.verifyNoInteractions(drugImportService);
    }

    // ---------- PUT /api/drugs/{id} ----------

    @Test
    public void drugController_updateDrug_ValidBody_Returns200AndCallsServiceWithIdAndRequest() throws Exception {
        // Arrange
        Mockito.when(drugService.updateDrug(ArgumentMatchers.eq(1L), ArgumentMatchers.any(DrugRequest.class)))
                .thenReturn(drug(1L, "Amoxicilina 250mg"));

        // Act
        mockMvc.perform(MockMvcRequestBuilders.put("/api/drugs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_DRUG_BODY))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Amoxicilina 250mg"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.purchasePrice").value(8500.50));

        // Assert
        ArgumentCaptor<DrugRequest> captor = ArgumentCaptor.forClass(DrugRequest.class);
        Mockito.verify(drugService).updateDrug(ArgumentMatchers.eq(1L), captor.capture());
        DrugRequest sent = captor.getValue();
        Assertions.assertThat(sent.name()).isEqualTo("Amoxicilina 250mg");
        Assertions.assertThat(sent.purchasePrice()).isEqualByComparingTo("8500.50");
        Assertions.assertThat(sent.salePrice()).isEqualByComparingTo("15000.00");
        Assertions.assertThat(sent.unitsAvailable()).isEqualTo(200);
    }

    @Test
    public void drugController_updateDrug_InvalidBody_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // -1 unidad: @Valid rechaza el cuerpo y el servicio no se llama.
        mockMvc.perform(MockMvcRequestBuilders.put("/api/drugs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody("Amoxicilina 250mg", "8500.50", "15000.00", "-1")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.unitsAvailable").value(UNITS_NEGATIVE_MESSAGE));
        Mockito.verifyNoInteractions(drugService);
    }

    @Test
    public void drugController_updateDrug_NonExistingId_Returns404() throws Exception {
        // Arrange
        Mockito.when(drugService.updateDrug(ArgumentMatchers.eq(99L), ArgumentMatchers.any(DrugRequest.class)))
                .thenThrow(new ResourceNotFoundException("Drug", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/drugs/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_DRUG_BODY))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(NOT_FOUND_MESSAGE));
    }

    @Test
    public void drugController_updateDrug_DuplicatedName_Returns409() throws Exception {
        // Arrange
        Mockito.when(drugService.updateDrug(ArgumentMatchers.eq(1L), ArgumentMatchers.any(DrugRequest.class)))
                .thenThrow(new IllegalOperationException(DUPLICATED_NAME_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/drugs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_DRUG_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(DUPLICATED_NAME_MESSAGE));
    }

    // ---------- DELETE /api/drugs/{id} ----------

    @Test
    public void drugController_deleteDrug_ExistingId_Returns204WithEmptyBodyAndCallsService() throws Exception {
        // Act + Assert
        // 204 sin cuerpo: ResponseEntity.noContent() no escribe nada.
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/drugs/1"))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andExpect(MockMvcResultMatchers.content().string(""));
        Mockito.verify(drugService).deleteDrugById(1L);
    }

    @Test
    public void drugController_deleteDrug_NonExistingId_Returns404WithMessage() throws Exception {
        // Arrange
        Mockito.doThrow(new ResourceNotFoundException("Drug", "id", 99L)).when(drugService).deleteDrugById(99L);

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/drugs/99"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(NOT_FOUND_MESSAGE));
    }

    @Test
    public void drugController_deleteDrug_DrugWithTreatments_Returns409() throws Exception {
        // Arrange
        Mockito.doThrow(new IllegalOperationException(HAS_TREATMENTS_MESSAGE)).when(drugService).deleteDrugById(1L);

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/drugs/1"))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(HAS_TREATMENTS_MESSAGE));
    }
}
