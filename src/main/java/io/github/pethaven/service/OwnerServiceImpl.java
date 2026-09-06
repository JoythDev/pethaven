package io.github.pethaven.service;

import io.github.pethaven.entity.Owner;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.repository.OwnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OwnerServiceImpl implements OwnerService {

    @Autowired
    private OwnerRepository ownerRepository;

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

    @Override
    public void deleteOwnerById(Long id) {
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