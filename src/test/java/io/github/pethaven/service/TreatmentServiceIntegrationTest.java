package io.github.pethaven.service;

import io.github.pethaven.dto.request.TreatmentDrugRequest;
import io.github.pethaven.dto.request.TreatmentRequest;
import io.github.pethaven.dto.response.TreatmentResponse;
import io.github.pethaven.entity.Drug;
import io.github.pethaven.entity.Evolution;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.ResourceNotFoundException;
import io.github.pethaven.repository.DrugRepository;
import io.github.pethaven.repository.OwnerRepository;
import io.github.pethaven.repository.PetRepository;
import io.github.pethaven.repository.TreatmentRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import io.github.pethaven.support.RepositoryTestData;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

// Pruebas de integración: el servicio, los repositorios y la base H2 son los reales (sin mocks).
// Cada prueba corre dentro de una transacción que se revierte al terminar, y la base es en
// memoria para no tocar el archivo pethaven-database de la app.
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:treatment-service-it;DB_CLOSE_DELAY=-1")
@Transactional
public class TreatmentServiceIntegrationTest {

    private final TreatmentService treatmentService;
    private final TreatmentRepository treatmentRepository;
    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final DrugRepository drugRepository;
    private final EntityManager entityManager;

    private Pet pet;
    private Veterinarian vet;
    private Drug drug;

    @Autowired
    public TreatmentServiceIntegrationTest(
            TreatmentService treatmentService,
            TreatmentRepository treatmentRepository,
            OwnerRepository ownerRepository,
            PetRepository petRepository,
            VeterinarianRepository veterinarianRepository,
            DrugRepository drugRepository,
            EntityManager entityManager
    ) {
        this.treatmentService = treatmentService;
        this.treatmentRepository = treatmentRepository;
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.drugRepository = drugRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    public void setUp() {
        Owner owner = ownerRepository.save(RepositoryTestData.owner());
        pet = petRepository.save(RepositoryTestData.pet(owner));
        vet = veterinarianRepository.save(RepositoryTestData.veterinarian());
        drug = drugRepository.save(Drug.builder()
                .name("Test drug " + System.nanoTime())
                .purchasePrice(new BigDecimal("8000.00"))
                .salePrice(new BigDecimal("15000.00"))
                .unitsAvailable(10)
                .unitsSold(0)
                .build());
        entityManager.flush();
    }

    // Arma la petición de un tratamiento con un solo medicamento (el de setUp).
    private TreatmentRequest request(Evolution evolution, String diagnosis, int units) {
        return new TreatmentRequest(
                pet.getId(), vet.getId(), null, 12.4, diagnosis, evolution, "Notas de prueba",
                List.of(new TreatmentDrugRequest(drug.getId(), units)));
    }

    @Test
    public void treatmentService_createTreatment_ValidAdmission_PersistsTreatmentAndUpdatesPetVetAndStock() {
        // Arrange
        TreatmentRequest request = request(null, "Otitis externa", 3);

        // Act
        TreatmentResponse response = treatmentService.createTreatment(request);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(response.id()).isNotNull();
        Assertions.assertThat(response.drugs()).hasSize(1);
        Assertions.assertThat(response.drugs().get(0).units()).isEqualTo(3);
        Assertions.assertThat(response.drugs().get(0).unitSalePrice()).isEqualByComparingTo(new BigDecimal("15000.00"));
        Assertions.assertThat(treatmentRepository.findById(response.id())).isPresent();

        Pet reloadedPet = petRepository.findById(pet.getId()).orElseThrow();
        Assertions.assertThat(reloadedPet.getDisease()).isEqualTo("Otitis externa");
        Assertions.assertThat(reloadedPet.getWeight()).isEqualTo(12.4);

        Veterinarian reloadedVet = veterinarianRepository.findById(vet.getId()).orElseThrow();
        Assertions.assertThat(reloadedVet.getAttentions()).isEqualTo(1);

        Drug reloadedDrug = drugRepository.findById(drug.getId()).orElseThrow();
        Assertions.assertThat(reloadedDrug.getUnitsAvailable()).isEqualTo(7);
        Assertions.assertThat(reloadedDrug.getUnitsSold()).isEqualTo(3);
    }

    @Test
    public void treatmentService_createTreatment_FollowUpOnSickPet_AccumulatesAttentionsAndStockMovements() {
        // Arrange
        treatmentService.createTreatment(request(null, "Otitis externa", 3));

        // Act
        TreatmentResponse followUp = treatmentService.createTreatment(request(Evolution.IMPROVING, "Otitis externa", 2));
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(followUp.evolution()).isEqualTo(Evolution.IMPROVING);
        Assertions.assertThat(veterinarianRepository.findById(vet.getId()).orElseThrow().getAttentions()).isEqualTo(2);
        Drug reloadedDrug = drugRepository.findById(drug.getId()).orElseThrow();
        Assertions.assertThat(reloadedDrug.getUnitsAvailable()).isEqualTo(5);
        Assertions.assertThat(reloadedDrug.getUnitsSold()).isEqualTo(5);
    }

    @Test
    public void treatmentService_createTreatment_InactivePet_ThrowsIllegalOperation() {
        // Arrange
        pet.setActive(false);
        petRepository.saveAndFlush(pet);
        TreatmentRequest request = request(null, "Otitis externa", 1);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("no está activa");
    }

    @Test
    public void treatmentService_createTreatment_InactiveVeterinarian_ThrowsIllegalOperation() {
        // Arrange
        vet.setActive(false);
        veterinarianRepository.saveAndFlush(vet);
        TreatmentRequest request = request(null, "Otitis externa", 1);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("no está activo");
    }

    @Test
    public void treatmentService_createTreatment_NoDrugsInStock_ThrowsIllegalOperation() {
        // Arrange: se deja todo el inventario en cero (la transacción de la prueba lo revierte).
        List<Drug> allDrugs = drugRepository.findAll();
        allDrugs.forEach(existing -> existing.setUnitsAvailable(0));
        drugRepository.saveAllAndFlush(allDrugs);
        TreatmentRequest request = request(null, "Otitis externa", 1);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("No hay medicamentos disponibles");
    }

    @Test
    public void treatmentService_createTreatment_NotEnoughUnitsOfDrug_ThrowsIllegalOperation() {
        // Arrange: hay 10 unidades y se piden 11.
        TreatmentRequest request = request(null, "Otitis externa", 11);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("No hay suficientes unidades");
    }

    @Test
    public void treatmentService_createTreatment_NonExistingPet_ThrowsResourceNotFound() {
        // Arrange
        TreatmentRequest request = new TreatmentRequest(
                999_999L, vet.getId(), null, 12.4, "Otitis externa", null, null,
                List.of(new TreatmentDrugRequest(drug.getId(), 1)));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void treatmentService_createTreatment_StablePetWithEvolution_ThrowsIllegalOperation() {
        // Arrange: la mascota está estable (sin diagnóstico), así que es un ingreso y no lleva evolución.
        TreatmentRequest request = request(Evolution.IMPROVING, "Otitis externa", 1);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.createTreatment(request))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("no lleva evolución");
    }

    @Test
    public void treatmentService_dischargePet_SickPet_CreatesRecoveredRecordAndLeavesPetStable() {
        // Arrange
        pet.setDisease("Gastroenteritis");
        petRepository.saveAndFlush(pet);

        // Act
        TreatmentResponse discharge = treatmentService.dischargePet(pet.getId(), vet.getId());
        entityManager.flush();
        entityManager.clear();

        // Assert
        Assertions.assertThat(discharge.evolution()).isEqualTo(Evolution.RECOVERED);
        Assertions.assertThat(discharge.diagnosis()).isEqualTo("Gastroenteritis");
        Assertions.assertThat(discharge.drugs()).isEmpty();
        Assertions.assertThat(petRepository.findById(pet.getId()).orElseThrow().getDisease()).isNull();
        // El alta no es un tratamiento: no suma atenciones.
        Assertions.assertThat(veterinarianRepository.findById(vet.getId()).orElseThrow().getAttentions()).isZero();
    }

    @Test
    public void treatmentService_dischargePet_StablePet_ThrowsIllegalOperation() {
        // Act + Assert (la mascota de setUp no tiene diagnóstico)
        Assertions.assertThatThrownBy(() -> treatmentService.dischargePet(pet.getId(), vet.getId()))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessageContaining("ya está estable");
    }

    @Test
    public void treatmentService_getTreatmentsByPetId_PetWithTreatment_ReturnsOnlyThatPetTreatments() {
        // Arrange
        TreatmentResponse created = treatmentService.createTreatment(request(null, "Otitis externa", 1));
        entityManager.flush();
        entityManager.clear();

        // Act
        List<TreatmentResponse> result = treatmentService.getTreatmentsByPetId(pet.getId());

        // Assert
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0).id()).isEqualTo(created.id());
        Assertions.assertThat(result.get(0).petId()).isEqualTo(pet.getId());
    }

    @Test
    public void treatmentService_getTreatmentById_NonExistingId_ThrowsResourceNotFound() {
        // Act + Assert
        Assertions.assertThatThrownBy(() -> treatmentService.getTreatmentById(999_999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
