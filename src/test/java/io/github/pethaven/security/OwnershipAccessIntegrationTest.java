package io.github.pethaven.security;

import io.github.pethaven.dto.request.OwnerRequest;
import io.github.pethaven.dto.request.VeterinarianRequest;
import io.github.pethaven.entity.Drug;
import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Role;
import io.github.pethaven.entity.Treatment;
import io.github.pethaven.entity.Veterinarian;
import io.github.pethaven.repository.DrugRepository;
import io.github.pethaven.repository.OwnerRepository;
import io.github.pethaven.repository.PetRepository;
import io.github.pethaven.repository.TreatmentRepository;
import io.github.pethaven.repository.VeterinarianRepository;
import io.github.pethaven.service.OwnerService;
import io.github.pethaven.service.VeterinarianService;
import io.github.pethaven.support.RepositoryTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ownership layer of the permission matrix (odd S11): an owner only reaches their own profile and pets, a
 * veterinarian only their own profile and treatments, and neither changes their own document or e-mail.
 * Runs through the real security chain, controllers and database; denied cases answer 403 with the API error.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class OwnershipAccessIntegrationTest {

    private static final String DENIED_MESSAGE = "No tienes permisos para acceder a este recurso.";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private OwnerService ownerService;
    @Autowired
    private VeterinarianService veterinarianService;
    @Autowired
    private OwnerRepository ownerRepository;
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    @Autowired
    private PetRepository petRepository;
    @Autowired
    private TreatmentRepository treatmentRepository;
    @Autowired
    private DrugRepository drugRepository;

    private Owner owner;
    private Owner otherOwner;
    private Pet pet;
    private Pet otherPet;
    private Veterinarian vet;
    private Veterinarian otherVet;
    private Treatment treatmentByVet;
    private Treatment treatmentByOtherVet;
    private Drug drug;

    @BeforeEach
    public void setUp() {
        owner = createOwner("111000111", "owner.one@pethaven.test");
        otherOwner = createOwner("222000222", "owner.two@pethaven.test");
        pet = petRepository.save(RepositoryTestData.pet(owner));
        otherPet = petRepository.save(RepositoryTestData.pet(otherOwner));
        vet = createVeterinarian("333000333", "vet.one@pethaven.test");
        otherVet = createVeterinarian("444000444", "vet.two@pethaven.test");
        treatmentByVet = treatmentRepository.save(RepositoryTestData.treatment(pet, vet, LocalDateTime.now().minusDays(1), null));
        treatmentByOtherVet = treatmentRepository.save(
                RepositoryTestData.treatment(pet, otherVet, LocalDateTime.now().minusDays(1), null));
        drug = drugRepository.save(Drug.builder()
                .name("Ownership drug " + System.nanoTime())
                .purchasePrice(new BigDecimal("8000.00"))
                .salePrice(new BigDecimal("15000.00"))
                .unitsAvailable(10)
                .unitsSold(0)
                .build());
    }

    // ---------- Dueño: solo su perfil y sus mascotas ----------

    @Test
    public void owner_ReadsOwnProfileAndPets_IsAllowed() throws Exception {
        perform(MockMvcRequestBuilders.get("/api/owners/{id}", owner.getId()), asOwner(owner)).andExpect(MockMvcResultMatchers.status().isOk());
        perform(MockMvcRequestBuilders.get("/api/owners/{id}/pets", owner.getId()), asOwner(owner)).andExpect(MockMvcResultMatchers.status().isOk());
        perform(MockMvcRequestBuilders.get("/api/pets/{id}", pet.getId()), asOwner(owner)).andExpect(MockMvcResultMatchers.status().isOk());
        perform(MockMvcRequestBuilders.get("/api/pets/{id}/treatments", pet.getId()), asOwner(owner)).andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void owner_ReadsAnotherOwnersProfileOrPets_Returns403() throws Exception {
        expectDenied(MockMvcRequestBuilders.get("/api/owners/{id}", otherOwner.getId()), asOwner(owner));
        expectDenied(MockMvcRequestBuilders.get("/api/owners/{id}/pets", otherOwner.getId()), asOwner(owner));
        expectDenied(MockMvcRequestBuilders.get("/api/pets/{id}", otherPet.getId()), asOwner(owner));
        expectDenied(MockMvcRequestBuilders.get("/api/pets/{id}/treatments", otherPet.getId()), asOwner(owner));
        expectDenied(MockMvcRequestBuilders.get("/api/pets/{id}/photo", otherPet.getId()), asOwner(owner));
    }

    @Test
    public void owner_ReadsAnotherOwnersPetNotAcceptingJson_StillReturns403() throws Exception {
        // The ApiErrorResponse body cannot be negotiated, but the 403 holds and no exception escapes the chain.
        perform(MockMvcRequestBuilders.get("/api/pets/{id}", otherPet.getId()).accept(MediaType.APPLICATION_XML), asOwner(owner))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    public void owner_ReadsPetThatDoesNotExist_Returns403() throws Exception {
        expectDenied(MockMvcRequestBuilders.get("/api/pets/{id}", Long.MAX_VALUE), asOwner(owner));
    }

    @Test
    public void owner_UpdatesOwnNameAndPassword_IsAllowed() throws Exception {
        perform(updateOwner(owner.getId(), owner.getDocument(), "Nuevo Nombre", owner.getEmail()), asOwner(owner))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Nuevo Nombre"));
    }

    @Test
    public void owner_ChangesOwnEmailOrDocument_Returns403() throws Exception {
        expectDenied(updateOwner(owner.getId(), owner.getDocument(), owner.getName(), "changed@pethaven.test"), asOwner(owner));
        expectDenied(updateOwner(owner.getId(), "999888777", owner.getName(), owner.getEmail()), asOwner(owner));
    }

    @Test
    public void owner_UpdatesAnotherOwner_Returns403() throws Exception {
        expectDenied(updateOwner(otherOwner.getId(), otherOwner.getDocument(), "Intruso", otherOwner.getEmail()), asOwner(owner));
    }

    @Test
    public void veterinarian_ChangesOwnerEmailAndDocument_IsAllowed() throws Exception {
        perform(updateOwner(owner.getId(), "999888777", owner.getName(), "changed@pethaven.test"), asVet(vet))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("changed@pethaven.test"));
    }

    @Test
    public void staff_ReadsAnyOwnerAndPet_IsAllowed() throws Exception {
        perform(MockMvcRequestBuilders.get("/api/owners/{id}", otherOwner.getId()), asAdmin()).andExpect(MockMvcResultMatchers.status().isOk());
        perform(MockMvcRequestBuilders.get("/api/pets/{id}/treatments", otherPet.getId()), asAdmin()).andExpect(MockMvcResultMatchers.status().isOk());
        perform(MockMvcRequestBuilders.get("/api/pets/{id}", otherPet.getId()), asVet(vet)).andExpect(MockMvcResultMatchers.status().isOk());
        perform(MockMvcRequestBuilders.get("/api/owners/{id}/pets", otherOwner.getId()), asVet(vet)).andExpect(MockMvcResultMatchers.status().isOk());
    }

    // ---------- Veterinario: solo su perfil ----------

    @Test
    public void veterinarian_ReadsOwnProfileAndTreatments_IsAllowed() throws Exception {
        perform(MockMvcRequestBuilders.get("/api/veterinarians/{id}", vet.getId()), asVet(vet)).andExpect(MockMvcResultMatchers.status().isOk());
        perform(MockMvcRequestBuilders.get("/api/veterinarians/{id}/treatments", vet.getId()), asVet(vet)).andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void veterinarian_ReadsAnotherVeterinarian_Returns403() throws Exception {
        expectDenied(MockMvcRequestBuilders.get("/api/veterinarians/{id}", otherVet.getId()), asVet(vet));
        expectDenied(MockMvcRequestBuilders.get("/api/veterinarians/{id}/treatments", otherVet.getId()), asVet(vet));
    }

    @Test
    public void veterinarian_UpdatesOwnNameAndSpecialty_IsAllowed() throws Exception {
        perform(updateVet(vet.getId(), vet.getDocument(), "Nueva Vet", vet.getEmail()), asVet(vet))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Nueva Vet"));
    }

    @Test
    public void veterinarian_ChangesOwnEmailOrDocument_Returns403() throws Exception {
        expectDenied(updateVet(vet.getId(), vet.getDocument(), vet.getName(), "changed.vet@pethaven.test"), asVet(vet));
        expectDenied(updateVet(vet.getId(), "999777555", vet.getName(), vet.getEmail()), asVet(vet));
    }

    @Test
    public void veterinarian_UpdatesAnotherVeterinarian_Returns403() throws Exception {
        expectDenied(updateVet(otherVet.getId(), otherVet.getDocument(), "Intruso", otherVet.getEmail()), asVet(vet));
    }

    @Test
    public void admin_ReadsAndChangesAnyVeterinarianIdentity_IsAllowed() throws Exception {
        perform(MockMvcRequestBuilders.get("/api/veterinarians/{id}/treatments", otherVet.getId()), asAdmin())
                .andExpect(MockMvcResultMatchers.status().isOk());
        perform(updateVet(otherVet.getId(), "999777555", otherVet.getName(), "changed.vet@pethaven.test"), asAdmin())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("changed.vet@pethaven.test"));
    }

    // ---------- Veterinario: tratamientos y altas a su nombre ----------

    @Test
    public void veterinarian_RegistersTreatmentInOwnName_IsAllowed() throws Exception {
        perform(createTreatment(vet.getId()), asVet(vet)).andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    public void veterinarian_RegistersTreatmentInAnotherVeterinariansName_Returns403() throws Exception {
        expectDenied(createTreatment(otherVet.getId()), asVet(vet));
    }

    @Test
    public void veterinarian_DischargesInAnotherVeterinariansName_Returns403() throws Exception {
        expectDenied(MockMvcRequestBuilders.post("/api/pets/{id}/discharge", pet.getId())
                .param("veterinarianId", otherVet.getId().toString()), asVet(vet));
    }

    @Test
    public void veterinarian_DischargesInOwnName_PassesAuthorization() throws Exception {
        // The discharge may still be refused by business rules (409 if the pet is already stable), never by access.
        int status = perform(MockMvcRequestBuilders.post("/api/pets/{id}/discharge", pet.getId())
                .param("veterinarianId", vet.getId().toString()), asVet(vet)).andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(403);
    }

    @Test
    public void veterinarian_AddsDrugToOwnTreatment_IsAllowed() throws Exception {
        perform(addDrug(treatmentByVet.getId()), asVet(vet)).andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    public void veterinarian_AddsDrugToAnotherVeterinariansTreatment_Returns403() throws Exception {
        expectDenied(addDrug(treatmentByOtherVet.getId()), asVet(vet));
    }

    // ---------- Helpers ----------

    private Owner createOwner(String document, String email) {
        Long id = ownerService.createOwner(new OwnerRequest(document, "Owner " + document, email, "password123", "3001234567")).id();
        return ownerRepository.findById(id).orElseThrow();
    }

    private Veterinarian createVeterinarian(String document, String email) {
        Long id = veterinarianService.createVeterinarian(
                new VeterinarianRequest(document, "Vet " + document, email, "password123", "Cirugía", null)).id();
        return veterinarianRepository.findById(id).orElseThrow();
    }

    private static RequestPostProcessor asOwner(Owner owner) {
        return SecurityMockMvcRequestPostProcessors.user(owner.getUser().getUsername()).roles(Role.OWNER);
    }

    private static RequestPostProcessor asVet(Veterinarian vet) {
        return SecurityMockMvcRequestPostProcessors.user(vet.getUser().getUsername()).roles(Role.VETERINARIAN);
    }

    private static RequestPostProcessor asAdmin() {
        return SecurityMockMvcRequestPostProcessors.user("admin@pethaven.test").roles(Role.ADMIN);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, RequestPostProcessor user) throws Exception {
        return mockMvc.perform(request.with(user));
    }

    private void expectDenied(MockHttpServletRequestBuilder request, RequestPostProcessor user) throws Exception {
        perform(request, user)
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(403))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(DENIED_MESSAGE));
    }

    private static MockHttpServletRequestBuilder updateOwner(Long id, String document, String name, String email) {
        return MockMvcRequestBuilders.put("/api/owners/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                {"document":"%s","name":"%s","email":"%s","password":"newPassword123","phone":"3001234567"}
                """.formatted(document, name, email));
    }

    private static MockHttpServletRequestBuilder updateVet(Long id, String document, String name, String email) {
        return MockMvcRequestBuilders.put("/api/veterinarians/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                {"document":"%s","name":"%s","email":"%s","password":"newPassword123","specialty":"Dermatología"}
                """.formatted(document, name, email));
    }

    private MockHttpServletRequestBuilder createTreatment(Long veterinarianId) {
        return MockMvcRequestBuilders.post("/api/treatments").contentType(MediaType.APPLICATION_JSON).content("""
                {"petId":%d,"veterinarianId":%d,"weight":12.4,"diagnosis":"Otitis","notes":"Notas",
                 "drugs":[{"drugId":%d,"units":1}]}
                """.formatted(pet.getId(), veterinarianId, drug.getId()));
    }

    private MockHttpServletRequestBuilder addDrug(Long treatmentId) {
        return MockMvcRequestBuilders.post("/api/treatments/{id}/drugs", treatmentId).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"drugId":%d,"units":1}
                        """.formatted(drug.getId()));
    }
}
