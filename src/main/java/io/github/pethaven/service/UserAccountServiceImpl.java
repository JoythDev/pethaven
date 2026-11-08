package io.github.pethaven.service;

import io.github.pethaven.entity.Role;
import io.github.pethaven.entity.UserEntity;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.repository.RoleRepository;
import io.github.pethaven.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserAccountServiceImpl implements UserAccountService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserAccountServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserEntity createAccount(String username, String rawPassword, String roleName) {
        ensureUsernameAvailable(username);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role '" + roleName + "' is not initialized."));

        UserEntity account = new UserEntity();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(rawPassword));
        account.setRoles(new ArrayList<>(List.of(role)));
        return account;
    }

    @Override
    public void updateAccount(UserEntity account, String username, String rawPassword) {
        if (!account.getUsername().equals(username)) {
            ensureUsernameAvailable(username);
            account.setUsername(username);
        }
        if (rawPassword != null) {
            account.setPassword(passwordEncoder.encode(rawPassword));
        }
    }

    private void ensureUsernameAvailable(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalOperationException("Ya existe una cuenta con el usuario '" + username + "'.");
        }
    }
}
