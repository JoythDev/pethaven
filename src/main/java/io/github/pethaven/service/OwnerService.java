package io.github.pethaven.service;

import io.github.pethaven.dto.request.OwnerRequest;
import io.github.pethaven.dto.request.OwnerUpdateRequest;
import io.github.pethaven.dto.response.OwnerResponse;
import io.github.pethaven.entity.Owner;

import java.util.List;

public interface OwnerService {

    public OwnerResponse getOwnerById(Long id);
    public List<OwnerResponse> getAllOwners();
    public OwnerResponse getOwnerByDocument(String document);
    public OwnerResponse getOwnerByEmail(String email);
    public OwnerResponse createOwner(OwnerRequest request);
    public OwnerResponse updateOwner(Long id, OwnerUpdateRequest request);
    public void deleteOwnerById(Long id);

    public Owner authenticate(String email, String password);

}