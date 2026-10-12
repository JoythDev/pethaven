package io.github.pethaven.dto.request;

import io.github.pethaven.entity.Species;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar o actualizar una mascota.
 *
 * @param name     nombre (obligatorio, máx. 60 caracteres)
 * @param ownerId  id del dueño (obligatorio; debe existir)
 * @param species  especie DOG o CAT (obligatoria)
 * @param breed    raza (obligatoria, máx. 60 caracteres)
 * @param age      edad en años (obligatoria, no negativa)
 * @param weight   peso en kg (obligatorio, no negativo)
 * @param disease  enfermedad (opcional, máx. 120 caracteres)
 * @param photoUrl URL de la foto (opcional, máx. 500 caracteres)
 */
public record PetRequest(
        @Schema(description = "Nombre de la mascota", example = "Buddy")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
        String name,

        @Schema(description = "Id del dueño", example = "31")
        @NotNull(message = "El ID del dueño es obligatorio")
        Long ownerId,

        @Schema(description = "Especie (DOG o CAT)", example = "DOG")
        @NotNull(message = "La especie es obligatoria")
        Species species,

        @Schema(description = "Raza", example = "Golden Retriever")
        @NotBlank(message = "La raza es obligatoria")
        @Size(max = 60, message = "La raza no puede superar los 60 caracteres")
        String breed,

        @Schema(description = "Edad en años", example = "3")
        @NotNull(message = "La edad es obligatoria")
        @PositiveOrZero(message = "La edad no puede ser negativa")
        Integer age,

        @Schema(description = "Peso en kg", example = "15.0")
        @NotNull(message = "El peso es obligatorio")
        @PositiveOrZero(message = "El peso no puede ser negativo")
        Double weight,

        @Schema(description = "Enfermedad (puede ser null)", example = "Otitis externa")
        @Size(max = 120, message = "La enfermedad no puede superar los 120 caracteres")
        String disease,

        @Schema(description = "URL de la foto (puede ser null)", example = "https://images.unsplash.com/photo-1552053831")
        @Size(max = 500, message = "La URL de la foto no puede superar los 500 caracteres")
        String photoUrl
) { }
