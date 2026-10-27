package io.github.pethaven.service;

import io.github.pethaven.entity.Administrator;

public interface AdministratorService {

    Administrator authenticate(String email, String password);

}
