package io.github.pethaven.exception;

import io.github.pethaven.support.WebMvcSecurityTestSupport;

import com.jayway.jsonpath.JsonPath;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// Manejadores que ningún endpoint de negocio alcanza: un controlador de prueba lanza cada excepción.
@WebMvcTest(controllers = GlobalExceptionHandlerTest.ThrowingController.class)
@WebMvcSecurityTestSupport
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTest.ThrowingController.class)
public class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc;

    @Autowired
    public GlobalExceptionHandlerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    // ---------- handleMaxUploadSizeExceeded ----------

    @Test
    public void globalExceptionHandler_handleMaxUploadSizeExceeded_ThrownDirectly_Returns413WithMessage() throws Exception {
        // Act + Assert
        // Excepción lanzada directamente: el manejador la traduce a 413.
        mockMvc.perform(MockMvcRequestBuilders.post("/test/errors/max-upload"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.PAYLOAD_TOO_LARGE.value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(HttpStatus.PAYLOAD_TOO_LARGE.value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("El archivo supera el tamaño máximo permitido (10 MB)."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/test/errors/max-upload"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors").isEmpty());
    }

    // ---------- handleMultipart ----------

    @Test
    public void globalExceptionHandler_handleMultipart_CauseIsMaxUploadSize_Returns413WithSameMessage() throws Exception {
        // Act + Assert
        // La causa es un exceso de tamaño: responde igual que MaxUploadSizeExceededException.
        mockMvc.perform(MockMvcRequestBuilders.post("/test/errors/multipart/max-upload-cause"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.PAYLOAD_TOO_LARGE.value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(HttpStatus.PAYLOAD_TOO_LARGE.value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("El archivo supera el tamaño máximo permitido (10 MB)."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/test/errors/multipart/max-upload-cause"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors").isEmpty());
    }

    @Test
    public void globalExceptionHandler_handleMultipart_WithoutCause_Returns400() throws Exception {
        // Act + Assert
        // Sin causa de exceso de tamaño: petición multipart mal formada.
        mockMvc.perform(MockMvcRequestBuilders.post("/test/errors/multipart/no-cause"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("La petición multipart es inválida o está mal formada."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/test/errors/multipart/no-cause"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors").isEmpty());
    }

    @Test
    public void globalExceptionHandler_handleMultipart_CauseIsOtherException_Returns400() throws Exception {
        // Act + Assert
        // Otra causa distinta de un exceso de tamaño también responde 400.
        mockMvc.perform(MockMvcRequestBuilders.post("/test/errors/multipart/other-cause"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("La petición multipart es inválida o está mal formada."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/test/errors/multipart/other-cause"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors").isEmpty());
    }

    // ---------- handleHandlerMethodValidation ----------

    @Test
    public void globalExceptionHandler_handleHandlerMethodValidation_SingleConstraint_Returns400WithParameterKey() throws Exception {
        // Act + Assert
        // Spring valida el parámetro sin @Validated; la clave de errors es el nombre del parámetro.
        mockMvc.perform(MockMvcRequestBuilders.get("/test/errors/validated-parameter").param("size", "0"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("La petición tiene parámetros inválidos."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/test/errors/validated-parameter"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.size").value("El tamaño mínimo es 1."));
    }

    @Test
    public void globalExceptionHandler_handleHandlerMethodValidation_TwoConstraintsOnParameter_JoinsMessagesWithComma() throws Exception {
        // Act
        // Dos restricciones sobre el mismo parámetro: ambas violaciones llegan con el mismo nombre de clave.
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/test/errors/validated-code").param("code", ""))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("La petición tiene parámetros inválidos."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/test/errors/validated-code"))
                .andReturn();

        // Assert
        // El orden de las violaciones no está garantizado: se comparan ambos mensajes sin importar el orden.
        String errorMessage = JsonPath.read(result.getResponse().getContentAsString(), "$.errors.code");
        Assertions.assertThat(errorMessage.split(", "))
                .containsExactlyInAnyOrder("Código obligatorio", "Código demasiado corto");
    }

    // ---------- handleConstraintViolation ----------

    @Test
    public void globalExceptionHandler_handleConstraintViolation_NestedPath_KeysByLastSegment() throws Exception {
        // Act + Assert
        // La ruta completa es order.address.street: la clave es solo el último segmento.
        mockMvc.perform(MockMvcRequestBuilders.get("/test/errors/constraint/nested"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("La petición tiene parámetros inválidos."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/test/errors/constraint/nested"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.street").value("La calle es obligatoria."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors['order.address.street']").doesNotExist());
    }

    @Test
    public void globalExceptionHandler_handleConstraintViolation_PathWithoutDots_KeepsPropertyName() throws Exception {
        // Act + Assert
        // Una ruta sin puntos se usa tal cual.
        mockMvc.perform(MockMvcRequestBuilders.get("/test/errors/constraint/top-level"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("La petición tiene parámetros inválidos."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/test/errors/constraint/top-level"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.street").value("La calle es obligatoria."));
    }

    // ---------- handleNoResourceFound ----------

    @Test
    public void globalExceptionHandler_handleNoResourceFound_UnmappedPath_Returns404() throws Exception {
        // Act + Assert
        // Ningún endpoint coincide: el manejo de recursos estáticos lanza NoResourceFoundException.
        mockMvc.perform(MockMvcRequestBuilders.get("/test/errors/does-not-exist"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(404))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("El recurso solicitado no existe."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/test/errors/does-not-exist"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors").isEmpty());
    }

    // ---------- handleResponseStatus ----------

    @Test
    public void globalExceptionHandler_handleResponseStatus_KnownStatusWithReason_ReturnsStatusAndReason() throws Exception {
        // Act + Assert
        // Código conocido con motivo: el mensaje es el motivo tal cual.
        mockMvc.perform(MockMvcRequestBuilders.get("/test/errors/status/with-reason"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.I_AM_A_TEAPOT.value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(HttpStatus.I_AM_A_TEAPOT.value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(HttpStatus.I_AM_A_TEAPOT.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Motivo de prueba."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/test/errors/status/with-reason"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors").isEmpty());
    }

    @Test
    public void globalExceptionHandler_handleResponseStatus_KnownStatusWithoutReason_ReturnsReasonPhrase() throws Exception {
        // Act + Assert
        // Sin motivo: el mensaje es la frase estándar del código.
        mockMvc.perform(MockMvcRequestBuilders.get("/test/errors/status/without-reason"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.I_AM_A_TEAPOT.value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(HttpStatus.I_AM_A_TEAPOT.value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(HttpStatus.I_AM_A_TEAPOT.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(HttpStatus.I_AM_A_TEAPOT.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/test/errors/status/without-reason"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors").isEmpty());
    }

    @Test
    public void globalExceptionHandler_handleResponseStatus_UnknownStatusCode_Returns500() throws Exception {
        // Act + Assert
        // El código 499 no existe en HttpStatus: el manejador responde 500 con la frase de ese código.
        mockMvc.perform(MockMvcRequestBuilders.get("/test/errors/status/unknown-code"))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(500))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/test/errors/status/unknown-code"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors").isEmpty());
    }

    // ---------- fieldErrors ----------

    @Test
    public void globalExceptionHandler_fieldErrors_GlobalError_UsesGlobalKey() throws Exception {
        // Act + Assert
        // Error sin campo: se guarda bajo la clave _global.
        mockMvc.perform(MockMvcRequestBuilders.get("/test/errors/field-errors/global"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("La petición tiene campos inválidos."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/test/errors/field-errors/global"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors['_global']").value("Error global de prueba."));
    }

    @Test
    public void globalExceptionHandler_fieldErrors_TwoErrorsOnSameField_KeepsFirstMessage() throws Exception {
        // Act + Assert
        // Dos errores del mismo campo: putIfAbsent conserva el primero.
        mockMvc.perform(MockMvcRequestBuilders.get("/test/errors/field-errors/duplicated-field"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("La petición tiene campos inválidos."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/test/errors/field-errors/duplicated-field"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.name").value("Primer mensaje de prueba."));
    }

    @Test
    public void globalExceptionHandler_fieldErrors_NullDefaultMessage_ReturnsInvalidValueText() throws Exception {
        // Act + Assert
        // Sin mensaje por defecto: se usa el texto de respaldo.
        mockMvc.perform(MockMvcRequestBuilders.get("/test/errors/field-errors/null-message"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("La petición tiene campos inválidos."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/test/errors/field-errors/null-message"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.email").value("Valor inválido"));
    }

    // ---------- soporte de prueba ----------

    // MethodParameter real de un método del controlador de prueba: lo exige MethodArgumentNotValidException.
    private static MethodParameter referenceParameter() throws NoSuchMethodException {
        return new MethodParameter(ThrowingController.class.getMethod("referenceMethod", String.class), 0);
    }

    // Objetos con validación anidada: producen rutas como "order.address.street".
    public record Address(@NotBlank(message = "La calle es obligatoria.") String street) {
    }

    public record Order(@Valid Address address) {
    }

    public record OrderHolder(@Valid Order order) {
    }

    // Controlador de prueba: cada endpoint lanza la excepción que el manejador debe traducir.
    @RestController
    @RequestMapping("/test/errors")
    public static class ThrowingController {

        private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

        @PostMapping("/max-upload")
        public void maxUpload() {
            throw new MaxUploadSizeExceededException(10L * 1024 * 1024);
        }

        @PostMapping("/multipart/max-upload-cause")
        public void multipartWithMaxUploadCause() {
            throw new MultipartException("Parte multipart de prueba.", new MaxUploadSizeExceededException(10L * 1024 * 1024));
        }

        @PostMapping("/multipart/no-cause")
        public void multipartWithoutCause() {
            throw new MultipartException("Parte multipart de prueba.");
        }

        @PostMapping("/multipart/other-cause")
        public void multipartWithOtherCause() {
            throw new MultipartException("Parte multipart de prueba.", new IllegalStateException("Causa de prueba."));
        }

        @GetMapping("/validated-parameter")
        public void validatedParameter(@RequestParam @Min(value = 1, message = "El tamaño mínimo es 1.") int size) {
        }

        @GetMapping("/validated-code")
        public void validatedCode(@RequestParam @NotBlank(message = "Código obligatorio")
                                  @Size(min = 3, message = "Código demasiado corto") String code) {
        }

        @GetMapping("/constraint/nested")
        public void nestedConstraintViolation() {
            throw new ConstraintViolationException(VALIDATOR.validate(new OrderHolder(new Order(new Address("")))));
        }

        @GetMapping("/constraint/top-level")
        public void topLevelConstraintViolation() {
            throw new ConstraintViolationException(VALIDATOR.validate(new Address("")));
        }

        @GetMapping("/status/with-reason")
        public void statusWithReason() {
            throw new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT, "Motivo de prueba.");
        }

        @GetMapping("/status/without-reason")
        public void statusWithoutReason() {
            throw new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT);
        }

        @GetMapping("/status/unknown-code")
        public void statusUnknownCode() {
            throw new ResponseStatusException(HttpStatusCode.valueOf(499));
        }

        @GetMapping("/field-errors/global")
        public void fieldErrorsGlobal() throws Exception {
            BindingResult bindingResult = new MapBindingResult(Map.of(), "sample");
            bindingResult.reject("sample.invalid", "Error global de prueba.");
            throw new MethodArgumentNotValidException(referenceParameter(), bindingResult);
        }

        @GetMapping("/field-errors/duplicated-field")
        public void fieldErrorsDuplicatedField() throws Exception {
            BindingResult bindingResult = new MapBindingResult(Map.of(), "sample");
            bindingResult.addError(new FieldError("sample", "name", "Primer mensaje de prueba."));
            bindingResult.addError(new FieldError("sample", "name", "Segundo mensaje de prueba."));
            throw new MethodArgumentNotValidException(referenceParameter(), bindingResult);
        }

        @GetMapping("/field-errors/null-message")
        public void fieldErrorsNullMessage() throws Exception {
            BindingResult bindingResult = new MapBindingResult(Map.of(), "sample");
            bindingResult.addError(new FieldError("sample", "email", (String) null));
            throw new MethodArgumentNotValidException(referenceParameter(), bindingResult);
        }

        // Método de referencia sin mapeo: solo aporta un MethodParameter real, nunca se invoca.
        public void referenceMethod(String value) {
        }
    }
}
