package io.github.pethaven.controller;

import io.github.pethaven.dto.request.AppointmentRescheduleRequest;
import io.github.pethaven.dto.response.AppointmentResponse;
import io.github.pethaven.dto.response.AvailabilityDayResponse;
import io.github.pethaven.entity.AppointmentActor;
import io.github.pethaven.entity.AppointmentReason;
import io.github.pethaven.entity.AppointmentStatus;
import io.github.pethaven.entity.Role;
import io.github.pethaven.entity.Species;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.service.AppointmentService;
import io.github.pethaven.support.WebMvcSecurityTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// Pruebas del controlador de citas con mocks: solo se levanta la capa web (MockMvc), sin base de datos.
@WebMvcTest(AppointmentController.class)
@WebMvcSecurityTestSupport
public class AppointmentControllerTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 24, 9, 30);
    private static final String VALIDATION_MESSAGE = "La petición tiene campos inválidos.";
    private static final String NOT_FOUND_MESSAGE = "El recurso 'Appointment' con id: '99' no fue encontrado";

    private final MockMvc mockMvc;

    @MockitoBean
    private AppointmentService appointmentService;

    @Autowired
    public AppointmentControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    static AppointmentResponse appointment(Long id, AppointmentStatus status) {
        return new AppointmentResponse(id, START, START.plusMinutes(30), AppointmentReason.CONTROL, status,
                10L, "Buddy", Species.DOG, false, null, null, 1L, "Laura Gómez", "3001234567",
                21L, "Dra. Valentina Soler", "Cojea", null, START.minusDays(2), null, null, null);
    }

    // ---------- GET /api/appointments/availability ----------

    @Test
    @WithMockUser(roles = Role.OWNER)
    public void getAvailability_Returns200WithDaysAndFreeSlots() throws Exception {
        Mockito.when(appointmentService.getAvailability()).thenReturn(List.of(
                new AvailabilityDayResponse(LocalDate.of(2026, 11, 24), true, List.of(LocalTime.of(8, 0), LocalTime.of(8, 30))),
                new AvailabilityDayResponse(LocalDate.of(2026, 11, 29), false, List.of())));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/appointments/availability"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].date").value("2026-11-24"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].open").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].freeSlots[1]").value("08:30:00"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].open").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].freeSlots.length()").value(0));
    }

    // ---------- GET /api/appointments/{id} ----------

    @Test
    @WithMockUser(roles = Role.OWNER)
    public void getAppointmentById_Exists_Returns200WithItsFields() throws Exception {
        Mockito.when(appointmentService.getAppointmentById(5L)).thenReturn(appointment(5L, AppointmentStatus.SCHEDULED));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/appointments/5"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(5))
                .andExpect(MockMvcResultMatchers.jsonPath("$.startTime").value("2026-11-24T09:30:00"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.endTime").value("2026-11-24T10:00:00"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.reason").value("CONTROL"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("SCHEDULED"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.petName").value("Buddy"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.provisionalPet").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.veterinarianName").value("Dra. Valentina Soler"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ownerNotes").value("Cojea"));
    }

    @Test
    @WithMockUser(roles = Role.VETERINARIAN)
    public void getAppointmentById_Missing_Returns404() throws Exception {
        Mockito.when(appointmentService.getAppointmentById(99L))
                .thenThrow(new ResourceNotFoundException("Appointment", "id", 99L));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/appointments/99"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(NOT_FOUND_MESSAGE));
    }

    // ---------- PUT /api/appointments/{id}/cancel ----------

    @Test
    @WithMockUser(roles = Role.OWNER)
    public void cancelAppointment_AsOwner_PassesOwnerAsActor() throws Exception {
        Mockito.when(appointmentService.cancelAppointment(5L, AppointmentActor.OWNER))
                .thenReturn(appointment(5L, AppointmentStatus.CANCELLED));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/appointments/5/cancel"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("CANCELLED"));
        Mockito.verify(appointmentService).cancelAppointment(5L, AppointmentActor.OWNER);
    }

    @Test
    @WithMockUser(roles = Role.VETERINARIAN)
    public void cancelAppointment_AsVeterinarian_PassesVeterinarianAsActor() throws Exception {
        Mockito.when(appointmentService.cancelAppointment(5L, AppointmentActor.VETERINARIAN))
                .thenReturn(appointment(5L, AppointmentStatus.CANCELLED));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/appointments/5/cancel"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        Mockito.verify(appointmentService).cancelAppointment(5L, AppointmentActor.VETERINARIAN);
    }

    @Test
    @WithMockUser(roles = Role.OWNER)
    public void cancelAppointment_BusinessRuleFails_Returns409WithItsMessage() throws Exception {
        String message = "Las citas se pueden cancelar hasta 2 horas antes: comunícate con la clínica.";
        Mockito.when(appointmentService.cancelAppointment(5L, AppointmentActor.OWNER))
                .thenThrow(new IllegalOperationException(message));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/appointments/5/cancel"))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(message));
    }

    // ---------- PUT /api/appointments/{id}/reschedule ----------

    @Test
    @WithMockUser(roles = Role.OWNER)
    public void rescheduleAppointment_ValidBody_PassesRequestAndActor() throws Exception {
        Mockito.when(appointmentService.rescheduleAppointment(ArgumentMatchers.eq(5L), ArgumentMatchers.any(),
                ArgumentMatchers.eq(AppointmentActor.OWNER))).thenReturn(appointment(5L, AppointmentStatus.SCHEDULED));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/appointments/5/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startTime\":\"2026-11-25T10:00:00\",\"ownerNotes\":\"Mejor en la mañana\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(5));
        Mockito.verify(appointmentService).rescheduleAppointment(5L,
                new AppointmentRescheduleRequest(LocalDateTime.of(2026, 11, 25, 10, 0), "Mejor en la mañana"),
                AppointmentActor.OWNER);
    }

    @Test
    @WithMockUser(roles = Role.VETERINARIAN)
    public void rescheduleAppointment_WithoutStartTime_Returns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/appointments/5/reschedule")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(VALIDATION_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.startTime")
                        .value("La fecha y hora de la cita es obligatoria"));
        Mockito.verifyNoInteractions(appointmentService);
    }

    @Test
    @WithMockUser(roles = Role.OWNER)
    public void rescheduleAppointment_NotesTooLong_Returns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/appointments/5/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startTime\":\"2026-11-25T10:00:00\",\"ownerNotes\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.ownerNotes")
                        .value("Las notas no pueden superar los 500 caracteres"));
    }
}
