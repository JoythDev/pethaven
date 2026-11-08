package io.github.pethaven.controller;

import io.github.pethaven.support.WebMvcSecurityTestSupport;
import io.github.pethaven.dto.response.AdministratorDashboardResponse;
import io.github.pethaven.service.AdministratorDashboardService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Pruebas del dashboard de administrador con mocks: solo se levanta la capa web (MockMvc), sin base de datos.
@WebMvcTest(AdministratorDashboardController.class)
@WebMvcSecurityTestSupport
public class AdministratorDashboardControllerTest {

    private static final String DASHBOARD_PATH = "/api/administrators/dashboard";

    // Mensajes del handler global, copiados del código fuente.
    private static final String UNEXPECTED_ERROR_MESSAGE = "Ocurrió un error inesperado en el servidor.";
    private static final String METHOD_NOT_ALLOWED_MESSAGE = "El método HTTP no está permitido para este recurso.";

    private final MockMvc mockMvc;

    @MockitoBean
    private AdministratorDashboardService dashboardService;

    @Autowired
    public AdministratorDashboardControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    // ---------- Datos de prueba ----------

    // Indicadores con un valor distinto en cada campo, para detectar campos intercambiados.
    private static AdministratorDashboardResponse fullSummary() {
        return new AdministratorDashboardResponse(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 1),
                "America/Bogota",
                15,
                List.of(
                        new AdministratorDashboardResponse.MedicationTreatmentCount(8L, "Amoxicilina 250mg", 4),
                        new AdministratorDashboardResponse.MedicationTreatmentCount(12L, "Meloxicam 5mg", 2)),
                5,
                1,
                150,
                143,
                new BigDecimal("350000.00"),
                new BigDecimal("128500.00"),
                List.of(
                        new AdministratorDashboardResponse.TopMedicationByUnitsSold(8L, "Amoxicilina 250mg", 72),
                        new AdministratorDashboardResponse.TopMedicationByUnitsSold(12L, "Meloxicam 5mg", 40),
                        new AdministratorDashboardResponse.TopMedicationByUnitsSold(3L, "Ivermectina 1%", 25)));
    }

    // ---------- GET /api/administrators/dashboard ----------

    @Test
    public void administratorDashboardController_getDashboardSummary_FullSummary_Returns200WithAllFields()
            throws Exception {
        // Arrange
        Mockito.when(dashboardService.getDashboardSummary()).thenReturn(fullSummary());

        // Act
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get(DASHBOARD_PATH))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.monthlyPeriodStart").value("2026-09-01"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.monthlyPeriodEndExclusive").value("2026-10-01"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.monthlyPeriodTimeZone").value("America/Bogota"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.administeredTreatmentsLastMonth").value(15))
                .andExpect(MockMvcResultMatchers.jsonPath("$.treatmentsByMedicationLastMonth.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.treatmentsByMedicationLastMonth[0].medicationId")
                        .value(8))
                .andExpect(MockMvcResultMatchers.jsonPath("$.treatmentsByMedicationLastMonth[0].medicationName")
                        .value("Amoxicilina 250mg"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.treatmentsByMedicationLastMonth[0].treatmentCount")
                        .value(4))
                .andExpect(MockMvcResultMatchers.jsonPath("$.treatmentsByMedicationLastMonth[1].medicationId")
                        .value(12))
                .andExpect(MockMvcResultMatchers.jsonPath("$.treatmentsByMedicationLastMonth[1].medicationName")
                        .value("Meloxicam 5mg"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.treatmentsByMedicationLastMonth[1].treatmentCount")
                        .value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.activeVeterinarians").value(5))
                .andExpect(MockMvcResultMatchers.jsonPath("$.inactiveVeterinarians").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalPets").value(150))
                .andExpect(MockMvcResultMatchers.jsonPath("$.activePets").value(143))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalSales").value(350000.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.grossProfit").value(128500.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.topThreeMedicationsByUnitsSold.length()").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.topThreeMedicationsByUnitsSold[0].medicationId")
                        .value(8))
                .andExpect(MockMvcResultMatchers.jsonPath("$.topThreeMedicationsByUnitsSold[0].medicationName")
                        .value("Amoxicilina 250mg"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.topThreeMedicationsByUnitsSold[0].unitsSold").value(72))
                .andExpect(MockMvcResultMatchers.jsonPath("$.topThreeMedicationsByUnitsSold[1].medicationId")
                        .value(12))
                .andExpect(MockMvcResultMatchers.jsonPath("$.topThreeMedicationsByUnitsSold[1].medicationName")
                        .value("Meloxicam 5mg"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.topThreeMedicationsByUnitsSold[1].unitsSold").value(40))
                .andExpect(MockMvcResultMatchers.jsonPath("$.topThreeMedicationsByUnitsSold[2].medicationId")
                        .value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.topThreeMedicationsByUnitsSold[2].medicationName")
                        .value("Ivermectina 1%"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.topThreeMedicationsByUnitsSold[2].unitsSold").value(25))
                .andReturn();

        // Assert
        // jsonPath compara los importes como números y no ve su escala: el texto exacto del cuerpo fija
        // dos decimales ("350000.00", no "350000.0" ni "350000").
        String body = result.getResponse().getContentAsString();
        Assertions.assertThat(body).contains("\"totalSales\":350000.00").contains("\"grossProfit\":128500.00");
        Mockito.verify(dashboardService, Mockito.times(1)).getDashboardSummary();
    }

    @Test
    public void administratorDashboardController_getDashboardSummary_NoActivity_Returns200WithZerosAndEmptyLists()
            throws Exception {
        // Arrange
        // Mes sin tratamientos, sin veterinarios, sin mascotas y sin ventas: contadores en cero y listas vacías.
        Mockito.when(dashboardService.getDashboardSummary()).thenReturn(new AdministratorDashboardResponse(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 1),
                "America/Bogota",
                0,
                List.of(),
                0,
                0,
                0,
                0,
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                List.of()));

        // Act
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get(DASHBOARD_PATH))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.administeredTreatmentsLastMonth").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.treatmentsByMedicationLastMonth.length()").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.activeVeterinarians").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.inactiveVeterinarians").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalPets").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.activePets").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalSales").value(0.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.grossProfit").value(0.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.topThreeMedicationsByUnitsSold.length()").value(0))
                .andReturn();

        // Assert
        String body = result.getResponse().getContentAsString();
        Assertions.assertThat(body).contains("\"totalSales\":0.00").contains("\"grossProfit\":0.00");
        Mockito.verify(dashboardService, Mockito.times(1)).getDashboardSummary();
    }

    @Test
    public void administratorDashboardController_getDashboardSummary_UnexpectedException_Returns500WithGenericBody()
            throws Exception {
        // Arrange
        // Un RuntimeException no mapeado llega al handler genérico, que no expone el detalle interno al cliente.
        Mockito.when(dashboardService.getDashboardSummary())
                .thenThrow(new RuntimeException("fallo simulado del servicio"));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.get(DASHBOARD_PATH))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(500))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Internal Server Error"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(UNEXPECTED_ERROR_MESSAGE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value(DASHBOARD_PATH));
        Mockito.verify(dashboardService, Mockito.times(1)).getDashboardSummary();
    }

    // ---------- Método HTTP no permitido ----------

    @Test
    public void administratorDashboardController_getDashboardSummary_PostMethod_Returns405AndDoesNotCallService()
            throws Exception {
        // Act + Assert
        // El endpoint solo acepta GET: el POST se rechaza antes de llegar al servicio.
        mockMvc.perform(MockMvcRequestBuilders.post(DASHBOARD_PATH))
                .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(405))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Method Not Allowed"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(METHOD_NOT_ALLOWED_MESSAGE));
        Mockito.verifyNoInteractions(dashboardService);
    }
}
