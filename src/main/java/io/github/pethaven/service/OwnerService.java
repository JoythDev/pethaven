package io.github.pethaven.service;

import io.github.pethaven.dto.request.OwnerRequest;
import io.github.pethaven.dto.request.OwnerUpdateRequest;
import io.github.pethaven.dto.response.OwnerResponse;
import io.github.pethaven.entity.Owner;

import java.util.List;

public interface OwnerService {

    Owner getOwnerEntity(Long id);
    List<OwnerResponse> getAllOwners();
    OwnerResponse getOwnerById(Long id);
    OwnerResponse getOwnerByDocument(String document);
    OwnerResponse getOwnerByEmail(String email);
    OwnerResponse createOwner(OwnerRequest request);
    OwnerResponse updateOwner(Long id, OwnerUpdateRequest request);
    void deleteOwnerById(Long id);

    Owner authenticate(String email, String password);
}
