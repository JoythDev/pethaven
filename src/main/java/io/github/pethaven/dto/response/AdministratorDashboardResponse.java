package io.github.pethaven.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resumen de KPIs para el dashboard de administrador.
 */
@Schema(description = "Resumen de indicadores del dashboard de administración")
public record AdministratorDashboardResponse(
        @Schema(description = "Primer día del mes calendario anterior, incluido", example = "2026-09-01")
        LocalDate monthlyPeriodStart,

        @Schema(description = "Primer día del mes actual, excluido del período", example = "2026-10-01")
        LocalDate monthlyPeriodEndExclusive,

        @Schema(description = "Zona horaria utilizada para determinar el mes reportado", example = "America/Bogota")
        String monthlyPeriodTimeZone,

        @Schema(description = "Tratamientos administrados en el mes anterior, excluyendo altas RECOVERED", example = "15")
        long administeredTreatmentsLastMonth,

        @Schema(description = "Tratamientos distintos del mes anterior que administraron cada medicamento")
        List<MedicationTreatmentCount> treatmentsByMedicationLastMonth,

        @Schema(description = "Veterinarios activos según Veterinarian.active", example = "5")
        long activeVeterinarians,

        @Schema(description = "Veterinarios inactivos según Veterinarian.active", example = "1")
        long inactiveVeterinarians,

        @Schema(description = "Todas las mascotas registradas, activas e inactivas", example = "150")
        long totalPets,

        @Schema(description = "Mascotas activas según Pet.active", example = "143")
        long activePets,

        @Schema(description = "Ventas históricas acumuladas de medicamentos administrados", example = "350000.00")
        BigDecimal totalSales,

        @Schema(description = "Utilidad bruta histórica: precio de venta menos precio de compra del snapshot, por unidades", example = "128500.00")
        BigDecimal grossProfit,

        @Schema(description = "Hasta tres medicamentos más administrados en toda la historia, ordenados por unidades")
        List<TopMedicationByUnitsSold> topThreeMedicationsByUnitsSold
) {

    @Schema(description = "Cantidad de tratamientos distintos del período que administraron un medicamento")
    public record MedicationTreatmentCount(
            @Schema(description = "Id del medicamento", example = "8")
            Long medicationId,

            @Schema(description = "Nombre del medicamento", example = "Amoxicilina 250mg")
            String medicationName,

            @Schema(description = "Número de tratamientos distintos", example = "4")
            long treatmentCount
    ) { }

    @Schema(description = "Medicamento y unidades administradas acumuladas")
    public record TopMedicationByUnitsSold(
            @Schema(description = "Id del medicamento", example = "8")
            Long medicationId,

            @Schema(description = "Nombre del medicamento", example = "Amoxicilina 250mg")
            String medicationName,

            @Schema(description = "Unidades administradas en toda la historia", example = "72")
            long unitsSold
    ) { }
}
