package io.github.pethaven.service;

import io.github.pethaven.entity.Owner;
import java.util.List;

public interface OwnerService {

    public Owner getOwnerById(Long id);
    public List<Owner> getAllOwners();
    public Owner getOwnerByDocument(String document);
    public Owner getOwnerByEmail(String email);
    public void createOwner(Owner owner);
    public void deleteOwnerById(Long id);

    // --- NUEVAS REGLAS DE NEGOCIO Y SEGURIDAD ---
    public Owner authenticate(String email, String password);
    public boolean isVeterinarian(Owner loggedOwner);
    public boolean canAccessOwner(Owner loggedOwner, Long targetOwnerId);

}