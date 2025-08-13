package com.mercado.filemonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mercado.filemonitor.dto.ProdutoDTO;
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
public class ProdutoJsonService {

    private final ObjectMapper objectMapper;

    public ProdutoJsonService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Gera arquivo JSON de produtos
     */
    public Path generateProdutoJsonFile(List<ProdutoDTO> produtos, Path outputDirectory, String originalFileName) throws IOException {
        log.info("Gerando arquivo JSON de produtos para {} itens", produtos.size());

        // Criar diretório de saída se não existir
        Files.createDirectories(outputDirectory);

        // Gerar nome do arquivo JSON
        String jsonFileName = generateProdutoJsonFileName(originalFileName);
        Path jsonFilePath = outputDirectory.resolve(jsonFileName);

        // Criar estrutura do JSON com metadados
        Map<String, Object> jsonOutput = new HashMap<>();
        //jsonOutput.put("metadata", createProdutoMetadata(originalFileName, produtos.size()));
        jsonOutput.put("produtos", produtos);

        // Escrever JSON no arquivo
        objectMapper.writeValue(jsonFilePath.toFile(), jsonOutput);

        log.info("Arquivo JSON de produtos gerado: {}", jsonFilePath);
        return jsonFilePath;
    }

    /**
     * Gera string JSON de produtos
     */
    public String generateProdutoJsonString(List<ProdutoDTO> produtos) throws IOException {
        Map<String, Object> jsonOutput = new HashMap<>();
        jsonOutput.put("produtos", produtos);
        jsonOutput.put("total", produtos.size());

        return objectMapper.writeValueAsString(jsonOutput);
    }

    /**
     * Gera nome do arquivo JSON para produtos
     */
    private String generateProdutoJsonFileName(String originalFileName) {
        String baseName = originalFileName.replaceAll("\\.[^.]+$", ""); // Remove extensão
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("produtos_%s_%s.json", baseName, timestamp);
    }

    /**
     * Cria metadados para o JSON de produtos
     */
    private Map<String, Object> createProdutoMetadata(String originalFileName, int produtoCount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source_file", originalFileName);
        metadata.put("processed_at", LocalDateTime.now());
        metadata.put("produto_count", produtoCount);
        metadata.put("version", "1.0");
        metadata.put("format", "positional_to_produto_json");
        metadata.put("mapping_rules", createMappingRules());
        return metadata;
    }

    /**
     * Documenta as regras de mapeamento
     */
    private Map<String, String> createMappingRules() {
        Map<String, String> rules = new HashMap<>();
        rules.put("produto.id", "positional.codigo (convertido para Long)");
        rules.put("produto.nome", "positional.nome (limitado a 50 chars)");
        rules.put("produto.categoria.nome", "positional.categoria");
        rules.put("produto.preco", "positional.valor");
        rules.put("produto.descricao", "gerado: nome + categoria (limitado a 100 chars)");
        rules.put("produto.codigo_barras", "gerado: 789 + codigo");
        return rules;
    }
}
