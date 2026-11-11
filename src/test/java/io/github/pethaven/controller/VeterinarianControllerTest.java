package io.github.pethaven.controller;

import io.github.pethaven.support.WebMvcSecurityTestSupport;
import io.github.pethaven.support.WithAnyApiRole;
import io.github.pethaven.dto.request.VeterinarianRequest;
import io.github.pethaven.dto.request.VeterinarianUpdateRequest;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.dto.response.VeterinarianResponse;
import io.github.pethaven.entity.Evolution;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.service.TreatmentService;
import io.github.pethaven.service.VeterinarianService;
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

import java.time.LocalDateTime;
import java.util.List;

// Pruebas del controlador de veterinarios con mocks: solo se levanta la capa web (MockMvc), sin base de datos.
@WebMvcTest(VeterinarianController.class)
@WebMvcSecurityTestSupport
@WithAnyApiRole
public class VeterinarianControllerTest {

    // Datos válidos de un veterinario: cada caso de validación cambia solo el campo que prueba.
    private static final String VALID_DOCUMENT = "241451123";
    private static final String VALID_NAME = "Dra. Valentina Soler";
    private static final String VALID_EMAIL = "valentina.soler@pethaven.com";
    private static final String VALID_PASSWORD = "vetclave2026";
    private static final String VALID_SPECIALTY = "Cuidados Críticos";
    private static final String VALID_PHOTO_URL = "https://images.example.test/valentina.jpg";

    // Cuerpo válido con todos los campos informados.
    private static final String VALID_VETERINARIAN_BODY = veterinarianBody(VALID_DOCUMENT, VALID_NAME, VALID_EMAIL,
            VALID_PASSWORD, VALID_SPECIALTY, VALID_PHOTO_URL);

    // Mensajes del handler global, copiados del código fuente.
    private static final String VALIDATION_MESSAGE = "La petición tiene campos inválidos.";
    private static final String DATA_CONFLICT_MESSAGE =
            "La operación entra en conflicto con datos existentes (valor duplicado o referencia inválida).";
    private static final String ID_TYPE_MESSAGE = "El parámetro 'id' tiene un formato inválido.";
    private static final String ACTIVE_MISSING_MESSAGE = "Falta el parámetro obligatorio 'active'.";
    private static final String ACTIVE_TYPE_MESSAGE = "El parámetro 'active' tiene un formato inválido.";
    private static final String VETERINARIAN_NOT_FOUND_MESSAGE =
            "El recurso 'Veterinarian' con id: '99' no fue encontrado";

    // Mensajes de VeterinarianServiceImpl.ensureUnique, con el documento y el correo del cuerpo válido.
    private static final String DOCUMENT_TAKEN_MESSAGE = "Ya existe un veterinario con el documento '241451123'.";
    private static final String EMAIL_TAKEN_MESSAGE =
            "Ya existe un veterinario con el correo 'valentina.soler@pethaven.com'.";

    // Mensajes de VeterinarianRequest y VeterinarianUpdateRequest, copiados de sus anotaciones de validación.
    private static final String DOCUMENT_REQUIRED_MESSAGE = "El documento es obligatorio";
    private static final String DOCUMENT_TOO_LONG_MESSAGE = "El documento no puede superar los 20 caracteres";
    private static final String NAME_REQUIRED_MESSAGE = "El nombre es obligatorio";
    private static final String NAME_TOO_LONG_MESSAGE = "El nombre no puede superar los 100 caracteres";
    private static final String EMAIL_REQUIRED_MESSAGE = "El correo electrónico es obligatorio";
    private static final String EMAIL_INVALID_MESSAGE = "El correo electrónico debe ser válido";
    private static final String EMAIL_TOO_LONG_MESSAGE = "El correo electrónico no puede superar los 120 caracteres";
    private static final String PASSWORD_REQUIRED_MESSAGE = "La contraseña es obligatoria";
    private static final String PASSWORD_LENGTH_MESSAGE = "La contraseña debe tener entre 8 y 255 caracteres";
    private static final String SPECIALTY_REQUIRED_MESSAGE = "La especialidad es obligatoria";
    private static final String SPECIALTY_TOO_LONG_MESSAGE = "La especialidad no puede superar los 80 caracteres";
    private static final String PHOTO_URL_TOO_LONG_MESSAGE = "La URL de la foto no puede superar los 500 caracteres";

    private final MockMvc mockMvc;

    @MockitoBean
    private VeterinarianService veterinarianService;

    @MockitoBean
    private TreatmentService treatmentService;

    @Autowired
    public VeterinarianControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    // Veterinario de respuesta con los datos válidos; el id, el nombre, las atenciones y el estado cambian por caso.
    private static VeterinarianResponse veterinarian(Long id, String name, Integer attentions, boolean active) {
        return new VeterinarianResponse(id, VALID_DOCUMENT, name, VALID_EMAIL, VALID_SPECIALTY, VALID_PHOTO_URL,
                attentions, active);
    }

    // Construye el JSON del veterinario. Los textos van entre comillas y un null de Java se envía como null JSON,
    // así cada caso de validación cambia solo el campo que quiere probar.
    private static String veterinarianBody(String document, String name, String email, String password,
                                           String specialty, String photoUrl) {
        return """
                {"document":%s,"name":%s,"email":%s,"password":%s,"specialty":%s,"photoUrl":%s}
                """.formatted(quoted(document), quoted(name), quoted(email), quoted(password), quoted(specialty),
                quoted(photoUrl));
    }

    // Un texto va entre comillas; un null de Java se convierte en null JSON.
    private static String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    // ---------- GET /api/veterinarians ----------

    @Test
    public void veterinarianController_getAllVeterinarians_VeterinariansExist_Returns200WithMainFields()
            throws Exception {
        // Arrange
        Mockito.when(veterinarianService.getAllVeterinarians()).thenReturn(List.of(
                veterinarian(1L, "Dra. Valentina Soler", 12, true),
                veterinarian(2L, "Dr. Mateo Rojas", 3, false)));

        // Act + Assert
        // Activos e inactivos se listan juntos; la respuesta nunca incluye la contraseña.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/veterinarians"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].document").value(VALID_DOCUMENT))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value(VALID_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].email").value(VALID_EMAIL))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].specialty").value(VALID_SPECIALTY))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].attentions").value(12))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].active").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].password").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].name").value("Dr. Mateo Rojas"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].active").value(false));
        Mockito.verify(veterinarianService).getAllVeterinarians();
        Mockito.verifyNoInteractions(treatmentService);
    }

    @Test
    public void veterinarianController_getAllVeterinarians_NoVeterinarians_Returns200WithEmptyArray()
            throws Exception {
        // Arrange
        Mockito.when(veterinarianService.getAllVeterinarians()).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/veterinarians"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(0));
        Mockito.verify(veterinarianService).getAllVeterinarians();
    }

    // ---------- GET /api/veterinarians/{id} ----------

    @Test
    public void veterinarianController_getVeterinarianById_ExistingId_Returns200WithAllFieldsAndNoPassword()
            throws Exception {
        // Arrange
        // Cada campo tiene un valor distinto para detectar campos intercambiados.
        Mockito.when(veterinarianService.getVeterinarianById(1L)).thenReturn(new VeterinarianResponse(1L,
                "103949123", "Dr. Mateo Rojas", "mateo.rojas@pethaven.com", "Medicina Interna",
                "https://images.example.test/mateo.jpg", 7, false));

        // Act + Assert
        // El DTO de respuesta no tiene campo de contraseña: la clave no aparece en el JSON.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/veterinarians/1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.document").value("103949123"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Dr. Mateo Rojas"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("mateo.rojas@pethaven.com"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.specialty").value("Medicina Interna"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.photoUrl").value("https://images.example.test/mateo.jpg"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.attentions").value(7))
                .andExpect(MockMvcResultMatchers.jsonPath("$.active").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.password").doesNotExist());
        Mockito.verify(veterinarianService).getVeterinarianById(1L);
    }

    @Test
    public void veterinarianController_getVeterinarianById_NonExistingId_Returns404WithErrorBody() throws Exception {
        // Arrange
        Mockito.when(veterinarianService.getVeterinarianById(99L))
                .thenThrow(new ResourceNotFoundException("Veterinarian", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/veterinarians/99"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(404))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Not Found"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VETERINARIAN_NOT_FOUND_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/api/veterinarians/99"));
    }

    @Test
    public void veterinarianController_getVeterinarianById_NonNumericId_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // "abc" no es un Long: la conversión del path variable falla y el handler responde 400.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/veterinarians/abc"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ID_TYPE_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService, treatmentService);
    }

    // ---------- POST /api/veterinarians ----------

    @Test
    public void veterinarianController_createVeterinarian_ValidBody_Returns201WithLocationHeaderBodyAndRequest()
            throws Exception {
        // Arrange
        Mockito.when(veterinarianService.createVeterinarian(ArgumentMatchers.any(VeterinarianRequest.class)))
                .thenReturn(veterinarian(7L, VALID_NAME, 0, true));

        // Act
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_VETERINARIAN_BODY))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(7))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(VALID_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$.attentions").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.active").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.password").doesNotExist())
                .andReturn();

        // Assert
        Assertions.assertThat(result.getResponse().getHeader("Location")).endsWith("/api/veterinarians/7");

        // El cuerpo llega al servicio tal cual; se comprueban todos los campos capturados.
        // Por ahora la contraseña viaja en texto plano; cambiará al implementar Spring Security + BCrypt + JWT
        // antes del despliegue a producción.
        ArgumentCaptor<VeterinarianRequest> captor = ArgumentCaptor.forClass(VeterinarianRequest.class);
        Mockito.verify(veterinarianService).createVeterinarian(captor.capture());
        VeterinarianRequest sent = captor.getValue();
        Assertions.assertThat(sent.document()).isEqualTo(VALID_DOCUMENT);
        Assertions.assertThat(sent.name()).isEqualTo(VALID_NAME);
        Assertions.assertThat(sent.email()).isEqualTo(VALID_EMAIL);
        Assertions.assertThat(sent.password()).isEqualTo(VALID_PASSWORD);
        Assertions.assertThat(sent.specialty()).isEqualTo(VALID_SPECIALTY);
        Assertions.assertThat(sent.photoUrl()).isEqualTo(VALID_PHOTO_URL);
    }

    @Test
    public void veterinarianController_createVeterinarian_MaximumBoundaryValues_Returns201AndCallsService()
            throws Exception {
        // Arrange
        // Correo de 120 caracteres con formato válido: 64 de parte local, @ y 55 de dominio.
        String maximumEmail = "a".repeat(64) + "@" + "b".repeat(50) + ".test";
        // URL de exactamente 500 caracteres: prefijo conocido completado con letras.
        String photoUrlPrefix = "https://images.example.test/";
        String maximumPhotoUrl = photoUrlPrefix + "a".repeat(500 - photoUrlPrefix.length());
        Mockito.when(veterinarianService.createVeterinarian(ArgumentMatchers.any(VeterinarianRequest.class)))
                .thenReturn(veterinarian(7L, VALID_NAME, 0, true));

        // Act
        // Todos los valores están exactamente en su límite máximo; como los límites son inclusivos, ninguno se rechaza.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody("9".repeat(20), "N".repeat(100), maximumEmail, "p".repeat(255),
                                "S".repeat(80), maximumPhotoUrl)))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        // Assert
        ArgumentCaptor<VeterinarianRequest> captor = ArgumentCaptor.forClass(VeterinarianRequest.class);
        Mockito.verify(veterinarianService).createVeterinarian(captor.capture());
        VeterinarianRequest sent = captor.getValue();
        Assertions.assertThat(sent.document()).hasSize(20);
        Assertions.assertThat(sent.name()).hasSize(100);
        Assertions.assertThat(sent.email()).hasSize(120);
        Assertions.assertThat(sent.password()).hasSize(255);
        Assertions.assertThat(sent.specialty()).hasSize(80);
        Assertions.assertThat(sent.photoUrl()).hasSize(500);
    }

    @Test
    public void veterinarianController_createVeterinarian_MinimumBoundaryValues_Returns201AndCallsService()
            throws Exception {
        // Arrange
        Mockito.when(veterinarianService.createVeterinarian(ArgumentMatchers.any(VeterinarianRequest.class)))
                .thenReturn(veterinarian(7L, "A", 0, true));

        // Act
        // Un carácter en documento, nombre y especialidad (mínimo no vacío de @NotBlank), contraseña de 8 caracteres
        // (mínimo inclusivo de @Size), correo corto con formato válido y sin foto (opcional): todo se acepta.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody("1", "A", "a@b.co", "12345678", "C", null)))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        // Assert
        ArgumentCaptor<VeterinarianRequest> captor = ArgumentCaptor.forClass(VeterinarianRequest.class);
        Mockito.verify(veterinarianService).createVeterinarian(captor.capture());
        VeterinarianRequest sent = captor.getValue();
        Assertions.assertThat(sent.document()).isEqualTo("1");
        Assertions.assertThat(sent.name()).isEqualTo("A");
        Assertions.assertThat(sent.email()).isEqualTo("a@b.co");
        Assertions.assertThat(sent.password()).hasSize(8);
        Assertions.assertThat(sent.specialty()).isEqualTo("C");
        Assertions.assertThat(sent.photoUrl()).isNull();
    }

    @Test
    public void veterinarianController_createVeterinarian_BlankDocument_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // Solo espacios: @NotBlank lo rechaza.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody("   ", VALID_NAME, VALID_EMAIL, VALID_PASSWORD, VALID_SPECIALTY,
                                VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.document").value(DOCUMENT_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_createVeterinarian_DocumentOver20Characters_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // 21 caracteres: solo falla @Size(max = 20).
        mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody("9".repeat(21), VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                                VALID_SPECIALTY, VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.document").value(DOCUMENT_TOO_LONG_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_createVeterinarian_BlankName_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // Solo espacios: @NotBlank lo rechaza.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, "   ", VALID_EMAIL, VALID_PASSWORD, VALID_SPECIALTY,
                                VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.name").value(NAME_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_createVeterinarian_NameOver100Characters_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // 101 caracteres: solo falla @Size(max = 100).
        mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, "N".repeat(101), VALID_EMAIL, VALID_PASSWORD,
                                VALID_SPECIALTY, VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.name").value(NAME_TOO_LONG_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_createVeterinarian_NullEmail_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // Sin correo: @NotBlank lo rechaza. Se usa null y no espacios, porque los espacios también fallarían @Email.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, null, VALID_PASSWORD, VALID_SPECIALTY,
                                VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.email").value(EMAIL_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_createVeterinarian_InvalidEmailFormat_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // Sin arroba: solo falla @Email; @NotBlank y @Size lo aceptan.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, "no-es-un-correo", VALID_PASSWORD,
                                VALID_SPECIALTY, VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.email").value(EMAIL_INVALID_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_createVeterinarian_EmailOver120Characters_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // 121 caracteres con formato válido: 64 de parte local, @ y 56 de dominio. Solo falla @Size(max = 120).
        String overLimitEmail = "a".repeat(64) + "@" + "b".repeat(51) + ".test";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, overLimitEmail, VALID_PASSWORD,
                                VALID_SPECIALTY, VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.email").value(EMAIL_TOO_LONG_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_createVeterinarian_BlankPassword_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // Ocho espacios: @NotBlank lo rechaza, pero @Size lo acepta porque su longitud es 8.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, VALID_EMAIL, " ".repeat(8),
                                VALID_SPECIALTY, VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.password").value(PASSWORD_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_createVeterinarian_PasswordBelowMinimum_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // 7 caracteres: solo falla @Size(min = 8).
        mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, VALID_EMAIL, "1234567",
                                VALID_SPECIALTY, VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.password").value(PASSWORD_LENGTH_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_createVeterinarian_PasswordOver255Characters_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // 256 caracteres: solo falla @Size(max = 255).
        mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, VALID_EMAIL, "p".repeat(256),
                                VALID_SPECIALTY, VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.password").value(PASSWORD_LENGTH_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_createVeterinarian_BlankSpecialty_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // Solo espacios: @NotBlank lo rechaza.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, VALID_EMAIL, VALID_PASSWORD, "   ",
                                VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.specialty").value(SPECIALTY_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_createVeterinarian_SpecialtyOver80Characters_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // 81 caracteres: solo falla @Size(max = 80).
        mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                                "S".repeat(81), VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.specialty").value(SPECIALTY_TOO_LONG_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_createVeterinarian_PhotoUrlOver500Characters_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // 501 caracteres: solo falla @Size(max = 500).
        mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                                VALID_SPECIALTY, "a".repeat(501))))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.photoUrl").value(PHOTO_URL_TOO_LONG_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_createVeterinarian_DuplicateDocument_Returns409WithServiceMessage()
            throws Exception {
        // Arrange
        // El servicio rechaza el documento ya registrado con el mensaje de VeterinarianServiceImpl.ensureUnique.
        Mockito.when(veterinarianService.createVeterinarian(ArgumentMatchers.any(VeterinarianRequest.class)))
                .thenThrow(new IllegalOperationException(DOCUMENT_TAKEN_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_VETERINARIAN_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(DOCUMENT_TAKEN_MESSAGE));
    }

    @Test
    public void veterinarianController_createVeterinarian_DuplicateEmail_Returns409WithServiceMessage()
            throws Exception {
        // Arrange
        // El servicio rechaza el correo ya registrado con el mensaje de VeterinarianServiceImpl.ensureUnique.
        Mockito.when(veterinarianService.createVeterinarian(ArgumentMatchers.any(VeterinarianRequest.class)))
                .thenThrow(new IllegalOperationException(EMAIL_TAKEN_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_VETERINARIAN_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(EMAIL_TAKEN_MESSAGE));
    }

    @Test
    public void veterinarianController_createVeterinarian_RecordRejectedByDatabase_Returns409()
            throws Exception {
        // Arrange
        // Si dos altas con el mismo documento o correo se guardan a la vez, la restricción única de la base de datos
        // rechaza la segunda y el handler global la responde como conflicto.
        Mockito.when(veterinarianService.createVeterinarian(ArgumentMatchers.any(VeterinarianRequest.class)))
                .thenThrow(new DataIntegrityViolationException("violación de restricción única"));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/veterinarians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_VETERINARIAN_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(DATA_CONFLICT_MESSAGE));
    }

    // ---------- PUT /api/veterinarians/{id} ----------

    @Test
    public void veterinarianController_updateVeterinarian_ValidBodyWithPassword_Returns200AndForwardsIdAndRequest()
            throws Exception {
        // Arrange
        Mockito.when(veterinarianService.updateVeterinarian(ArgumentMatchers.eq(1L),
                        ArgumentMatchers.any(VeterinarianUpdateRequest.class)))
                .thenReturn(veterinarian(1L, "Dra. Valentina Soler Ruiz", 12, true));

        // Act
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody("300112233", "Dra. Valentina Soler Ruiz",
                                "valentina.ruiz@pethaven.com", "nuevaClave2026", "Medicina Interna",
                                "https://images.example.test/valentina-nueva.jpg")))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Dra. Valentina Soler Ruiz"));

        // Assert
        // El id llega como path variable y el cuerpo completo llega al servicio.
        // Como en el alta, la contraseña viaja en texto plano hasta el servicio (ver el comentario de POST).
        ArgumentCaptor<VeterinarianUpdateRequest> captor = ArgumentCaptor.forClass(VeterinarianUpdateRequest.class);
        Mockito.verify(veterinarianService).updateVeterinarian(ArgumentMatchers.eq(1L), captor.capture());
        VeterinarianUpdateRequest sent = captor.getValue();
        Assertions.assertThat(sent.document()).isEqualTo("300112233");
        Assertions.assertThat(sent.name()).isEqualTo("Dra. Valentina Soler Ruiz");
        Assertions.assertThat(sent.email()).isEqualTo("valentina.ruiz@pethaven.com");
        Assertions.assertThat(sent.password()).isEqualTo("nuevaClave2026");
        Assertions.assertThat(sent.specialty()).isEqualTo("Medicina Interna");
        Assertions.assertThat(sent.photoUrl()).isEqualTo("https://images.example.test/valentina-nueva.jpg");
    }

    @Test
    public void veterinarianController_updateVeterinarian_NullPassword_Returns200AndForwardsNullPassword()
            throws Exception {
        // Arrange
        Mockito.when(veterinarianService.updateVeterinarian(ArgumentMatchers.eq(1L),
                        ArgumentMatchers.any(VeterinarianUpdateRequest.class)))
                .thenReturn(veterinarian(1L, VALID_NAME, 12, true));

        // Act
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, VALID_EMAIL, null, VALID_SPECIALTY,
                                VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        // Assert
        // A diferencia del alta, la contraseña es opcional: null llega al servicio, que conserva la actual.
        ArgumentCaptor<VeterinarianUpdateRequest> captor = ArgumentCaptor.forClass(VeterinarianUpdateRequest.class);
        Mockito.verify(veterinarianService).updateVeterinarian(ArgumentMatchers.eq(1L), captor.capture());
        Assertions.assertThat(captor.getValue().password()).isNull();
    }

    @Test
    public void veterinarianController_updateVeterinarian_BlankPassword_Returns200AndForwardsNullPassword()
            throws Exception {
        // Arrange
        Mockito.when(veterinarianService.updateVeterinarian(ArgumentMatchers.eq(1L),
                        ArgumentMatchers.any(VeterinarianUpdateRequest.class)))
                .thenReturn(veterinarian(1L, VALID_NAME, 12, true));

        // Act
        // Una contraseña en blanco cuenta como omitida: el constructor compacto de VeterinarianUpdateRequest la
        // convierte en null antes de validar, así que tres espacios (menos de 8 caracteres) no provocan un 400.
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, VALID_EMAIL, "   ", VALID_SPECIALTY,
                                VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        // Assert
        ArgumentCaptor<VeterinarianUpdateRequest> captor = ArgumentCaptor.forClass(VeterinarianUpdateRequest.class);
        Mockito.verify(veterinarianService).updateVeterinarian(ArgumentMatchers.eq(1L), captor.capture());
        Assertions.assertThat(captor.getValue().password()).isNull();
    }

    @Test
    public void veterinarianController_updateVeterinarian_MaximumBoundaryValues_Returns200AndCallsService()
            throws Exception {
        // Arrange
        String maximumEmail = "a".repeat(64) + "@" + "b".repeat(50) + ".test";
        String photoUrlPrefix = "https://images.example.test/";
        String maximumPhotoUrl = photoUrlPrefix + "a".repeat(500 - photoUrlPrefix.length());
        Mockito.when(veterinarianService.updateVeterinarian(ArgumentMatchers.eq(1L),
                        ArgumentMatchers.any(VeterinarianUpdateRequest.class)))
                .thenReturn(veterinarian(1L, VALID_NAME, 12, true));

        // Act
        // Todos los valores están en su límite máximo; la contraseña de 255 caracteres también se acepta.
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody("9".repeat(20), "N".repeat(100), maximumEmail, "p".repeat(255),
                                "S".repeat(80), maximumPhotoUrl)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        // Assert
        ArgumentCaptor<VeterinarianUpdateRequest> captor = ArgumentCaptor.forClass(VeterinarianUpdateRequest.class);
        Mockito.verify(veterinarianService).updateVeterinarian(ArgumentMatchers.eq(1L), captor.capture());
        VeterinarianUpdateRequest sent = captor.getValue();
        Assertions.assertThat(sent.document()).hasSize(20);
        Assertions.assertThat(sent.name()).hasSize(100);
        Assertions.assertThat(sent.email()).hasSize(120);
        Assertions.assertThat(sent.password()).hasSize(255);
        Assertions.assertThat(sent.specialty()).hasSize(80);
        Assertions.assertThat(sent.photoUrl()).hasSize(500);
    }

    @Test
    public void veterinarianController_updateVeterinarian_MinimumBoundaryValues_Returns200AndCallsService()
            throws Exception {
        // Arrange
        Mockito.when(veterinarianService.updateVeterinarian(ArgumentMatchers.eq(1L),
                        ArgumentMatchers.any(VeterinarianUpdateRequest.class)))
                .thenReturn(veterinarian(1L, "A", 0, true));

        // Act
        // Un carácter en documento, nombre y especialidad, contraseña de 8 caracteres (mínimo de @Size) y sin foto.
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody("1", "A", "a@b.co", "12345678", "C", null)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        // Assert
        ArgumentCaptor<VeterinarianUpdateRequest> captor = ArgumentCaptor.forClass(VeterinarianUpdateRequest.class);
        Mockito.verify(veterinarianService).updateVeterinarian(ArgumentMatchers.eq(1L), captor.capture());
        VeterinarianUpdateRequest sent = captor.getValue();
        Assertions.assertThat(sent.document()).isEqualTo("1");
        Assertions.assertThat(sent.name()).isEqualTo("A");
        Assertions.assertThat(sent.email()).isEqualTo("a@b.co");
        Assertions.assertThat(sent.password()).hasSize(8);
        Assertions.assertThat(sent.specialty()).isEqualTo("C");
        Assertions.assertThat(sent.photoUrl()).isNull();
    }

    @Test
    public void veterinarianController_updateVeterinarian_BlankDocument_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // Solo espacios: @NotBlank lo rechaza.
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody("   ", VALID_NAME, VALID_EMAIL, VALID_PASSWORD, VALID_SPECIALTY,
                                VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.document").value(DOCUMENT_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_updateVeterinarian_DocumentOver20Characters_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // 21 caracteres: solo falla @Size(max = 20).
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody("9".repeat(21), VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                                VALID_SPECIALTY, VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.document").value(DOCUMENT_TOO_LONG_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_updateVeterinarian_BlankName_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // Solo espacios: @NotBlank lo rechaza.
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, "   ", VALID_EMAIL, VALID_PASSWORD, VALID_SPECIALTY,
                                VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.name").value(NAME_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_updateVeterinarian_NameOver100Characters_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // 101 caracteres: solo falla @Size(max = 100).
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, "N".repeat(101), VALID_EMAIL, VALID_PASSWORD,
                                VALID_SPECIALTY, VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.name").value(NAME_TOO_LONG_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_updateVeterinarian_NullEmail_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // Sin correo: @NotBlank lo rechaza. Se usa null y no espacios, porque los espacios también fallarían @Email.
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, null, VALID_PASSWORD, VALID_SPECIALTY,
                                VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.email").value(EMAIL_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_updateVeterinarian_InvalidEmailFormat_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // Sin arroba: solo falla @Email; @NotBlank y @Size lo aceptan.
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, "no-es-un-correo", VALID_PASSWORD,
                                VALID_SPECIALTY, VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.email").value(EMAIL_INVALID_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_updateVeterinarian_EmailOver120Characters_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // 121 caracteres con formato válido: solo falla @Size(max = 120).
        String overLimitEmail = "a".repeat(64) + "@" + "b".repeat(51) + ".test";
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, overLimitEmail, VALID_PASSWORD,
                                VALID_SPECIALTY, VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.email").value(EMAIL_TOO_LONG_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_updateVeterinarian_PasswordBelowMinimum_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // Con contraseña presente, 7 caracteres solo fallan @Size(min = 8); la contraseña no se trata como omitida.
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, VALID_EMAIL, "1234567",
                                VALID_SPECIALTY, VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.password").value(PASSWORD_LENGTH_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_updateVeterinarian_PasswordOver255Characters_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // 256 caracteres: solo falla @Size(max = 255).
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, VALID_EMAIL, "p".repeat(256),
                                VALID_SPECIALTY, VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.password").value(PASSWORD_LENGTH_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_updateVeterinarian_BlankSpecialty_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // Solo espacios: @NotBlank lo rechaza.
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, VALID_EMAIL, VALID_PASSWORD, "   ",
                                VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.specialty").value(SPECIALTY_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_updateVeterinarian_SpecialtyOver80Characters_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // 81 caracteres: solo falla @Size(max = 80).
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                                "S".repeat(81), VALID_PHOTO_URL)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.specialty").value(SPECIALTY_TOO_LONG_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_updateVeterinarian_PhotoUrlOver500Characters_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // 501 caracteres: solo falla @Size(max = 500).
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(veterinarianBody(VALID_DOCUMENT, VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                                VALID_SPECIALTY, "a".repeat(501))))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.photoUrl").value(PHOTO_URL_TOO_LONG_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_updateVeterinarian_NonExistingVeterinarian_Returns404WithErrorBody()
            throws Exception {
        // Arrange
        // El servicio busca al veterinario antes de comprobar la unicidad: si no existe, responde 404.
        Mockito.when(veterinarianService.updateVeterinarian(ArgumentMatchers.eq(99L),
                        ArgumentMatchers.any(VeterinarianUpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Veterinarian", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_VETERINARIAN_BODY))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(404))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VETERINARIAN_NOT_FOUND_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/api/veterinarians/99"));
    }

    @Test
    public void veterinarianController_updateVeterinarian_DocumentOfAnotherVeterinarian_Returns409WithServiceMessage()
            throws Exception {
        // Arrange
        // El documento pertenece a otro veterinario: el servicio lo rechaza con el mensaje de ensureUnique.
        Mockito.when(veterinarianService.updateVeterinarian(ArgumentMatchers.eq(1L),
                        ArgumentMatchers.any(VeterinarianUpdateRequest.class)))
                .thenThrow(new IllegalOperationException(DOCUMENT_TAKEN_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_VETERINARIAN_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(DOCUMENT_TAKEN_MESSAGE));
    }

    @Test
    public void veterinarianController_updateVeterinarian_EmailOfAnotherVeterinarian_Returns409WithServiceMessage()
            throws Exception {
        // Arrange
        // El correo pertenece a otro veterinario: el servicio lo rechaza con el mensaje de ensureUnique.
        Mockito.when(veterinarianService.updateVeterinarian(ArgumentMatchers.eq(1L),
                        ArgumentMatchers.any(VeterinarianUpdateRequest.class)))
                .thenThrow(new IllegalOperationException(EMAIL_TAKEN_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_VETERINARIAN_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(EMAIL_TAKEN_MESSAGE));
    }

    @Test
    public void veterinarianController_updateVeterinarian_RecordRejectedByDatabase_Returns409()
            throws Exception {
        // Arrange
        // Si el cambio colisiona con un registro guardado a la vez, la restricción única de la base de datos lo rechaza.
        Mockito.when(veterinarianService.updateVeterinarian(ArgumentMatchers.eq(1L),
                        ArgumentMatchers.any(VeterinarianUpdateRequest.class)))
                .thenThrow(new DataIntegrityViolationException("violación de restricción única"));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_VETERINARIAN_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(DATA_CONFLICT_MESSAGE));
    }

    // ---------- PUT /api/veterinarians/{id}/active ----------

    @Test
    public void veterinarianController_switchVeterinarianActiveStatus_ActiveTrue_Returns200AndForwardsTrue()
            throws Exception {
        // Arrange
        Mockito.when(veterinarianService.switchVeterinarianActiveStatus(1L, true))
                .thenReturn(veterinarian(1L, VALID_NAME, 12, true));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1/active").param("active", "true"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.active").value(true));
        Mockito.verify(veterinarianService).switchVeterinarianActiveStatus(1L, true);
    }

    @Test
    public void veterinarianController_switchVeterinarianActiveStatus_ActiveFalse_Returns200AndForwardsFalse()
            throws Exception {
        // Arrange
        Mockito.when(veterinarianService.switchVeterinarianActiveStatus(1L, false))
                .thenReturn(veterinarian(1L, VALID_NAME, 12, false));

        // Act + Assert
        // false no es el valor por defecto: el controlador debe reenviarlo tal cual.
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1/active").param("active", "false"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.active").value(false));
        Mockito.verify(veterinarianService).switchVeterinarianActiveStatus(1L, false);
    }

    @Test
    public void veterinarianController_switchVeterinarianActiveStatus_MissingActive_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // Sin el parámetro active no hay estado que aplicar: el handler responde 400 antes de llegar al servicio.
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1/active"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ACTIVE_MISSING_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_switchVeterinarianActiveStatus_InvalidActive_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // "abc" no se puede convertir a boolean: el handler responde 400 antes de llegar al servicio.
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/1/active").param("active", "abc"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ACTIVE_TYPE_MESSAGE));
        Mockito.verifyNoInteractions(veterinarianService);
    }

    @Test
    public void veterinarianController_switchVeterinarianActiveStatus_NonExistingVeterinarian_Returns404()
            throws Exception {
        // Arrange
        Mockito.when(veterinarianService.switchVeterinarianActiveStatus(99L, true))
                .thenThrow(new ResourceNotFoundException("Veterinarian", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/veterinarians/99/active").param("active", "true"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VETERINARIAN_NOT_FOUND_MESSAGE));
    }

    // ---------- GET /api/veterinarians/{id}/treatments ----------

    @Test
    public void veterinarianController_getTreatmentsByVeterinarianId_VeterinarianWithTreatments_Returns200WithList()
            throws Exception {
        // Arrange
        Mockito.when(veterinarianService.getVeterinarianById(2L)).thenReturn(veterinarian(2L, VALID_NAME, 2, true));
        Mockito.when(treatmentService.getTreatmentsByVeterinarianId(2L)).thenReturn(List.of(
                new TreatmentResponse(5L, LocalDateTime.of(2026, 10, 12, 10, 30, 15), 31L, "Buddy", 2L, VALID_NAME,
                        12.5, "Otitis externa", Evolution.IMPROVING, "Menos inflamación que en la consulta anterior.",
                        List.of()),
                new TreatmentResponse(6L, LocalDateTime.of(2026, 10, 13, 9, 0), 32L, "Luna", 2L, VALID_NAME, 4.5,
                        "Fractura de pata", null, null, List.of())));

        // Act
        // El controlador comprueba primero que el veterinario existe y después pide sus tratamientos.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/veterinarians/2/treatments"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(5))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].date").value("2026-10-12T10:30:15"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].petId").value(31))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].petName").value("Buddy"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].veterinarianId").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].veterinarianName").value(VALID_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].weight").value(12.5))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].diagnosis").value("Otitis externa"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].evolution").value("IMPROVING"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].notes")
                        .value("Menos inflamación que en la consulta anterior."))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").value(6))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].evolution").value((Object) null))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].notes").value((Object) null));

        // Assert
        InOrder inOrder = Mockito.inOrder(veterinarianService, treatmentService);
        inOrder.verify(veterinarianService).getVeterinarianById(2L);
        inOrder.verify(treatmentService).getTreatmentsByVeterinarianId(2L);
    }

    @Test
    public void veterinarianController_getTreatmentsByVeterinarianId_NoTreatments_Returns200WithEmptyArray()
            throws Exception {
        // Arrange
        Mockito.when(veterinarianService.getVeterinarianById(2L)).thenReturn(veterinarian(2L, VALID_NAME, 0, true));
        Mockito.when(treatmentService.getTreatmentsByVeterinarianId(2L)).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/veterinarians/2/treatments"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(0));
        Mockito.verify(treatmentService).getTreatmentsByVeterinarianId(2L);
    }

    @Test
    public void veterinarianController_getTreatmentsByVeterinarianId_NonExistingVeterinarian_Returns404AndNoTreatmentCall()
            throws Exception {
        // Arrange
        // El veterinario se comprueba antes de consultar tratamientos: si no existe, responde 404 sin llegar a ellos.
        Mockito.when(veterinarianService.getVeterinarianById(99L))
                .thenThrow(new ResourceNotFoundException("Veterinarian", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/veterinarians/99/treatments"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VETERINARIAN_NOT_FOUND_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/api/veterinarians/99/treatments"));
        Mockito.verifyNoInteractions(treatmentService);
    }
}
