package io.github.pethaven.service;

import io.github.pethaven.dto.request.VeterinarianRequest;
import io.github.pethaven.dto.request.VeterinarianUpdateRequest;
import io.github.pethaven.dto.response.VeterinarianResponse;
import io.github.pethaven.entity.Veterinarian;
import java.util.List;

public interface VeterinarianService {

    Veterinarian getVeterinarianEntity(Long id);
    List<VeterinarianResponse> getAllVeterinarians();
    VeterinarianResponse getVeterinarianById(Long id);
    VeterinarianResponse getVeterinarianByDocument(String document);
    VeterinarianResponse getVeterinarianByEmail(String email);
    VeterinarianResponse createVeterinarian(VeterinarianRequest request);
    VeterinarianResponse updateVeterinarian(Long id, VeterinarianUpdateRequest updateRequest);
    VeterinarianResponse switchVeterinarianActiveStatus(Long id, boolean active);

    Veterinarian authenticate(String email, String password);

}
