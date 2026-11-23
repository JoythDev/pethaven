package io.github.pethaven.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita las tareas programadas (recordatorio diario de citas). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
