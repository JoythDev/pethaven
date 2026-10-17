package io.github.pethaven.service;

import io.github.pethaven.dto.response.DrugResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ExcelReadingService {
    List<DrugResponse> loadDrugsFromExcel(InputStream inputStream) throws IOException;
}
