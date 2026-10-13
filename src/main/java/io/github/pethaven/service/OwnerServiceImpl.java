package io.github.pethaven.service;

import io.github.pethaven.dto.request.OwnerRequest;
import io.github.pethaven.dto.request.OwnerUpdateRequest;
import io.github.pethaven.dto.response.OwnerResponse;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.InvalidCredentialsException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.mapper.OwnerMapper;
import io.github.pethaven.repository.OwnerRepository;
import io.github.pethaven.repository.TreatmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OwnerServiceImpl implements OwnerService {

    private final OwnerRepository ownerRepository;
    private final OwnerMapper ownerMapper;
    private final TreatmentRepository treatmentRepository;

    @Autowired
    public OwnerServiceImpl(
            OwnerRepository ownerRepository,
            OwnerMapper ownerMapper,
            TreatmentRepository treatmentRepository
    ) {
        this.ownerRepository = ownerRepository;
        this.ownerMapper = ownerMapper;
        this.treatmentRepository = treatmentRepository;
    }

    @Override
    public Owner getOwnerEntity(Long id) {
        return ownerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Owner", "id", id));
    }

    @Override
    public List<OwnerResponse> getAllOwners() {
        return ownerRepository.findAll().stream()
                .map(ownerMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OwnerResponse getOwnerById(Long id) {
        return ownerMapper.toResponse(getOwnerEntity(id));
    }

    @Override
    public OwnerResponse getOwnerByDocument(String document) {
        Owner owner = ownerRepository.findByDocument(document)
                .orElseThrow(() -> new ResourceNotFoundException("Owner", "document", document));
        return ownerMapper.toResponse(owner);
    }

    @Override
    public OwnerResponse getOwnerByEmail(String email) {
        Owner owner = ownerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Owner", "email", email));
        return ownerMapper.toResponse(owner);
    }

    @Override
    public OwnerResponse createOwner(OwnerRequest request) {
        ensureUnique(request.document(), request.email(), null);
        Owner owner = ownerMapper.toEntity(request);
        Owner saved = ownerRepository.save(owner);
        return ownerMapper.toResponse(saved);
    }

    // La actualización aplica únicamente los campos editables sobre la entidad ya
    // persistida. Las mascotas existentes se conservan intactas: el formulario no las
    // envía, así que la colección llega vacía y un merge de JPA las perdería.
    @Override
    @Transactional
    public OwnerResponse updateOwner(Long id, OwnerUpdateRequest updateRequest) {
        Owner existing = getOwnerEntity(id);
        ensureUnique(updateRequest.document(), updateRequest.email(), id);
        ownerMapper.applyUpdate(updateRequest, existing);
        Owner updated = ownerRepository.save(existing);
        return ownerMapper.toResponse(updated);
    }

    private void ensureUnique(String document, String email, Long selfId) {
        boolean documentTaken = selfId == null
                ? ownerRepository.existsByDocument(document)
                : ownerRepository.existsByDocumentAndIdNot(document, selfId);
        if (documentTaken) {
            throw new IllegalOperationException("Ya existe un dueño con el documento '" + document + "'.");
        }
        boolean emailTaken = selfId == null
                ? ownerRepository.existsByEmail(email)
                : ownerRepository.existsByEmailAndIdNot(email, selfId);
        if (emailTaken) {
            throw new IllegalOperationException("Ya existe un dueño con el correo '" + email + "'.");
        }
    }

    // Un dueño con mascotas que ya tienen tratamientos registrados no se puede eliminar:
    // se perdería ese historial médico. Si ninguna mascota tiene tratamientos, la cascada
    // física normal (Owner -> Pet a nivel de base de datos) es segura porque no hay nada
    // debajo de esas mascotas que se pierda.
    @Override
    @Transactional
    public void deleteOwnerById(Long id) {
        Owner owner = getOwnerEntity(id);

        if (treatmentRepository.existsByPetOwnerId(id)) {
            throw new IllegalOperationException(
                    "No se puede eliminar a '" + owner.getName()
                            + "' porque una o más de sus mascotas tienen tratamientos registrados.");
        }

        ownerRepository.deleteById(id);
    }

    @Override
    public Owner authenticate(String email, String password) {
        Owner owner = ownerRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException());
        if (owner.getPassword().equals(password)) {
            return owner;
        }
        throw new InvalidCredentialsException();
    }
}
