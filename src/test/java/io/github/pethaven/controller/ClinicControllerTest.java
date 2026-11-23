package io.github.pethaven.controller;

import io.github.pethaven.dto.response.ClinicHoursResponse;
import io.github.pethaven.dto.response.ClinicResponse;
import io.github.pethaven.service.ClinicService;
import io.github.pethaven.support.WebMvcSecurityTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

// Pruebas del controlador de la clínica con mocks: público, sin token.
@WebMvcTest(ClinicController.class)
@WebMvcSecurityTestSupport
public class ClinicControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private ClinicService clinicService;

    @Autowired
    public ClinicControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    public void getClinic_WithoutToken_Returns200WithSiteAndHours() throws Exception {
        Mockito.when(clinicService.getClinic()).thenReturn(new ClinicResponse("PetHaven Chapinero", "Calle 63 n.º 9-41",
                "601 555 0123", "America/Bogota", 30, List.of(
                        new ClinicHoursResponse(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(17, 0)),
                        new ClinicHoursResponse(DayOfWeek.SUNDAY, null, null))));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/clinic"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("PetHaven Chapinero"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.address").value("Calle 63 n.º 9-41"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.phone").value("601 555 0123"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.timeZone").value("America/Bogota"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.slotMinutes").value(30))
                .andExpect(MockMvcResultMatchers.jsonPath("$.hours[0].day").value("MONDAY"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.hours[0].open").value("08:00:00"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.hours[0].close").value("17:00:00"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.hours[1].open").doesNotExist());
    }
}
