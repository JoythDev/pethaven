package io.github.pethaven.service;

import io.github.pethaven.entity.Veterinarian;
import java.util.List;

public interface VeterinarianService {

    public Veterinarian getVeterinarianById(Long id);
    public List<Veterinarian> getAllVeterinarians();
    public Veterinarian getVeterinarianByDocument(String document);
    public Veterinarian getVeterinarianByEmail(String email);
    public void createVeterinarian(Veterinarian veterinarian);
    public void switchVeterinarianActiveStatus(Long id, boolean active);

}
