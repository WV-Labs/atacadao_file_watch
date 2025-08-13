package com.mercado.filemonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mercado.filemonitor.dto.PositionalRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class JsonGeneratorService {

    private final ObjectMapper objectMapper;

    public JsonGeneratorService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public Path generateJsonFile(List<PositionalRecord> records, Path outputDirectory, String originalFileName) throws IOException {
        log.info("Gerando arquivo JSON para {} registros", records.size());

        // Criar diretório de saída se não existir
        Files.createDirectories(outputDirectory);

        // Gerar nome do arquivo JSON
        String jsonFileName = generateJsonFileName(originalFileName);
        Path jsonFilePath = outputDirectory.resolve(jsonFileName);

        // Criar estrutura do JSON com metadados
        Map<String, Object> jsonOutput = new HashMap<>();
        jsonOutput.put("metadata", createMetadata(originalFileName, records.size()));
        jsonOutput.put("records", records);

        // Escrever JSON no arquivo
        objectMapper.writeValue(jsonFilePath.toFile(), jsonOutput);

        log.info("Arquivo JSON gerado: {}", jsonFilePath);
        return jsonFilePath;
    }

    private String generateJsonFileName(String originalFileName) {
        String baseName = originalFileName.replaceAll("\\.[^.]+$", ""); // Remove extensão
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s_%s.json", baseName, timestamp);
    }

    private Map<String, Object> createMetadata(String originalFileName, int recordCount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source_file", originalFileName);
        metadata.put("processed_at", LocalDateTime.now());
        metadata.put("record_count", recordCount);
        metadata.put("version", "1.0");
        metadata.put("format", "positional_to_json");
        return metadata;
    }

    public String generateJsonString(List<PositionalRecord> records) throws IOException {
        Map<String, Object> jsonOutput = new HashMap<>();
        jsonOutput.put("records", records);
        jsonOutput.put("count", records.size());

        return objectMapper.writeValueAsString(jsonOutput);
    }
}
