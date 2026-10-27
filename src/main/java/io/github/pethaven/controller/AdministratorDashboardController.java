package io.github.pethaven.controller;

import io.github.pethaven.dto.response.AdministratorDashboardResponse;
import io.github.pethaven.service.AdministratorDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Administrators", description = "Indicadores del dashboard de administración")
@RestController
@RequestMapping("/api/administrators")
public class AdministratorDashboardController {

    private final AdministratorDashboardService dashboardService;

    public AdministratorDashboardController(AdministratorDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(
            summary = "Obtener indicadores del dashboard administrativo",
            description = "Devuelve los nueve KPI de AC29 en un resumen. Los indicadores mensuales usan el mes calendario anterior en America/Bogota; las ventas, utilidad bruta y medicamentos más administrados son acumulados históricos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen de indicadores",
                    content = @Content(schema = @Schema(implementation = AdministratorDashboardResponse.class)))
    })
    @GetMapping("/dashboard")
    public AdministratorDashboardResponse getDashboardSummary() {
        return dashboardService.getDashboardSummary();
    }
}
