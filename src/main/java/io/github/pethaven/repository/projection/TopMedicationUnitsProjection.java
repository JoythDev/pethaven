package io.github.pethaven.repository.projection;

/**
 * Agregado de solo lectura para las unidades administradas de todos los tiempos por medicamento.
 */
public interface TopMedicationUnitsProjection {
    Long getMedicationId();
    String getMedicationName();
    Long getUnitsSold();
}
