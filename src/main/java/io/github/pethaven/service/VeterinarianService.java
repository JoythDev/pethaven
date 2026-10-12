package io.github.pethaven.service;

import io.github.pethaven.entity.Veterinarian;
import java.util.List;

public interface VeterinarianService {

    Veterinarian getVeterinarianById(Long id);
    List<Veterinarian> getAllVeterinarians();
    Veterinarian getVeterinarianByDocument(String document);
    Veterinarian getVeterinarianByEmail(String email);
    void createVeterinarian(Veterinarian veterinarian);
    void updateVeterinarian(Long id, Veterinarian formData);
    void switchVeterinarianActiveStatus(Long id, boolean active);

    public Veterinarian authenticate(String email, String password);

}
