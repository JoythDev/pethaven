package io.github.pethaven.notification;

import com.sun.net.httpserver.HttpServer;
import io.github.pethaven.config.ClinicScheduleProperties;
import io.github.pethaven.entity.Appointment;
import io.github.pethaven.entity.AppointmentReason;
import io.github.pethaven.entity.AppointmentStatus;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.repository.AppointmentRepository;
import io.github.pethaven.service.ClinicSchedule;
import io.github.pethaven.support.RepositoryTestData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

// Avisos de citas (S16.12): contenido, envío tras confirmar la transacción, recordatorio diario y envío por Brevo.
public class AppointmentNotifierTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 24, 9, 30);

    private EmailSender emailSender;
    private AppointmentNotifier notifier;
    private Appointment appointment;

    @BeforeEach
    public void setUp() {
        emailSender = Mockito.mock(EmailSender.class);
        notifier = new AppointmentNotifier(emailSender, Runnable::run);
        Owner owner = RepositoryTestData.owner();
        owner.setName("Laura <b>Gómez</b>");
        owner.setEmail("laura@pethaven.test");
        Pet pet = RepositoryTestData.pet(owner);
        pet.setName("Kira & Co");
        Veterinarian vet = RepositoryTestData.veterinarian();
        vet.setName("Dra. Valentina Soler");
        appointment = RepositoryTestData.appointment(owner, pet, vet, START);
        appointment.setReason(AppointmentReason.VACCINATION);
    }

    @AfterEach
    public void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private EmailMessage sentMessage() {
        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        Mockito.verify(emailSender).send(captor.capture());
        return captor.getValue();
    }

    @Test
    public void booked_WithoutTransaction_SendsToTheOwnerWithTheAppointmentDetails() {
        notifier.booked(appointment);

        EmailMessage message = sentMessage();
        Assertions.assertThat(message.to()).isEqualTo("laura@pethaven.test");
        Assertions.assertThat(message.subject()).isEqualTo("Cita agendada para Kira & Co");
        Assertions.assertThat(message.text()).isEqualTo("Hola Laura <b>Gómez</b>, tu cita de vacunación para Kira & Co quedó "
                + "agendada el martes 24 de noviembre a las 9:30 con Dra. Valentina Soler. Te avisaremos un día antes.");
    }

    @Test
    public void booked_HtmlBody_EscapesUserProvidedNames() {
        notifier.booked(appointment);

        String html = sentMessage().html();
        Assertions.assertThat(html).contains("Laura &lt;b&gt;Gómez&lt;/b&gt;").contains("Kira &amp; Co")
                .doesNotContain("<b>Gómez</b>");
    }

    @Test
    public void rescheduledCancelledAndReminder_UseTheirOwnWording() {
        appointment.setCancellationReason("Veterinario no disponible");
        notifier.rescheduled(appointment);
        notifier.cancelled(appointment);
        notifier.reminder(appointment);

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        Mockito.verify(emailSender, Mockito.times(3)).send(captor.capture());
        List<EmailMessage> messages = captor.getAllValues();
        Assertions.assertThat(messages.get(0).subject()).isEqualTo("Tu cita de Kira & Co cambió de fecha");
        Assertions.assertThat(messages.get(0).text()).contains("ahora es el martes 24 de noviembre a las 9:30");
        Assertions.assertThat(messages.get(1).subject()).isEqualTo("Cita cancelada: Kira & Co");
        Assertions.assertThat(messages.get(1).text()).contains("se canceló (Veterinario no disponible).");
        Assertions.assertThat(messages.get(2).subject()).isEqualTo("Recordatorio: mañana es la cita de Kira & Co");
    }

    @Test
    public void provisionalPet_UsesItsProvisionalName() {
        appointment.setPet(null);
        appointment.setProvisionalPetName("Luna");

        notifier.booked(appointment);

        Assertions.assertThat(sentMessage().subject()).isEqualTo("Cita agendada para Luna");
    }

    @Test
    public void insideATransaction_SendsOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        notifier.booked(appointment);
        Mockito.verifyNoInteractions(emailSender);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        Mockito.verify(emailSender).send(Mockito.any());
    }

    @Test
    public void insideATransactionThatRollsBack_SendsNothing() {
        TransactionSynchronizationManager.initSynchronization();

        notifier.cancelled(appointment);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        Mockito.verifyNoInteractions(emailSender);
    }

    @Test
    public void cancellationReasonWithFormatOrHtmlCharacters_IsSafe() {
        appointment.setCancellationReason("100% <b>ocupado</b>");

        notifier.cancelled(appointment);

        EmailMessage message = sentMessage();
        Assertions.assertThat(message.text()).contains("se canceló (100% <b>ocupado</b>).");
        Assertions.assertThat(message.html()).contains("100% &lt;b&gt;ocupado&lt;/b&gt;");
    }

    @Test
    public void providerFailure_IsSwallowed() {
        Mockito.doThrow(new IllegalStateException("caído")).when(emailSender).send(Mockito.any());

        Assertions.assertThatCode(() -> notifier.booked(appointment)).doesNotThrowAnyException();
    }

    // ---------- Recordatorio diario ----------

    @Test
    public void reminderScheduler_TomorrowsScheduledAppointments_NotifiesAndMarksThem() {
        AppointmentRepository repository = Mockito.mock(AppointmentRepository.class);
        ClinicSchedule schedule = new ClinicSchedule(new ClinicScheduleProperties(ZoneId.of("America/Bogota"), 30,
                Map.of(DayOfWeek.TUESDAY, "08:00-17:00"), 14, 2),
                Clock.fixed(Instant.parse("2026-11-23T23:00:00Z"), ZoneId.of("UTC")));
        appointment.setId(7L);
        Appointment alreadyHandled = RepositoryTestData.appointment(appointment.getOwner(), appointment.getPet(),
                appointment.getVeterinarian(), START.plusHours(1));
        alreadyHandled.setId(8L);
        Mockito.when(repository.findByStatusAndStartTimeGreaterThanEqualAndStartTimeLessThanAndReminderSentAtIsNull(
                        AppointmentStatus.SCHEDULED, START.toLocalDate().atStartOfDay(), START.toLocalDate().plusDays(1).atStartOfDay()))
                .thenReturn(List.of(appointment, alreadyHandled));
        LocalDateTime now = LocalDateTime.of(2026, 11, 23, 18, 0);
        Mockito.when(repository.markReminderSent(7L, now)).thenReturn(1);
        // Otra instancia o un cambio simultáneo ya la marcó: no se repite el aviso.
        Mockito.when(repository.markReminderSent(8L, now)).thenReturn(0);

        int sent = new AppointmentReminderScheduler(repository, notifier, schedule).sendTomorrowReminders();

        Assertions.assertThat(sent).isEqualTo(1);
        Assertions.assertThat(sentMessage().subject()).startsWith("Recordatorio");
    }

    // ---------- Proveedores ----------

    @Test
    public void brevoEmailSender_PostsTheMessageAndFailsOnProviderErrors() throws IOException {
        AtomicReference<String> apiKey = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        int[] status = {201};
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/smtp/email", exchange -> {
            apiKey.set(exchange.getRequestHeaders().getFirst("api-key"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(status[0], -1);
            exchange.close();
        });
        server.start();
        try {
            BrevoEmailSender sender = new BrevoEmailSender("clave", "no-reply@pethaven.test", "PetHaven",
                    "http://127.0.0.1:" + server.getAddress().getPort(), 3);
            EmailMessage message = new EmailMessage("laura@pethaven.test", "Asunto", "Texto", "<p>Texto</p>");

            sender.send(message);
            Assertions.assertThat(apiKey.get()).isEqualTo("clave");
            Assertions.assertThat(body.get()).contains("\"laura@pethaven.test\"").contains("\"Asunto\"")
                    .contains("no-reply@pethaven.test").contains("<p>Texto</p>");

            status[0] = 500;
            Assertions.assertThatThrownBy(() -> sender.send(message)).isInstanceOf(IllegalStateException.class);
        } finally {
            server.stop(0);
        }
        Assertions.assertThatThrownBy(() -> new BrevoEmailSender("", "x@pethaven.test", "PetHaven", "http://localhost", 3))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("BREVO_API_KEY");
    }

    @Test
    public void logEmailSender_DoesNotThrow() {
        Assertions.assertThatCode(() -> new LogEmailSender().send(new EmailMessage("a@b.test", "S", "T", "H")))
                .doesNotThrowAnyException();
    }
}
