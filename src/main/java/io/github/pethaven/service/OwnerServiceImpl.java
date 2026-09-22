package io.github.pethaven.service;

import io.github.pethaven.entity.Owner;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.repository.OwnerRepository;
import io.github.pethaven.repository.TreatmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OwnerServiceImpl implements OwnerService {

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Override
    public Owner getOwnerById(Long id) {
        return ownerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Owner", "id", id));
    }

    @Override
    public List<Owner> getAllOwners() {
        return ownerRepository.findAll();
    }

    @Override
    public Owner getOwnerByDocument(String document) {
        return ownerRepository.findByDocument(document)
                .orElseThrow(() -> new ResourceNotFoundException("Owner", "document", document));
    }

    @Override
    public Owner getOwnerByEmail(String email) {
        return ownerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Owner", "email", email));
    }

    @Override
    public void createOwner(Owner owner) {
        ownerRepository.save(owner);
    }

    // Un dueño con mascotas que ya tienen tratamientos registrados no se puede eliminar:
    // se perdería ese historial médico. Si ninguna mascota tiene tratamientos, la cascada
    // física normal (Owner -> Pet a nivel de base de datos) es segura porque no hay nada
    // debajo de esas mascotas que se pierda.
    @Override
    @Transactional
    public void deleteOwnerById(Long id) {
        Owner owner = getOwnerById(id);

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
        throw new RuntimeException("Correo o contraseña incorrectos.");
    }
}