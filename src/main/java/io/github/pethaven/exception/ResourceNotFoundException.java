package io.github.pethaven.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        // Esto generará un mensaje como: "El recurso 'Owner' con document: '123456789' no fue encontrado"
        super(String.format("El recurso '%s' con %s: '%s' no fue encontrado", resourceName, fieldName, fieldValue));
    }
}
