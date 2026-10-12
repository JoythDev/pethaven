package io.github.pethaven.dto.response;

public record VeterinarianResponse(
        Long id,
        String document,
        String name,
        String email,
        String specialty,
        String photoUrl,
        Integer attentions,
        Boolean active
) { }
