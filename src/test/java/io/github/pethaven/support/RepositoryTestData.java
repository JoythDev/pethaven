package io.github.pethaven.support;

import io.github.pethaven.entity.Drug;
import io.github.pethaven.entity.Evolution;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Species;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.TreatmentDrug;
import io.github.pethaven.entity.Veterinarian;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Construye entidades sin guardar con valores válidos y únicos para los tests de repositorio.
 *
 * <p>Cada método de fábrica incrementa un contador estático para que los valores de documento,
 * correo y nombre nunca colisionen con las restricciones de unicidad de dueños y veterinarios,
 * incluso cuando un solo test construye varias fixtures. Esta clase no realiza ninguna
 * operación de persistencia.</p>
 */
public final class RepositoryTestData {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private RepositoryTestData() {
    }

    public static Owner owner() {
        int sequence = SEQUENCE.incrementAndGet();
        return Owner.builder()
                .document("DOC-" + sequence)
                .name("Owner " + sequence)
                .email("owner" + sequence + "@pethaven.test")
                .password("secret-password")
                .phone("3001234567")
                .build();
    }

    public static Pet pet(Owner owner) {
        int sequence = SEQUENCE.incrementAndGet();
        return Pet.builder()
                .name("Pet " + sequence)
                .owner(owner)
                .species(Species.DOG)
                .breed("Labrador")
                .age(3)
                .weight(10.5)
                .build();
    }

    public static Veterinarian veterinarian() {
        int sequence = SEQUENCE.incrementAndGet();
        return Veterinarian.builder()
                .document("VET-" + sequence)
                .name("Veterinarian " + sequence)
                .email("vet" + sequence + "@pethaven.test")
                .password("secret-password")
                .specialty("General medicine")
                .build();
    }

    public static Treatment treatment(Pet pet, Veterinarian veterinarian, LocalDateTime date, Evolution evolution) {
        return Treatment.builder()
                .pet(pet)
                .veterinarian(veterinarian)
                .date(date)
                .weight(10.5)
                .diagnosis("Test diagnosis")
                .evolution(evolution)
                .notes("Test notes")
                .build();
    }

    public static Drug drug() {
        int sequence = SEQUENCE.incrementAndGet();
        return drug("Drug " + sequence);
    }

    /**
     * Construye un medicamento sin guardar con el nombre indicado y precios de catálogo fijos.
     *
     * <p>Los precios de catálogo se fijan en 1.00 y 2.00, deliberadamente distintos de los precios
     * guardados en cada fila de tratamiento, para que los tests distingan ambas fuentes.</p>
     */
    public static Drug drug(String name) {
        return Drug.builder()
                .name(name)
                .purchasePrice(new BigDecimal("1.00"))
                .salePrice(new BigDecimal("2.00"))
                .unitsAvailable(100)
                .unitsSold(0)
                .build();
    }

    public static TreatmentDrug treatmentDrug(
            Treatment treatment,
            Drug drug,
            int units,
            BigDecimal unitPurchasePrice,
            BigDecimal unitSalePrice
    ) {
        return TreatmentDrug.builder()
                .treatment(treatment)
                .drug(drug)
                .units(units)
                .unitPurchasePrice(unitPurchasePrice)
                .unitSalePrice(unitSalePrice)
                .build();
    }
}
