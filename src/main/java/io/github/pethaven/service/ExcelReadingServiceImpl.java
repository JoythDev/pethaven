package io.github.pethaven.service;

import io.github.pethaven.dto.request.DrugRequest;
import io.github.pethaven.dto.response.DrugResponse;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelReadingServiceImpl implements ExcelReadingService {

    private final DrugService drugService;

    @Autowired
    public ExcelReadingServiceImpl(DrugService drugService) {
        this.drugService = drugService;
    }

    @Override
    public List<DrugResponse> loadDrugsFromExcel(String filePath) throws IOException {
        Workbook workbook = openWorkbook(filePath);
        Sheet sheet = workbook.getSheetAt(0);

        List<DrugResponse> loadedDrugs = new ArrayList<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            DrugRequest request = readDrugFromRow(sheet.getRow(i));
            loadedDrugs.add(drugService.createDrug(request));
        }

        return loadedDrugs;
    }

    private Workbook openWorkbook(String filePath) throws IOException {
        try (InputStream file = new FileInputStream(filePath)) {
            return WorkbookFactory.create(file);
        }
    }

    private DrugRequest readDrugFromRow(Row row) {
        String name = row.getCell(0).getStringCellValue();
        BigDecimal purchasePrice = BigDecimal.valueOf(row.getCell(1).getNumericCellValue());
        BigDecimal salePrice = BigDecimal.valueOf(row.getCell(2).getNumericCellValue());
        Integer unitsAvailable = (int) row.getCell(3).getNumericCellValue();
        return new DrugRequest(name, purchasePrice, salePrice, unitsAvailable);
    }

}
