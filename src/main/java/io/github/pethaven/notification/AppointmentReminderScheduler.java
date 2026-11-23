package io.github.pethaven.notification;

import io.github.pethaven.entity.Appointment;
import io.github.pethaven.entity.AppointmentStatus;
import io.github.pethaven.repository.AppointmentRepository;
import io.github.pethaven.service.ClinicSchedule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Recordatorio diario: avisa a los dueños de las citas programadas para el día siguiente, una sola vez por cita. Cada
 * cita se marca con una actualización condicional, así un cambio simultáneo de una cita no deshace las demás y otra
 * instancia de la aplicación no repite el aviso.
 */
@Slf4j
@Component
public class AppointmentReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentNotifier appointmentNotifier;
    private final ClinicSchedule clinicSchedule;

    public AppointmentReminderScheduler(
            AppointmentRepository appointmentRepository,
            AppointmentNotifier appointmentNotifier,
            ClinicSchedule clinicSchedule
    ) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentNotifier = appointmentNotifier;
        this.clinicSchedule = clinicSchedule;
    }

    /** @return cuántos recordatorios se enviaron */
    @Scheduled(cron = "${app.appointments.reminder-cron}", zone = "${app.clinic.zone}")
    @Transactional
    public int sendTomorrowReminders() {
        LocalDate tomorrow = clinicSchedule.today().plusDays(1);
        LocalDateTime now = clinicSchedule.now();
        List<Appointment> appointments = appointmentRepository
                .findByStatusAndStartTimeGreaterThanEqualAndStartTimeLessThanAndReminderSentAtIsNull(
                        AppointmentStatus.SCHEDULED, tomorrow.atStartOfDay(), tomorrow.plusDays(1).atStartOfDay());
        int sent = 0;
        for (Appointment appointment : appointments) {
            if (appointmentRepository.markReminderSent(appointment.getId(), now) == 1) {
                appointmentNotifier.reminder(appointment);
                sent++;
            }
        }
        log.info("Recordatorios de citas para {}: {}", tomorrow, sent);
        return sent;
    }
}
