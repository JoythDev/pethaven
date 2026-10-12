package io.github.pethaven.dto.response;

import io.github.pethaven.entity.Species;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Mascota registrada, con datos de su dueño.
 *
 * @param id        id de la mascota
 * @param name      nombre
 * @param species   especie (DOG o CAT)
 * @param breed     raza
 * @param age       edad en años
 * @param weight    peso en kg
 * @param disease   enfermedad (puede ser null)
 * @param photoUrl  URL de la foto (puede ser null)
 * @param active    true si está en la veterinaria; false si está en su casa
 * @param ownerId   id del dueño
 * @param ownerName nombre del dueño
 */
@Schema(description = "Mascota registrada")
public record PetResponse(
        @Schema(description = "Id de la mascota", example = "1")
        Long id,

        @Schema(description = "Nombre", example = "Buddy")
        String name,

        @Schema(description = "Especie (DOG o CAT)", example = "DOG")
        Species species,

        @Schema(description = "Raza", example = "Golden Retriever")
        String breed,

        @Schema(description = "Edad en años", example = "3")
        Integer age,

        @Schema(description = "Peso en kg", example = "15.0")
        Double weight,

        @Schema(description = "Enfermedad (puede ser null)", example = "Otitis externa")
        String disease,

        @Schema(description = "URL de la foto (puede ser null)", example = "https://images.unsplash.com/photo-1552053831")
        String photoUrl,

        @Schema(description = "Estado: en la veterinaria (true) o en su casa (false)", example = "true")
        Boolean active,

        @Schema(description = "Id del dueño", example = "31")
        Long ownerId,

        @Schema(description = "Nombre del dueño", example = "Diego Aguilar")
        String ownerName
) { }
