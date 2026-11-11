package io.github.pethaven.service;

import io.github.pethaven.dto.request.VeterinarianRequest;
import io.github.pethaven.dto.request.VeterinarianUpdateRequest;
import io.github.pethaven.dto.response.VeterinarianResponse;
import io.github.pethaven.entity.Role;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.mapper.VeterinarianMapper;
import io.github.pethaven.repository.VeterinarianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VeterinarianServiceImpl implements VeterinarianService {

    private final VeterinarianRepository veterinarianRepository;
    private final VeterinarianMapper veterinarianMapper;
    private final UserAccountService userAccountService;

    @Autowired
    public VeterinarianServiceImpl(
            VeterinarianRepository veterinarianRepository,
            VeterinarianMapper veterinarianMapper,
            UserAccountService userAccountService
    ) {
        this.veterinarianRepository = veterinarianRepository;
        this.veterinarianMapper = veterinarianMapper;
        this.userAccountService = userAccountService;
    }

    @Override
    public Veterinarian getVeterinarianEntity(Long id) {
        return veterinarianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinarian", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VeterinarianResponse> getAllVeterinarians() {
        return veterinarianRepository.findAll().stream()
                .map(veterinarianMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VeterinarianResponse getVeterinarianById(Long id) {
        return veterinarianMapper.toResponse(getVeterinarianEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public VeterinarianResponse getVeterinarianByDocument(String document) {
        Veterinarian veterinarian = veterinarianRepository.findByDocument(document)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinarian", "document", document));
        return veterinarianMapper.toResponse(veterinarian);
    }

    @Override
    @Transactional(readOnly = true)
    public VeterinarianResponse getVeterinarianByEmail(String email) {
        Veterinarian veterinarian = veterinarianRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinarian", "email", email));
        return veterinarianMapper.toResponse(veterinarian);
    }

    @Override
    @Transactional
    public VeterinarianResponse createVeterinarian(VeterinarianRequest request) {
        ensureUnique(request.document(), request.email(), null);
        Veterinarian veterinarian = veterinarianMapper.toEntity(request);
        // El email del perfil también es el username de la cuenta de login.
        veterinarian.setUser(userAccountService.createAccount(request.email(), request.password(), Role.VETERINARIAN));
        Veterinarian saved = veterinarianRepository.save(veterinarian);
        return veterinarianMapper.toResponse(saved);
    }

    // La actualización aplica únicamente los campos editables sobre la entidad ya
    // persistida. Los tratamientos, el estado activo y el contador de atenciones se
    // conservan: no forman parte del request y un merge de JPA los reiniciaría.
    @Override
    @Transactional
    public VeterinarianResponse updateVeterinarian(Long id, VeterinarianUpdateRequest updateRequest) {
        Veterinarian existing = getVeterinarianEntity(id);
        ensureUnique(updateRequest.document(), updateRequest.email(), id);
        veterinarianMapper.applyUpdate(updateRequest, existing);
        userAccountService.updateAccount(existing.getUser(), updateRequest.email(), updateRequest.password());
        Veterinarian updated = veterinarianRepository.save(existing);
        return veterinarianMapper.toResponse(updated);
    }

    private void ensureUnique(String document, String email, Long selfId) {
        boolean documentTaken = selfId == null
                ? veterinarianRepository.existsByDocument(document)
                : veterinarianRepository.existsByDocumentAndIdNot(document, selfId);
        if (documentTaken) {
            throw new IllegalOperationException("Ya existe un veterinario con el documento '" + document + "'.");
        }
        boolean emailTaken = selfId == null
                ? veterinarianRepository.existsByEmail(email)
                : veterinarianRepository.existsByEmailAndIdNot(email, selfId);
        if (emailTaken) {
            throw new IllegalOperationException("Ya existe un veterinario con el correo '" + email + "'.");
        }
    }

    // NOTA: Un veterinario no se puede eliminar directamente, solo se puede desactivar.
    // Sus tratamientos deben conservarse como historial (Treatment.veterinarian es obligatorio).

    @Override
    @Transactional
    public VeterinarianResponse switchVeterinarianActiveStatus(Long id, boolean active) {
        Veterinarian veterinarian = getVeterinarianEntity(id);
        veterinarian.setActive(active);
        // La cuenta de login sigue al perfil: desactivado no inicia sesión y sus tokens dejan de servir.
        veterinarian.getUser().setEnabled(active);
        veterinarianRepository.save(veterinarian);
        return veterinarianMapper.toResponse(veterinarian);
    }

}
