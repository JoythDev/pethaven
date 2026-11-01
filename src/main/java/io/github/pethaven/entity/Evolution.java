package io.github.pethaven.entity;

// Evolución de la mascota en una consulta de seguimiento. En el ingreso (la mascota llega
// estable, sin diagnóstico activo) no hay evolución: el tratamiento la guarda en null.
// RECOVERED solo lo usa el alta (sin medicamentos), nunca un tratamiento.
public enum Evolution {
    STABLE,
    IMPROVING,
    WORSENING,
    RECOVERED
}
