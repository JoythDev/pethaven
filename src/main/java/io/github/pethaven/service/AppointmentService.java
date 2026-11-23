package io.github.pethaven.service;

import io.github.pethaven.dto.request.AppointmentRequest;
import io.github.pethaven.dto.request.AppointmentRescheduleRequest;
import io.github.pethaven.dto.response.AppointmentResponse;
import io.github.pethaven.dto.response.AvailabilityDayResponse;
import io.github.pethaven.entity.Appointment;
import io.github.pethaven.entity.AppointmentActor;

import java.util.List;

public interface AppointmentService {

    Appointment getAppointmentEntity(Long id);
    AppointmentResponse getAppointmentById(Long id);
    List<AvailabilityDayResponse> getAvailability();
    List<AppointmentResponse> getAppointmentsByOwnerId(Long ownerId);
    AppointmentResponse bookAppointment(Long ownerId, AppointmentRequest request);
    AppointmentResponse cancelAppointment(Long id, AppointmentActor actor);
    AppointmentResponse rescheduleAppointment(Long id, AppointmentRescheduleRequest request, AppointmentActor actor);

}
