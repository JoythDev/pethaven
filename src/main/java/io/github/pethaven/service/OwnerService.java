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

}
