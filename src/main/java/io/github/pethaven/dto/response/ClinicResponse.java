package io.github.pethaven.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Datos de la sede y su horario de atención para citas.
 *
 * @param name        nombre de la sede
 * @param address     dirección
 * @param phone       teléfono
 * @param timeZone    zona horaria del horario y de las citas
 * @param slotMinutes duración de cada franja (y de cada cita)
 * @param hours       horario de lunes a domingo
 */
@Schema(description = "Sede y horario de la clínica")
public record ClinicResponse(
        @Schema(description = "Nombre de la sede", example = "PetHaven Chapinero")
        String name,

        @Schema(description = "Dirección", example = "Calle 63 n.º 9-41")
        String address,

        @Schema(description = "Teléfono", example = "601 555 0123")
        String phone,

        @Schema(description = "Zona horaria", example = "America/Bogota")
        String timeZone,

        @Schema(description = "Duración de cada franja en minutos", example = "30")
        int slotMinutes,

        @Schema(description = "Horario de lunes a domingo")
        List<ClinicHoursResponse> hours
) { }
