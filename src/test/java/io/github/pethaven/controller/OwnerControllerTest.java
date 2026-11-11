package io.github.pethaven.controller;

import io.github.pethaven.support.WebMvcSecurityTestSupport;
import io.github.pethaven.support.WithAnyApiRole;
import io.github.pethaven.dto.request.OwnerRequest;
import io.github.pethaven.dto.request.OwnerUpdateRequest;
import io.github.pethaven.dto.response.OwnerResponse;
import io.github.pethaven.dto.response.PetResponse;
import io.github.pethaven.entity.Species;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

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
}
