package io.github.pethaven.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Respuesta estándar de error de la API.
 *
 * @param timestamp momento del error (UTC)
 * @param status    código HTTP
 * @param error     frase estándar del código HTTP
 * @param message   mensaje legible para el usuario
 * @param path      ruta de la petición
 * @param errors    errores por campo (solo en errores de validación; vacío en el resto)
 */
@Schema(description = "Respuesta estándar de error de la API")
public record ApiErrorResponse(
        @Schema(description = "Momento del error (UTC)", example = "2026-10-09T14:03:21.551Z")
        Instant timestamp,

        @Schema(description = "Código HTTP", example = "404")
        int status,

        @Schema(description = "Frase estándar del código HTTP", example = "Not Found")
        String error,

        @Schema(description = "Mensaje legible para el usuario", example = "El recurso 'Owner' con id: '99' no fue encontrado")
        String message,

        @Schema(description = "Ruta de la petición", example = "/api/owners/99")
        String path,

        @Schema(description = "Errores por campo (solo en validación; vacío en el resto)",
                example = "{\"document\": \"El documento es obligatorio\"}")
        Map<String, String> errors
) {

    public static ApiErrorResponse of(HttpStatus status, String message, String path) {
        return of(status, message, path, Map.of());
    }

    public static ApiErrorResponse of(HttpStatus status, String message, String path,
                                      Map<String, String> errors) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                errors == null ? Map.of() : new LinkedHashMap<>(errors)
        );
    }
}
