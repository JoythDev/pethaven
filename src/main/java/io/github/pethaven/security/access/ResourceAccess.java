package io.github.pethaven.security.access;

/**
 * Comprobaciones de propiedad de la matriz de permisos, usadas desde las expresiones {@code @PreAuthorize}
 * como el bean {@code @access}. Cada comprobación recibe el usuario de login del usuario autenticado
 * ({@code authentication.name}, el correo de la cuenta) y responde si ese usuario es el dueño del recurso.
 * Un recurso que no existe no pertenece a nadie, así que la comprobación responde {@code false}.
 */
public interface ResourceAccess {

    /** Nombre del bean usado en las expresiones {@code @PreAuthorize}. */
    String BEAN_NAME = "access";

    /** El perfil de dueño {@code ownerId} es el del propio usuario. */
    boolean isOwner(Long ownerId, String username);

    /**
     * El perfil de dueño {@code ownerId} es el del propio usuario y la actualización conserva su documento y
     * correo actuales (un dueño no puede cambiar su propia identidad; solo el personal puede hacerlo).
     */
    boolean isOwnerKeepingIdentity(Long ownerId, String document, String email, String username);

    /** La mascota {@code petId} pertenece al usuario (como su dueño). */
    boolean ownsPet(Long petId, String username);

    /** El perfil de veterinario {@code veterinarianId} es el del propio usuario. */
    boolean isVeterinarian(Long veterinarianId, String username);

    /**
     * El perfil de veterinario {@code veterinarianId} es el del propio usuario y la actualización conserva su
     * documento y correo actuales (un veterinario no puede cambiar su propia identidad; solo un administrador
     * puede hacerlo).
     */
    boolean isVeterinarianKeepingIdentity(Long veterinarianId, String document, String email, String username);

    /** El tratamiento {@code treatmentId} lo registró el usuario (como su veterinario). */
    boolean authoredTreatment(Long treatmentId, String username);
}
