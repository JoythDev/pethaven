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
    private final TreatmentRepository treatmentRepository;
    private final OwnerMapper ownerMapper;

    @Autowired
    public OwnerServiceImpl(
            OwnerRepository ownerRepository,
            TreatmentRepository treatmentRepository,
            OwnerMapper ownerMapper) {
        this.ownerRepository = ownerRepository;
        this.treatmentRepository = treatmentRepository;
        this.ownerMapper = ownerMapper;
    }

    @Override
    public OwnerResponse getOwnerById(Long id) {
        Owner owner = ownerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Owner", "id", id));
        return ownerMapper.toResponse(owner);
    }

    @Override
    public List<OwnerResponse> getAllOwners() {
        return ownerRepository.findAll().stream()
                .map(ownerMapper::toResponse)
                .collect(Collectors.toList());
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
        Owner owner = ownerMapper.toEntity(request);
        owner = ownerRepository.save(owner);
        return ownerMapper.toResponse(owner);
    }

    // La actualización copia únicamente los campos editables del formulario sobre la
    // entidad ya persistida. Las mascotas existentes se conservan intactas: el formulario
    // no las envía, así que la colección llega vacía y un merge de JPA las perdería.
    @Override
    @Transactional
    public OwnerResponse updateOwner(Long id, OwnerUpdateRequest updateRequest) {
        Owner existing = ownerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Owner", "id", id));
        existing.setName(updateRequest.name());
        existing.setDocument(updateRequest.document());
        existing.setPhone(updateRequest.phone());
        existing.setEmail(updateRequest.email());
        existing.setPassword(updateRequest.password());
        Owner savedOwner = ownerRepository.save(existing);
        return ownerMapper.toResponse(savedOwner);
    }

    // Un dueño con mascotas que ya tienen tratamientos registrados no se puede eliminar:
    // se perdería ese historial médico. Si ninguna mascota tiene tratamientos, la cascada
    // física normal (Owner -> Pet a nivel de base de datos) es segura porque no hay nada
    // debajo de esas mascotas que se pierda.
    @Override
    @Transactional
    public void deleteOwnerById(Long id) {
        Owner owner = ownerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Owner", "id", id));

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
                .orElseThrow(() -> new ResourceNotFoundException("Owner", "email", email));
        if (owner.getPassword().equals(password)) {
            return owner;
        }
        throw new InvalidCredentialsException();
    }
}