package io.github.pethaven.security;

import io.github.pethaven.entity.Administrator;
import io.github.pethaven.entity.Role;
import io.github.pethaven.repository.AdministratorRepository;
import io.github.pethaven.repository.RoleRepository;
import io.github.pethaven.service.UserAccountService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Datos de seguridad mínimos, en cualquier entorno y de forma idempotente:
 * <ul>
 *   <li>Los roles {@code ADMIN}, {@code VETERINARIAN} y {@code OWNER}, si faltan.</li>
 *   <li>El primer administrador, solo si no existe ninguno y están definidas {@code ADMIN_EMAIL} y
 *       {@code ADMIN_PASSWORD} (y opcionalmente {@code ADMIN_NAME}). Nunca modifica un administrador existente.</li>
 * </ul>
 * Se ejecuta antes que cualquier otro runner (importación de medicamentos y datos de ejemplo).
 * Una configuración incompleta o inválida del administrador impide arrancar la aplicación.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecuritySeeder implements ApplicationRunner {

    private static final List<String> ROLES = List.of(Role.ADMIN, Role.VETERINARIAN, Role.OWNER);

    private final RoleRepository roleRepository;
    private final AdministratorRepository administratorRepository;
    private final UserAccountService userAccountService;
    private final Validator validator;
    private final AdminSeed adminSeed;

    public SecuritySeeder(
            RoleRepository roleRepository,
            AdministratorRepository administratorRepository,
            UserAccountService userAccountService,
            Validator validator,
            @Value("${app.seed.admin.name:Administrador}") String adminName,
            @Value("${app.seed.admin.email:}") String adminEmail,
            @Value("${app.seed.admin.password:}") String adminPassword
    ) {
        this.roleRepository = roleRepository;
        this.administratorRepository = administratorRepository;
        this.userAccountService = userAccountService;
        this.validator = validator;
        this.adminSeed = new AdminSeed(adminName, adminEmail, adminPassword);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRoles();
        seedFirstAdministrator();
    }

    private void seedRoles() {
        for (String name : ROLES) {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(new Role(name));
                log.info("Rol creado: {}", name);
            }
        }
    }

    private void seedFirstAdministrator() {
        if (administratorRepository.count() > 0) {
            return;
        }
        if (isBlank(adminSeed.email()) && isBlank(adminSeed.password())) {
            log.warn("No existe ningún administrador y no están definidas ADMIN_EMAIL ni ADMIN_PASSWORD: "
                    + "no se crea el administrador inicial.");
            return;
        }
        validate(adminSeed);

        Administrator administrator = Administrator.builder()
                .name(adminSeed.name())
                .email(adminSeed.email())
                .user(userAccountService.createAccount(adminSeed.email(), adminSeed.password(), Role.ADMIN))
                .build();
        administratorRepository.save(administrator);
        log.info("Administrador inicial creado: {}", adminSeed.email());
    }

    /** Falla con un mensaje por variable de entorno; nunca incluye el valor de la contraseña. */
    private void validate(AdminSeed seed) {
        Set<ConstraintViolation<AdminSeed>> violations = validator.validate(seed);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new IllegalStateException("Configuración inválida del administrador inicial: " + detail);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Mismas reglas que el alta de perfiles y el login: el administrador creado siempre puede iniciar sesión. */
    record AdminSeed(
            @NotBlank(message = "ADMIN_NAME no puede estar vacío")
            @Size(max = 100, message = "ADMIN_NAME no puede superar los 100 caracteres")
            String name,

            @NotBlank(message = "ADMIN_EMAIL es obligatorio junto con ADMIN_PASSWORD")
            @Email(message = "ADMIN_EMAIL debe ser un correo válido")
            @Size(max = 120, message = "ADMIN_EMAIL no puede superar los 120 caracteres")
            String email,

            @NotBlank(message = "ADMIN_PASSWORD es obligatoria junto con ADMIN_EMAIL")
            @Size(min = 8, max = 255, message = "ADMIN_PASSWORD debe tener entre 8 y 255 caracteres")
            String password
    ) { }
}
