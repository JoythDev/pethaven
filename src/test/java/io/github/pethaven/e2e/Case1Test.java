package io.github.pethaven.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Caso 1: un veterinario ingresa (falla una vez), registra un dueño (se equivoca una vez) y su
 * mascota, y el dueño entra al portal de clientes a ver los datos de la mascota.
 *
 * Requisito: el frontend debe estar corriendo con ng serve en el puerto 4200
 * (el API de la prueba se levanta en el puerto 8080 del perfil "test").
 * El login del dueño usa correo, no cédula: es lo que pide el sistema.
 */
@Tag("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class Case1Test {

    private static final String FRONT = "http://localhost:4200";
    private static final String VET_EMAIL = DatabaseInit.VET_EMAIL;
    private static final String VET_PASSWORD = DatabaseInit.VET_PASSWORD;
    private static final String OWNER_DOCUMENT = "1020304050";
    private static final String OWNER_EMAIL = "carlos.perez@example.com";
    private static final String OWNER_PASSWORD = "clave12345";
    private static final String PET_NAME = "Firulais";
    private static final String PET_BREED = "Labrador";

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-extensions");
        // El aviso de contraseñas filtradas de Chrome roba el foco del teclado tras el login.
        options.addArguments("--disable-features=PasswordLeakDetection,PasswordManagerOnboarding");
        options.setExperimentalOption("prefs", java.util.Map.of("credentials_enable_service", false, "profile.password_manager_enabled", false, "profile.password_manager_leak_detection", false));
        // Sin pantalla (CI): -De2e.headless=true. Se fija el tamaño para no caer en el diseño móvil.
        if (Boolean.getBoolean("e2e.headless")) {
            options.addArguments("--headless=new", "--window-size=1920,1080");
        }
        this.driver = new ChromeDriver(options);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    }

    @AfterEach
    public void tearDown() {
        if (this.driver != null) {
            this.driver.quit();
        }
    }

    @Test
    public void testCase1() {
        // 1. Login del veterinario: primero con contraseña errada, luego correcta.
        driver.get(FRONT + "/vet/login");
        login(VET_EMAIL, "incorrecta");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("loginError")));
        assertThat(driver.getCurrentUrl()).endsWith("/vet/login");

        clearAndType(By.id("password"), VET_PASSWORD);
        driver.findElement(By.id("btnLogin")).click();
        wait.until(ExpectedConditions.urlContains("/veterinario"));

        // 2. Registro del cliente: primero con un correo mal escrito, luego corregido.
        driver.get(FRONT + "/veterinario/duenos/add");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("name")));
        driver.findElement(By.id("name")).sendKeys("Carlos Pérez");
        driver.findElement(By.id("document")).sendKeys(OWNER_DOCUMENT);
        driver.findElement(By.id("phone")).sendKeys("3001234567");
        driver.findElement(By.id("email")).sendKeys("correo-invalido");
        driver.findElement(By.id("password")).sendKeys(OWNER_PASSWORD);
        driver.findElement(By.id("btnGuardar")).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("emailError")));
        assertThat(driver.getCurrentUrl()).endsWith("/duenos/add");

        clearAndType(By.id("email"), OWNER_EMAIL);
        driver.findElement(By.id("btnGuardar")).click();
        wait.until(d -> d.findElements(By.className("owner-document")).stream()
                .anyMatch(td -> td.getText().equals(OWNER_DOCUMENT)));

        // 3. Registro de la mascota asociada al dueño.
        driver.get(FRONT + "/veterinario/mascotas/add");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("name")));
        driver.findElement(By.id("name")).sendKeys(PET_NAME);
        driver.findElement(By.id("breed")).sendKeys(PET_BREED);
        clearAndType(By.id("age"), "3");
        driver.findElement(By.id("weight")).sendKeys("12");
        driver.findElement(By.id("photoUrl")).sendKeys("https://example.com/firulais.jpg");
        driver.findElement(By.id("owner")).click();
        // Hay más de un dueño: se elige el recién creado por su documento.
        wait.until(d -> findOwnerOption() != null);
        findOwnerOption().click();
        driver.findElement(By.id("btnGuardar")).click();
        wait.until(ExpectedConditions.urlMatches(".*/veterinario/mascotas/[0-9]+$"));

        // 4. El dueño ingresa al portal de clientes y ve su mascota.
        driver.get(FRONT + "/login");
        login(OWNER_EMAIL, OWNER_PASSWORD);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("pet-name")));

        List<WebElement> names = driver.findElements(By.className("pet-name"));
        assertThat(names).hasSize(1);
        assertThat(names.get(0).getText()).isEqualTo(PET_NAME);
        assertThat(driver.findElement(By.className("pet-info")).getText()).contains(PET_BREED);
    }

    private WebElement findOwnerOption() {
        return driver.findElements(By.className("p-select-option")).stream()
                .filter(o -> o.getText().contains(OWNER_DOCUMENT)).findFirst().orElse(null);
    }

    private void login(String email, String password) {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("email")));
        clearAndType(By.id("email"), email);
        clearAndType(By.id("password"), password);
        driver.findElement(By.id("btnLogin")).click();
    }

    // clear() no siempre dispara el evento de Angular; CTRL+A y escribir encima sí.
    private void clearAndType(By locator, String text) {
        WebElement input = driver.findElement(locator);
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), text);
    }
}
