package io.github.pethaven.service;

import io.github.pethaven.dto.response.AdministratorDashboardResponse;
import io.github.pethaven.entity.Evolution;
import io.github.pethaven.repository.PetRepository;
import io.github.pethaven.repository.TreatmentDrugRepository;
import io.github.pethaven.repository.TreatmentRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import io.github.pethaven.repository.projection.MedicationTreatmentCountProjection;
import io.github.pethaven.repository.projection.TopMedicationUnitsProjection;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

// Pruebas del servicio del dashboard con repositorios simulados y la fecha del reporte congelada, sin Spring ni base de datos.
@ExtendWith(MockitoExtension.class)
public class AdministratorDashboardServiceMockTest {

    @Mock
    private PetRepository petRepository;
    @Mock
    private VeterinarianRepository veterinarianRepository;
    @Mock
    private TreatmentRepository treatmentRepository;
    @Mock
    private TreatmentDrugRepository treatmentDrugRepository;

    private AdministratorDashboardServiceImpl administratorDashboardService;

    @BeforeEach
    public void setUp() {
        administratorDashboardService = new AdministratorDashboardServiceImpl(
                petRepository, veterinarianRepository, treatmentRepository, treatmentDrugRepository);
    }

    // ---------- Helpers ----------

    private AdministratorDashboardResponse summaryOn(LocalDate today) {
        // El servicio calcula el período con LocalDate.now(ZoneId.of("America/Bogota")) y no recibe un Clock.
        // Se simula la clase LocalDate, pero solo la llamada con esa zona exacta; cualquier otra zona ejecuta
        // el reloj real, de modo que un cambio de zona en el servicio haría fallar las fechas esperadas.
        try (MockedStatic<LocalDate> mockedLocalDate =
                     Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            mockedLocalDate.when(() -> LocalDate.now(ZoneId.of("America/Bogota"))).thenReturn(today);
            return administratorDashboardService.getDashboardSummary();
        }
    }

    private void stubMedicationLists(
            List<MedicationTreatmentCountProjection> treatmentCounts,
            List<TopMedicationUnitsProjection> topMedications) {
        // Las dos consultas del dashboard se ejecutan siempre: devolver null provocaría un NullPointerException.
        // Se simulan con matchers porque los argumentos exactos del período se verifican en las pruebas de fechas.
        Mockito.when(treatmentDrugRepository.findMedicationTreatmentCountsBetween(
                        ArgumentMatchers.any(LocalDateTime.class),
                        ArgumentMatchers.any(LocalDateTime.class),
                        ArgumentMatchers.any(Evolution.class)))
                .thenReturn(treatmentCounts);
        Mockito.when(treatmentDrugRepository.findTopMedicationsByAllTimeUnitsSold(
                        ArgumentMatchers.any(Pageable.class)))
                .thenReturn(topMedications);
    }

    private MedicationTreatmentCountProjection medicationTreatmentCount(
            Long medicationId, String medicationName, Long treatmentCount) {
        return new MedicationTreatmentCountProjection() {
            @Override
            public Long getMedicationId() {
                return medicationId;
            }

            @Override
            public String getMedicationName() {
                return medicationName;
            }

            @Override
            public Long getTreatmentCount() {
                return treatmentCount;
            }
        };
    }

    private TopMedicationUnitsProjection topMedicationUnits(
            Long medicationId, String medicationName, Long unitsSold) {
        return new TopMedicationUnitsProjection() {
            @Override
            public Long getMedicationId() {
                return medicationId;
            }

            @Override
            public String getMedicationName() {
                return medicationName;
            }

            @Override
            public Long getUnitsSold() {
                return unitsSold;
            }
        };
    }

    // ---------- getDashboardSummary ----------

    @Test
    public void administratorDashboardService_getDashboardSummary_MidMonth_UsesPreviousCalendarMonthAndMapsEveryScalar() {
        // Arrange
        LocalDate today = LocalDate.of(2026, 11, 15);
        stubMedicationLists(List.of(), List.of());
        Mockito.when(treatmentRepository.countAdministeredTreatmentsBetween(
                        ArgumentMatchers.any(LocalDateTime.class),
                        ArgumentMatchers.any(LocalDateTime.class),
                        ArgumentMatchers.any(Evolution.class)))
                .thenReturn(11L);
        Mockito.when(veterinarianRepository.countByActiveTrue()).thenReturn(4L);
        Mockito.when(veterinarianRepository.countByActiveFalse()).thenReturn(2L);
        Mockito.when(petRepository.count()).thenReturn(30L);
        Mockito.when(petRepository.countByActiveTrue()).thenReturn(25L);
        Mockito.when(treatmentDrugRepository.sumAllTimeSales()).thenReturn(new BigDecimal("250000.00"));
        Mockito.when(treatmentDrugRepository.sumAllTimeGrossProfit()).thenReturn(new BigDecimal("81000.00"));

        // Act
        AdministratorDashboardResponse response = summaryOn(today);

        // Assert
        // El período es el mes calendario anterior completo: del 1 de octubre al 1 de noviembre, con fin exclusivo.
        Assertions.assertThat(response.monthlyPeriodStart()).isEqualTo(LocalDate.of(2026, 10, 1));
        Assertions.assertThat(response.monthlyPeriodEndExclusive()).isEqualTo(LocalDate.of(2026, 11, 1));
        Assertions.assertThat(response.monthlyPeriodTimeZone()).isEqualTo("America/Bogota");
        // Cada KPI se toma del repositorio correspondiente: ningún valor se reutiliza ni se cruza.
        Assertions.assertThat(response.administeredTreatmentsLastMonth()).isEqualTo(11L);
        Assertions.assertThat(response.activeVeterinarians()).isEqualTo(4L);
        Assertions.assertThat(response.inactiveVeterinarians()).isEqualTo(2L);
        Assertions.assertThat(response.totalPets()).isEqualTo(30L);
        Assertions.assertThat(response.activePets()).isEqualTo(25L);
        Assertions.assertThat(response.totalSales()).isEqualByComparingTo(new BigDecimal("250000.00"));
        Assertions.assertThat(response.grossProfit()).isEqualByComparingTo(new BigDecimal("81000.00"));

        // Ambas consultas del período reciben el rango exacto en la zona del reporte y excluyen las altas RECOVERED.
        Mockito.verify(treatmentRepository).countAdministeredTreatmentsBetween(
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 11, 1, 0, 0),
                Evolution.RECOVERED);
        Mockito.verify(treatmentDrugRepository).findMedicationTreatmentCountsBetween(
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 11, 1, 0, 0),
                Evolution.RECOVERED);
    }

    @Test
    public void administratorDashboardService_getDashboardSummary_January_RollsTheYearBackForThePreviousPeriod() {
        // Arrange
        LocalDate today = LocalDate.of(2026, 1, 10);
        stubMedicationLists(List.of(), List.of());

        // Act
        AdministratorDashboardResponse response = summaryOn(today);

        // Assert
        // El cambio de año no rompe el cálculo: el mes anterior de enero de 2026 es diciembre de 2025.
        Assertions.assertThat(response.monthlyPeriodStart()).isEqualTo(LocalDate.of(2025, 12, 1));
        Assertions.assertThat(response.monthlyPeriodEndExclusive()).isEqualTo(LocalDate.of(2026, 1, 1));
        Mockito.verify(treatmentRepository).countAdministeredTreatmentsBetween(
                LocalDateTime.of(2025, 12, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0),
                Evolution.RECOVERED);
        Mockito.verify(treatmentDrugRepository).findMedicationTreatmentCountsBetween(
                LocalDateTime.of(2025, 12, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0),
                Evolution.RECOVERED);
    }

    @Test
    public void administratorDashboardService_getDashboardSummary_FirstDayOfMonth_ReportsTheImmediatelyPreviousMonth() {
        // Arrange
        LocalDate today = LocalDate.of(2026, 3, 1);
        stubMedicationLists(List.of(), List.of());

        // Act
        AdministratorDashboardResponse response = summaryOn(today);

        // Assert
        // El día 1 también reporta el mes anterior completo: el mes en curso no entra en el período.
        Assertions.assertThat(response.monthlyPeriodStart()).isEqualTo(LocalDate.of(2026, 2, 1));
        Assertions.assertThat(response.monthlyPeriodEndExclusive()).isEqualTo(LocalDate.of(2026, 3, 1));
        Mockito.verify(treatmentRepository).countAdministeredTreatmentsBetween(
                LocalDateTime.of(2026, 2, 1, 0, 0),
                LocalDateTime.of(2026, 3, 1, 0, 0),
                Evolution.RECOVERED);
        Mockito.verify(treatmentDrugRepository).findMedicationTreatmentCountsBetween(
                LocalDateTime.of(2026, 2, 1, 0, 0),
                LocalDateTime.of(2026, 3, 1, 0, 0),
                Evolution.RECOVERED);
    }

    @Test
    public void administratorDashboardService_getDashboardSummary_MedicationLists_MapsRowsInOrderAndAsksForTheTopThree() {
        // Arrange
        stubMedicationLists(
                List.of(
                        medicationTreatmentCount(8L, "Amoxicilina 250mg", 4L),
                        medicationTreatmentCount(2L, "Ibuprofeno 400mg", 7L)),
                List.of(
                        topMedicationUnits(8L, "Amoxicilina 250mg", 72L),
                        topMedicationUnits(2L, "Ibuprofeno 400mg", 45L),
                        topMedicationUnits(5L, "Meloxicam 2mg", 31L)));

        // Act
        AdministratorDashboardResponse response = summaryOn(LocalDate.of(2026, 11, 15));

        // Assert
        // Cada fila del repositorio se proyecta con su id, su nombre y su agregado, conservando el orden recibido.
        Assertions.assertThat(response.treatmentsByMedicationLastMonth()).containsExactly(
                new AdministratorDashboardResponse.MedicationTreatmentCount(8L, "Amoxicilina 250mg", 4L),
                new AdministratorDashboardResponse.MedicationTreatmentCount(2L, "Ibuprofeno 400mg", 7L));
        Assertions.assertThat(response.topThreeMedicationsByUnitsSold()).containsExactly(
                new AdministratorDashboardResponse.TopMedicationByUnitsSold(8L, "Amoxicilina 250mg", 72L),
                new AdministratorDashboardResponse.TopMedicationByUnitsSold(2L, "Ibuprofeno 400mg", 45L),
                new AdministratorDashboardResponse.TopMedicationByUnitsSold(5L, "Meloxicam 2mg", 31L));

        // El ranking histórico se limita a las tres primeras posiciones desde la primera página.
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Mockito.verify(treatmentDrugRepository).findTopMedicationsByAllTimeUnitsSold(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        Assertions.assertThat(pageable).isEqualTo(PageRequest.of(0, 3));
        Assertions.assertThat(pageable.getPageNumber()).isZero();
        Assertions.assertThat(pageable.getPageSize()).isEqualTo(3);
    }

    @Test
    public void administratorDashboardService_getDashboardSummary_NoData_ReturnsEmptyListsAndZeroAmounts() {
        // Arrange
        stubMedicationLists(List.of(), List.of());
        Mockito.when(treatmentDrugRepository.sumAllTimeSales()).thenReturn(null);
        Mockito.when(treatmentDrugRepository.sumAllTimeGrossProfit()).thenReturn(null);

        // Act
        AdministratorDashboardResponse response = summaryOn(LocalDate.of(2026, 11, 15));

        // Assert
        // Sin filas, las dos listas quedan vacías y no nulas.
        Assertions.assertThat(response.treatmentsByMedicationLastMonth()).isEmpty();
        Assertions.assertThat(response.topThreeMedicationsByUnitsSold()).isEmpty();
        // Sin historial de ventas, las sumas nulas de SQL se convierten en cero.
        Assertions.assertThat(response.totalSales()).isEqualByComparingTo(BigDecimal.ZERO);
        Assertions.assertThat(response.grossProfit()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    public void administratorDashboardService_getDashboardSummary_NonNullTotals_PassesThemThroughUnchanged() {
        // Arrange
        BigDecimal sales = new BigDecimal("125000.00");
        BigDecimal grossProfit = new BigDecimal("47000.00");
        stubMedicationLists(List.of(), List.of());
        Mockito.when(treatmentDrugRepository.sumAllTimeSales()).thenReturn(sales);
        Mockito.when(treatmentDrugRepository.sumAllTimeGrossProfit()).thenReturn(grossProfit);

        // Act
        AdministratorDashboardResponse response = summaryOn(LocalDate.of(2026, 11, 15));

        // Assert
        // El valor por defecto solo aplica a nulos: una suma real se entrega sin cambios.
        Assertions.assertThat(response.totalSales()).isEqualTo(sales);
        Assertions.assertThat(response.grossProfit()).isEqualTo(grossProfit);
    }
}
