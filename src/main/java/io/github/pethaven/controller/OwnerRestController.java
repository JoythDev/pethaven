package io.github.pethaven.controller;

import io.github.pethaven.dto.request.OwnerRequest;
import io.github.pethaven.dto.request.OwnerUpdateRequest;
import io.github.pethaven.dto.response.OwnerResponse;
import io.github.pethaven.service.OwnerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owners")
public class OwnerRestController {

    private final OwnerService ownerService;

    @Autowired
    public OwnerRestController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping
    public List<OwnerResponse> getAllOwners() {
        return ownerService.getAllOwners();
    }

    @GetMapping("/{id}")
    public OwnerResponse getOwnerById(@PathVariable Long id) {
        return ownerService.getOwnerById(id);
    }

    @PostMapping
    public ResponseEntity<OwnerResponse> createOwner(@Valid @RequestBody OwnerRequest request) {
        OwnerResponse newOwner = ownerService.createOwner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(newOwner);
    }

    @PutMapping("/{id}")
    public OwnerResponse updateOwner(@PathVariable Long id, @Valid @RequestBody OwnerUpdateRequest request) {
        return ownerService.updateOwner(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOwner(@PathVariable Long id) {
        ownerService.deleteOwnerById(id);
        return ResponseEntity.noContent().build();
    }

}
