package io.github.pethaven.security.access;

/**
 * Ownership checks of the permission matrix, used from {@code @PreAuthorize} expressions as the bean
 * {@code @access}. Each check receives the login username of the authenticated user
 * ({@code authentication.name}, the account e-mail) and answers whether that user is the resource's owner.
 * A resource that does not exist belongs to nobody, so the check answers {@code false}.
 */
public interface ResourceAccess {

    /** Bean name used in the {@code @PreAuthorize} expressions. */
    String BEAN_NAME = "access";

    /** The owner profile {@code ownerId} is the user's own. */
    boolean isOwner(Long ownerId, String username);

    /**
     * The owner profile {@code ownerId} is the user's own and the update keeps its current document and
     * e-mail (an owner may not change their own identity, only staff may).
     */
    boolean isOwnerKeepingIdentity(Long ownerId, String document, String email, String username);

    /** The pet {@code petId} belongs to the user (as its owner). */
    boolean ownsPet(Long petId, String username);

    /** The veterinarian profile {@code veterinarianId} is the user's own. */
    boolean isVeterinarian(Long veterinarianId, String username);

    /**
     * The veterinarian profile {@code veterinarianId} is the user's own and the update keeps its current
     * document and e-mail (a veterinarian may not change their own identity, only an administrator may).
     */
    boolean isVeterinarianKeepingIdentity(Long veterinarianId, String document, String email, String username);

    /** The treatment {@code treatmentId} was registered by the user (as its veterinarian). */
    boolean authoredTreatment(Long treatmentId, String username);
}
