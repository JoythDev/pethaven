package io.github.pethaven.notification;

import io.github.pethaven.entity.Appointment;
import io.github.pethaven.entity.AppointmentReason;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Avisa al dueño por correo de los cambios de sus citas. El correo se arma dentro de la transacción y se envía
 * en segundo plano solo si esta confirma; un fallo del proveedor se registra y no afecta la operación.
 */
@Slf4j
@Component
public class AppointmentNotifier {

    private static final Locale SPANISH = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", SPANISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm", SPANISH);
    private static final Map<AppointmentReason, String> REASONS = new EnumMap<>(Map.of(
            AppointmentReason.CONTROL, "control",
            AppointmentReason.VACCINATION, "vacunación",
            AppointmentReason.CONSULTATION, "consulta general",
            AppointmentReason.DEWORMING, "desparasitación",
            AppointmentReason.NUTRITION, "nutrición",
            AppointmentReason.FIRST_VISIT, "primera consulta",
            AppointmentReason.DISCHARGE, "alta y entrega",
            AppointmentReason.BRIEFING, "reunión informativa"));

    private final EmailSender emailSender;
    private final Executor executor;
    private final List<String> suppressedDomains;

    /**
     * @param suppressedDomains dominios a los que nunca se envían avisos (los correos de las cuentas de ejemplo)
     */
    public AppointmentNotifier(
            EmailSender emailSender,
            @Qualifier("applicationTaskExecutor") Executor executor,
            @Value("${app.mail.suppressed-domains:example.com}") List<String> suppressedDomains
    ) {
        this.emailSender = emailSender;
        this.executor = executor;
        this.suppressedDomains = suppressedDomains.stream()
                .map(domain -> domain.trim().toLowerCase(Locale.ROOT))
                .filter(domain -> !domain.isEmpty())
                .toList();
    }

    public void booked(Appointment appointment) {
        notify(appointment, "Cita agendada para %s",
                "tu cita de %s para %s quedó agendada el %s a las %s con %s. Te avisaremos un día antes.", "");
    }

    public void rescheduled(Appointment appointment) {
        notify(appointment, "Tu cita de %s cambió de fecha",
                "tu cita de %s para %s ahora es el %s a las %s con %s. Te avisaremos un día antes.", "");
    }

    public void cancelled(Appointment appointment) {
        String reason = appointment.getCancellationReason() == null ? "" : " (" + appointment.getCancellationReason() + ")";
        notify(appointment, "Cita cancelada: %s",
                "tu cita de %s para %s del %s a las %s con %s se canceló%s. Puedes agendar una nueva desde PetHaven.",
                reason);
    }

    public void reminder(Appointment appointment) {
        notify(appointment, "Recordatorio: mañana es la cita de %s",
                "te recordamos que mañana tienes cita de %s para %s, el %s a las %s con %s.", "");
    }

    private void notify(Appointment appointment, String subjectTemplate, String bodyTemplate, String detail) {
        if (isSuppressed(appointment.getOwner().getEmail())) {
            log.info("Aviso de cita omitido: el correo del dueño es de un dominio de ejemplo.");
            return;
        }
        EmailMessage message = compose(appointment, subjectTemplate, bodyTemplate, detail);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(() -> deliver(message));
                }
            });
        } else {
            executor.execute(() -> deliver(message));
        }
    }

    private static EmailMessage compose(Appointment appointment, String subjectTemplate, String bodyTemplate, String detail) {
        String petName = appointment.getPet() != null ? appointment.getPet().getName() : appointment.getProvisionalPetName();
        String ownerName = appointment.getOwner().getName();
        String[] values = {
                REASONS.get(appointment.getReason()),
                petName,
                DAY.format(appointment.getStartTime()),
                TIME.format(appointment.getStartTime()),
                appointment.getVeterinarian().getName(),
                detail
        };
        String text = "Hola " + ownerName + ", " + bodyTemplate.formatted((Object[]) values);
        String[] escaped = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            escaped[i] = escape(values[i]);
        }
        String html = "<div style=\"font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:24px;color:#261a10\">"
                + "<p>Hola " + escape(ownerName) + ", " + bodyTemplate.formatted((Object[]) escaped) + "</p>"
                + "<p style=\"color:#6b5a4c\">PetHaven</p></div>";
        return new EmailMessage(appointment.getOwner().getEmail(), subjectTemplate.formatted(petName), text, html);
    }

    private boolean isSuppressed(String email) {
        String domain = email.substring(email.lastIndexOf('@') + 1).toLowerCase(Locale.ROOT);
        return suppressedDomains.contains(domain);
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value, StandardCharsets.UTF_8.name());
    }

    private void deliver(EmailMessage message) {
        try {
            emailSender.send(message);
        } catch (RuntimeException e) {
            // Sin el contenido ni el destinatario: el aviso es opcional y la operación ya se confirmó.
            log.warn("No se pudo enviar un aviso de cita: {}", e.getMessage());
        }
    }
}
