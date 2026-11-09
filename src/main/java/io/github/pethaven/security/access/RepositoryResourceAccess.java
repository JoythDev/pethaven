package io.github.pethaven.security.access;

import io.github.pethaven.repository.OwnerRepository;
import io.github.pethaven.repository.PetRepository;
import io.github.pethaven.repository.TreatmentRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import org.springframework.stereotype.Component;

/**
 * Comprobaciones de propiedad resueltas por la base de datos: cada una es una sola consulta que cruza el
 * recurso con la cuenta de usuario por su usuario de login, así que no hace falta buscar el perfil ni cargar
 * relaciones perezosas.
 */
@Component(ResourceAccess.BEAN_NAME)
public class RepositoryResourceAccess implements ResourceAccess {

    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final TreatmentRepository treatmentRepository;

    public RepositoryResourceAccess(
            OwnerRepository ownerRepository,
            PetRepository petRepository,
            VeterinarianRepository veterinarianRepository,
            TreatmentRepository treatmentRepository
    ) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.treatmentRepository = treatmentRepository;
    }

    @Override
    public boolean isOwner(Long ownerId, String username) {
        return ownerId != null && ownerRepository.existsByIdAndUserUsername(ownerId, username);
    }

    @Override
    public boolean isOwnerKeepingIdentity(Long ownerId, String document, String email, String username) {
        return ownerId != null
                && ownerRepository.existsByIdAndUserUsernameAndDocumentAndEmail(ownerId, username, document, email);
    }

    @Override
    public boolean ownsPet(Long petId, String username) {
        return petId != null && petRepository.existsByIdAndOwnerUserUsername(petId, username);
    }

    @Override
    public boolean isVeterinarian(Long veterinarianId, String username) {
        return veterinarianId != null && veterinarianRepository.existsByIdAndUserUsername(veterinarianId, username);
    }

    @Override
    public boolean isVeterinarianKeepingIdentity(Long veterinarianId, String document, String email, String username) {
        return veterinarianId != null && veterinarianRepository
                .existsByIdAndUserUsernameAndDocumentAndEmail(veterinarianId, username, document, email);
    }

    @Override
    public boolean authoredTreatment(Long treatmentId, String username) {
        return treatmentId != null && treatmentRepository.existsByIdAndVeterinarianUserUsername(treatmentId, username);
    }
}
