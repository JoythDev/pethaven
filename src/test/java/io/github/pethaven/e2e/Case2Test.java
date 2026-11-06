package io.github.pethaven.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.time.Duration;
import java.util.Map;
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
 * Caso 2: el veterinario busca una mascota y le registra un tratamiento con un medicamento;
 * vuelve a buscarla para verificar el historial, y el administrador revisa las unidades
 * suministradas y las ganancias en el dashboard.
 *
 * Requisito: el frontend debe estar corriendo con {@code ng serve} en el puerto 4200.
 */
@Tag("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class Case2Test {

    private static final String FRONT = "http://localhost:4200";
    private static final String DIAGNOSIS = "Infección respiratoria";
    private static final int UNITS = 3;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-extensions");
        // El aviso de contraseñas filtradas de Chrome roba el foco del teclado tras el login.
        options.addArguments("--disable-features=PasswordLeakDetection,PasswordManagerOnboarding");
        options.setExperimentalOption("prefs", Map.of("credentials_enable_service", false,
                "profile.password_manager_enabled", false, "profile.password_manager_leak_detection", false));
        // options.addArguments("--headless");
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
    public void testCase2() {
        // 1. Login del veterinario.
        driver.get(FRONT + "/vet/login");
        login(DatabaseInit.VET_EMAIL, DatabaseInit.VET_PASSWORD);
        wait.until(ExpectedConditions.urlContains("/veterinario"));

        // 2. Buscar la mascota, abrir su ficha y registrar el tratamiento.
        openPetDetail();
        wait.until(ExpectedConditions.elementToBeClickable(By.id("btnRegistrarTratamiento")));
        driver.findElement(By.id("btnRegistrarTratamiento")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("diagnosis")));
        clearAndType(By.id("weight"), "10");
        driver.findElement(By.id("diagnosis")).sendKeys(DIAGNOSIS);
        driver.findElement(By.id("drug-0")).click();
        wait.until(d -> findOption(DatabaseInit.DRUG_NAME) != null);
        findOption(DatabaseInit.DRUG_NAME).click();
        clearAndType(By.id("units-0"), String.valueOf(UNITS));
        driver.findElement(By.id("btnGuardar")).click();
        wait.until(ExpectedConditions.urlMatches(".*/veterinario/mascotas/[0-9]+$"));

        // 3. Buscar de nuevo la mascota y verificar el tratamiento en su historial.
        openPetDetail();
        wait.until(d -> d.findElements(By.className("treatment-diagnosis")).size() == 1);
        assertThat(driver.findElement(By.className("treatment-diagnosis")).getText()).isEqualTo(DIAGNOSIS);
        String drugLine = driver.findElement(By.className("treatment-drug")).getText();
        assertThat(drugLine).contains(DatabaseInit.DRUG_NAME).contains(UNITS + " unidades");

        // 4. El administrador verifica unidades suministradas, ventas y utilidad.
        driver.get(FRONT + "/admin/login");
        login(DatabaseInit.ADMIN_EMAIL, DatabaseInit.ADMIN_PASSWORD);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("totalSales")));

        assertThat(digits(By.id("totalSales"))).isEqualTo(String.valueOf(UNITS * DatabaseInit.DRUG_SALE_PRICE));
        assertThat(digits(By.id("grossProfit")))
                .isEqualTo(String.valueOf(UNITS * (DatabaseInit.DRUG_SALE_PRICE - DatabaseInit.DRUG_PURCHASE_PRICE)));
        wait.until(d -> d.findElements(By.className("top-med-name")).size() == 1);
        assertThat(driver.findElement(By.className("top-med-name")).getText()).isEqualTo(DatabaseInit.DRUG_NAME);
        assertThat(digits(By.className("top-med-units"))).isEqualTo(String.valueOf(UNITS));
    }

    // Lista de mascotas -> búsqueda por nombre -> ficha de la única coincidencia.
    private void openPetDetail() {
        driver.get(FRONT + "/veterinario/mascotas");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("searchPets")));
        driver.findElement(By.id("searchPets")).sendKeys(DatabaseInit.PET_NAME);
        wait.until(d -> d.findElements(By.className("pet-row-name")).size() == 1);
        driver.findElement(By.className("btn-detail")).click();
        wait.until(ExpectedConditions.urlMatches(".*/veterinario/mascotas/[0-9]+$"));
    }

    private WebElement findOption(String text) {
        return driver.findElements(By.className("p-select-option")).stream()
                .filter(o -> o.getText().contains(text)).findFirst().orElse(null);
    }

    // Quita símbolo y separadores de miles del formato de moneda/cantidad.
    private String digits(By locator) {
        return driver.findElement(locator).getText().replaceAll("[^0-9]", "");
    }

    private void login(String email, String password) {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("email")));
        clearAndType(By.id("email"), email);
        clearAndType(By.id("password"), password);
        driver.findElement(By.id("btnLogin")).click();
    }

    private void clearAndType(By locator, String text) {
        driver.findElement(locator).sendKeys(Keys.chord(Keys.CONTROL, "a"), text);
    }
}
