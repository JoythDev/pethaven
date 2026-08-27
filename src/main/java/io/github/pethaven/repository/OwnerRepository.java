package io.github.pethaven.repository;

import io.github.pethaven.entity.Owner;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class OwnerRepository {

    Map<Long, Owner> owners = new HashMap<>();

    public OwnerRepository() {
        owners.put(1L, new Owner(1L, "123456789", "Andrew Smith", "andrew.smith@example.com", "password123", "555-1234"));
        owners.put(2L, new Owner(2L, "987654321", "Jane Doe", "jane.doe@example.com", "password456", "555-5678"));
        owners.put(3L, new Owner(3L, "555555555", "John Doe", "john.doe@example.com", "password789", "555-9012"));
        owners.put(4L, new Owner(4L, "111222333", "Alice Johnson", "alice.johnson@example.com", "password012", "555-3456"));
        owners.put(5L, new Owner(5L, "444555666", "Bob Brown", "bob.brown@example.com", "password345", "555-7890"));
    }

    public List<Owner> findAll() {
        return owners.values().stream().toList();
    }

    public Owner findById(Long id) {
        return owners.get(id);
    }

    public Owner findByDocument(String document) {
        return owners.values().stream()
                .filter(owner -> owner.getDocument().equals(document))
                .findFirst()
                .orElse(null);
    }

    public Owner findByEmail(String email) {
        return owners.values().stream()
                .filter(owner -> owner.getEmail().equals(email))
                .findFirst()
                .orElse(null);
    }

    public void save(Owner owner) {
        if (owner.getId() == null) {
            owner.setId(nextId());
        }
        owners.put(owner.getId(), owner);
    }

    private Long nextId() {
        return owners.keySet().stream()
                .max(Long::compareTo)
                .orElse(0L) + 1;
    }

    public void deleteById(Long id) {
        owners.remove(id);
    }

}
