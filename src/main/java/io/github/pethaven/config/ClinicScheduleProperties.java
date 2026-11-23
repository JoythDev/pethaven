package io.github.pethaven.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.Map;

/**
 * Horario de atención de la clínica ({@code app.clinic.*}).
 *
 * @param zone            zona horaria de la clínica (las citas se guardan en hora local)
 * @param slotMinutes     duración de cada franja y de cada cita
 * @param hours           horario por día con el formato {@code HH:mm-HH:mm}; un día sin entrada está cerrado
 * @param bookingDays     días hacia adelante que puede reservar el dueño (hoy incluido)
 * @param minNoticeHours  antelación mínima del dueño para reservar, reprogramar o cancelar
 */
@ConfigurationProperties(prefix = "app.clinic")
public record ClinicScheduleProperties(
        ZoneId zone,
        int slotMinutes,
        Map<DayOfWeek, String> hours,
        int bookingDays,
        int minNoticeHours
) { }
