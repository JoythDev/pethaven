package io.github.pethaven.dto.response;

public record OwnerResponse(
    Long id,
    String document,
    String name,
    String email,
    String phone
) { }
