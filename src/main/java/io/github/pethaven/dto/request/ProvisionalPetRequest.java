package io.github.pethaven.dto.request;

import io.github.pethaven.entity.Species;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos provisionales de una mascota aún no registrada; el veterinario la registra en la consulta.
 *
 * @param name    nombre (obligatorio, máx. 60 caracteres)
 * @param species especie (obligatoria: DOG o CAT)
 * @param breed   raza (opcional, máx. 60 caracteres)
 * @param age     edad en años (opcional, entre 0 y 40)
 */
public record ProvisionalPetRequest(
        @Schema(description = "Nombre de la mascota", example = "Kira")
        @NotBlank(message = "El nombre de la mascota es obligatorio")
        @Size(max = 60, message = "El nombre de la mascota no puede superar los 60 caracteres")
        String name,

        @Schema(description = "Especie de la mascota", example = "DOG")
        @NotNull(message = "La especie de la mascota es obligatoria")
        Species species,

        @Schema(description = "Raza (opcional)", example = "Criollo")
        @Size(max = 60, message = "La raza no puede superar los 60 caracteres")
        String breed,

        @Schema(description = "Edad en años (opcional)", example = "2")
        @Min(value = 0, message = "La edad no puede ser negativa")
        @Max(value = 40, message = "La edad no puede superar los 40 años")
        Integer age
) { }
