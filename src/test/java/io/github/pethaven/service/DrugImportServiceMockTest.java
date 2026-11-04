package io.github.pethaven.service;

import io.github.pethaven.dto.request.DrugRequest;
import io.github.pethaven.dto.response.DrugImportSummaryResponse;
import io.github.pethaven.dto.response.DrugResponse;
import io.github.pethaven.entity.Drug;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.InvalidExcelFormatException;
import io.github.pethaven.repository.DrugRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// Pruebas de la importación masiva con mocks: la lectura del Excel está simulada y se valida la orquestación.
@ExtendWith(MockitoExtension.class)
public class DrugImportServiceMockTest {

    // Firma ZIP de un archivo XLSX.
    private static final byte[] ZIP_SIGNATURE = {0x50, 0x4B, 0x03, 0x04};
    // Firma OLE2 de un archivo XLS.
    private static final byte[] OLE2_SIGNATURE = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };
    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Mock
    private ExcelReadingService excelReadingService;
    @Mock
    private DrugService drugService;
    @Mock
    private DrugRepository drugRepository;

    private DrugImportServiceImpl drugImportService;

    @BeforeEach
    public void setUp() {
        drugImportService = new DrugImportServiceImpl(excelReadingService, drugService, drugRepository);
    }

    // ---------- Helpers ----------

    private byte[] content(byte[] signature) {
        // El relleno es irrelevante: el servicio solo valida la firma y delega el parseo en el lector simulado.
        byte[] content = new byte[signature.length + 8];
        System.arraycopy(signature, 0, content, 0, signature.length);
        for (int i = signature.length; i < content.length; i++) {
            content[i] = (byte) 0x20;
        }
        return content;
    }

    private byte[] xlsxContent() {
        return content(ZIP_SIGNATURE);
    }

    private byte[] xlsContent() {
        return content(OLE2_SIGNATURE);
    }

    private MockMultipartFile multipartFile(String originalFilename, byte[] content) {
        return new MockMultipartFile("file", originalFilename, CONTENT_TYPE, content);
    }

    private DrugRequest drugRequest(String name) {
        return new DrugRequest(name, new BigDecimal("8000.00"), new BigDecimal("15000.00"), 200);
    }

    private DrugResponse drugResponse(Long id, String name) {
        return new DrugResponse(id, name, new BigDecimal("8000.00"), new BigDecimal("15000.00"), 200, 0);
    }

    private Drug drug(String name) {
        return Drug.builder().name(name).build();
    }

    private void stubRead(List<DrugRequest> drugs, String sheetName, int skippedBlankRows) throws IOException {
        Mockito.when(excelReadingService.readDrugsFromExcel(ArgumentMatchers.any(InputStream.class)))
                .thenReturn(new ExcelReadingResult(sheetName, drugs, skippedBlankRows));
    }

    private List<DrugRequest> duplicatedRequests(int count) {
        // Los nombres se agregan de mayor a menor para comprobar que el mensaje respeta el orden de aparición.
        List<DrugRequest> requests = new ArrayList<>();
        for (int i = count; i >= 1; i--) {
            String name = "Medicamento " + (i < 10 ? "0" + i : String.valueOf(i));
            requests.add(drugRequest(name));
            requests.add(drugRequest(name));
        }
        return requests;
    }

    // ---------- Archivo vacío ----------

    @Test
    public void drugImportService_importFromExcel_NullFile_ThrowsEmptyFile() {
        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugImportService.importFromExcel((MultipartFile) null))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("El archivo Excel está vacío. Seleccione un archivo con datos de medicamentos.");
        // Sin archivo no se lee ni se consulta nada.
        Mockito.verifyNoInteractions(excelReadingService, drugService, drugRepository);
    }

    @Test
    public void drugImportService_importFromExcel_EmptyFile_ThrowsEmptyFile() {
        // Arrange
        MockMultipartFile file = multipartFile("medicamentos.xlsx", new byte[0]);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugImportService.importFromExcel(file))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("El archivo Excel está vacío. Seleccione un archivo con datos de medicamentos.");
        // Un archivo de cero bytes se rechaza antes de validar la extensión y antes de leerlo.
        Mockito.verifyNoInteractions(excelReadingService, drugService, drugRepository);
    }

    // ---------- Validación de extensión ----------

    @Test
    public void drugImportService_importFromExcel_UppercaseXlsxExtension_ImportsFile() throws IOException {
        // Arrange
        DrugRequest request = drugRequest("Amoxicilina 250mg");
        stubRead(List.of(request), "Medicamentos", 0);
        Mockito.when(drugRepository.findByNameIn(ArgumentMatchers.anyList())).thenReturn(List.of());

        // Act
        DrugImportSummaryResponse result = drugImportService.importFromExcel(
                multipartFile("medicamentos.XLSX", xlsxContent()));

        // Assert
        // La extensión se compara en minúsculas: ".XLSX" es válida y el nombre original se conserva.
        Assertions.assertThat(result)
                .isEqualTo(new DrugImportSummaryResponse("medicamentos.XLSX", "Medicamentos", 1, 1, 0));
        Mockito.verify(drugService).createDrug(request);
    }

    @Test
    public void drugImportService_importFromExcel_LowercaseXlsExtension_ImportsFile() throws IOException {
        // Arrange
        DrugRequest request = drugRequest("Meloxicam 5mg");
        stubRead(List.of(request), "Inventario", 0);
        Mockito.when(drugRepository.findByNameIn(ArgumentMatchers.anyList())).thenReturn(List.of());

        // Act
        DrugImportSummaryResponse result = drugImportService.importFromExcel(
                multipartFile("inventario.xls", xlsContent()));

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new DrugImportSummaryResponse("inventario.xls", "Inventario", 1, 1, 0));
        Mockito.verify(drugService).createDrug(request);
    }

    @Test
    public void drugImportService_importFromExcel_CsvExtension_ThrowsUnsupportedExtension() {
        // Arrange
        MockMultipartFile file = multipartFile("medicamentos.csv", xlsxContent());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugImportService.importFromExcel(file))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("El archivo 'medicamentos.csv' tiene una extensión no soportada. "
                        + "Use un archivo .xlsx o .xls.");
        // La extensión se rechaza antes de leer el contenido o consultar la base de datos.
        Mockito.verifyNoInteractions(excelReadingService, drugService, drugRepository);
    }

    @Test
    public void drugImportService_importFromExcel_NullOriginalFilename_ThrowsUnsupportedExtensionForDefaultName() {
        // Arrange
        MultipartFile file = Mockito.mock(MultipartFile.class);
        Mockito.when(file.isEmpty()).thenReturn(false);
        Mockito.when(file.getOriginalFilename()).thenReturn(null);

        // Act + Assert
        // Sin nombre original se usa el nombre por defecto "archivo", que no tiene extensión de Excel.
        Assertions.assertThatThrownBy(() -> drugImportService.importFromExcel(file))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("El archivo 'archivo' tiene una extensión no soportada. "
                        + "Use un archivo .xlsx o .xls.");
        Mockito.verifyNoInteractions(excelReadingService, drugService, drugRepository);
    }

    @Test
    public void drugImportService_importFromExcel_BlankOriginalFilename_ThrowsUnsupportedExtensionForDefaultName() {
        // Arrange
        MockMultipartFile file = multipartFile("   ", xlsxContent());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugImportService.importFromExcel(file))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("El archivo 'archivo' tiene una extensión no soportada. "
                        + "Use un archivo .xlsx o .xls.");
        Mockito.verifyNoInteractions(excelReadingService, drugService, drugRepository);
    }

    @Test
    public void drugImportService_importFromExcel_PathLikeFilename_UsesOnlyFileNameInSummary() throws IOException {
        // Arrange
        DrugRequest request = drugRequest("Ivermectina 1%");
        stubRead(List.of(request), "Medicamentos", 1);
        Mockito.when(drugRepository.findByNameIn(ArgumentMatchers.anyList())).thenReturn(List.of());

        // Act
        DrugImportSummaryResponse result = drugImportService.importFromExcel(
                multipartFile("carpeta/sub/medicamentos.xlsx", xlsxContent()));

        // Assert
        // StringUtils.getFilename descarta los directorios: el resumen muestra solo el nombre del archivo.
        Assertions.assertThat(result)
                .isEqualTo(new DrugImportSummaryResponse("medicamentos.xlsx", "Medicamentos", 1, 1, 1));
    }

    // ---------- Validación de la firma binaria ----------

    @Test
    public void drugImportService_importFromExcel_NonExcelContent_ThrowsUnsupportedFormat() {
        // Arrange
        MockMultipartFile file = multipartFile(
                "medicamentos.xlsx", "hola mundo texto".getBytes(StandardCharsets.UTF_8));

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugImportService.importFromExcel(file))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("El archivo 'medicamentos.xlsx' no es un archivo Excel válido. "
                        + "Use un archivo .xlsx o .xls.");
        Mockito.verifyNoInteractions(excelReadingService, drugService, drugRepository);
    }

    @Test
    public void drugImportService_importFromExcel_ContentShorterThanSignature_ThrowsUnsupportedFormat() {
        // Arrange
        // El servicio exige al menos cuatro bytes para comparar la firma ZIP.
        MockMultipartFile file = multipartFile("medicamentos.xlsx", new byte[]{0x50, 0x4B, 0x03});

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugImportService.importFromExcel(file))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("El archivo 'medicamentos.xlsx' no es un archivo Excel válido. "
                        + "Use un archivo .xlsx o .xls.");
        Mockito.verifyNoInteractions(excelReadingService, drugService, drugRepository);
    }

    @Test
    public void drugImportService_importFromExcel_FourByteNonExcelContent_ThrowsUnsupportedFormat() {
        // Arrange
        // Cuatro bytes no bastan para comparar la firma OLE2: el servicio rechaza sin leer fuera del arreglo.
        MockMultipartFile file = multipartFile("medicamentos.xlsx", new byte[]{0x00, 0x01, 0x02, 0x03});

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugImportService.importFromExcel(file))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("El archivo 'medicamentos.xlsx' no es un archivo Excel válido. "
                        + "Use un archivo .xlsx o .xls.");
        Mockito.verifyNoInteractions(excelReadingService, drugService, drugRepository);
    }

    @Test
    public void drugImportService_importFromExcel_XlsxNamedFileWithOle2Signature_ImportsFile() throws IOException {
        // Arrange
        DrugRequest request = drugRequest("Amoxicilina 500mg");
        stubRead(List.of(request), "Medicamentos", 0);
        Mockito.when(drugRepository.findByNameIn(ArgumentMatchers.anyList())).thenReturn(List.of());

        // Act
        DrugImportSummaryResponse result = drugImportService.importFromExcel(
                multipartFile("medicamentos.xlsx", xlsContent()));

        // Assert
        // La firma binaria y la extensión se validan de forma independiente: un XLS con nombre .xlsx es aceptado.
        Assertions.assertThat(result)
                .isEqualTo(new DrugImportSummaryResponse("medicamentos.xlsx", "Medicamentos", 1, 1, 0));
    }

    // ---------- Errores del lector ----------

    @Test
    public void drugImportService_importFromExcel_ReaderThrowsInvalidExcelFormat_PropagatesSameException()
            throws IOException {
        // Arrange
        InvalidExcelFormatException expected =
                new InvalidExcelFormatException("El archivo Excel no contiene ninguna hoja.");
        Mockito.when(excelReadingService.readDrugsFromExcel(ArgumentMatchers.any(InputStream.class)))
                .thenThrow(expected);

        // Act
        Throwable thrown = Assertions.catchThrowable(
                () -> drugImportService.importFromExcel(multipartFile("medicamentos.xlsx", xlsxContent())));

        // Assert
        // Un error de formato ya tipado se propaga sin envolverlo en otra excepción.
        Assertions.assertThat(thrown).isSameAs(expected);
        Assertions.assertThat(thrown).hasMessage("El archivo Excel no contiene ninguna hoja.");
        Mockito.verifyNoInteractions(drugService, drugRepository);
    }

    @Test
    public void drugImportService_importFromExcel_ReaderThrowsIOException_ThrowsUnreadableWithCause()
            throws IOException {
        // Arrange
        IOException cause = new IOException("archivo dañado");
        Mockito.when(excelReadingService.readDrugsFromExcel(ArgumentMatchers.any(InputStream.class)))
                .thenThrow(cause);

        // Act + Assert
        Assertions.assertThatThrownBy(
                        () -> drugImportService.importFromExcel(multipartFile("medicamentos.xlsx", xlsxContent())))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("No se pudo leer el archivo Excel 'medicamentos.xlsx'. "
                        + "Verifique que no esté dañado ni protegido con contraseña.")
                .hasCauseReference(cause);
        Mockito.verifyNoInteractions(drugService, drugRepository);
    }

    @Test
    public void drugImportService_importFromExcel_ReaderThrowsRuntimeException_ThrowsUnreadableWithCause()
            throws IOException {
        // Arrange
        IllegalStateException cause = new IllegalStateException("error inesperado del lector");
        Mockito.when(excelReadingService.readDrugsFromExcel(ArgumentMatchers.any(InputStream.class)))
                .thenThrow(cause);

        // Act + Assert
        // Cualquier fallo no tipado del lector también se traduce en un archivo ilegible.
        Assertions.assertThatThrownBy(
                        () -> drugImportService.importFromExcel(multipartFile("medicamentos.xlsx", xlsxContent())))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("No se pudo leer el archivo Excel 'medicamentos.xlsx'. "
                        + "Verifique que no esté dañado ni protegido con contraseña.")
                .hasCauseReference(cause);
        Mockito.verifyNoInteractions(drugService, drugRepository);
    }

    @Test
    public void drugImportService_importFromExcel_GetInputStreamThrowsIOException_ThrowsUnreadableWithCause()
            throws IOException {
        // Arrange
        IOException cause = new IOException("no se pudo abrir el archivo");
        MultipartFile file = Mockito.mock(MultipartFile.class);
        Mockito.when(file.isEmpty()).thenReturn(false);
        Mockito.when(file.getOriginalFilename()).thenReturn("medicamentos.xlsx");
        Mockito.when(file.getInputStream()).thenThrow(cause);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugImportService.importFromExcel(file))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("No se pudo leer el archivo Excel 'medicamentos.xlsx'. "
                        + "Verifique que no esté dañado ni protegido con contraseña.")
                .hasCauseReference(cause);
        Mockito.verifyNoInteractions(excelReadingService, drugService, drugRepository);
    }

    // ---------- Validación de filas ----------

    @Test
    public void drugImportService_importFromExcel_NoDrugRows_ThrowsNoDataRows() throws IOException {
        // Arrange
        stubRead(List.of(), "Medicamentos", 4);

        // Act + Assert
        Assertions.assertThatThrownBy(
                        () -> drugImportService.importFromExcel(multipartFile("medicamentos.xlsx", xlsxContent())))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("El archivo Excel no contiene filas de datos en la hoja 'Medicamentos'.");
        // Sin filas no se consultan duplicados en la base de datos ni se crea ningún medicamento.
        Mockito.verifyNoInteractions(drugService, drugRepository);
    }

    @Test
    public void drugImportService_importFromExcel_DuplicatedNamesInFile_ThrowsIllegalOperation() throws IOException {
        // Arrange
        // El orden de aparición manda: Amoxicilina y Meloxicam se repiten, Ivermectina aparece una sola vez.
        stubRead(List.of(
                drugRequest("Amoxicilina 250mg"),
                drugRequest("Meloxicam 5mg"),
                drugRequest("Amoxicilina 250mg"),
                drugRequest("Ivermectina 1%"),
                drugRequest("Meloxicam 5mg")), "Medicamentos", 0);

        // Act + Assert
        Assertions.assertThatThrownBy(
                        () -> drugImportService.importFromExcel(multipartFile("medicamentos.xlsx", xlsxContent())))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("El archivo contiene medicamentos duplicados: 'Amoxicilina 250mg', 'Meloxicam 5mg'. "
                        + "No se importó ningún medicamento.");
        // Los duplicados se detectan antes de consultar la base de datos y no se crea nada.
        Mockito.verifyNoInteractions(drugService, drugRepository);
    }

    @Test
    public void drugImportService_importFromExcel_NamesAlreadyInDatabase_ThrowsIllegalOperationWithSortedNames()
            throws IOException {
        // Arrange
        stubRead(List.of(
                drugRequest("Meloxicam 5mg"),
                drugRequest("Amoxicilina 250mg"),
                drugRequest("Ivermectina 1%")), "Medicamentos", 0);
        // El repositorio devuelve las entidades en desorden: el mensaje debe listarlas ordenadas.
        Mockito.when(drugRepository.findByNameIn(ArgumentMatchers.anyList()))
                .thenReturn(List.of(drug("Meloxicam 5mg"), drug("Amoxicilina 250mg")));

        // Act + Assert
        Assertions.assertThatThrownBy(
                        () -> drugImportService.importFromExcel(multipartFile("medicamentos.xlsx", xlsxContent())))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("Ya existen medicamentos con estos nombres: 'Amoxicilina 250mg', 'Meloxicam 5mg'. "
                        + "No se importó ningún medicamento.");

        // La consulta recibe los nombres distintos del archivo, ordenados alfabéticamente.
        ArgumentCaptor<Collection<String>> namesCaptor = ArgumentCaptor.captor();
        Mockito.verify(drugRepository).findByNameIn(namesCaptor.capture());
        Assertions.assertThat(namesCaptor.getValue())
                .containsExactly("Amoxicilina 250mg", "Ivermectina 1%", "Meloxicam 5mg");

        // Ningún medicamento se llega a crear.
        Mockito.verifyNoInteractions(drugService);
    }

    @Test
    public void drugImportService_importFromExcel_TwelveDuplicatedNames_ListsFirstTenAndRemainingCount()
            throws IOException {
        // Arrange
        stubRead(duplicatedRequests(12), "Medicamentos", 0);

        // Act + Assert
        Assertions.assertThatThrownBy(
                        () -> drugImportService.importFromExcel(multipartFile("medicamentos.xlsx", xlsxContent())))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("El archivo contiene medicamentos duplicados: "
                        + "'Medicamento 12', 'Medicamento 11', 'Medicamento 10', 'Medicamento 09', "
                        + "'Medicamento 08', 'Medicamento 07', 'Medicamento 06', 'Medicamento 05', "
                        + "'Medicamento 04', 'Medicamento 03' y 2 más. No se importó ningún medicamento.");
        Mockito.verifyNoInteractions(drugService, drugRepository);
    }

    @Test
    public void drugImportService_importFromExcel_TenDuplicatedNames_ListsAllWithoutRemainingSuffix()
            throws IOException {
        // Arrange
        stubRead(duplicatedRequests(10), "Medicamentos", 0);

        // Act + Assert
        // Con exactamente diez nombres se listan todos y no se agrega el sufijo " y N más".
        Assertions.assertThatThrownBy(
                        () -> drugImportService.importFromExcel(multipartFile("medicamentos.xlsx", xlsxContent())))
                .isInstanceOf(IllegalOperationException.class)
                .hasMessage("El archivo contiene medicamentos duplicados: "
                        + "'Medicamento 10', 'Medicamento 09', 'Medicamento 08', 'Medicamento 07', "
                        + "'Medicamento 06', 'Medicamento 05', 'Medicamento 04', 'Medicamento 03', "
                        + "'Medicamento 02', 'Medicamento 01'. No se importó ningún medicamento.");
        Mockito.verifyNoInteractions(drugService, drugRepository);
    }

    // ---------- Importación exitosa ----------

    @Test
    public void drugImportService_importFromExcel_ThreeValidRequests_CreatesDrugsInFileOrderAndReturnsSummary()
            throws IOException {
        // Arrange
        DrugRequest first = drugRequest("Amoxicilina 250mg");
        DrugRequest second = drugRequest("Ibuprofeno 400mg");
        DrugRequest third = drugRequest("Meloxicam 5mg");
        byte[] rawContent = xlsxContent();
        List<byte[]> contentReceivedByReader = new ArrayList<>();
        Mockito.when(excelReadingService.readDrugsFromExcel(ArgumentMatchers.any(InputStream.class)))
                .thenAnswer(invocation -> {
                    InputStream received = invocation.getArgument(0);
                    contentReceivedByReader.add(received.readAllBytes());
                    return new ExcelReadingResult("Medicamentos", List.of(first, second, third), 2);
                });
        Mockito.when(drugRepository.findByNameIn(ArgumentMatchers.anyList())).thenReturn(List.of());
        Mockito.when(drugService.createDrug(first)).thenReturn(drugResponse(1L, "Amoxicilina 250mg"));
        Mockito.when(drugService.createDrug(second)).thenReturn(drugResponse(2L, "Ibuprofeno 400mg"));
        Mockito.when(drugService.createDrug(third)).thenReturn(drugResponse(3L, "Meloxicam 5mg"));

        // Act
        DrugImportSummaryResponse result = drugImportService.importFromExcel(
                multipartFile("medicamentos.xlsx", xlsxContent()));

        // Assert
        Assertions.assertThat(result)
                .isEqualTo(new DrugImportSummaryResponse("medicamentos.xlsx", "Medicamentos", 3, 3, 2));

        // El lector recibe el contenido completo: tras validar la firma, el servicio retrocede el stream.
        Assertions.assertThat(contentReceivedByReader).hasSize(1);
        Assertions.assertThat(contentReceivedByReader.get(0)).containsExactly(rawContent);

        // La consulta de nombres existentes recibe los del archivo, distintos y ordenados.
        Mockito.verify(drugRepository)
                .findByNameIn(List.of("Amoxicilina 250mg", "Ibuprofeno 400mg", "Meloxicam 5mg"));

        // Se crea un medicamento por cada fila, en el orden del archivo y con la misma instancia leída.
        ArgumentCaptor<DrugRequest> requestsCaptor = ArgumentCaptor.forClass(DrugRequest.class);
        Mockito.verify(drugService, Mockito.times(3)).createDrug(requestsCaptor.capture());
        Assertions.assertThat(requestsCaptor.getAllValues()).hasSize(3);
        Assertions.assertThat(requestsCaptor.getAllValues().get(0)).isSameAs(first);
        Assertions.assertThat(requestsCaptor.getAllValues().get(1)).isSameAs(second);
        Assertions.assertThat(requestsCaptor.getAllValues().get(2)).isSameAs(third);
    }

    // ---------- Sobrecarga de InputStream ----------

    @Test
    public void drugImportService_importFromExcel_StreamImport_ReturnsSummaryWithGivenFileName() throws IOException {
        // Arrange
        DrugRequest request = drugRequest("Amoxicilina 250mg");
        stubRead(List.of(request), "Hoja1", 0);
        Mockito.when(drugRepository.findByNameIn(ArgumentMatchers.anyList())).thenReturn(List.of());
        Mockito.when(drugService.createDrug(request)).thenReturn(drugResponse(1L, "Amoxicilina 250mg"));

        // Act
        DrugImportSummaryResponse result = drugImportService.importFromExcel(
                new ByteArrayInputStream(xlsxContent()), "medicamentos.xlsx");

        // Assert
        Mockito.verify(excelReadingService).readDrugsFromExcel(ArgumentMatchers.any(InputStream.class));
        Assertions.assertThat(result)
                .isEqualTo(new DrugImportSummaryResponse("medicamentos.xlsx", "Hoja1", 1, 1, 0));
    }

    @Test
    public void drugImportService_importFromExcel_StreamNullFileName_ThrowsUnsupportedExtensionForDefaultName() {
        // Arrange
        ByteArrayInputStream inputStream = new ByteArrayInputStream(xlsxContent());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugImportService.importFromExcel(inputStream, null))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("El archivo 'archivo' tiene una extensión no soportada. "
                        + "Use un archivo .xlsx o .xls.");
        Mockito.verifyNoInteractions(excelReadingService, drugService, drugRepository);
    }

    @Test
    public void drugImportService_importFromExcel_StreamBlankFileName_ThrowsUnsupportedExtensionForDefaultName() {
        // Arrange
        ByteArrayInputStream inputStream = new ByteArrayInputStream(xlsxContent());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugImportService.importFromExcel(inputStream, "   "))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("El archivo 'archivo' tiene una extensión no soportada. "
                        + "Use un archivo .xlsx o .xls.");
        Mockito.verifyNoInteractions(excelReadingService, drugService, drugRepository);
    }

    @Test
    public void drugImportService_importFromExcel_StreamCsvFileName_ThrowsUnsupportedExtension() {
        // Arrange
        ByteArrayInputStream inputStream = new ByteArrayInputStream(xlsxContent());

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugImportService.importFromExcel(inputStream, "medicamentos.csv"))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("El archivo 'medicamentos.csv' tiene una extensión no soportada. "
                        + "Use un archivo .xlsx o .xls.");
        Mockito.verifyNoInteractions(excelReadingService, drugService, drugRepository);
    }

    @Test
    public void drugImportService_importFromExcel_StreamReaderThrowsIOException_ThrowsUnreadableWithCause()
            throws IOException {
        // Arrange
        IOException cause = new IOException("archivo dañado");
        Mockito.when(excelReadingService.readDrugsFromExcel(ArgumentMatchers.any(InputStream.class)))
                .thenThrow(cause);

        // Act + Assert
        Assertions.assertThatThrownBy(() -> drugImportService.importFromExcel(
                        new ByteArrayInputStream(xlsxContent()), "medicamentos.xlsx"))
                .isInstanceOf(InvalidExcelFormatException.class)
                .hasMessage("No se pudo leer el archivo Excel 'medicamentos.xlsx'. "
                        + "Verifique que no esté dañado ni protegido con contraseña.")
                .hasCauseReference(cause);
        Mockito.verifyNoInteractions(drugService, drugRepository);
    }

    @Test
    public void drugImportService_importFromExcel_StreamReaderThrowsInvalidExcelFormat_PropagatesSameException()
            throws IOException {
        // Arrange
        InvalidExcelFormatException expected =
                new InvalidExcelFormatException("El archivo Excel no contiene ninguna hoja.");
        Mockito.when(excelReadingService.readDrugsFromExcel(ArgumentMatchers.any(InputStream.class)))
                .thenThrow(expected);

        // Act
        Throwable thrown = Assertions.catchThrowable(() -> drugImportService.importFromExcel(
                new ByteArrayInputStream(xlsxContent()), "medicamentos.xlsx"));

        // Assert
        Assertions.assertThat(thrown).isSameAs(expected);
        Mockito.verifyNoInteractions(drugService, drugRepository);
    }
}
