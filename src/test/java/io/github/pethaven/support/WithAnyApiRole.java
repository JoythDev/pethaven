package io.github.pethaven.support;

import io.github.pethaven.entity.Role;
import org.springframework.security.test.context.support.WithMockUser;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runs a controller test as a user holding every API role, so the role layer of the permission matrix lets
 * each request through and the test exercises only the controller's own behavior. Which role may reach each
 * operation is covered by {@code PermissionMatrixWebTest}.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@WithMockUser(roles = {Role.ADMIN, Role.VETERINARIAN, Role.OWNER})
public @interface WithAnyApiRole {
}
