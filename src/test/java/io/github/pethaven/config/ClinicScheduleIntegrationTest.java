package io.github.pethaven.config;

import io.github.pethaven.dto.response.ClinicResponse;
import io.github.pethaven.service.ClinicSchedule;
import io.github.pethaven.service.ClinicService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

// El horario de application.properties (S16.3): lunes a viernes 8:00-17:00, sábado 8:00-13:00, domingo cerrado.
@SpringBootTest
@ActiveProfiles("test")
public class ClinicScheduleIntegrationTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);

    @Autowired
    private ClinicSchedule clinicSchedule;
    @Autowired
    private ClinicService clinicService;

    @Test
    public void configuredSchedule_HasThirtyMinuteSlotsInBogota() {
        Assertions.assertThat(clinicSchedule.zone()).isEqualTo(ZoneId.of("America/Bogota"));
        Assertions.assertThat(clinicSchedule.slotDuration()).isEqualTo(Duration.ofMinutes(30));
        Assertions.assertThat(clinicSchedule.minNoticeHours()).isEqualTo(2);
    }

    @Test
    public void configuredSchedule_WeekdaysSaturdayAndClosedSunday() {
        for (int day = 0; day < 5; day++) {
            Assertions.assertThat(clinicSchedule.slotsOf(MONDAY.plusDays(day))).hasSize(18)
                    .first().extracting(start -> start.toLocalTime()).isEqualTo(LocalTime.of(8, 0));
        }
        Assertions.assertThat(clinicSchedule.slotsOf(MONDAY.plusDays(5))).hasSize(10)
                .last().extracting(start -> start.toLocalTime()).isEqualTo(LocalTime.of(12, 30));
        Assertions.assertThat(clinicSchedule.isOpen(MONDAY.plusDays(6))).isFalse();
        Assertions.assertThat(clinicSchedule.slotsOf(MONDAY.plusDays(6))).isEmpty();
    }

    @Test
    public void clinicService_ReturnsTheConfiguredSiteAndWeek() {
        ClinicResponse clinic = clinicService.getClinic();

        Assertions.assertThat(clinic.name()).isEqualTo("PetHaven Chapinero");
        Assertions.assertThat(clinic.address()).isEqualTo("Calle 63 n.º 9-41");
        Assertions.assertThat(clinic.phone()).isEqualTo("601 555 0123");
        Assertions.assertThat(clinic.timeZone()).isEqualTo("America/Bogota");
        Assertions.assertThat(clinic.slotMinutes()).isEqualTo(30);
        Assertions.assertThat(clinic.hours()).hasSize(7);
        Assertions.assertThat(clinic.hours().get(0).close()).isEqualTo(LocalTime.of(17, 0));
        Assertions.assertThat(clinic.hours().get(5).close()).isEqualTo(LocalTime.of(13, 0));
        Assertions.assertThat(clinic.hours().get(6).open()).isNull();
    }

    @Test
    public void ownerWindow_EndsThirteenDaysAfterToday() {
        Assertions.assertThat(clinicSchedule.lastBookableDay()).isEqualTo(clinicSchedule.today().plusDays(13));
    }
}
