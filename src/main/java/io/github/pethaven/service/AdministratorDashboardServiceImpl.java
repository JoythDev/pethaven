package io.github.pethaven.service;

import io.github.pethaven.dto.response.AdministratorDashboardResponse;
import io.github.pethaven.entity.Evolution;
import io.github.pethaven.repository.PetRepository;
import io.github.pethaven.repository.TreatmentDrugRepository;
import io.github.pethaven.repository.TreatmentRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import io.github.pethaven.repository.projection.MedicationTreatmentCountProjection;
import io.github.pethaven.repository.projection.TopMedicationUnitsProjection;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
public class AdministratorDashboardServiceImpl implements AdministratorDashboardService {

    private static final ZoneId REPORTING_ZONE = ZoneId.of("America/Bogota");

    private final PetRepository petRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final TreatmentRepository treatmentRepository;
    private final TreatmentDrugRepository treatmentDrugRepository;

    public AdministratorDashboardServiceImpl(
            PetRepository petRepository,
            VeterinarianRepository veterinarianRepository,
            TreatmentRepository treatmentRepository,
            TreatmentDrugRepository treatmentDrugRepository
    ) {
        this.petRepository = petRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.treatmentRepository = treatmentRepository;
        this.treatmentDrugRepository = treatmentDrugRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdministratorDashboardResponse getDashboardSummary() {
        LocalDate currentMonthStart = LocalDate.now(REPORTING_ZONE).withDayOfMonth(1);
        LocalDate previousMonthStart = currentMonthStart.minusMonths(1);
        LocalDateTime startInclusive = previousMonthStart.atStartOfDay();
        LocalDateTime endExclusive = currentMonthStart.atStartOfDay();

        List<AdministratorDashboardResponse.MedicationTreatmentCount> medicationTreatmentCounts =
                treatmentDrugRepository.findMedicationTreatmentCountsBetween(
                                startInclusive, endExclusive, Evolution.RECOVERED)
                        .stream()
                        .map(this::toMedicationTreatmentCount)
                        .toList();

        List<AdministratorDashboardResponse.TopMedicationByUnitsSold> topMedications =
                treatmentDrugRepository.findTopMedicationsByAllTimeUnitsSold(PageRequest.of(0, 3))
                        .stream()
                        .map(this::toTopMedication)
                        .toList();

        return new AdministratorDashboardResponse(
                previousMonthStart,
                currentMonthStart,
                REPORTING_ZONE.getId(),
                treatmentRepository.countAdministeredTreatmentsBetween(
                        startInclusive, endExclusive, Evolution.RECOVERED),
                medicationTreatmentCounts,
                veterinarianRepository.countByActiveTrue(),
                veterinarianRepository.countByActiveFalse(),
                petRepository.count(),
                petRepository.countByActiveTrue(),
                Objects.requireNonNullElse(treatmentDrugRepository.sumAllTimeSales(), BigDecimal.ZERO),
                Objects.requireNonNullElse(treatmentDrugRepository.sumAllTimeGrossProfit(), BigDecimal.ZERO),
                topMedications
        );
    }

    private AdministratorDashboardResponse.MedicationTreatmentCount toMedicationTreatmentCount(
            MedicationTreatmentCountProjection projection) {
        return new AdministratorDashboardResponse.MedicationTreatmentCount(
                projection.getMedicationId(),
                projection.getMedicationName(),
                projection.getTreatmentCount()
        );
    }

    private AdministratorDashboardResponse.TopMedicationByUnitsSold toTopMedication(
            TopMedicationUnitsProjection projection) {
        return new AdministratorDashboardResponse.TopMedicationByUnitsSold(
                projection.getMedicationId(),
                projection.getMedicationName(),
                projection.getUnitsSold()
        );
    }
}
