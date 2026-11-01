package io.github.pethaven.service;

import io.github.pethaven.dto.request.DrugRequest;
import io.github.pethaven.dto.response.DrugImportSummaryResponse;
import io.github.pethaven.dto.response.DrugResponse;
import io.github.pethaven.entity.Drug;
import io.github.pethaven.exception.IllegalOperationException;
import io.github.pethaven.exception.InvalidExcelFormatException;
import io.github.pethaven.repository.DrugRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DrugImportServiceImpl implements DrugImportService {

    // Firma ZIP (XLSX)
    private static final byte[] ZIP_MAGIC = {0x50, 0x4B, 0x03, 0x04};
    // Firma OLE2 (XLS)
    private static final byte[] OLE2_MAGIC = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };
    private static final int SIGNATURE_LENGTH = 8;
    private static final int MINIMUM_SIGNATURE_LENGTH = 4;
    private static final int MAX_LISTED_NAMES = 10;
    private static final String DEFAULT_FILE_NAME = "archivo";

    private final ExcelReadingService excelReadingService;
    private final DrugService drugService;
    private final DrugRepository drugRepository;

    @Autowired
    public DrugImportServiceImpl(
            ExcelReadingService excelReadingService,
            DrugService drugService,
            DrugRepository drugRepository
    ) {
        this.excelReadingService = excelReadingService;
        this.drugService = drugService;
        this.drugRepository = drugRepository;
    }

    @Override
    @Transactional
    public DrugImportSummaryResponse importFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw InvalidExcelFormatException.emptyFile();
        }

        String fileName = StringUtils.getFilename(file.getOriginalFilename());
        if (!StringUtils.hasText(fileName)) {
            fileName = DEFAULT_FILE_NAME;
        }
        validateExtension(fileName);

        try (InputStream inputStream = file.getInputStream()) {
            return doImport(inputStream, fileName);
        } catch (InvalidExcelFormatException e) {
            throw e;
        } catch (IOException e) {
            throw InvalidExcelFormatException.unreadable(fileName, e);
        }
    }

    @Override
    @Transactional
    public DrugImportSummaryResponse importFromExcel(InputStream inputStream, String fileName) {
        String displayName = StringUtils.hasText(fileName) ? fileName : DEFAULT_FILE_NAME;
        validateExtension(displayName);

        try {
            return doImport(inputStream, displayName);
        } catch (InvalidExcelFormatException e) {
            throw e;
        } catch (IOException e) {
            throw InvalidExcelFormatException.unreadable(displayName, e);
        }
    }

    /**
     * Valida el contenido binario, parsea el archivo y persiste todo o nada.
     */
    private DrugImportSummaryResponse doImport(InputStream inputStream, String fileName) throws IOException {
        BufferedInputStream buffered = new BufferedInputStream(inputStream);
        buffered.mark(SIGNATURE_LENGTH);
        byte[] signature = buffered.readNBytes(SIGNATURE_LENGTH);
        buffered.reset();

        if (signature.length < MINIMUM_SIGNATURE_LENGTH
                || (!startsWith(signature, ZIP_MAGIC) && !startsWith(signature, OLE2_MAGIC))) {
            throw InvalidExcelFormatException.unsupportedFormat(fileName);
        }

        ExcelReadingResult result;
        try {
            result = excelReadingService.readDrugsFromExcel(buffered);
        } catch (InvalidExcelFormatException e) {
            throw e;
        } catch (Exception e) {
            throw InvalidExcelFormatException.unreadable(fileName, e);
        }

        if (result.drugs().isEmpty()) {
            throw InvalidExcelFormatException.noDataRows(result.sheetName());
        }

        // Primero se validan los duplicados internos del archivo; después los existentes en la base.
        List<String> duplicatedInFile = findDuplicatedNames(result.drugs());
        if (!duplicatedInFile.isEmpty()) {
            throw new IllegalOperationException("El archivo contiene medicamentos duplicados: "
                    + formatNames(duplicatedInFile) + ". No se importó ningún medicamento.");
        }

        List<String> existingInDatabase = findExistingNames(result.drugs());
        if (!existingInDatabase.isEmpty()) {
            throw new IllegalOperationException("Ya existen medicamentos con estos nombres: "
                    + formatNames(existingInDatabase) + ". No se importó ningún medicamento.");
        }

        List<DrugResponse> imported = new ArrayList<>();
        for (DrugRequest request : result.drugs()) {
            imported.add(drugService.createDrug(request));
        }

        return new DrugImportSummaryResponse(
                fileName,
                result.sheetName(),
                result.drugs().size(),
                imported.size(),
                result.skippedBlankRows()
        );
    }

    private void validateExtension(String fileName) {
        String lowerCaseName = fileName.toLowerCase(Locale.ROOT);
        if (!lowerCaseName.endsWith(".xlsx") && !lowerCaseName.endsWith(".xls")) {
            throw InvalidExcelFormatException.unsupportedExtension(fileName);
        }
    }

    private boolean startsWith(byte[] data, byte[] signature) {
        if (data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Nombres repetidos dentro del archivo, preservando el orden de aparición.
     */
    private List<String> findDuplicatedNames(List<DrugRequest> drugs) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (DrugRequest drug : drugs) {
            counts.merge(drug.name(), 1, Integer::sum);
        }

        List<String> duplicated = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 1) {
                duplicated.add(entry.getKey());
            }
        }
        return duplicated;
    }

    /**
     * Nombres del archivo que ya existen en la base de datos.
     */
    private List<String> findExistingNames(List<DrugRequest> drugs) {
        List<String> names = drugs.stream()
                .map(DrugRequest::name)
                .distinct()
                .sorted()
                .toList();

        if (names.isEmpty()) {
            return List.of();
        }

        return drugRepository.findByNameIn(names).stream()
                .map(Drug::getName)
                .sorted()
                .toList();
    }

    /**
     * Lista como máximo {@value #MAX_LISTED_NAMES} nombres entre comillas simples.
     * Si hay más, agrega " y N más" para no devolver mensajes gigantes.
     */
    private String formatNames(List<String> names) {
        int listedCount = Math.min(names.size(), MAX_LISTED_NAMES);
        String listed = names.stream()
                .limit(listedCount)
                .map(name -> "'" + name + "'")
                .collect(Collectors.joining(", "));

        if (names.size() > MAX_LISTED_NAMES) {
            return listed + " y " + (names.size() - MAX_LISTED_NAMES) + " más";
        }
        return listed;
    }

}
