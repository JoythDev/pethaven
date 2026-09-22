package io.github.pethaven.service;

import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.repository.VeterinarianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VeterinarianServiceImpl implements VeterinarianService {

    @Autowired
    private VeterinarianRepository veterinarianRepository;

    @Override
    public Veterinarian getVeterinarianById(Long id) {
        return veterinarianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinarian", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Veterinarian> getAllVeterinarians() {
        return veterinarianRepository.findAll();
    }

    @Override
    public Veterinarian getVeterinarianByDocument(String document) {
        return veterinarianRepository.findByDocument(document)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinarian", "document", document));
    }

    @Override
    public Veterinarian getVeterinarianByEmail(String email) {
        return veterinarianRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinarian", "email", email));
    }

    @Override
    public void createVeterinarian(Veterinarian veterinarian) {
        veterinarianRepository.save(veterinarian);
    }

    // La actualización copia únicamente los campos editables del formulario sobre la
    // entidad ya persistida. Los tratamientos, el estado activo y el contador de
    // atenciones se conservan: el formulario no los envía y un merge de JPA los reiniciaría.
    @Override
    @Transactional
    public void updateVeterinarian(Long id, Veterinarian formData) {
        Veterinarian existing = getVeterinarianById(id);
        existing.setName(formData.getName());
        existing.setDocument(formData.getDocument());
        existing.setEmail(formData.getEmail());
        existing.setPassword(formData.getPassword());
        existing.setSpecialty(formData.getSpecialty());
        existing.setPhotoUrl(formData.getPhotoUrl());
        veterinarianRepository.save(existing);
    }

    // NOTA: Un veterinario no se puede eliminar directamente, solo se puede desactivar.
    // Sus tratamientos deben conservarse como historial (Treatment.veterinarian es obligatorio).

    @Override
    @Transactional
    public void switchVeterinarianActiveStatus(Long id, boolean active) {
        Veterinarian veterinarian = getVeterinarianById(id);
        veterinarian.setActive(active);
        veterinarianRepository.save(veterinarian);
    }

}
