package io.github.pethaven.support;

import io.github.pethaven.security.SecurityConfig;
import io.github.pethaven.security.jwt.JwtAuthEntryPoint;
import io.github.pethaven.security.jwt.JwtProvider;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Loads the application's real security filter chain into a {@code @WebMvcTest} slice.
 * <p>
 * The slice picks up {@code JwtFilter} automatically (it is a {@code Filter} bean), so its
 * collaborators are mocked here. {@link SecurityConfig} is imported so that controller tests
 * run through the same authorization, CSRF and CORS rules as the application.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({SecurityConfig.class, JwtAuthEntryPoint.class})
@MockitoBean(types = {JwtProvider.class, UserDetailsService.class})
public @interface WebMvcSecurityTestSupport {
}
