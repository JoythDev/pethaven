package io.github.pethaven.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Rol del usuario autenticado.
 */
@Schema(description = "Rol del usuario autenticado")
public enum AuthRole {
    OWNER,
    VETERINARIAN
}
