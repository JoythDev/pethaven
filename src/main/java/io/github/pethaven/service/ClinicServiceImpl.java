package io.github.pethaven.service;

import io.github.pethaven.config.ClinicScheduleProperties;
import io.github.pethaven.dto.response.ClinicHoursResponse;
import io.github.pethaven.dto.response.ClinicResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.Arrays;

@Service
public class ClinicServiceImpl implements ClinicService {

    private final ClinicScheduleProperties properties;
    private final ClinicSchedule clinicSchedule;

    @Autowired
    public ClinicServiceImpl(ClinicScheduleProperties properties, ClinicSchedule clinicSchedule) {
        this.properties = properties;
        this.clinicSchedule = clinicSchedule;
    }

    @Override
    public ClinicResponse getClinic() {
        return new ClinicResponse(
                properties.name(),
                properties.address(),
                properties.phone(),
                clinicSchedule.zone().getId(),
                (int) clinicSchedule.slotDuration().toMinutes(),
                Arrays.stream(DayOfWeek.values())
                        .map(day -> new ClinicHoursResponse(day, clinicSchedule.openingTime(day), clinicSchedule.closingTime(day)))
                        .toList());
    }
}
