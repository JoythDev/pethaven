package io.github.pethaven.service;

import io.github.pethaven.entity.Administrator;
import io.github.pethaven.exception.InvalidCredentialsException;
import io.github.pethaven.repository.AdministratorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdministratorServiceImpl implements AdministratorService {

    private final AdministratorRepository administratorRepository;

    @Autowired
    public AdministratorServiceImpl(AdministratorRepository administratorRepository) {
        this.administratorRepository = administratorRepository;
    }

    @Override
    public Administrator authenticate(String email, String password) {
        Administrator administrator = administratorRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException());
        if (administrator.getPassword().equals(password)) {
            return administrator;
        }
        throw new InvalidCredentialsException();
    }

}
