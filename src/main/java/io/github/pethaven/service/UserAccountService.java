package io.github.pethaven.service;

import io.github.pethaven.entity.UserEntity;

/**
 * Gestiona la cuenta de login ({@link UserEntity}) asociada a cada perfil
 * (Owner, Veterinarian, Administrator). Las credenciales viven únicamente en la cuenta,
 * siempre hasheadas con el {@code PasswordEncoder} configurado.
 */
public interface UserAccountService {

    /**
     * Construye una cuenta nueva, aún sin persistir, con el rol indicado. El perfil
     * que la contiene la persiste mediante su cascada.
     *
     * @throws io.github.pethaven.exception.IllegalOperationException si el username ya está en uso
     */
    UserEntity createAccount(String username, String rawPassword, String roleName);

    /**
     * Aplica un cambio de username y, cuando {@code rawPassword} no es null, un cambio de contraseña.
     *
     * @throws io.github.pethaven.exception.IllegalOperationException si el nuevo username pertenece a otra cuenta
     */
    void updateAccount(UserEntity account, String username, String rawPassword);

    /** Devuelve true solo si la cuenta existe y la contraseña en texto plano coincide con su hash. */
    boolean passwordMatches(UserEntity account, String rawPassword);
}
