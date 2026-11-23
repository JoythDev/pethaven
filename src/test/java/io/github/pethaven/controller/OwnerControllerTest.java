package io.github.pethaven.controller;

import io.github.pethaven.support.WebMvcSecurityTestSupport;
import io.github.pethaven.support.WithAnyApiRole;
import io.github.pethaven.dto.request.AppointmentRequest;
import io.github.pethaven.dto.request.OwnerRequest;
import io.github.pethaven.dto.request.ProvisionalPetRequest;
import io.github.pethaven.dto.request.OwnerUpdateRequest;
import io.github.pethaven.dto.response.AppointmentResponse;
import io.github.pethaven.dto.response.OwnerResponse;
import io.github.pethaven.dto.response.PetResponse;
import io.github.pethaven.entity.AppointmentReason;
import io.github.pethaven.entity.AppointmentStatus;
import io.github.pethaven.entity.Species;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.service.AppointmentService;
import io.github.pethaven.service.OwnerService;
import io.github.pethaven.service.PetService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.List;

// Pruebas del controlador con mocks: solo se levanta la capa web (MockMvc), sin base de datos.
// Los servicios son falsos y se les dice qué devolver; aquí se revisa que el controlador
// responda con la ruta, el código HTTP y el JSON correctos para cada tipo de consulta.
@WebMvcTest(OwnerController.class)
@WebMvcSecurityTestSupport
@WithAnyApiRole
public class OwnerControllerTest {

    private static final String VALID_CREATE_BODY = """
            {"document":"111222333","name":"Carlos Perez","email":"carlos@pethaven.test","password":"password123","phone":"3001234567"}
            """;

    private static final String VALID_UPDATE_BODY = """
            {"document":"111222333","name":"Carlos Perez Editado","email":"carlos@pethaven.test","password":null,"phone":"3001234567"}
            """;

    // Nombre en blanco y contraseña demasiado corta: no pasa la validación.
    private static final String INVALID_CREATE_BODY = """
            {"document":"111222333","name":"","email":"carlos@pethaven.test","password":"123","phone":"3001234567"}
            """;

    private final MockMvc mockMvc;

    @MockitoBean
    private OwnerService ownerService;

    @MockitoBean
    private PetService petService;

    @MockitoBean
    private AppointmentService appointmentService;

    @Autowired
    public OwnerControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private OwnerResponse owner(Long id, String name) {
        return new OwnerResponse(id, "DOC-" + id, name, "owner" + id + "@pethaven.test", "3001234567");
    }

    // ---------- GET ----------

    @Test
    public void ownerController_getAllOwners_TwoOwners_Returns200WithList() throws Exception {
        // Arrange
        Mockito.when(ownerService.getAllOwners()).thenReturn(List.of(owner(1L, "Ana"), owner(2L, "Luis")));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/owners"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("Ana"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].name").value("Luis"));
    }

    @Test
    public void ownerController_getOwnerById_ExistingId_Returns200WithOwner() throws Exception {
        // Arrange
        Mockito.when(ownerService.getOwnerById(1L)).thenReturn(owner(1L, "Ana"));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/owners/1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Ana"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("owner1@pethaven.test"));
    }

    @Test
    public void ownerController_getOwnerById_NonExistingId_Returns404() throws Exception {
        // Arrange
        Mockito.when(ownerService.getOwnerById(99L)).thenThrow(new ResourceNotFoundException("Owner", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/owners/99"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(404));
    }

    @Test
    public void ownerController_getPetsByOwnerId_ExistingOwner_Returns200WithPets() throws Exception {
        // Arrange
        Mockito.when(ownerService.getOwnerById(1L)).thenReturn(owner(1L, "Ana"));
        PetResponse pet = new PetResponse(
                7L, "Firulais", Species.DOG, "Labrador", 3, 10.5, null, null, true, 1L, "Ana");
        Mockito.when(petService.getPetsByOwnerId(1L)).thenReturn(List.of(pet));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/owners/1/pets"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("Firulais"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].ownerId").value(1));
    }

    // ---------- POST ----------

    @Test
    public void ownerController_createOwner_ValidBody_Returns201WithLocationHeader() throws Exception {
        // Arrange
        Mockito.when(ownerService.createOwner(ArgumentMatchers.any(OwnerRequest.class)))
                .thenReturn(owner(3L, "Carlos Perez"));

        // Act
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Carlos Perez"))
                .andReturn();

        // Assert
        Assertions.assertThat(result.getResponse().getHeader("Location")).endsWith("/api/owners/3");
    }

    @Test
    public void ownerController_createOwner_InvalidBody_Returns400AndDoesNotCallService() throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_CREATE_BODY))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors").exists());
        Mockito.verifyNoInteractions(ownerService);
    }

    @Test
    public void ownerController_createOwner_DuplicatedDocument_Returns409() throws Exception {
        // Arrange
        Mockito.when(ownerService.createOwner(ArgumentMatchers.any(OwnerRequest.class)))
                .thenThrow(new IllegalOperationException("Ya existe un dueño con ese documento."));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409));
    }

    // ---------- PUT ----------

    @Test
    public void ownerController_updateOwner_ValidBody_Returns200WithUpdatedOwner() throws Exception {
        // Arrange
        Mockito.when(ownerService.updateOwner(ArgumentMatchers.eq(1L), ArgumentMatchers.any(OwnerUpdateRequest.class)))
                .thenReturn(owner(1L, "Carlos Perez Editado"));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/owners/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Carlos Perez Editado"));
    }

    @Test
    public void ownerController_updateOwner_NonExistingId_Returns404() throws Exception {
        // Arrange
        Mockito.when(ownerService.updateOwner(ArgumentMatchers.eq(99L), ArgumentMatchers.any(OwnerUpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Owner", "id", 99L));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/owners/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    // ---------- DELETE ----------

    @Test
    public void ownerController_deleteOwner_ExistingId_Returns204AndCallsService() throws Exception {
        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/owners/1"))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        Mockito.verify(ownerService).deleteOwnerById(1L);
    }

    @Test
    public void ownerController_deleteOwner_OwnerWithTreatments_Returns409() throws Exception {
        // Arrange
        Mockito.doThrow(new IllegalOperationException("El dueño tiene mascotas con tratamientos registrados."))
                .when(ownerService).deleteOwnerById(1L);

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/owners/1"))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409));
    }

    // ---------- Citas del dueño ----------

    private static final String PET_CHOICE_MESSAGE = "Indica una mascota registrada o los datos de una mascota nueva, pero no ambos";

    private ResultActions bookWith(String body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/owners/1/appointments")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    public void getAppointmentsByOwnerId_Returns200WithTheOwnersAppointments() throws Exception {
        Mockito.when(appointmentService.getAppointmentsByOwnerId(1L)).thenReturn(List.of(
                AppointmentControllerTest.appointment(5L, AppointmentStatus.COMPLETED),
                AppointmentControllerTest.appointment(6L, AppointmentStatus.SCHEDULED)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/owners/1/appointments"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").value(6));
    }

    @Test
    public void bookAppointment_RegisteredPet_Returns201WithLocationOfTheAppointment() throws Exception {
        AppointmentResponse booked = AppointmentControllerTest.appointment(7L, AppointmentStatus.SCHEDULED);
        Mockito.when(appointmentService.bookAppointment(ArgumentMatchers.eq(1L), ArgumentMatchers.any())).thenReturn(booked);

        bookWith("{\"petId\":10,\"reason\":\"CONTROL\",\"startTime\":\"2026-11-24T09:30:00\",\"ownerNotes\":\"Cojea\"}")
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.header().string("Location", "http://localhost/api/appointments/7"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(7));
        Mockito.verify(appointmentService).bookAppointment(1L, new AppointmentRequest(10L, null, AppointmentReason.CONTROL,
                LocalDateTime.of(2026, 11, 24, 9, 30), "Cojea"));
    }

    @Test
    public void bookAppointment_ProvisionalPet_PassesItsData() throws Exception {
        Mockito.when(appointmentService.bookAppointment(ArgumentMatchers.eq(1L), ArgumentMatchers.any()))
                .thenReturn(AppointmentControllerTest.appointment(8L, AppointmentStatus.SCHEDULED));

        bookWith("{\"provisionalPet\":{\"name\":\"Kira\",\"species\":\"CAT\",\"age\":2},\"startTime\":\"2026-11-24T09:30:00\"}")
                .andExpect(MockMvcResultMatchers.status().isCreated());
        Mockito.verify(appointmentService).bookAppointment(1L, new AppointmentRequest(null,
                new ProvisionalPetRequest("Kira", Species.CAT, null, 2), null, LocalDateTime.of(2026, 11, 24, 9, 30), null));
    }

    @Test
    public void bookAppointment_BothOrNeitherPet_Returns400() throws Exception {
        for (String body : new String[]{
                "{\"startTime\":\"2026-11-24T09:30:00\"}",
                "{\"petId\":10,\"provisionalPet\":{\"name\":\"Kira\",\"species\":\"CAT\"},\"startTime\":\"2026-11-24T09:30:00\"}"}) {
            bookWith(body)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.errors.petChoiceValid").value(PET_CHOICE_MESSAGE));
        }
        Mockito.verifyNoInteractions(appointmentService);
    }

    @Test
    public void bookAppointment_InvalidFields_Returns400WithEachMessage() throws Exception {
        bookWith("{\"provisionalPet\":{\"name\":\" \",\"species\":null,\"age\":41},\"ownerNotes\":\"" + "a".repeat(501) + "\"}")
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.startTime").value("La fecha y hora de la cita es obligatoria"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors['provisionalPet.name']").value("El nombre de la mascota es obligatorio"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors['provisionalPet.species']").value("La especie de la mascota es obligatoria"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors['provisionalPet.age']").value("La edad no puede superar los 40 años"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.ownerNotes").value("Las notas no pueden superar los 500 caracteres"));
        Mockito.verifyNoInteractions(appointmentService);
    }

    @Test
    public void bookAppointment_BusinessRuleFails_Returns409() throws Exception {
        Mockito.when(appointmentService.bookAppointment(ArgumentMatchers.eq(1L), ArgumentMatchers.any()))
                .thenThrow(new IllegalOperationException("Ese motivo no se puede reservar desde la app."));

        bookWith("{\"petId\":10,\"reason\":\"DISCHARGE\",\"startTime\":\"2026-11-24T09:30:00\"}")
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Ese motivo no se puede reservar desde la app."));
    }
}
