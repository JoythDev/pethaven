package io.github.pethaven.service;

import io.github.pethaven.entity.Owner;
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
        return ownerRepository.findById(id);
    }

    @Override
    public List<Owner> getAllOwners() {
        return ownerRepository.findAll();
    }

    @Override
    public Owner getOwnerByDocument(String document) {
        return ownerRepository.findByDocument(document);
    }

    @Override
    public Owner getOwnerByEmail(String email) {
        return ownerRepository.findByEmail(email);
    }

    @Override
    public void createOwner(Owner owner) {
        ownerRepository.save(owner);
    }

    @Override
    public void deleteOwnerById(Long id) {
        ownerRepository.deleteById(id);
    }

    // --- IMPLEMENTACIÓN DE REGLAS DE NEGOCIO ---

    @Override
    public Owner authenticate(String email, String password) {
        Owner owner = ownerRepository.findByEmail(email);
        // Si el usuario existe y la contraseña coincide, retorna el objeto; si no, null.
        if (owner != null && owner.getPassword().equals(password)) {
            return owner;
        }
        return null;
    }

    @Override
    public boolean isVeterinarian(Owner loggedOwner) {
        // La regla de negocio dicta que si no hay sesión de dueño, es el Veterinario implícito
        return loggedOwner == null;
    }

    @Override
    public boolean canAccessOwner(Owner loggedOwner, Long targetOwnerId) {
        // Puede acceder si es el veterinario O si es su propio perfil
        return isVeterinarian(loggedOwner) || loggedOwner.getId().equals(targetOwnerId);
    }
}