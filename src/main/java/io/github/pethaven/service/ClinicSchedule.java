package io.github.pethaven.service;

import io.github.pethaven.config.ClinicScheduleProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Reglas del horario de la clínica: franjas de cada día, hora local y ventana de reserva del dueño.
 */
@Component
public class ClinicSchedule {

    private final Clock clock;
    private final ZoneId zone;
    private final Duration slot;
    private final Map<DayOfWeek, LocalTime[]> openingHours = new EnumMap<>(DayOfWeek.class);
    private final int bookingDays;
    private final Duration minNotice;

    public ClinicSchedule(ClinicScheduleProperties properties, Clock clock) {
        this.clock = clock;
        this.zone = properties.zone();
        this.slot = Duration.ofMinutes(properties.slotMinutes());
        this.bookingDays = properties.bookingDays();
        this.minNotice = Duration.ofHours(properties.minNoticeHours());
        if (properties.hours() != null) {
            properties.hours().forEach((day, range) -> {
                String[] bounds = range.split("-");
                openingHours.put(day, new LocalTime[]{LocalTime.parse(bounds[0].trim()), LocalTime.parse(bounds[1].trim())});
            });
        }
    }

    public LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), zone);
    }

    public LocalDate today() {
        return now().toLocalDate();
    }

    public ZoneId zone() {
        return zone;
    }

    public Duration slotDuration() {
        return slot;
    }

    /** Hora de apertura del día de la semana, o null si la clínica no atiende ese día. */
    public LocalTime openingTime(DayOfWeek day) {
        LocalTime[] hours = openingHours.get(day);
        return hours == null ? null : hours[0];
    }

    /** Hora de cierre del día de la semana, o null si la clínica no atiende ese día. */
    public LocalTime closingTime(DayOfWeek day) {
        LocalTime[] hours = openingHours.get(day);
        return hours == null ? null : hours[1];
    }

    public boolean isOpen(LocalDate date) {
        return openingHours.containsKey(date.getDayOfWeek());
    }

    /** Inicio de cada franja del día, en orden; vacío si la clínica está cerrada. */
    public List<LocalDateTime> slotsOf(LocalDate date) {
        List<LocalDateTime> slots = new ArrayList<>();
        LocalTime[] hours = openingHours.get(date.getDayOfWeek());
        if (hours == null) {
            return slots;
        }
        for (LocalTime time = hours[0]; !time.plus(slot).isAfter(hours[1]); time = time.plus(slot)) {
            slots.add(date.atTime(time));
        }
        return slots;
    }

    /** La hora coincide exactamente con el inicio de una franja del horario. */
    public boolean isSlotStart(LocalDateTime start) {
        return slotsOf(start.toLocalDate()).contains(start);
    }

    /** Último día que puede reservar el dueño. */
    public LocalDate lastBookableDay() {
        return today().plusDays(bookingDays - 1L);
    }

    /** La hora está dentro de la ventana del dueño: con la antelación mínima y antes del fin de la ventana. */
    public boolean isWithinOwnerWindow(LocalDateTime start) {
        return hasOwnerNotice(start) && !start.toLocalDate().isAfter(lastBookableDay());
    }

    /** Todavía queda al menos la antelación mínima del dueño antes de la hora indicada. */
    public boolean hasOwnerNotice(LocalDateTime start) {
        return !start.isBefore(now().plus(minNotice));
    }

    public long minNoticeHours() {
        return minNotice.toHours();
    }
}
