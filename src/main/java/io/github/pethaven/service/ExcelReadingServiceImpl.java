package io.github.pethaven.service;

import io.github.pethaven.dto.request.DrugRequest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;

@Service
public class ExcelReadingServiceImpl implements ExcelReadingService {

    private final DrugService drugService;

    @Autowired
    public ExcelReadingServiceImpl(DrugService drugService) {
        this.drugService = drugService;
    }

    @Override
    public void loadDrugsFromExcel(String filePath) {
        try (InputStream file = new FileInputStream(filePath)) {
            Workbook workbook = WorkbookFactory.create(file);
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                String name = row.getCell(0).getStringCellValue();
                BigDecimal purchasePrice = BigDecimal.valueOf(row.getCell(1).getNumericCellValue());
                BigDecimal salePrice = BigDecimal.valueOf(row.getCell(2).getNumericCellValue());
                Integer unitsAvailable = (int) row.getCell(3).getNumericCellValue();
                DrugRequest request = new DrugRequest(name, purchasePrice, salePrice, unitsAvailable);
                drugService.createDrug(request);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
