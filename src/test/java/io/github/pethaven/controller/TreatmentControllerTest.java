package io.github.pethaven.controller;

import io.github.pethaven.dto.request.TreatmentDrugRequest;
import io.github.pethaven.dto.request.TreatmentRequest;
import io.github.pethaven.dto.response.TreatmentDrugResponse;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.entity.Evolution;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.service.TreatmentDrugService;
import io.github.pethaven.service.TreatmentService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Pruebas del controlador de tratamientos con mocks: solo se levanta la capa web (MockMvc), sin base de datos.
@WebMvcTest(TreatmentController.class)
public class TreatmentControllerTest {

    // Datos válidos de la consulta, reutilizados por los casos de validación: cada caso cambia solo el campo que prueba.
    private static final String VALID_DATE = "2026-10-12T10:30:15";
    private static final String VALID_DIAGNOSIS = "Otitis externa";
    private static final String VALID_NOTES = "Menos inflamación que en la consulta anterior.";
    private static final String VALID_DRUGS = "[{\"drugId\":4,\"units\":3}]";

    // Cuerpo válido de un tratamiento: mascota 31, veterinario 2, un medicamento y todos los campos informados.
    private static final String VALID_TREATMENT_BODY = treatmentBody(31L, 2L, VALID_DATE, 12.5, VALID_DIAGNOSIS,
            Evolution.IMPROVING, VALID_NOTES, VALID_DRUGS);

    // Cuerpo válido de un medicamento administrado: medicamento 4 y tres unidades.
    private static final String VALID_DRUG_BODY = drugBody(4L, 3);

    // Mensajes del handler global, copiados del código fuente.
    private static final String VALIDATION_MESSAGE = "La petición tiene campos inválidos.";
    private static final String DATA_CONFLICT_MESSAGE =
            "La operación entra en conflicto con datos existentes (valor duplicado o referencia inválida).";
    private static final String ID_TYPE_MESSAGE = "El parámetro 'id' tiene un formato inválido.";
    private static final String TREATMENT_NOT_FOUND_MESSAGE = "El recurso 'Treatment' con id: '99' no fue encontrado";
    private static final String PET_NOT_FOUND_MESSAGE = "El recurso 'Pet' con id: '99' no fue encontrado";
    private static final String VETERINARIAN_NOT_FOUND_MESSAGE =
            "El recurso 'Veterinarian' con id: '99' no fue encontrado";
    private static final String DRUG_NOT_FOUND_MESSAGE = "El recurso 'Drug' con id: '99' no fue encontrado";

    // Mensajes de las reglas de negocio de TreatmentServiceImpl y TreatmentDrugServiceImpl, con nombres de ejemplo.
    private static final String PET_INACTIVE_TREATMENT_MESSAGE =
            "La mascota 'Buddy' no está activa: no se le pueden registrar tratamientos.";
    private static final String VETERINARIAN_INACTIVE_MESSAGE =
            "El veterinario 'Dra. Valentina Soler' no está activo: no puede registrar atenciones.";
    private static final String NO_DRUGS_IN_STOCK_MESSAGE =
            "No hay medicamentos disponibles en el inventario: no se pueden registrar tratamientos.";
    private static final String STABLE_PET_WITH_EVOLUTION_MESSAGE =
            "La mascota 'Buddy' está estable: es un ingreso y no lleva evolución.";
    private static final String SICK_PET_WITHOUT_EVOLUTION_MESSAGE =
            "La mascota 'Buddy' está en tratamiento por 'Otitis externa': indica su evolución.";
    private static final String RECOVERED_EVOLUTION_MESSAGE =
            "Un tratamiento siempre formula medicamentos: para dejar estable a la mascota dale el alta.";
    private static final String STABLE_EVOLUTION_DIAGNOSIS_MESSAGE =
            "Con evolución 'sin cambios' el diagnóstico debe seguir siendo 'Otitis externa'.";
    private static final String PET_INACTIVE_DRUG_MESSAGE =
            "La mascota 'Buddy' no está activa: no se le pueden administrar medicamentos.";
    private static final String DRUG_ALREADY_ADMINISTERED_MESSAGE =
            "La droga 'Amoxicilina 250mg' ya fue administrada en este tratamiento.";
    private static final String INSUFFICIENT_UNITS_MESSAGE =
            "No hay suficientes unidades disponibles de 'Amoxicilina 250mg'. Disponibles: 2, solicitadas: 3.";

    // Mensajes de TreatmentRequest y TreatmentDrugRequest, copiados de sus anotaciones de validación.
    private static final String PET_ID_REQUIRED_MESSAGE = "El ID de la mascota es obligatorio";
    private static final String VETERINARIAN_ID_REQUIRED_MESSAGE = "El ID del veterinario es obligatorio";
    private static final String DATE_IN_FUTURE_MESSAGE = "La fecha del tratamiento no puede ser futura";
    private static final String WEIGHT_REQUIRED_MESSAGE = "El peso es obligatorio";
    private static final String WEIGHT_TOO_LOW_MESSAGE = "El peso debe ser de al menos 0.1 kg";
    private static final String WEIGHT_TOO_HIGH_MESSAGE = "El peso no puede superar los 120 kg";
    private static final String DIAGNOSIS_REQUIRED_MESSAGE = "El diagnóstico es obligatorio";
    private static final String DIAGNOSIS_TOO_LONG_MESSAGE = "El diagnóstico no puede superar los 200 caracteres";
    private static final String NOTES_TOO_LONG_MESSAGE = "Las observaciones no pueden superar los 1000 caracteres";
    private static final String DRUGS_REQUIRED_MESSAGE = "El tratamiento debe incluir al menos un medicamento";
    private static final String DRUG_ID_REQUIRED_MESSAGE = "El ID del medicamento es obligatorio";
    private static final String UNITS_REQUIRED_MESSAGE = "La cantidad de unidades es obligatoria";
    private static final String UNITS_NOT_POSITIVE_MESSAGE = "La cantidad de unidades debe ser mayor que cero";

    private final MockMvc mockMvc;

    @MockitoBean
    private TreatmentService treatmentService;

    @MockitoBean
    private TreatmentDrugService treatmentDrugService;

    @Autowired
    public TreatmentControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    // Tratamiento de respuesta de la mascota 31, atendida por el veterinario 2. Peso con escala exacta en binario.
    private static TreatmentResponse treatment(Long id, List<TreatmentDrugResponse> drugs) {
        return new TreatmentResponse(id, LocalDateTime.of(2026, 10, 12, 10, 30, 15), 31L, "Buddy", 2L,
                "Dra. Valentina Soler", 12.5, VALID_DIAGNOSIS, Evolution.IMPROVING, VALID_NOTES, drugs);
    }

    // Tratamiento de ingreso: la mascota llega estable, así que no lleva evolución, observaciones ni medicamentos.
    private static TreatmentResponse admission(Long id) {
        return new TreatmentResponse(id, LocalDateTime.of(2026, 10, 13, 9, 0), 32L, "Luna", 2L,
                "Dra. Valentina Soler", 4.5, "Fractura de pata", null, null, List.of());
    }

    // Renglón de medicamento administrado, con precios de escala 2.
    private static TreatmentDrugResponse treatmentDrug(Long id, Long drugId, String drugName, Integer units) {
        return new TreatmentDrugResponse(id, drugId, drugName, units, new BigDecimal("8500.50"),
                new BigDecimal("15000.00"));
    }

    // Construye el JSON del tratamiento. Los textos van entre comillas, la evolución se envía como nombre del enum
    // y un null de Java se envía como null JSON; la lista de medicamentos llega ya escrita en JSON.
    private static String treatmentBody(Long petId, Long veterinarianId, String date, Double weight, String diagnosis,
                                        Evolution evolution, String notes, String drugs) {
        return """
                {"petId":%s,"veterinarianId":%s,"date":%s,"weight":%s,"diagnosis":%s,
                "evolution":%s,"notes":%s,"drugs":%s}
                """.formatted(petId, veterinarianId, quoted(date), weight, quoted(diagnosis),
                evolution == null ? "null" : quoted(evolution.name()), quoted(notes), drugs);
    }

    // Construye el JSON de un medicamento administrado; un null de Java se envía como null JSON.
    private static String drugBody(Long drugId, Integer units) {
        return """
                {"drugId":%s,"units":%s}
                """.formatted(drugId, units);
    }

    // Construye la lista JSON con un único medicamento administrado.
    private static String drugsBody(Long drugId, Integer units) {
        return "[{\"drugId\":%s,\"units\":%s}]".formatted(drugId, units);
    }

    // Un texto va entre comillas; un null de Java se convierte en null JSON.
    private static String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    // ---------- GET /api/treatments ----------

    @Test
    public void treatmentController_getAllTreatments_TreatmentsExist_Returns200WithMainFieldsAndDrugsOfEachItem()
            throws Exception {
        // Arrange
        Mockito.when(treatmentService.getAllTreatments()).thenReturn(List.of(
                treatment(5L, List.of(treatmentDrug(9L, 4L, "Amoxicilina 250mg", 3))),
                admission(6L)));

        // Act + Assert
        // Cada elemento conserva su fecha, mascota, veterinario y medicamentos; el ingreso no lleva evolución.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/treatments"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(5))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].date").value("2026-10-12T10:30:15"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].petId").value(31))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].petName").value("Buddy"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].veterinarianId").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].veterinarianName").value("Dra. Valentina Soler"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].weight").value(12.5))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].diagnosis").value("Otitis externa"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].evolution").value("IMPROVING"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].notes")
                        .value("Menos inflamación que en la consulta anterior."))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].drugs.length()").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].drugs[0].drugName").value("Amoxicilina 250mg"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].drugs[0].unitSalePrice").value(15000.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").value(6))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].date").value("2026-10-13T09:00:00"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].petName").value("Luna"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].evolution").value((Object) null))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].notes").value((Object) null))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].drugs.length()").value(0));
        Mockito.verify(treatmentService).getAllTreatments();
        Mockito.verifyNoInteractions(treatmentDrugService);
    }

    @Test
    public void treatmentController_getAllTreatments_NoTreatments_Returns200WithEmptyArray() throws Exception {
        // Arrange
        Mockito.when(treatmentService.getAllTreatments()).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/treatments"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(0));
        Mockito.verify(treatmentService).getAllTreatments();
    }

    // ---------- GET /api/treatments/{id} ----------

    @Test
    public void treatmentController_getTreatmentById_ExistingId_Returns200WithAllFields() throws Exception {
        // Arrange
        // Cada id y cada cantidad son distintos entre sí, para que un campo intercambiado con otro haga fallar la prueba.
        Mockito.when(treatmentService.getTreatmentById(5L))
                .thenReturn(treatment(5L, List.of(treatmentDrug(9L, 4L, "Amoxicilina 250mg", 3))));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/treatments/5"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(5))
                .andExpect(MockMvcResultMatchers.jsonPath("$.date").value("2026-10-12T10:30:15"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.petId").value(31))
                .andExpect(MockMvcResultMatchers.jsonPath("$.petName").value("Buddy"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.veterinarianId").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.veterinarianName").value("Dra. Valentina Soler"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.weight").value(12.5))
                .andExpect(MockMvcResultMatchers.jsonPath("$.diagnosis").value("Otitis externa"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.evolution").value("IMPROVING"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.notes")
                        .value("Menos inflamación que en la consulta anterior."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.drugs.length()").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.drugs[0].id").value(9))
                .andExpect(MockMvcResultMatchers.jsonPath("$.drugs[0].drugId").value(4))
                .andExpect(MockMvcResultMatchers.jsonPath("$.drugs[0].drugName").value("Amoxicilina 250mg"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.drugs[0].units").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.drugs[0].unitPurchasePrice").value(8500.50))
                .andExpect(MockMvcResultMatchers.jsonPath("$.drugs[0].unitSalePrice").value(15000.00));
        Mockito.verify(treatmentService).getTreatmentById(5L);
    }

    @Test
    public void treatmentController_getTreatmentById_Admission_Returns200WithNullEvolutionAndNotes() throws Exception {
        // Arrange
        Mockito.when(treatmentService.getTreatmentById(6L)).thenReturn(admission(6L));

        // Act + Assert
        // El ingreso no tiene evolución ni observaciones: ambos viajan como null JSON.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/treatments/6"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.evolution").value((Object) null))
                .andExpect(MockMvcResultMatchers.jsonPath("$.notes").value((Object) null))
                .andExpect(MockMvcResultMatchers.jsonPath("$.drugs.length()").value(0));
        Mockito.verify(treatmentService).getTreatmentById(6L);
    }

    @Test
    public void treatmentController_getTreatmentById_NonExistingId_Returns404WithErrorBody() throws Exception {
        // Arrange
        Mockito.when(treatmentService.getTreatmentById(99L))
                .thenThrow(new ResourceNotFoundException("Treatment", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/treatments/99"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(404))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Not Found"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(TREATMENT_NOT_FOUND_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/api/treatments/99"));
    }

    @Test
    public void treatmentController_getTreatmentById_NonNumericId_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // "abc" no es un Long: la conversión del path variable falla y el handler responde 400.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/treatments/abc"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ID_TYPE_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService, treatmentDrugService);
    }

    // ---------- POST /api/treatments ----------

    @Test
    public void treatmentController_createTreatment_ValidBody_Returns201WithLocationHeaderAndBody() throws Exception {
        // Arrange
        Mockito.when(treatmentService.createTreatment(ArgumentMatchers.any(TreatmentRequest.class)))
                .thenReturn(treatment(5L, List.of(treatmentDrug(9L, 4L, "Amoxicilina 250mg", 3))));

        // Act
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TREATMENT_BODY))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(5))
                .andExpect(MockMvcResultMatchers.jsonPath("$.petName").value("Buddy"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.drugs[0].drugName").value("Amoxicilina 250mg"))
                .andReturn();

        // Assert
        Assertions.assertThat(result.getResponse().getHeader("Location")).endsWith("/api/treatments/5");

        // El cuerpo llega al servicio tal cual; se comprueban todos los campos capturados, incluido el medicamento.
        ArgumentCaptor<TreatmentRequest> captor = ArgumentCaptor.forClass(TreatmentRequest.class);
        Mockito.verify(treatmentService).createTreatment(captor.capture());
        TreatmentRequest sent = captor.getValue();
        Assertions.assertThat(sent.petId()).isEqualTo(31L);
        Assertions.assertThat(sent.veterinarianId()).isEqualTo(2L);
        Assertions.assertThat(sent.date()).isEqualTo(LocalDateTime.of(2026, 10, 12, 10, 30, 15));
        Assertions.assertThat(sent.weight()).isEqualTo(12.5);
        Assertions.assertThat(sent.diagnosis()).isEqualTo("Otitis externa");
        Assertions.assertThat(sent.evolution()).isEqualTo(Evolution.IMPROVING);
        Assertions.assertThat(sent.notes()).isEqualTo("Menos inflamación que en la consulta anterior.");
        Assertions.assertThat(sent.drugs()).hasSize(1);
        Assertions.assertThat(sent.drugs().get(0).drugId()).isEqualTo(4L);
        Assertions.assertThat(sent.drugs().get(0).units()).isEqualTo(3);
    }

    @Test
    public void treatmentController_createTreatment_OptionalFieldsNull_Returns201AndForwardsNulls() throws Exception {
        // Arrange
        // Sin fecha el servicio usa la actual, sin evolución es un ingreso y sin observaciones no hay texto.
        // Los tres campos son opcionales: el cuerpo es válido.
        Mockito.when(treatmentService.createTreatment(ArgumentMatchers.any(TreatmentRequest.class)))
                .thenReturn(admission(6L));

        // Act
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(32L, 2L, null, 4.5, "Fractura de pata", null, null, VALID_DRUGS)))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        // Assert
        ArgumentCaptor<TreatmentRequest> captor = ArgumentCaptor.forClass(TreatmentRequest.class);
        Mockito.verify(treatmentService).createTreatment(captor.capture());
        TreatmentRequest sent = captor.getValue();
        Assertions.assertThat(sent.date()).isNull();
        Assertions.assertThat(sent.evolution()).isNull();
        Assertions.assertThat(sent.notes()).isNull();
    }

    @Test
    public void treatmentController_createTreatment_MaximumBoundaryValues_Returns201AndCallsService() throws Exception {
        // Arrange
        Mockito.when(treatmentService.createTreatment(ArgumentMatchers.any(TreatmentRequest.class)))
                .thenReturn(treatment(5L, List.of()));

        // Act
        // Peso 120 (máximo inclusivo de @DecimalMax), diagnóstico de 200 caracteres y observaciones de 1000:
        // los tres están exactamente en el límite y se aceptan.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, 2L, VALID_DATE, 120.0, "D".repeat(200), Evolution.IMPROVING,
                                "N".repeat(1000), VALID_DRUGS)))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        // Assert
        ArgumentCaptor<TreatmentRequest> captor = ArgumentCaptor.forClass(TreatmentRequest.class);
        Mockito.verify(treatmentService).createTreatment(captor.capture());
        TreatmentRequest sent = captor.getValue();
        Assertions.assertThat(sent.weight()).isEqualTo(120.0);
        Assertions.assertThat(sent.diagnosis()).hasSize(200);
        Assertions.assertThat(sent.notes()).hasSize(1000);
    }

    @Test
    public void treatmentController_createTreatment_MinimumBoundaryValues_Returns201AndCallsService() throws Exception {
        // Arrange
        Mockito.when(treatmentService.createTreatment(ArgumentMatchers.any(TreatmentRequest.class)))
                .thenReturn(treatment(5L, List.of(treatmentDrug(9L, 4L, "Amoxicilina 250mg", 1))));

        // Act
        // Peso 0.1 (mínimo inclusivo de @DecimalMin), un carácter en el diagnóstico (mínimo no vacío de @NotBlank),
        // un único medicamento (mínimo de @NotEmpty) y una unidad (mínimo de @Positive): todos se aceptan.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, 2L, VALID_DATE, 0.1, "A", Evolution.IMPROVING, null,
                                drugsBody(4L, 1))))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        // Assert
        ArgumentCaptor<TreatmentRequest> captor = ArgumentCaptor.forClass(TreatmentRequest.class);
        Mockito.verify(treatmentService).createTreatment(captor.capture());
        TreatmentRequest sent = captor.getValue();
        Assertions.assertThat(sent.weight()).isEqualTo(0.1);
        Assertions.assertThat(sent.diagnosis()).isEqualTo("A");
        Assertions.assertThat(sent.drugs()).hasSize(1);
        Assertions.assertThat(sent.drugs().get(0).units()).isEqualTo(1);
    }

    @Test
    public void treatmentController_createTreatment_NullPetId_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(null, 2L, VALID_DATE, 12.5, VALID_DIAGNOSIS, Evolution.IMPROVING,
                                VALID_NOTES, VALID_DRUGS)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.petId").value(PET_ID_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService);
    }

    @Test
    public void treatmentController_createTreatment_NullVeterinarianId_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, null, VALID_DATE, 12.5, VALID_DIAGNOSIS, Evolution.IMPROVING,
                                VALID_NOTES, VALID_DRUGS)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.veterinarianId")
                        .value(VETERINARIAN_ID_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService);
    }

    @Test
    public void treatmentController_createTreatment_FutureDate_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // Fecha fija en 2099: @PastOrPresent la rechaza sin depender del reloj del sistema.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, 2L, "2099-01-01T10:00:00", 12.5, VALID_DIAGNOSIS,
                                Evolution.IMPROVING, VALID_NOTES, VALID_DRUGS)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.date").value(DATE_IN_FUTURE_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService);
    }

    @Test
    public void treatmentController_createTreatment_NullWeight_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, 2L, VALID_DATE, null, VALID_DIAGNOSIS, Evolution.IMPROVING,
                                VALID_NOTES, VALID_DRUGS)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.weight").value(WEIGHT_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService);
    }

    @Test
    public void treatmentController_createTreatment_WeightBelowMinimum_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // 0.05 kg: solo falla @DecimalMin("0.1").
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, 2L, VALID_DATE, 0.05, VALID_DIAGNOSIS, Evolution.IMPROVING,
                                VALID_NOTES, VALID_DRUGS)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.weight").value(WEIGHT_TOO_LOW_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService);
    }

    @Test
    public void treatmentController_createTreatment_WeightAboveMaximum_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // 120.5 kg: solo falla @DecimalMax("120").
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, 2L, VALID_DATE, 120.5, VALID_DIAGNOSIS, Evolution.IMPROVING,
                                VALID_NOTES, VALID_DRUGS)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.weight").value(WEIGHT_TOO_HIGH_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService);
    }

    @Test
    public void treatmentController_createTreatment_BlankDiagnosis_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // Solo espacios: @NotBlank lo rechaza.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, 2L, VALID_DATE, 12.5, "   ", Evolution.IMPROVING,
                                VALID_NOTES, VALID_DRUGS)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.diagnosis").value(DIAGNOSIS_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService);
    }

    @Test
    public void treatmentController_createTreatment_DiagnosisOver200Characters_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // 201 caracteres: solo falla @Size(max = 200).
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, 2L, VALID_DATE, 12.5, "D".repeat(201), Evolution.IMPROVING,
                                VALID_NOTES, VALID_DRUGS)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.diagnosis").value(DIAGNOSIS_TOO_LONG_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService);
    }

    @Test
    public void treatmentController_createTreatment_NotesOver1000Characters_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // 1001 caracteres: solo falla @Size(max = 1000).
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, 2L, VALID_DATE, 12.5, VALID_DIAGNOSIS, Evolution.IMPROVING,
                                "N".repeat(1001), VALID_DRUGS)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.notes").value(NOTES_TOO_LONG_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService);
    }

    @Test
    public void treatmentController_createTreatment_NullDrugs_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // Sin lista de medicamentos: @NotEmpty la rechaza.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, 2L, VALID_DATE, 12.5, VALID_DIAGNOSIS, Evolution.IMPROVING,
                                VALID_NOTES, "null")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.drugs").value(DRUGS_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService);
    }

    @Test
    public void treatmentController_createTreatment_EmptyDrugs_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // Lista vacía: un tratamiento sin medicamentos no es válido.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, 2L, VALID_DATE, 12.5, VALID_DIAGNOSIS, Evolution.IMPROVING,
                                VALID_NOTES, "[]")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.drugs").value(DRUGS_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService);
    }

    @Test
    public void treatmentController_createTreatment_DrugWithNullDrugId_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // Los medicamentos se validan en cascada (@Valid): el error llega con el índice del elemento.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, 2L, VALID_DATE, 12.5, VALID_DIAGNOSIS, Evolution.IMPROVING,
                                VALID_NOTES, "[{\"drugId\":null,\"units\":3}]")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors['drugs[0].drugId']")
                        .value(DRUG_ID_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService);
    }

    @Test
    public void treatmentController_createTreatment_DrugWithNullUnits_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, 2L, VALID_DATE, 12.5, VALID_DIAGNOSIS, Evolution.IMPROVING,
                                VALID_NOTES, "[{\"drugId\":4,\"units\":null}]")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors['drugs[0].units']")
                        .value(UNITS_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService);
    }

    @Test
    public void treatmentController_createTreatment_DrugWithZeroUnits_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // Cero: solo falla @Positive (el mínimo válido es 1).
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, 2L, VALID_DATE, 12.5, VALID_DIAGNOSIS, Evolution.IMPROVING,
                                VALID_NOTES, "[{\"drugId\":4,\"units\":0}]")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors['drugs[0].units']")
                        .value(UNITS_NOT_POSITIVE_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService);
    }

    @Test
    public void treatmentController_createTreatment_SecondDrugWithNegativeUnits_Returns400WithIndexedFieldKey()
            throws Exception {
        // Act + Assert
        // El primer renglón es válido y el segundo no: la clave del error indica cuál es el renglón inválido.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, 2L, VALID_DATE, 12.5, VALID_DIAGNOSIS, Evolution.IMPROVING,
                                VALID_NOTES, "[{\"drugId\":4,\"units\":3},{\"drugId\":5,\"units\":-1}]")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors['drugs[1].units']")
                        .value(UNITS_NOT_POSITIVE_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService);
    }

    @Test
    public void treatmentController_createTreatment_NonExistingPet_Returns404WithErrorBody() throws Exception {
        // Arrange
        // La mascota se busca antes de registrar el tratamiento: si no existe, responde 404.
        Mockito.when(treatmentService.createTreatment(ArgumentMatchers.any(TreatmentRequest.class)))
                .thenThrow(new ResourceNotFoundException("Pet", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TREATMENT_BODY))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(404))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(PET_NOT_FOUND_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/api/treatments"));
    }

    @Test
    public void treatmentController_createTreatment_NonExistingVeterinarian_Returns404WithErrorBody() throws Exception {
        // Arrange
        Mockito.when(treatmentService.createTreatment(ArgumentMatchers.any(TreatmentRequest.class)))
                .thenThrow(new ResourceNotFoundException("Veterinarian", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TREATMENT_BODY))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(404))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VETERINARIAN_NOT_FOUND_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/api/treatments"));
    }

    @Test
    public void treatmentController_createTreatment_InactivePet_Returns409() throws Exception {
        // Arrange
        Mockito.when(treatmentService.createTreatment(ArgumentMatchers.any(TreatmentRequest.class)))
                .thenThrow(new IllegalOperationException(PET_INACTIVE_TREATMENT_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TREATMENT_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(PET_INACTIVE_TREATMENT_MESSAGE));
    }

    @Test
    public void treatmentController_createTreatment_InactiveVeterinarian_Returns409() throws Exception {
        // Arrange
        Mockito.when(treatmentService.createTreatment(ArgumentMatchers.any(TreatmentRequest.class)))
                .thenThrow(new IllegalOperationException(VETERINARIAN_INACTIVE_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TREATMENT_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VETERINARIAN_INACTIVE_MESSAGE));
    }

    @Test
    public void treatmentController_createTreatment_NoDrugsInStock_Returns409() throws Exception {
        // Arrange
        Mockito.when(treatmentService.createTreatment(ArgumentMatchers.any(TreatmentRequest.class)))
                .thenThrow(new IllegalOperationException(NO_DRUGS_IN_STOCK_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TREATMENT_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(NO_DRUGS_IN_STOCK_MESSAGE));
    }

    @Test
    public void treatmentController_createTreatment_StablePetWithEvolution_Returns409() throws Exception {
        // Arrange
        // Un ingreso no lleva evolución: la regla de negocio lo rechaza cuando la mascota está estable.
        Mockito.when(treatmentService.createTreatment(ArgumentMatchers.any(TreatmentRequest.class)))
                .thenThrow(new IllegalOperationException(STABLE_PET_WITH_EVOLUTION_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TREATMENT_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(STABLE_PET_WITH_EVOLUTION_MESSAGE));
    }

    @Test
    public void treatmentController_createTreatment_SickPetWithoutEvolution_Returns409() throws Exception {
        // Arrange
        // Una mascota en tratamiento necesita evolución: sin ella la regla de negocio lo rechaza.
        Mockito.when(treatmentService.createTreatment(ArgumentMatchers.any(TreatmentRequest.class)))
                .thenThrow(new IllegalOperationException(SICK_PET_WITHOUT_EVOLUTION_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, 2L, VALID_DATE, 12.5, VALID_DIAGNOSIS, null, VALID_NOTES,
                                VALID_DRUGS)))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(SICK_PET_WITHOUT_EVOLUTION_MESSAGE));
    }

    @Test
    public void treatmentController_createTreatment_RecoveredEvolution_Returns409() throws Exception {
        // Arrange
        // RECOVERED solo lo usa el alta: un tratamiento con esa evolución lo rechaza la regla de negocio.
        Mockito.when(treatmentService.createTreatment(ArgumentMatchers.any(TreatmentRequest.class)))
                .thenThrow(new IllegalOperationException(RECOVERED_EVOLUTION_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, 2L, VALID_DATE, 12.5, VALID_DIAGNOSIS, Evolution.RECOVERED,
                                VALID_NOTES, VALID_DRUGS)))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(RECOVERED_EVOLUTION_MESSAGE));
    }

    @Test
    public void treatmentController_createTreatment_StableEvolutionWithChangedDiagnosis_Returns409()
            throws Exception {
        // Arrange
        // "Sin cambios" conserva el diagnóstico actual; cambiarlo la regla de negocio lo rechaza.
        Mockito.when(treatmentService.createTreatment(ArgumentMatchers.any(TreatmentRequest.class)))
                .thenThrow(new IllegalOperationException(STABLE_EVOLUTION_DIAGNOSIS_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treatmentBody(31L, 2L, VALID_DATE, 12.5, "Dermatitis", Evolution.STABLE, null,
                                VALID_DRUGS)))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(STABLE_EVOLUTION_DIAGNOSIS_MESSAGE));
    }

    @Test
    public void treatmentController_createTreatment_RecordRejectedByDatabase_Returns409() throws Exception {
        // Arrange
        // Si la mascota o el veterinario se eliminan justo antes de guardar, la base de datos rechaza el registro
        // por clave foránea y el handler global lo responde como conflicto.
        Mockito.when(treatmentService.createTreatment(ArgumentMatchers.any(TreatmentRequest.class)))
                .thenThrow(new DataIntegrityViolationException("violación de clave foránea"));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TREATMENT_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(DATA_CONFLICT_MESSAGE));
    }

    // ---------- GET /api/treatments/{id}/drugs ----------

    @Test
    public void treatmentController_getTreatmentDrugsByTreatmentId_ExistingTreatment_Returns200WithEveryField()
            throws Exception {
        // Arrange
        Mockito.when(treatmentService.getTreatmentById(5L))
                .thenReturn(treatment(5L, List.of(treatmentDrug(9L, 4L, "Amoxicilina 250mg", 3))));
        Mockito.when(treatmentDrugService.getTreatmentDrugsByTreatmentId(5L)).thenReturn(List.of(
                treatmentDrug(9L, 4L, "Amoxicilina 250mg", 3),
                new TreatmentDrugResponse(10L, 7L, "Ivermectina", 1, new BigDecimal("1200.00"),
                        new BigDecimal("2500.75"))));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/treatments/5/drugs"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(9))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].drugId").value(4))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].drugName").value("Amoxicilina 250mg"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].units").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].unitPurchasePrice").value(8500.50))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].unitSalePrice").value(15000.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").value(10))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].drugId").value(7))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].drugName").value("Ivermectina"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].units").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].unitPurchasePrice").value(1200.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].unitSalePrice").value(2500.75));

        // El orden importa: primero se comprueba que el tratamiento existe y después se piden sus medicamentos.
        InOrder inOrder = Mockito.inOrder(treatmentService, treatmentDrugService);
        inOrder.verify(treatmentService).getTreatmentById(5L);
        inOrder.verify(treatmentDrugService).getTreatmentDrugsByTreatmentId(5L);
    }

    @Test
    public void treatmentController_getTreatmentDrugsByTreatmentId_TreatmentWithoutDrugs_Returns200WithEmptyArray()
            throws Exception {
        // Arrange
        Mockito.when(treatmentService.getTreatmentById(6L)).thenReturn(admission(6L));
        Mockito.when(treatmentDrugService.getTreatmentDrugsByTreatmentId(6L)).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/treatments/6/drugs"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(0));
        Mockito.verify(treatmentDrugService).getTreatmentDrugsByTreatmentId(6L);
    }

    @Test
    public void treatmentController_getTreatmentDrugsByTreatmentId_NonExistingTreatment_Returns404AndDoesNotCallDrugService()
            throws Exception {
        // Arrange
        // El tratamiento se comprueba antes de consultar sus medicamentos: si no existe, responde 404 sin llegar a ellos.
        Mockito.when(treatmentService.getTreatmentById(99L))
                .thenThrow(new ResourceNotFoundException("Treatment", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/treatments/99/drugs"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(TREATMENT_NOT_FOUND_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/api/treatments/99/drugs"));
        Mockito.verifyNoInteractions(treatmentDrugService);
    }

    @Test
    public void treatmentController_getTreatmentDrugsByTreatmentId_NonNumericId_Returns400AndDoesNotCallServices()
            throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/treatments/abc/drugs"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ID_TYPE_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService, treatmentDrugService);
    }

    // ---------- POST /api/treatments/{id}/drugs ----------

    @Test
    public void treatmentController_createTreatmentDrug_ValidBody_Returns201WithBodyAndWithoutLocation()
            throws Exception {
        // Arrange
        Mockito.when(treatmentDrugService.createTreatmentDrug(ArgumentMatchers.eq(5L),
                        ArgumentMatchers.any(TreatmentDrugRequest.class)))
                .thenReturn(treatmentDrug(9L, 4L, "Amoxicilina 250mg", 3));

        // Act + Assert
        // No existe endpoint de detalle del renglón, así que la respuesta no lleva cabecera Location.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments/5/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_DRUG_BODY))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.header().doesNotExist("Location"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(9))
                .andExpect(MockMvcResultMatchers.jsonPath("$.drugId").value(4))
                .andExpect(MockMvcResultMatchers.jsonPath("$.drugName").value("Amoxicilina 250mg"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.units").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.unitPurchasePrice").value(8500.50))
                .andExpect(MockMvcResultMatchers.jsonPath("$.unitSalePrice").value(15000.00));

        // Assert
        // El id del tratamiento y el cuerpo llegan al servicio tal cual.
        ArgumentCaptor<TreatmentDrugRequest> captor = ArgumentCaptor.forClass(TreatmentDrugRequest.class);
        Mockito.verify(treatmentDrugService).createTreatmentDrug(ArgumentMatchers.eq(5L), captor.capture());
        TreatmentDrugRequest sent = captor.getValue();
        Assertions.assertThat(sent.drugId()).isEqualTo(4L);
        Assertions.assertThat(sent.units()).isEqualTo(3);
    }

    @Test
    public void treatmentController_createTreatmentDrug_MinimumUnits_Returns201AndForwardsOneUnit() throws Exception {
        // Arrange
        Mockito.when(treatmentDrugService.createTreatmentDrug(ArgumentMatchers.eq(5L),
                        ArgumentMatchers.any(TreatmentDrugRequest.class)))
                .thenReturn(treatmentDrug(9L, 4L, "Amoxicilina 250mg", 1));

        // Act
        // Una unidad es el mínimo válido de @Positive: se acepta.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments/5/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody(4L, 1)))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        // Assert
        ArgumentCaptor<TreatmentDrugRequest> captor = ArgumentCaptor.forClass(TreatmentDrugRequest.class);
        Mockito.verify(treatmentDrugService).createTreatmentDrug(ArgumentMatchers.eq(5L), captor.capture());
        Assertions.assertThat(captor.getValue().units()).isEqualTo(1);
    }

    @Test
    public void treatmentController_createTreatmentDrug_NullDrugId_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments/5/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody(null, 3)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.drugId").value(DRUG_ID_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(treatmentDrugService);
    }

    @Test
    public void treatmentController_createTreatmentDrug_NullUnits_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments/5/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody(4L, null)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.units").value(UNITS_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(treatmentDrugService);
    }

    @Test
    public void treatmentController_createTreatmentDrug_ZeroUnits_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // Cero: solo falla @Positive (el mínimo válido es 1).
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments/5/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody(4L, 0)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.units").value(UNITS_NOT_POSITIVE_MESSAGE));
        Mockito.verifyNoInteractions(treatmentDrugService);
    }

    @Test
    public void treatmentController_createTreatmentDrug_NegativeUnits_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments/5/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody(4L, -1)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.units").value(UNITS_NOT_POSITIVE_MESSAGE));
        Mockito.verifyNoInteractions(treatmentDrugService);
    }

    @Test
    public void treatmentController_createTreatmentDrug_NonNumericId_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // "abc" no es un Long: la conversión del path variable falla antes de leer el cuerpo.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments/abc/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_DRUG_BODY))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ID_TYPE_MESSAGE));
        Mockito.verifyNoInteractions(treatmentDrugService);
    }

    @Test
    public void treatmentController_createTreatmentDrug_NonExistingTreatment_Returns404WithErrorBody() throws Exception {
        // Arrange
        Mockito.when(treatmentDrugService.createTreatmentDrug(ArgumentMatchers.eq(99L),
                        ArgumentMatchers.any(TreatmentDrugRequest.class)))
                .thenThrow(new ResourceNotFoundException("Treatment", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments/99/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_DRUG_BODY))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(404))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(TREATMENT_NOT_FOUND_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/api/treatments/99/drugs"));
    }

    @Test
    public void treatmentController_createTreatmentDrug_NonExistingDrug_Returns404WithErrorBody() throws Exception {
        // Arrange
        Mockito.when(treatmentDrugService.createTreatmentDrug(ArgumentMatchers.eq(5L),
                        ArgumentMatchers.any(TreatmentDrugRequest.class)))
                .thenThrow(new ResourceNotFoundException("Drug", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments/5/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(drugBody(99L, 3)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(404))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(DRUG_NOT_FOUND_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/api/treatments/5/drugs"));
    }

    @Test
    public void treatmentController_createTreatmentDrug_InactivePet_Returns409() throws Exception {
        // Arrange
        Mockito.when(treatmentDrugService.createTreatmentDrug(ArgumentMatchers.eq(5L),
                        ArgumentMatchers.any(TreatmentDrugRequest.class)))
                .thenThrow(new IllegalOperationException(PET_INACTIVE_DRUG_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments/5/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_DRUG_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(PET_INACTIVE_DRUG_MESSAGE));
    }

    @Test
    public void treatmentController_createTreatmentDrug_DrugAlreadyAdministered_Returns409() throws Exception {
        // Arrange
        Mockito.when(treatmentDrugService.createTreatmentDrug(ArgumentMatchers.eq(5L),
                        ArgumentMatchers.any(TreatmentDrugRequest.class)))
                .thenThrow(new IllegalOperationException(DRUG_ALREADY_ADMINISTERED_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments/5/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_DRUG_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(DRUG_ALREADY_ADMINISTERED_MESSAGE));
    }

    @Test
    public void treatmentController_createTreatmentDrug_InsufficientUnits_Returns409() throws Exception {
        // Arrange
        Mockito.when(treatmentDrugService.createTreatmentDrug(ArgumentMatchers.eq(5L),
                        ArgumentMatchers.any(TreatmentDrugRequest.class)))
                .thenThrow(new IllegalOperationException(INSUFFICIENT_UNITS_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments/5/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_DRUG_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(INSUFFICIENT_UNITS_MESSAGE));
    }

    @Test
    public void treatmentController_createTreatmentDrug_RecordRejectedByDatabase_Returns409() throws Exception {
        // Arrange
        // Si el medicamento o el tratamiento se eliminan justo antes de guardar, la base de datos rechaza el renglón.
        Mockito.when(treatmentDrugService.createTreatmentDrug(ArgumentMatchers.eq(5L),
                        ArgumentMatchers.any(TreatmentDrugRequest.class)))
                .thenThrow(new DataIntegrityViolationException("violación de clave foránea"));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/treatments/5/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_DRUG_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(DATA_CONFLICT_MESSAGE));
    }
}
