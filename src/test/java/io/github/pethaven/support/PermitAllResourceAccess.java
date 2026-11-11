package io.github.pethaven.support;

import io.github.pethaven.security.access.ResourceAccess;
import org.springframework.boot.test.context.TestComponent;

/**
 * Ownership checks that always pass, for {@code @WebMvcTest} slices: controller tests exercise the controller's
 * own behavior, and ownership is covered against the database by {@code OwnershipAccessIntegrationTest}.
 */
@TestComponent(ResourceAccess.BEAN_NAME)
public class PermitAllResourceAccess implements ResourceAccess {

    @Override
    public boolean isOwner(Long ownerId, String username) {
        return true;
    }

    @Override
    public boolean isOwnerKeepingIdentity(Long ownerId, String document, String email, String username) {
        return true;
    }

    @Override
    public boolean ownsPet(Long petId, String username) {
        return true;
    }

    @Override
    public boolean isVeterinarian(Long veterinarianId, String username) {
        return true;
    }

    @Override
    public boolean isVeterinarianKeepingIdentity(Long veterinarianId, String document, String email, String username) {
        return true;
    }

    @Override
    public boolean authoredTreatment(Long treatmentId, String username) {
        return true;
    }
}
