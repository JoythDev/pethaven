package io.github.pethaven.service;

import io.github.pethaven.dto.request.AppointmentRequest;
import io.github.pethaven.dto.request.AppointmentCompletionRequest;
import io.github.pethaven.dto.request.AppointmentRescheduleRequest;
import io.github.pethaven.dto.request.PetRequest;
import io.github.pethaven.dto.request.VeterinarianAppointmentRequest;
import io.github.pethaven.dto.response.AppointmentResponse;
import io.github.pethaven.dto.response.AvailabilityDayResponse;
import io.github.pethaven.entity.Appointment;
import io.github.pethaven.entity.AppointmentActor;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    Appointment getAppointmentEntity(Long id);
    AppointmentResponse getAppointmentById(Long id);
    List<AvailabilityDayResponse> getAvailability();
    List<AppointmentResponse> getAppointmentsByOwnerId(Long ownerId);
    AppointmentResponse bookAppointment(Long ownerId, AppointmentRequest request);
    AppointmentResponse cancelAppointment(Long id, AppointmentActor actor);
    AppointmentResponse rescheduleAppointment(Long id, AppointmentRescheduleRequest request, AppointmentActor actor);

    List<AppointmentResponse> getAppointmentsByVeterinarianId(Long veterinarianId, LocalDate from, LocalDate to);
    List<AvailabilityDayResponse> getVeterinarianAvailability(Long veterinarianId, LocalDate from, LocalDate to);
    AppointmentResponse scheduleForHospitalizedPet(Long veterinarianId, VeterinarianAppointmentRequest request);
    AppointmentResponse startAppointment(Long id);
    AppointmentResponse completeAppointment(Long id, AppointmentCompletionRequest request);
    AppointmentResponse markNoShow(Long id);
    AppointmentResponse registerProvisionalPet(Long id, PetRequest request);

}
