package io.github.pethaven.security.otp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class OtpConfig {

    /** Reloj del servicio de códigos; las pruebas lo sustituyen para controlar el paso del tiempo. */
    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock otpClock() {
        return Clock.systemUTC();
    }
}
