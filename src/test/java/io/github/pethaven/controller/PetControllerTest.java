package io.github.pethaven.controller;

import io.github.pethaven.dto.request.PetRequest;
import io.github.pethaven.dto.response.PetResponse;
import io.github.pethaven.dto.response.TreatmentDrugResponse;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.entity.Evolution;
import io.github.pethaven.entity.Species;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.service.PetService;
import io.github.pethaven.service.TreatmentService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
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
import java.util.Optional;

// Pruebas del controlador de mascotas con mocks: solo se levanta la capa web (MockMvc), sin base de datos.
@WebMvcTest(PetController.class)
public class PetControllerTest {

    // Cuerpo válido con foto: la URL debe llegar intacta al servicio.
    private static final String VALID_PET_BODY = petBody(
            "Buddy", 31L, "DOG", "Golden Retriever", 3, 15.0, "https://images.example.test/buddy.jpg");

    // Cabecera de una PNG: el servicio está simulado; solo se comprueba que los bytes lleguen intactos.
    private static final byte[] PHOTO_BYTES = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    // Mensajes del handler global, de las excepciones de los servicios y de la validación, copiados del código fuente.
    private static final String VALIDATION_MESSAGE = "La petición tiene campos inválidos.";
    private static final String DATA_CONFLICT_MESSAGE =
            "La operación entra en conflicto con datos existentes (valor duplicado o referencia inválida).";
    private static final String PET_NOT_FOUND_MESSAGE = "El recurso 'Pet' con id: '99' no fue encontrado";
    private static final String OWNER_NOT_FOUND_MESSAGE = "El recurso 'Owner' con id: '99' no fue encontrado";
    private static final String VETERINARIAN_NOT_FOUND_MESSAGE =
            "El recurso 'Veterinarian' con id: '99' no fue encontrado";
    private static final String ID_TYPE_MESSAGE = "El parámetro 'id' tiene un formato inválido.";
    private static final String ACTIVE_MISSING_MESSAGE = "Falta el parámetro obligatorio 'active'.";
    private static final String ACTIVE_TYPE_MESSAGE = "El parámetro 'active' tiene un formato inválido.";
    private static final String VETERINARIAN_ID_MISSING_MESSAGE =
            "Falta el parámetro obligatorio 'veterinarianId'.";
    private static final String VETERINARIAN_ID_TYPE_MESSAGE =
            "El parámetro 'veterinarianId' tiene un formato inválido.";
    private static final String STABLE_PET_MESSAGE = "La mascota 'Buddy' ya está estable.";
    private static final String INACTIVE_VETERINARIAN_MESSAGE =
            "El veterinario 'Dra. Valentina Soler' no está activo: no puede dar de alta.";

    // Mensajes de PetRequest, copiados de sus anotaciones de validación.
    private static final String NAME_REQUIRED_MESSAGE = "El nombre es obligatorio";
    private static final String NAME_TOO_LONG_MESSAGE = "El nombre no puede superar los 60 caracteres";
    private static final String OWNER_ID_REQUIRED_MESSAGE = "El ID del dueño es obligatorio";
    private static final String SPECIES_REQUIRED_MESSAGE = "La especie es obligatoria";
    private static final String BREED_REQUIRED_MESSAGE = "La raza es obligatoria";
    private static final String BREED_TOO_LONG_MESSAGE = "La raza no puede superar los 60 caracteres";
    private static final String AGE_REQUIRED_MESSAGE = "La edad es obligatoria";
    private static final String AGE_NEGATIVE_MESSAGE = "La edad no puede ser negativa";
    private static final String AGE_TOO_HIGH_MESSAGE = "La edad no puede superar los 30 años";
    private static final String WEIGHT_REQUIRED_MESSAGE = "El peso es obligatorio";
    private static final String WEIGHT_TOO_LOW_MESSAGE = "El peso debe ser de al menos 0.1 kg";
    private static final String WEIGHT_TOO_HIGH_MESSAGE = "El peso no puede superar los 120 kg";
    private static final String PHOTO_URL_TOO_LONG_MESSAGE =
            "La URL de la foto no puede superar los 500 caracteres";

    private final MockMvc mockMvc;

    @MockitoBean
    private PetService petService;

    @MockitoBean
    private TreatmentService treatmentService;

    @Autowired
    public PetControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    // Mascota de respuesta: peso con escala exacta en binario y dueño con id distinto de cero.
    private static PetResponse pet(Long id, String name, boolean active) {
        return new PetResponse(id, name, Species.DOG, "Golden Retriever", 3, 12.5, null, null, active, 31L,
                "Diego Aguilar");
    }

    // Construye el JSON de la mascota. Los textos se entrecomillan y un null de Java se envía como null JSON,
    // así cada caso de validación cambia solo el campo que quiere probar.
    private static String petBody(String name, Long ownerId, String species, String breed, Integer age,
                                  Double weight, String photoUrl) {
        return """
                {"name":%s,"ownerId":%s,"species":%s,"breed":%s,"age":%s,"weight":%s,"photoUrl":%s}
                """.formatted(quoted(name), ownerId, quoted(species), quoted(breed), age, weight, quoted(photoUrl));
    }

    // Un texto va entre comillas; un null de Java se convierte en null JSON.
    private static String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    // ---------- GET /api/pets ----------

    @Test
    public void petController_getAllPets_NoSearchParam_Returns200AndCallsGetAllPets() throws Exception {
        // Arrange
        Mockito.when(petService.getAllPets()).thenReturn(List.of(pet(1L, "Buddy", true), pet(2L, "Luna", false)));

        // Act + Assert
        // Sin el parámetro search se listan todas las mascotas y nunca se llama a la búsqueda.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/pets"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("Buddy"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].name").value("Luna"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].active").value(false));
        Mockito.verify(petService).getAllPets();
        Mockito.verify(petService, Mockito.never()).searchPets(ArgumentMatchers.anyString());
    }

    @Test
    public void petController_getAllPets_BlankSearch_Returns200AndCallsGetAllPets() throws Exception {
        // Arrange
        Mockito.when(petService.getAllPets()).thenReturn(List.of(pet(1L, "Buddy", true)));

        // Act + Assert
        // Un search formado solo por espacios cuenta como ausente: se listan todas las mascotas.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/pets").param("search", "   "))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1));
        Mockito.verify(petService).getAllPets();
        Mockito.verify(petService, Mockito.never()).searchPets(ArgumentMatchers.anyString());
    }

    @Test
    public void petController_getAllPets_SearchWithSpaces_CallsSearchPetsWithTrimmedTerm() throws Exception {
        // Arrange
        Mockito.when(petService.searchPets("bud")).thenReturn(List.of(pet(1L, "Buddy", true)));

        // Act + Assert
        // El término llega sin espacios sobrantes y la lista completa nunca se pide.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/pets").param("search", "  bud  "))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("Buddy"));
        Mockito.verify(petService).searchPets("bud");
        Mockito.verify(petService, Mockito.never()).getAllPets();
    }

    // ---------- GET /api/pets/{id} ----------

    @Test
    public void petController_getPetById_ExistingId_Returns200WithAllFields() throws Exception {
        // Arrange
        // Cada campo tiene un valor distinto del por defecto para detectar campos intercambiados.
        Mockito.when(petService.getPetById(1L)).thenReturn(new PetResponse(1L, "Luna", Species.CAT, "Persa", 4, 4.5,
                "Otitis externa", "https://images.example.test/luna.jpg", false, 32L, "Laura Mejia"));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/pets/1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Luna"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.species").value("CAT"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.breed").value("Persa"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.age").value(4))
                .andExpect(MockMvcResultMatchers.jsonPath("$.weight").value(4.5))
                .andExpect(MockMvcResultMatchers.jsonPath("$.disease").value("Otitis externa"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.photoUrl").value("https://images.example.test/luna.jpg"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.active").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ownerId").value(32))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ownerName").value("Laura Mejia"));
        Mockito.verify(petService).getPetById(1L);
    }

    @Test
    public void petController_getPetById_NonExistingId_Returns404WithErrorBody() throws Exception {
        // Arrange
        Mockito.when(petService.getPetById(99L)).thenThrow(new ResourceNotFoundException("Pet", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/pets/99"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(404))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Not Found"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(PET_NOT_FOUND_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/api/pets/99"));
    }

    @Test
    public void petController_getPetById_NonNumericId_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // "abc" no es un Long: la conversión del path variable falla y el handler responde 400.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/pets/abc"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ID_TYPE_MESSAGE));
        Mockito.verifyNoInteractions(petService);
    }

    // ---------- POST /api/pets ----------

    @Test
    public void petController_createPet_ValidBody_Returns201WithLocationHeaderAndBody() throws Exception {
        // Arrange
        Mockito.when(petService.createPet(ArgumentMatchers.any(PetRequest.class))).thenReturn(pet(7L, "Buddy", true));

        // Act
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PET_BODY))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(7))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Buddy"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.active").value(true))
                .andReturn();

        // Assert
        Assertions.assertThat(result.getResponse().getHeader("Location")).endsWith("/api/pets/7");

        // El cuerpo llega al servicio tal cual; se comprueban los campos capturados.
        ArgumentCaptor<PetRequest> captor = ArgumentCaptor.forClass(PetRequest.class);
        Mockito.verify(petService).createPet(captor.capture());
        PetRequest sent = captor.getValue();
        Assertions.assertThat(sent.name()).isEqualTo("Buddy");
        Assertions.assertThat(sent.ownerId()).isEqualTo(31L);
        Assertions.assertThat(sent.species()).isEqualTo(Species.DOG);
        Assertions.assertThat(sent.breed()).isEqualTo("Golden Retriever");
        Assertions.assertThat(sent.age()).isEqualTo(3);
        Assertions.assertThat(sent.weight()).isEqualTo(15.0);
        Assertions.assertThat(sent.photoUrl()).isEqualTo("https://images.example.test/buddy.jpg");
    }

    @Test
    public void petController_createPet_MaximumBoundaryValues_Returns201AndCallsService() throws Exception {
        // Arrange
        // Prefijo de la URL, completado con letras 'a' hasta exactamente 500 caracteres.
        String photoUrlPrefix = "https://images.example.test/";
        String maximumPhotoUrl = photoUrlPrefix + "a".repeat(500 - photoUrlPrefix.length());
        Mockito.when(petService.createPet(ArgumentMatchers.any(PetRequest.class))).thenReturn(pet(7L, "Buddy", true));

        // Act
        // Todos los valores están en el límite superior; como los límites son inclusivos, ninguno se rechaza.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("A".repeat(60), 31L, "DOG", "B".repeat(60), 30, 120.0, maximumPhotoUrl)))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        // Assert
        // El servicio recibe los valores tal cual: se comprueban las longitudes exactas y los valores numéricos.
        ArgumentCaptor<PetRequest> captor = ArgumentCaptor.forClass(PetRequest.class);
        Mockito.verify(petService).createPet(captor.capture());
        PetRequest sent = captor.getValue();
        Assertions.assertThat(sent.name().length()).isEqualTo(60);
        Assertions.assertThat(sent.breed().length()).isEqualTo(60);
        Assertions.assertThat(sent.age()).isEqualTo(30);
        Assertions.assertThat(sent.weight()).isEqualTo(120.0);
        Assertions.assertThat(sent.photoUrl().length()).isEqualTo(500);
    }

    @Test
    public void petController_createPet_MinimumBoundaryValues_Returns201AndCallsService() throws Exception {
        // Arrange
        Mockito.when(petService.createPet(ArgumentMatchers.any(PetRequest.class))).thenReturn(pet(7L, "Buddy", true));

        // Act
        // Edad 0 y peso 0.1 son los mínimos inclusivos de @PositiveOrZero y @DecimalMin: se aceptan.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("Buddy", 31L, "DOG", "Golden Retriever", 0, 0.1, null)))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        // Assert
        // El servicio recibe la edad 0 y el peso 0.1 sin cambios.
        ArgumentCaptor<PetRequest> captor = ArgumentCaptor.forClass(PetRequest.class);
        Mockito.verify(petService).createPet(captor.capture());
        PetRequest sent = captor.getValue();
        Assertions.assertThat(sent.age()).isEqualTo(0);
        Assertions.assertThat(sent.weight()).isEqualTo(0.1);
    }

    @Test
    public void petController_createPet_BlankName_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // Solo espacios: @NotBlank lo rechaza.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("   ", 31L, "DOG", "Golden Retriever", 3, 15.0, null)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.name").value(NAME_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(petService);
    }

    @Test
    public void petController_createPet_NameOver60Characters_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // 61 caracteres: solo falla @Size(max = 60).
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("A".repeat(61), 31L, "DOG", "Golden Retriever", 3, 15.0, null)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.name").value(NAME_TOO_LONG_MESSAGE));
        Mockito.verifyNoInteractions(petService);
    }

    @Test
    public void petController_createPet_NullOwnerId_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("Buddy", null, "DOG", "Golden Retriever", 3, 15.0, null)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.ownerId").value(OWNER_ID_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(petService);
    }

    @Test
    public void petController_createPet_NullSpecies_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("Buddy", 31L, null, "Golden Retriever", 3, 15.0, null)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.species").value(SPECIES_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(petService);
    }

    @Test
    public void petController_createPet_BlankBreed_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("Buddy", 31L, "DOG", "   ", 3, 15.0, null)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.breed").value(BREED_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(petService);
    }

    @Test
    public void petController_createPet_BreedOver60Characters_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // 61 caracteres: solo falla @Size(max = 60).
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("Buddy", 31L, "DOG", "B".repeat(61), 3, 15.0, null)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.breed").value(BREED_TOO_LONG_MESSAGE));
        Mockito.verifyNoInteractions(petService);
    }

    @Test
    public void petController_createPet_NullAge_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("Buddy", 31L, "DOG", "Golden Retriever", null, 15.0, null)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.age").value(AGE_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(petService);
    }

    @Test
    public void petController_createPet_NegativeAge_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // -1 años: solo falla @PositiveOrZero; @Max(30) lo acepta.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("Buddy", 31L, "DOG", "Golden Retriever", -1, 15.0, null)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.age").value(AGE_NEGATIVE_MESSAGE));
        Mockito.verifyNoInteractions(petService);
    }

    @Test
    public void petController_createPet_AgeOver30_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // 31 años: solo falla @Max(30); @PositiveOrZero lo acepta.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("Buddy", 31L, "DOG", "Golden Retriever", 31, 15.0, null)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.age").value(AGE_TOO_HIGH_MESSAGE));
        Mockito.verifyNoInteractions(petService);
    }

    @Test
    public void petController_createPet_NullWeight_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("Buddy", 31L, "DOG", "Golden Retriever", 3, null, null)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.weight").value(WEIGHT_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(petService);
    }

    @Test
    public void petController_createPet_WeightBelowMinimum_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // 0.05 kg: solo falla @DecimalMin("0.1").
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("Buddy", 31L, "DOG", "Golden Retriever", 3, 0.05, null)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.weight").value(WEIGHT_TOO_LOW_MESSAGE));
        Mockito.verifyNoInteractions(petService);
    }

    @Test
    public void petController_createPet_WeightAboveMaximum_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // 120.5 kg: solo falla @DecimalMax("120").
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("Buddy", 31L, "DOG", "Golden Retriever", 3, 120.5, null)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.weight").value(WEIGHT_TOO_HIGH_MESSAGE));
        Mockito.verifyNoInteractions(petService);
    }

    @Test
    public void petController_createPet_PhotoUrlOver500Characters_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // 501 caracteres: solo falla @Size(max = 500).
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("Buddy", 31L, "DOG", "Golden Retriever", 3, 15.0, "a".repeat(501))))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.photoUrl").value(PHOTO_URL_TOO_LONG_MESSAGE));
        Mockito.verifyNoInteractions(petService);
    }

    @Test
    public void petController_createPet_NonExistingOwner_Returns404WithErrorBody() throws Exception {
        // Arrange
        // El servicio busca al dueño antes de guardar la mascota: si no existe, responde 404.
        Mockito.when(petService.createPet(ArgumentMatchers.any(PetRequest.class)))
                .thenThrow(new ResourceNotFoundException("Owner", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("Buddy", 99L, "DOG", "Golden Retriever", 3, 15.0, null)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(404))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(OWNER_NOT_FOUND_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/api/pets"));
    }

    @Test
    public void petController_createPet_OwnerRemovedConcurrently_Returns409() throws Exception {
        // Arrange
        // Si el dueño se elimina justo después de buscarlo, la base de datos rechaza la fila por clave foránea
        // y el handler global lo responde como conflicto.
        Mockito.when(petService.createPet(ArgumentMatchers.any(PetRequest.class)))
                .thenThrow(new DataIntegrityViolationException("violación de clave foránea"));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PET_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(DATA_CONFLICT_MESSAGE));
    }

    // ---------- PUT /api/pets/{id} ----------

    @Test
    public void petController_updatePet_ValidBody_Returns200AndCallsServiceWithIdAndRequest() throws Exception {
        // Arrange
        Mockito.when(petService.updatePet(ArgumentMatchers.eq(1L), ArgumentMatchers.any(PetRequest.class)))
                .thenReturn(pet(1L, "Buddy Editado", true));

        // Act
        mockMvc.perform(MockMvcRequestBuilders.put("/api/pets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("Buddy Editado", 32L, "CAT", "Persa", 4, 4.5, null)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Buddy Editado"));

        // Assert
        // El id llega como path variable y el cuerpo completo llega al servicio, incluido el nuevo dueño.
        ArgumentCaptor<PetRequest> captor = ArgumentCaptor.forClass(PetRequest.class);
        Mockito.verify(petService).updatePet(ArgumentMatchers.eq(1L), captor.capture());
        PetRequest sent = captor.getValue();
        Assertions.assertThat(sent.name()).isEqualTo("Buddy Editado");
        Assertions.assertThat(sent.ownerId()).isEqualTo(32L);
        Assertions.assertThat(sent.species()).isEqualTo(Species.CAT);
        Assertions.assertThat(sent.age()).isEqualTo(4);
        Assertions.assertThat(sent.weight()).isEqualTo(4.5);
        Assertions.assertThat(sent.photoUrl()).isNull();
    }

    @Test
    public void petController_updatePet_BlankName_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/pets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("   ", 31L, "DOG", "Golden Retriever", 3, 15.0, null)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.name").value(NAME_REQUIRED_MESSAGE));
        Mockito.verifyNoInteractions(petService);
    }

    @Test
    public void petController_updatePet_NonExistingPet_Returns404() throws Exception {
        // Arrange
        Mockito.when(petService.updatePet(ArgumentMatchers.eq(99L), ArgumentMatchers.any(PetRequest.class)))
                .thenThrow(new ResourceNotFoundException("Pet", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/pets/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PET_BODY))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(PET_NOT_FOUND_MESSAGE));
    }

    @Test
    public void petController_updatePet_NonExistingOwner_Returns404() throws Exception {
        // Arrange
        // Reasignar la mascota a un dueño que no existe también responde 404.
        Mockito.when(petService.updatePet(ArgumentMatchers.eq(1L), ArgumentMatchers.any(PetRequest.class)))
                .thenThrow(new ResourceNotFoundException("Owner", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/pets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(petBody("Buddy", 99L, "DOG", "Golden Retriever", 3, 15.0, null)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(OWNER_NOT_FOUND_MESSAGE));
    }

    @Test
    public void petController_updatePet_OwnerRemovedConcurrently_Returns409() throws Exception {
        // Arrange
        // Al reasignar el dueño, una eliminación concurrente hace que la base de datos rechace el cambio por clave foránea.
        Mockito.when(petService.updatePet(ArgumentMatchers.eq(1L), ArgumentMatchers.any(PetRequest.class)))
                .thenThrow(new DataIntegrityViolationException("violación de clave foránea"));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/pets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PET_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(DATA_CONFLICT_MESSAGE));
    }

    // ---------- PUT /api/pets/{id}/active ----------

    @Test
    public void petController_switchPetActiveStatus_ActiveTrue_Returns200AndForwardsTrue() throws Exception {
        // Arrange
        Mockito.when(petService.switchPetActiveStatus(1L, true)).thenReturn(pet(1L, "Buddy", true));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/pets/1/active").param("active", "true"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.active").value(true));
        Mockito.verify(petService).switchPetActiveStatus(1L, true);
    }

    @Test
    public void petController_switchPetActiveStatus_ActiveFalse_Returns200AndForwardsFalse() throws Exception {
        // Arrange
        Mockito.when(petService.switchPetActiveStatus(1L, false)).thenReturn(pet(1L, "Buddy", false));

        // Act + Assert
        // false no es el valor por defecto: el controlador debe reenviarlo tal cual.
        mockMvc.perform(MockMvcRequestBuilders.put("/api/pets/1/active").param("active", "false"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.active").value(false));
        Mockito.verify(petService).switchPetActiveStatus(1L, false);
    }

    @Test
    public void petController_switchPetActiveStatus_MissingActiveParam_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // Sin el parámetro active no hay estado que aplicar: el handler responde 400 antes de llegar al servicio.
        mockMvc.perform(MockMvcRequestBuilders.put("/api/pets/1/active"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ACTIVE_MISSING_MESSAGE));
        Mockito.verifyNoInteractions(petService);
    }

    @Test
    public void petController_switchPetActiveStatus_InvalidActiveParam_Returns400AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // "abc" no se puede convertir a boolean: el handler responde 400 antes de llegar al servicio.
        mockMvc.perform(MockMvcRequestBuilders.put("/api/pets/1/active").param("active", "abc"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ACTIVE_TYPE_MESSAGE));
        Mockito.verifyNoInteractions(petService);
    }

    @Test
    public void petController_switchPetActiveStatus_NonExistingPet_Returns404() throws Exception {
        // Arrange
        Mockito.when(petService.switchPetActiveStatus(99L, true))
                .thenThrow(new ResourceNotFoundException("Pet", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/pets/99/active").param("active", "true"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(PET_NOT_FOUND_MESSAGE));
    }

    // ---------- GET /api/pets/{id}/treatments ----------

    @Test
    public void petController_getTreatmentsByPetId_PetWithTreatments_Returns200WithList() throws Exception {
        // Arrange
        Mockito.when(petService.getPetById(1L)).thenReturn(pet(1L, "Buddy", true));
        Mockito.when(treatmentService.getTreatmentsByPetId(1L)).thenReturn(List.of(new TreatmentResponse(
                5L, LocalDateTime.of(2026, 10, 12, 10, 30, 15), 1L, "Buddy", 2L, "Dra. Valentina Soler", 12.5,
                "Otitis externa", Evolution.IMPROVING, "Menos inflamación que en la consulta anterior.",
                List.of(new TreatmentDrugResponse(4L, 2L, "Amoxicilina 250mg", 2,
                        new BigDecimal("8500.50"), new BigDecimal("15000.00"))))));

        // Act + Assert
        // El controlador comprueba primero que la mascota existe (petService.getPetById) y después pide sus tratamientos.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/pets/1/treatments"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(5))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].date").value("2026-10-12T10:30:15"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].petId").value(1))
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
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].drugs[0].units").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].drugs[0].unitSalePrice").value(15000.00));
        Mockito.verify(petService).getPetById(1L);
        Mockito.verify(treatmentService).getTreatmentsByPetId(1L);
    }

    @Test
    public void petController_getTreatmentsByPetId_PetWithoutTreatments_Returns200WithEmptyArray() throws Exception {
        // Arrange
        Mockito.when(treatmentService.getTreatmentsByPetId(1L)).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/pets/1/treatments"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(0));
        Mockito.verify(treatmentService).getTreatmentsByPetId(1L);
    }

    @Test
    public void petController_getTreatmentsByPetId_NonExistingPet_Returns404AndDoesNotCallTreatmentService()
            throws Exception {
        // Arrange
        // La mascota se comprueba antes de consultar tratamientos: si no existe, responde 404 sin llegar a ellos.
        Mockito.when(petService.getPetById(99L)).thenThrow(new ResourceNotFoundException("Pet", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/pets/99/treatments"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(PET_NOT_FOUND_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService);
    }

    // ---------- GET /api/pets/{id}/photo ----------

    @Test
    public void petController_getPetPhoto_PhotoAvailable_Returns200WithBytesAndHeaders() throws Exception {
        // Arrange
        Mockito.when(petService.getPetPhoto(1L))
                .thenReturn(Optional.of(new PetService.Photo(PHOTO_BYTES, "image/png")));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/pets/1/photo"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().bytes(PHOTO_BYTES))
                .andExpect(MockMvcResultMatchers.header().string("Content-Type", "image/png"))
                .andExpect(MockMvcResultMatchers.header().string("X-Content-Type-Options", "nosniff"));
        Mockito.verify(petService).getPetPhoto(1L);
    }

    @Test
    public void petController_getPetPhoto_NoPhoto_Returns404WithEmptyBody() throws Exception {
        // Arrange
        // Sin foto, con URL inválida o si la descarga falla, el servicio devuelve un Optional vacío.
        Mockito.when(petService.getPetPhoto(1L)).thenReturn(Optional.empty());

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/pets/1/photo"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.content().string(""));
    }

    @Test
    public void petController_getPetPhoto_NonExistingPet_Returns404WithErrorBody() throws Exception {
        // Arrange
        // Buscar la mascota lanza ResourceNotFoundException antes de descargar nada.
        Mockito.when(petService.getPetPhoto(99L)).thenThrow(new ResourceNotFoundException("Pet", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/pets/99/photo"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(404))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(PET_NOT_FOUND_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/api/pets/99/photo"));
    }

    // ---------- POST /api/pets/{id}/discharge ----------

    @Test
    public void petController_dischargePet_ValidVeterinarian_Returns200AndForwardsBothIds() throws Exception {
        // Arrange
        Mockito.when(treatmentService.dischargePet(1L, 2L)).thenReturn(new TreatmentResponse(
                6L, LocalDateTime.of(2026, 10, 12, 10, 30, 15), 1L, "Buddy", 2L, "Dra. Valentina Soler", 12.5,
                "Otitis externa", Evolution.RECOVERED, "Alta: la mascota queda estable.", List.of()));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets/1/discharge").param("veterinarianId", "2"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(6))
                .andExpect(MockMvcResultMatchers.jsonPath("$.evolution").value("RECOVERED"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.diagnosis").value("Otitis externa"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.notes").value("Alta: la mascota queda estable."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.drugs.length()").value(0));
        Mockito.verify(treatmentService).dischargePet(1L, 2L);
        Mockito.verifyNoInteractions(petService);
    }

    @Test
    public void petController_dischargePet_MissingVeterinarianId_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // Sin veterinarianId no se sabe quién da el alta: el handler responde 400 antes de llegar al servicio.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets/1/discharge"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VETERINARIAN_ID_MISSING_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService, petService);
    }

    @Test
    public void petController_dischargePet_NonNumericVeterinarianId_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        // "abc" no es un Long: el handler responde 400 antes de llegar al servicio.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets/1/discharge").param("veterinarianId", "abc"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VETERINARIAN_ID_TYPE_MESSAGE));
        Mockito.verifyNoInteractions(treatmentService, petService);
    }

    @Test
    public void petController_dischargePet_NonExistingPet_Returns404() throws Exception {
        // Arrange
        Mockito.when(treatmentService.dischargePet(99L, 2L))
                .thenThrow(new ResourceNotFoundException("Pet", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets/99/discharge").param("veterinarianId", "2"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(PET_NOT_FOUND_MESSAGE));
    }

    @Test
    public void petController_dischargePet_NonExistingVeterinarian_Returns404() throws Exception {
        // Arrange
        Mockito.when(treatmentService.dischargePet(1L, 99L))
                .thenThrow(new ResourceNotFoundException("Veterinarian", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets/1/discharge").param("veterinarianId", "99"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VETERINARIAN_NOT_FOUND_MESSAGE));
    }

    @Test
    public void petController_dischargePet_PetAlreadyStable_Returns409() throws Exception {
        // Arrange
        Mockito.when(treatmentService.dischargePet(1L, 2L))
                .thenThrow(new IllegalOperationException(STABLE_PET_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets/1/discharge").param("veterinarianId", "2"))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(STABLE_PET_MESSAGE));
    }

    @Test
    public void petController_dischargePet_InactiveVeterinarian_Returns409() throws Exception {
        // Arrange
        Mockito.when(treatmentService.dischargePet(1L, 2L))
                .thenThrow(new IllegalOperationException(INACTIVE_VETERINARIAN_MESSAGE));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets/1/discharge").param("veterinarianId", "2"))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(INACTIVE_VETERINARIAN_MESSAGE));
    }

    @Test
    public void petController_dischargePet_RecordRejectedByDatabase_Returns409() throws Exception {
        // Arrange
        // Si la mascota o el veterinario se eliminan justo antes de guardar el alta, la base de datos rechaza
        // el registro por clave foránea y el handler global lo responde como conflicto.
        Mockito.when(treatmentService.dischargePet(1L, 2L))
                .thenThrow(new DataIntegrityViolationException("violación de clave foránea"));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/pets/1/discharge").param("veterinarianId", "2"))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(DATA_CONFLICT_MESSAGE));
    }
}
