package io.github.pethaven.repository.projection;

/**
 * Agregado de solo lectura para el número de tratamientos distintos que administraron un medicamento.
 */
public interface MedicationTreatmentCountProjection {
    Long getMedicationId();
    String getMedicationName();
    Long getTreatmentCount();
}
