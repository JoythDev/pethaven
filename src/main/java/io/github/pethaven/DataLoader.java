package io.github.pethaven;

import io.github.pethaven.entity.Owner;
import io.github.pethaven.entity.Pet;
import io.github.pethaven.entity.Species;
import io.github.pethaven.repository.OwnerRepository;
import io.github.pethaven.repository.PetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Component
@Transactional
public class DataLoader implements CommandLineRunner {

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PetRepository petRepository;

    @Override
    public void run(String... args) throws Exception {

        Random random = new Random(42);

        // ==================================
        // ==== Cargar dueños de ejemplo ====
        // ==================================

        ownerRepository.save(Owner.builder().name("John Doe").document("123456789").phone("1234567890").email("john.doe@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Jane Doe").document("987654321").phone("0987654321").email("jane.doe@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Alice Smith").document("456789123").phone("4567891230").email("alice.smith@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Bob Johnson").document("789123456").phone("7891234560").email("bob.johnson@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Charlie Brown").document("321654987").phone("3216549870").email("charlie.brown@example.com").password("password123").build());

        ownerRepository.save(Owner.builder().name("María García").document("145230786").phone("612345678").email("maria.garcia@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("José Rodríguez").document("287134590").phone("623456789").email("jose.rodriguez@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Ana Martínez").document("376495821").phone("634567891").email("ana.martinez@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Carlos López").document("461728935").phone("645678912").email("carlos.lopez@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Laura Sánchez").document("592816374").phone("645678923").email("laura.sanchez@example.com").password("ejemplo123").build());
        ownerRepository.save(Owner.builder().name("David Pérez").document("675834129").phone("656789012").email("david.perez@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Carmen Fernández").document("712948365").phone("667890145").email("carmen.fernandez@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Javier Gómez").document("829415763").phone("678901256").email("javier.gomez@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Elena Ruiz").document("846173920").phone("689012367").email("elena.ruiz@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Miguel Torres").document("954268137").phone("691234578").email("miguel.torres@example.com").password("ejemplo123").build());

        ownerRepository.save(Owner.builder().name("Pilar Ramírez").document("162584739").phone("602345689").email("pilar.ramirez@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Antonio Navarro").document("273859164").phone("613456791").email("antonio.navarro@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Lucía Díaz").document("384925716").phone("624567812").email("lucia.diaz@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Sergio Morales").document("495136827").phone("635678923").email("sergio.morales@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Isabel Ortega").document("516247938").phone("646789014").email("isabel.ortega@example.com").password("ejemplo123").build());
        ownerRepository.save(Owner.builder().name("Pedro Vargas").document("627358149").phone("657890125").email("pedro.vargas@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Marta Herrera").document("738469251").phone("668901236").email("marta.herrera@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Roberto Castillo").document("849571362").phone("679012347").email("roberto.castillo@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Rosa Jiménez").document("849572473").phone("681234458").email("rosa.jimenez@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Alberto Mendoza").document("951683584").phone("692345569").email("alberto.mendoza@example.com").password("ejemplo123").build());

        ownerRepository.save(Owner.builder().name("Cristina Vega").document("162794695").phone("603456671").email("cristina.vega@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Andrés Rubio").document("273815716").phone("614567782").email("andres.rubio@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Paula Castro").document("384926827").phone("625678893").email("paula.castro@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Francisco Molina").document("495137938").phone("636789914").email("francisco.molina@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Raquel Delgado").document("516248149").phone("647890125").email("raquel.delgado@example.com").password("ejemplo123").build());
        ownerRepository.save(Owner.builder().name("Diego Aguilar").document("527359251").phone("658901236").email("diego.aguilar@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Nuria Campos").document("638461362").phone("669012347").email("nuria.campos@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Álvaro Rivera").document("749572473").phone("671234458").email("alvaro.rivera@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Beatriz Reyes").document("851683584").phone("682345569").email("beatriz.reyes@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Manuel Cabrera").document("962794695").phone("693456671").email("manuel.cabrera@example.com").password("ejemplo123").build());

        ownerRepository.save(Owner.builder().name("Silvia Montes").document("173815716").phone("604567782").email("silvia.montes@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Jorge Flores").document("284926827").phone("615678893").email("jorge.flores@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Adriana Salas").document("395137938").phone("626789914").email("adriana.salas@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Óscar Ibáñez").document("416248149").phone("637890125").email("oscar.ibanez@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Natalia Vidal").document("538470362").phone("648901236").email("natalia.vidal@example.com").password("ejemplo123").build());
        ownerRepository.save(Owner.builder().name("Ricardo Marín").document("648372915").phone("659012347").email("ricardo.marin@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Gloria Sanz").document("759683026").phone("661234458").email("gloria.sanz@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Enrique Espinosa").document("861794137").phone("672345569").email("enrique.espinosa@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Teresa Rojas").document("972815248").phone("683456671").email("teresa.rojas@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Gabriel Cruz").document("173815717").phone("694567782").email("gabriel.cruz@example.com").password("ejemplo123").build());

        ownerRepository.save(Owner.builder().name("Ángela Prieto").document("284926828").phone("605678893").email("angela.prieto@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Fernando Cano").document("395137939").phone("616789914").email("fernando.cano@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Mónica Serrano").document("416248150").phone("627890125").email("monica.serrano@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Rubén Lozano").document("527359252").phone("638901236").email("ruben.lozano@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Irene Blanco").document("638461363").phone("649012347").email("irene.blanco@example.com").password("ejemplo123").build());
        ownerRepository.save(Owner.builder().name("Pablo Gallego").document("749572474").phone("651234458").email("pablo.gallego@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Susana Ferrer").document("851683585").phone("662345569").email("susana.ferrer@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Adrián Salas").document("962794696").phone("673456671").email("adrian.salas@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Verónica Soto").document("173815718").phone("684567782").email("veronica.soto@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Raúl Carmona").document("284926829").phone("695678893").email("raul.carmona@example.com").password("ejemplo123").build());

        ownerRepository.save(Owner.builder().name("Andrea Miranda").document("395137940").phone("606789914").email("andrea.miranda@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Santiago Guerrero").document("416248151").phone("617890125").email("santiago.guerrero@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Noelia Aguilar").document("527359253").phone("628901236").email("noelia.aguilar@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Iván Pardo").document("638461364").phone("639012347").email("ivan.pardo@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Eva Soler").document("749572475").phone("641234458").email("eva.soler@example.com").password("ejemplo123").build());
        ownerRepository.save(Owner.builder().name("Héctor Vargas").document("851683586").phone("652345569").email("hector.vargas@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Claudia Franco").document("962794697").phone("663456671").email("claudia.franco@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Gustavo Peña").document("173815719").phone("674567782").email("gustavo.pena@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Alba Domínguez").document("284926830").phone("685678893").email("alba.dominguez@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Mario Bravo").document("395137941").phone("696789914").email("mario.bravo@example.com").password("ejemplo123").build());

        ownerRepository.save(Owner.builder().name("Sara Villar").document("416248152").phone("607890125").email("sara.villar@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Emilio Fuentes").document("527359254").phone("618901236").email("emilio.fuentes@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Rocío Gallardo").document("638461365").phone("629012347").email("rocio.gallardo@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Tomás Escobar").document("749572476").phone("631234458").email("tomas.escobar@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Lidia Valdés").document("851683587").phone("642345569").email("lidia.valdes@example.com").password("ejemplo123").build());
        ownerRepository.save(Owner.builder().name("Bernardo Ríos").document("962794698").phone("653456671").email("bernardo.rios@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Sonia Acosta").document("173815720").phone("664567782").email("sonia.acosta@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Marcos Vidal").document("284926831").phone("675678893").email("marcos.vidal@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Bruno Márquez").document("395137942").phone("686789914").email("bruno.marquez@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Daniela Cortés").document("416248153").phone("697890125").email("daniela.cortes@example.com").password("ejemplo123").build());

        ownerRepository.save(Owner.builder().name("Félix Sandoval").document("527359255").phone("608901236").email("felix.sandoval@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Elvira Méndez").document("638461366").phone("619012347").email("elvira.mendez@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Iván Peña").document("749572477").phone("621234458").email("ivan.pena@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Sonia Gil").document("851683588").phone("632345569").email("sonia.gil@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Ramón Expósito").document("962794699").phone("643456671").email("ramon.exposito@example.com").password("ejemplo123").build());
        ownerRepository.save(Owner.builder().name("Carla Redondo").document("173815721").phone("654567782").email("carla.redondo@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Julio Navas").document("284926832").phone("665678893").email("julio.navas@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Marisol Vega").document("395137943").phone("676789914").email("marisol.vega@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Teo Camacho").document("416248154").phone("687890125").email("teo.camacho@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Julieta Pons").document("527359256").phone("698901236").email("julieta.pons@example.com").password("ejemplo123").build());

        ownerRepository.save(Owner.builder().name("Abel Salgado").document("638461367").phone("609012347").email("abel.salgado@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Estrella Cabrera").document("749572478").phone("611234458").email("estrella.cabrera@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Imanol Rivas").document("851683589").phone("622345569").email("imanol.rivas@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Aroa Sáez").document("962794700").phone("633456671").email("aroa.saez@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Germán Peralta").document("173815722").phone("644567782").email("german.peralta@example.com").password("ejemplo123").build());
        ownerRepository.save(Owner.builder().name("Celia Bermejo").document("284926833").phone("655678893").email("celia.bermejo@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Igor Sanz").document("395137944").phone("666789914").email("igor.sanz@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Leire Ortiz").document("416248155").phone("677890125").email("leire.ortiz@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Ramiro Estévez").document("527359257").phone("688901236").email("ramiro.estevez@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Yolanda Cuevas").document("638461368").phone("699901236").email("yolanda.cuevas@example.com").password("ejemplo123").build());

        ownerRepository.save(Owner.builder().name("Ismael Arias").document("749572479").phone("610234567").email("ismael.arias@example.com").password("password123").build());
        ownerRepository.save(Owner.builder().name("Aitor Lasa").document("851683590").phone("620345678").email("aitor.lasa@example.com").password("clave123").build());
        ownerRepository.save(Owner.builder().name("Berta Anaya").document("962794701").phone("630456789").email("berta.anaya@example.com").password("mascota123").build());
        ownerRepository.save(Owner.builder().name("Guillermo Fuster").document("173815723").phone("640567891").email("guillermo.fuster@example.com").password("pass1234").build());
        ownerRepository.save(Owner.builder().name("Rodrigo Alcántara").document("284926834").phone("650567892").email("rodrigo.alcantara@example.com").password("ejemplo123").build());

        // ====================================
        // ==== Cargar mascotas de ejemplo ====
        // ====================================
        petRepository.save(Pet.builder().name("Buddy").species(Species.DOG).breed("Golden Retriever").age(3).weight(15.0).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1633722715463-d30f4f325e24?fm=jpg&q=60&w=3000&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8cGVycm8lMjBnb2xkZW4lMjByZXRyaWV2ZXJ8ZW58MHx8MHx8fDA%3D").build());
        petRepository.save(Pet.builder().name("Whiskers").species(Species.CAT).breed("Persian").age(2).weight(4.5).disease("None").isActive(true).photoUrl("https://eu-central-1.graphassets.com/AnwjgMYRvQfWK3bRPjoq3z/resize=height:778,width:1080/output=format:webp/GftmE5Qtm0AtcNcRWRA1").build());
        petRepository.save(Pet.builder().name("Max").species(Species.DOG).breed("German Shepherd").age(4).weight(20.0).disease("None").isActive(true).photoUrl("https://ask.woodgreen.org.uk/media/pages/images/5979b7d0bc-1727379943/german-shepherd-900x900-crop-52-5-28-8.jpg").build());
        petRepository.save(Pet.builder().name("Luna").species(Species.CAT).breed("Siamese").age(1).weight(3.0).disease("None").isActive(true).photoUrl("https://assets.elanco.com/8e0bf1c2-1ae4-001f-9257-f2be3c683fb1/fca42f04-2474-4302-a238-990c8aebfe8c/Siamese_cat_1110x740.jpg").build());
        petRepository.save(Pet.builder().name("Charlie").species(Species.DOG).breed("Beagle").age(5).weight(10.0).disease("None").isActive(true).photoUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQRWp3zpN9nyTOC-i1UVNYwutRtjTHDpc40wIIE1BSUTn0kMqAk6ztLwffh&s=10").build());
        petRepository.save(Pet.builder().name("Rocky").species(Species.CAT).breed("Sphynx").age(6).weight(4.0).disease("None").photoUrl("https://images.unsplash.com/photo-1574158622682-e40e69881006?fm=jpg&q=60&w=3000&auto=format&fit=crop").isActive(false).build());

        petRepository.save(Pet.builder().name("Toby").species(Species.DOG).breed("Golden Retriever").age(3).weight(14.5).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=1").build());
        petRepository.save(Pet.builder().name("Thor").species(Species.DOG).breed("Pastor Alemán").age(5).weight(32.0).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=2").build());
        petRepository.save(Pet.builder().name("Kira").species(Species.CAT).breed("Bengalí").age(2).weight(3.8).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=1").build());
        petRepository.save(Pet.builder().name("Nina").species(Species.CAT).breed("Europeo Común").age(4).weight(4.2).disease("Conjuntivitis").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=2").build());
        petRepository.save(Pet.builder().name("Zeus").species(Species.DOG).breed("Dóberman").age(4).weight(35.0).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=3").build());
        petRepository.save(Pet.builder().name("Apolo").species(Species.DOG).breed("Labrador Retriever").age(2).weight(12.0).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=4").build());
        petRepository.save(Pet.builder().name("Misha").species(Species.CAT).breed("Persa").age(6).weight(5.1).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=3").build());
        petRepository.save(Pet.builder().name("Laia").species(Species.CAT).breed("Abisinio").age(3).weight(3.5).disease("Asma felino").isActive(false).photoUrl("https://loremflickr.com/500/400/kitten?lock=4").build());
        petRepository.save(Pet.builder().name("Rayo").species(Species.DOG).breed("Galgo Español").age(7).weight(27.5).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=5").build());
        petRepository.save(Pet.builder().name("Bruno").species(Species.DOG).breed("Boxer").age(3).weight(28.0).disease("Displasia de cadera").isActive(true).photoUrl("https://placedog.net/500/400?id=6").build());
        petRepository.save(Pet.builder().name("Mia").species(Species.CAT).breed("Siamés").age(1).weight(2.9).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=5").build());
        petRepository.save(Pet.builder().name("Nube").species(Species.CAT).breed("Angora Turco").age(5).weight(4.4).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=6").build());
        petRepository.save(Pet.builder().name("Diesel").species(Species.DOG).breed("Rottweiler").age(6).weight(41.0).disease("Artritis").isActive(true).photoUrl("https://placedog.net/500/400?id=7").build());
        petRepository.save(Pet.builder().name("Titán").species(Species.DOG).breed("Dogo Argentino").age(5).weight(38.5).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=8").build());
        petRepository.save(Pet.builder().name("India").species(Species.CAT).breed("Bosque de Noruega").age(4).weight(5.6).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=7").build());
        petRepository.save(Pet.builder().name("Nala").species(Species.CAT).breed("Maine Coon").age(3).weight(6.2).disease("None").isActive(false).photoUrl("https://loremflickr.com/500/400/kitten?lock=8").build());
        petRepository.save(Pet.builder().name("Duna").species(Species.DOG).breed("Mestizo").age(2).weight(9.8).disease("Leishmaniasis").isActive(true).photoUrl("https://placedog.net/500/400?id=9").build());
        petRepository.save(Pet.builder().name("Canela").species(Species.DOG).breed("Cocker Spaniel").age(8).weight(13.2).disease("Otitis externa").isActive(true).photoUrl("https://placedog.net/500/400?id=10").build());
        petRepository.save(Pet.builder().name("Gala").species(Species.CAT).breed("Ragdoll").age(2).weight(4.0).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=9").build());
        petRepository.save(Pet.builder().name("Frida").species(Species.CAT).breed("Azul Ruso").age(10).weight(5.0).disease("Leucemia felina").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=10").build());
        petRepository.save(Pet.builder().name("Rocco").species(Species.DOG).breed("Pastor Belga").age(4).weight(30.0).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=11").build());
        petRepository.save(Pet.builder().name("Nero").species(Species.DOG).breed("Schnauzer").age(9).weight(8.5).disease("Cataratas").isActive(true).photoUrl("https://placedog.net/500/400?id=12").build());
        petRepository.save(Pet.builder().name("Dalí").species(Species.CAT).breed("Europeo Común").age(10).weight(5.0).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=11").build());
        petRepository.save(Pet.builder().name("Gaudí").species(Species.CAT).breed("Siamés").age(4).weight(3.9).disease("Dermatitis alérgica").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=12").build());
        petRepository.save(Pet.builder().name("Rudy").species(Species.DOG).breed("Beagle").age(5).weight(11.0).disease("Tos de las perreras").isActive(true).photoUrl("https://placedog.net/500/400?id=13").build());
        petRepository.save(Pet.builder().name("Cometa").species(Species.DOG).breed("Border Collie").age(3).weight(16.5).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=14").build());
        petRepository.save(Pet.builder().name("Tomasa").species(Species.CAT).breed("Europeo Común").age(12).weight(4.6).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=13").build());
        petRepository.save(Pet.builder().name("Matilda").species(Species.CAT).breed("Persa").age(7).weight(4.8).disease("Estreñimiento").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=14").build());
        petRepository.save(Pet.builder().name("Simba").species(Species.DOG).breed("Shar Pei").age(2).weight(19.5).disease("Obesidad").isActive(true).photoUrl("https://placedog.net/500/400?id=15").build());
        petRepository.save(Pet.builder().name("Otto").species(Species.DOG).breed("Teckel").age(6).weight(8.2).disease("Hernia discal").isActive(true).photoUrl("https://placedog.net/500/400?id=16").build());
        petRepository.save(Pet.builder().name("Cleo").species(Species.CAT).breed("Bengalí").age(3).weight(4.1).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=15").build());
        petRepository.save(Pet.builder().name("Casimira").species(Species.CAT).breed("Himalayo").age(8).weight(4.4).disease("Panleucopenia felina").isActive(false).photoUrl("https://loremflickr.com/500/400/kitten?lock=16").build());
        petRepository.save(Pet.builder().name("Firulais").species(Species.DOG).breed("Mestizo").age(4).weight(12.4).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=17").build());
        petRepository.save(Pet.builder().name("Iker").species(Species.DOG).breed("Husky Siberiano").age(3).weight(24.0).disease("Otitis externa").isActive(false).photoUrl("https://placedog.net/500/400?id=18").build());
        petRepository.save(Pet.builder().name("Aisha").species(Species.CAT).breed("Abisinio").age(2).weight(3.2).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=17").build());
        petRepository.save(Pet.builder().name("Mora").species(Species.CAT).breed("Europeo Común").age(5).weight(3.9).disease("Conjuntivitis").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=18").build());
        petRepository.save(Pet.builder().name("Balto").species(Species.DOG).breed("Akita Inu").age(7).weight(33.0).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=19").build());
        petRepository.save(Pet.builder().name("Trueno").species(Species.DOG).breed("Pastor Alemán").age(4).weight(29.5).disease("Displasia de cadera").isActive(true).photoUrl("https://placedog.net/500/400?id=20").build());
        petRepository.save(Pet.builder().name("Miel").species(Species.CAT).breed("Persa").age(4).weight(4.3).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=19").build());
        petRepository.save(Pet.builder().name("Azúcar").species(Species.CAT).breed("Siamés").age(6).weight(3.7).disease("Asma felino").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=20").build());
        petRepository.save(Pet.builder().name("Lobo").species(Species.DOG).breed("Mestizo").age(11).weight(34.2).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=21").build());
        petRepository.save(Pet.builder().name("Trufa").species(Species.DOG).breed("Caniche").age(9).weight(6.5).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=22").build());
        petRepository.save(Pet.builder().name("Vainilla").species(Species.CAT).breed("Angora Turco").age(5).weight(4.5).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=21").build());
        petRepository.save(Pet.builder().name("Sol").species(Species.CAT).breed("Europeo Común").age(3).weight(3.3).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=22").build());
        petRepository.save(Pet.builder().name("Chispa").species(Species.DOG).breed("Carlino").age(4).weight(7.8).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=23").build());
        petRepository.save(Pet.builder().name("Perla").species(Species.DOG).breed("Bichón Frisé").age(6).weight(5.4).disease("Dermatitis alérgica").isActive(true).photoUrl("https://placedog.net/500/400?id=24").build());
        petRepository.save(Pet.builder().name("Cielo").species(Species.CAT).breed("Bosque de Noruega").age(6).weight(5.4).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=23").build());
        petRepository.save(Pet.builder().name("Pandora").species(Species.CAT).breed("Persa").age(3).weight(4.2).disease("Urolitiasis (piedras en la vejiga)").isActive(false).photoUrl("https://loremflickr.com/500/400/kitten?lock=24").build());
        petRepository.save(Pet.builder().name("Anubis").species(Species.DOG).breed("Mastín Español").age(5).weight(42.0).disease("Leishmaniasis").isActive(true).photoUrl("https://placedog.net/500/400?id=25").build());
        petRepository.save(Pet.builder().name("Halcón").species(Species.DOG).breed("Pointer").age(6).weight(26.0).disease("Tos de las perreras").isActive(true).photoUrl("https://placedog.net/500/400?id=26").build());
        petRepository.save(Pet.builder().name("Vega").species(Species.CAT).breed("Azul Ruso").age(7).weight(4.6).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=25").build());
        petRepository.save(Pet.builder().name("Altair").species(Species.CAT).breed("Europeo Común").age(2).weight(3.4).disease("Dermatofitosis (tiña)").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=26").build());
        petRepository.save(Pet.builder().name("Fénix").species(Species.DOG).breed("Setter Irlandés").age(3).weight(27.0).disease("Parvovirus").isActive(false).photoUrl("https://placedog.net/500/400?id=27").build());
        petRepository.save(Pet.builder().name("Atlas").species(Species.DOG).breed("Gran Danés").age(4).weight(50.0).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=28").build());
        petRepository.save(Pet.builder().name("Dakar").species(Species.CAT).breed("Ragdoll").age(4).weight(5.5).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=27").build());
        petRepository.save(Pet.builder().name("Kenya").species(Species.CAT).breed("Bengalí").age(1).weight(2.4).disease("None").isActive(false).photoUrl("https://loremflickr.com/500/400/kitten?lock=28").build());

        petRepository.save(Pet.builder().name("Neptuno").species(Species.DOG).breed("San Bernardo").age(8).weight(45.0).disease("Moquillo canino").isActive(true).photoUrl("https://placedog.net/500/400?id=29").build());
        petRepository.save(Pet.builder().name("Sancho").species(Species.DOG).breed("Podenco Andaluz").age(6).weight(20.0).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=30").build());
        petRepository.save(Pet.builder().name("Kiara").species(Species.CAT).breed("Maine Coon").age(5).weight(6.0).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=29").build());
        petRepository.save(Pet.builder().name("Dakota").species(Species.CAT).breed("Europeo Común").age(9).weight(4.9).disease("Conjuntivitis").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=30").build());
        petRepository.save(Pet.builder().name("Mordisco").species(Species.DOG).breed("Bulldog Francés").age(4).weight(12.0).disease("Obesidad").isActive(true).photoUrl("https://placedog.net/500/400?id=31").build());
        petRepository.save(Pet.builder().name("Colmillo").species(Species.DOG).breed("Pastor Belga").age(5).weight(28.5).disease("Hipotiroidismo").isActive(true).photoUrl("https://placedog.net/500/400?id=32").build());
        petRepository.save(Pet.builder().name("Mamba").species(Species.CAT).breed("Esfinge").age(3).weight(4.8).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=31").build());
        petRepository.save(Pet.builder().name("Rumba").species(Species.CAT).breed("Munchkin").age(2).weight(3.1).disease("Obesidad").isActive(false).photoUrl("https://loremflickr.com/500/400/kitten?lock=32").build());
        petRepository.save(Pet.builder().name("Garra").species(Species.DOG).breed("Dóberman").age(5).weight(34.0).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=33").build());
        petRepository.save(Pet.builder().name("Tigre").species(Species.DOG).breed("Mestizo").age(7).weight(22.0).disease("Otitis externa").isActive(true).photoUrl("https://placedog.net/500/400?id=34").build());
        petRepository.save(Pet.builder().name("Ronda").species(Species.CAT).breed("Europeo Común").age(14).weight(5.2).disease("None").isActive(false).photoUrl("https://loremflickr.com/500/400/kitten?lock=33").build());
        petRepository.save(Pet.builder().name("Neo").species(Species.CAT).breed("Persa").age(11).weight(4.6).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=34").build());
        petRepository.save(Pet.builder().name("Osito").species(Species.DOG).breed("Chihuahua").age(3).weight(2.6).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=35").build());
        petRepository.save(Pet.builder().name("Oso").species(Species.DOG).breed("San Bernardo").age(9).weight(52.0).disease("Displasia de cadera").isActive(true).photoUrl("https://placedog.net/500/400?id=36").build());
        petRepository.save(Pet.builder().name("Eros").species(Species.CAT).breed("Angora Turco").age(8).weight(4.0).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=35").build());
        petRepository.save(Pet.builder().name("Noa").species(Species.CAT).breed("Europeo Común").age(6).weight(4.4).disease("Alergia alimentaria").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=36").build());
        petRepository.save(Pet.builder().name("Lupa").species(Species.DOG).breed("Braco Alemán").age(5).weight(27.0).disease("Artritis").isActive(false).photoUrl("https://placedog.net/500/400?id=37").build());
        petRepository.save(Pet.builder().name("Loba").species(Species.DOG).breed("Mestizo").age(9).weight(30.4).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=38").build());
        petRepository.save(Pet.builder().name("Lua").species(Species.CAT).breed("Maine Coon").age(10).weight(6.4).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=37").build());
        petRepository.save(Pet.builder().name("Lila").species(Species.CAT).breed("Persa").age(13).weight(4.1).disease("Cataratas").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=38").build());
        petRepository.save(Pet.builder().name("Quijote").species(Species.DOG).breed("Pointer").age(6).weight(28.0).disease("Tos de las perreras").isActive(true).photoUrl("https://placedog.net/500/400?id=39").build());
        petRepository.save(Pet.builder().name("Rocinante").species(Species.DOG).breed("Setter Irlandés").age(4).weight(26.5).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=40").build());
        petRepository.save(Pet.builder().name("Bigotes").species(Species.CAT).breed("Siamés").age(9).weight(3.6).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=39").build());
        petRepository.save(Pet.builder().name("Botas").species(Species.CAT).breed("Europeo Común").age(15).weight(4.7).disease("None").isActive(false).photoUrl("https://loremflickr.com/500/400/kitten?lock=40").build());
        petRepository.save(Pet.builder().name("Chaplin").species(Species.DOG).breed("Bulldog Francés").age(8).weight(13.5).disease("Leishmaniasis").isActive(true).photoUrl("https://placedog.net/500/400?id=41").build());
        petRepository.save(Pet.builder().name("Mordelón").species(Species.DOG).breed("Mestizo").age(5).weight(18.0).disease("Intoxicación alimentaria").isActive(true).photoUrl("https://placedog.net/500/400?id=42").build());
        petRepository.save(Pet.builder().name("Calcetines").species(Species.CAT).breed("Persa").age(10).weight(4.5).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=41").build());
        petRepository.save(Pet.builder().name("Toño").species(Species.CAT).breed("Abisinio").age(6).weight(3.4).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=42").build());
        petRepository.save(Pet.builder().name("Duke").species(Species.DOG).breed("Golden Retriever").age(10).weight(30.0).disease("None").isActive(false).photoUrl("https://placedog.net/500/400?id=43").build());
        petRepository.save(Pet.builder().name("Nico").species(Species.DOG).breed("Mestizo").age(3).weight(10.5).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=44").build());
        petRepository.save(Pet.builder().name("Pepe").species(Species.CAT).breed("Europeo Común").age(8).weight(4.0).disease("Infección del tracto urinario").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=43").build());
        petRepository.save(Pet.builder().name("Chata").species(Species.CAT).breed("Angora Turco").age(12).weight(4.7).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=44").build());
        petRepository.save(Pet.builder().name("Atila").species(Species.DOG).breed("Dogo Argentino").age(6).weight(40.0).disease("Moquillo canino").isActive(true).photoUrl("https://placedog.net/500/400?id=45").build());
        petRepository.save(Pet.builder().name("Sultán").species(Species.DOG).breed("Malamute de Alaska").age(5).weight(36.5).disease("Displasia de cadera").isActive(true).photoUrl("https://placedog.net/500/400?id=46").build());
        petRepository.save(Pet.builder().name("Rosita").species(Species.CAT).breed("Persa").age(4).weight(3.8).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=45").build());
        petRepository.save(Pet.builder().name("Manchas").species(Species.CAT).breed("Abisinio").age(2).weight(3.1).disease("None").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=46").build());
        petRepository.save(Pet.builder().name("Pirata").species(Species.DOG).breed("Mestizo").age(10).weight(21.5).disease("None").isActive(true).photoUrl("https://placedog.net/500/400?id=47").build());
        petRepository.save(Pet.builder().name("Guapo").species(Species.DOG).breed("Chihuahua").age(6).weight(3.5).disease("Hipotiroidismo").isActive(true).photoUrl("https://placedog.net/500/400?id=48").build());
        petRepository.save(Pet.builder().name("Mota").species(Species.CAT).breed("Bengalí").age(3).weight(4.3).disease("Conjuntivitis").isActive(true).photoUrl("https://loremflickr.com/500/400/kitten?lock=47").build());
        petRepository.save(Pet.builder().name("Copo").species(Species.CAT).breed("Europeo Común").age(7).weight(5.2).disease("None").isActive(false).photoUrl("https://loremflickr.com/500/400/kitten?lock=48").build());
        petRepository.save(Pet.builder().name("Churro").species(Species.DOG).breed("Bulldog Francés").age(5).weight(11.8).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1552053831-71594a27632d?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Bombón").species(Species.DOG).breed("Caniche").age(7).weight(6.9).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Melocotón").species(Species.CAT).breed("Maine Coon").age(6).weight(6.1).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Mandarina").species(Species.CAT).breed("Europeo Común").age(11).weight(4.8).disease("None").isActive(false).photoUrl("https://images.unsplash.com/photo-1518791841217-8f162f1e1131?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Caramelo").species(Species.DOG).breed("Labrador Retriever").age(4).weight(15.2).disease("Artritis").isActive(true).photoUrl("https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Turrón").species(Species.DOG).breed("Chihuahua").age(2).weight(2.8).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1518717758536-85ae29035b6d?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Cereza").species(Species.CAT).breed("Siamés").age(5).weight(3.6).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Garbancito").species(Species.CAT).breed("Europeo Común").age(2).weight(2.7).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1543852786-1cf6624b9987?w=800&q=60&auto=format&fit=crop").build());

        petRepository.save(Pet.builder().name("Galleta").species(Species.DOG).breed("Beagle").age(9).weight(12.0).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1561037404-61cd46aa615b?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Migas").species(Species.DOG).breed("Mestizo").age(14).weight(17.3).disease("Artritis").isActive(false).photoUrl("https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Nuez").species(Species.CAT).breed("Ragdoll").age(3).weight(4.6).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1519052537078-e6302a4968d4?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Aitana").species(Species.CAT).breed("Persa").age(6).weight(4.4).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1495360010541-f48722b34f7d?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Fideo").species(Species.DOG).breed("Teckel").age(7).weight(7.4).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1537151625747-768eb6cf92b2?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Bolita").species(Species.DOG).breed("Carlino").age(8).weight(8.9).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1477884213360-7e9d7dcc1e48?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Felina").species(Species.CAT).breed("Europeo Común").age(16).weight(4.0).disease("Insuficiencia renal crónica").isActive(false).photoUrl("https://images.unsplash.com/photo-1592194996308-7b43878e84a6?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Mostaza").species(Species.CAT).breed("Bengalí").age(4).weight(4.5).disease("None").isActive(true).photoUrl("https://eu-central-1.graphassets.com/AnwjgMYRvQfWK3bRPjoq3z/resize=height:778,width:1080/output=format:webp/GftmE5Qtm0AtcNcRWRA1").build());

        petRepository.save(Pet.builder().name("Rex").species(Species.DOG).breed("Pastor Alemán").age(8).weight(35.5).disease("Leishmaniasis").isActive(true).photoUrl("https://images.unsplash.com/photo-1561037404-61cd46aa615b?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Keko").species(Species.DOG).breed("Golden Retriever").age(5).weight(17.0).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Pimienta").species(Species.CAT).breed("Esfinge").age(3).weight(5.0).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1519052537078-e6302a4968d4?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Rubia").species(Species.CAT).breed("Europeo Común").age(5).weight(4.2).disease("Leucemia felina").isActive(true).photoUrl("https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Ringo").species(Species.DOG).breed("Border Collie").age(6).weight(19.0).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1537151625747-768eb6cf92b2?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Turbo").species(Species.DOG).breed("Husky Siberiano").age(3).weight(21.5).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1477884213360-7e9d7dcc1e48?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Lucero").species(Species.CAT).breed("Persa").age(9).weight(4.1).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1518791841217-8f162f1e1131?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Capitana").species(Species.CAT).breed("Europeo Común").age(6).weight(4.5).disease("Obesidad").isActive(true).photoUrl("https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Yako").species(Species.DOG).breed("Boxer").age(9).weight(30.0).disease("Tos de las perreras").isActive(true).photoUrl("https://images.unsplash.com/photo-1552053831-71594a27632d?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Chiquitín").species(Species.DOG).breed("Chihuahua").age(4).weight(3.2).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Pereza").species(Species.CAT).breed("Angora Turco").age(7).weight(4.6).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1543852786-1cf6624b9987?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Tinieblas").species(Species.CAT).breed("Europeo Común").age(8).weight(4.3).disease("Dermatofitosis (tiña)").isActive(true).photoUrl("https://images.unsplash.com/photo-1519052537078-e6302a4968d4?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Milú").species(Species.DOG).breed("Caniche").age(6).weight(5.6).disease("None").isActive(false).photoUrl("https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Barrabás").species(Species.DOG).breed("Dóberman").age(7).weight(38.0).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1518717758536-85ae29035b6d?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Orejas").species(Species.CAT).breed("Abisinio").age(4).weight(3.6).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Coliflor").species(Species.CAT).breed("Persa").age(4).weight(4.7).disease("None").isActive(false).photoUrl("https://images.unsplash.com/photo-1543852786-1cf6624b9987?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Trasto").species(Species.DOG).breed("Mestizo").age(11).weight(20.5).disease("Moquillo canino").isActive(true).photoUrl("https://images.unsplash.com/photo-1552053831-71594a27632d?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Pacha").species(Species.DOG).breed("Shiba Inu").age(3).weight(9.5).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Albahaca").species(Species.CAT).breed("Europeo Común").age(2).weight(2.6).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1519052537078-e6302a4968d4?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Romero").species(Species.CAT).breed("Bengalí").age(5).weight(4.7).disease("Conjuntivitis").isActive(true).photoUrl("https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Karma").species(Species.DOG).breed("Shar Pei").age(4).weight(20.0).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1537151625747-768eb6cf92b2?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Grumo").species(Species.DOG).breed("Podenco Andaluz").age(8).weight(22.5).disease("Displasia de cadera").isActive(false).photoUrl("https://images.unsplash.com/photo-1477884213360-7e9d7dcc1e48?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Salvia").species(Species.CAT).breed("Angora Turco").age(10).weight(4.3).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Ámbar").species(Species.CAT).breed("Europeo Común").age(3).weight(3.8).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1543852786-1cf6624b9987?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Trozo").species(Species.DOG).breed("Mestizo").age(12).weight(24.0).disease("Otitis externa").isActive(true).photoUrl("https://images.unsplash.com/photo-1560807707-8cc77767d783?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Brownie").species(Species.DOG).breed("Labrador Retriever").age(6).weight(28.0).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1591160690555-5debfba289f0?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Topacio").species(Species.CAT).breed("Europeo Común").age(3).weight(3.8).disease("None").isActive(true).photoUrl("https://images.unsplash.com/photo-1518791841217-8f162f1e1131?w=800&q=60&auto=format&fit=crop").build());
        petRepository.save(Pet.builder().name("Esmeralda").species(Species.CAT).breed("Siamés").age(7).weight(3.9).disease("Infección del tracto urinario").isActive(true).photoUrl("https://eu-central-1.graphassets.com/AnwjgMYRvQfWK3bRPjoq3z/resize=height:778,width:1080/output=format:webp/GftmE5Qtm0AtcNcRWRA1").build());
        petRepository.save(Pet.builder().name("Oreo").species(Species.DOG).breed("Bichón Frisé").age(5).weight(6.2).disease("None").isActive(true).photoUrl("https://ask.woodgreen.org.uk/media/pages/images/5979b7d0bc-1727379943/german-shepherd-900x900-crop-52-5-28-8.jpg").build());
        petRepository.save(Pet.builder().name("Canelo").species(Species.DOG).breed("Galgo Español").age(10).weight(29.0).disease("Artritis").isActive(false).photoUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQRWp3zpN9nyTOC-i1UVNYwutRtjTHDpc40wIIE1BSUTn0kMqAk6ztLwffh&s=10").build());
        petRepository.save(Pet.builder().name("Zafiro").species(Species.CAT).breed("Europeo Común").age(9).weight(4.6).disease("None").isActive(true).photoUrl("https://assets.elanco.com/8e0bf1c2-1ae4-001f-9257-f2be3c683fb1/fca42f04-2474-4302-a238-990c8aebfe8c/Siamese_cat_1110x740.jpg").build());
        petRepository.save(Pet.builder().name("Jade").species(Species.CAT).breed("Persa").age(12).weight(4.4).disease("None").isActive(false).photoUrl("https://images.unsplash.com/photo-1495360010541-f48722b34f7d?w=800&q=60&auto=format&fit=crop").build());

        // ===========================================
        // ==== Asignar mascotas a dueños al azar ====
        // ===========================================

        int ownerCount = (int) ownerRepository.count();
        for (Pet pet : petRepository.findAll()) {
            Owner owner = ownerRepository.findById((long) (random.nextInt(ownerCount) + 1)).orElseThrow();
            pet.setOwner(owner);
            petRepository.save(pet);
        }
    }

}

