package com.mercado.filemonitor.controller;

import com.mercado.filemonitor.config.FileMonitorConfig;
import com.mercado.filemonitor.dto.PositionalRecord;
import com.mercado.filemonitor.dto.ProdutoDTO;
import com.mercado.filemonitor.entity.FileRecord;
import com.mercado.filemonitor.repository.FileRecordRepository;
import com.mercado.filemonitor.service.FileParserService;
import com.mercado.filemonitor.service.FileProcessingService;
import com.mercado.filemonitor.service.ProdutoJsonService;
import com.mercado.filemonitor.service.ProdutoMapperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private static final String STATUS = "status";
    private static final String MESSAGE = "message";
    public static final String ERROR = "error";
    private final FileRecordRepository fileRecordRepository;
    private final FileProcessingService fileProcessingService;
    private final FileParserService fileParserService;
    private final ProdutoMapperService produtoMapperService;
    private final ProdutoJsonService produtoJsonService;
    private final FileMonitorConfig fileMonitorConfig;

    @GetMapping
    public ResponseEntity<Page<FileRecord>> getAllFileRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "processedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<FileRecord> records = fileRecordRepository.findAll(pageable);

        return ResponseEntity.ok(records);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileRecord> getFileRecord(@PathVariable Long id) {
        Optional<FileRecord> fileRecord = fileRecordRepository.findById(id);
        return fileRecord.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<FileRecord>> getFileRecordsByStatus(
            @PathVariable FileRecord.ProcessingStatus status) {
        List<FileRecord> records = fileRecordRepository.findByStatus(status);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("total_files", fileRecordRepository.count());
        stats.put("completed", fileRecordRepository.countByStatus(FileRecord.ProcessingStatus.COMPLETED));
        stats.put("processing", fileRecordRepository.countByStatus(FileRecord.ProcessingStatus.PROCESSING));
        stats.put("errors", fileRecordRepository.countByStatus(FileRecord.ProcessingStatus.ERROR));
        stats.put("pending", fileRecordRepository.countByStatus(FileRecord.ProcessingStatus.PENDING));

        // Estatísticas das últimas 24 horas
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        List<FileRecord> recentRecords = fileRecordRepository.findByProcessedAtBetween(yesterday, LocalDateTime.now());
        stats.put("processed_last_24h", recentRecords.size());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put(STATUS, "UP");
        health.put("service", "File Monitor");
        health.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(health);
    }


    // ========== PROCESSAMENTO DE ARQUIVOS ==========

    @PostMapping("/process")
    public ResponseEntity<Map<String, String>> processFile(@RequestParam String filePath) {
        Map<String, String> response = new HashMap<>();

        try {
            Path path = Paths.get(filePath);

            if (!fileProcessingService.shouldProcessFile(path)) {
                response.put(STATUS, "skipped");
                response.put(MESSAGE, "Arquivo já foi processado ou não precisa ser reprocessado");
                return ResponseEntity.ok(response);
            }

            fileProcessingService.processFile(path);

            response.put(STATUS, "processing");
            response.put(MESSAGE, "Processamento iniciado para o arquivo: " + filePath);
            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            log.error("Erro ao processar arquivo manualmente: {}", e.getMessage(), e);
            response.put(STATUS, ERROR);
            response.put(MESSAGE, "Erro ao iniciar processamento: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    // ========== ENDPOINTS DE PRODUTOS ==========

    @GetMapping("/produtos/preview")
    public ResponseEntity<Map<String, Object>> previewProdutos(@RequestParam String filePath) {
        Map<String, Object> response = new HashMap<>();

        try {
            Path path = Paths.get(filePath);

            // Parse do arquivo
            List<PositionalRecord> records = fileParserService.parsePositionalFile(path);

            // Mapeamento para produtos
            List<ProdutoDTO> produtos = produtoMapperService.mapToProdutos(records);

            response.put(STATUS, "success");
            response.put("total_records", records.size());
            response.put("total_produtos", produtos.size());
            response.put("produtos", produtos.size() > 10 ? produtos.subList(0, 10) : produtos); // Primeiros 10
            response.put(MESSAGE, produtos.size() > 10 ? "Mostrando primeiros 10 produtos" : "Todos os produtos");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao gerar preview de produtos: {}", e.getMessage(), e);
            response.put(STATUS, ERROR);
            response.put(MESSAGE, "Erro ao processar arquivo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/produtos/generate")
    public ResponseEntity<Map<String, String>> generateProdutoJson(@RequestParam String filePath) {
        Map<String, String> response = new HashMap<>();

        try {
            Path path = Paths.get(filePath);
            Path outputDir = Paths.get(fileMonitorConfig.getOutputDirectory());
            Path outputDirJson = Paths.get(fileMonitorConfig.getOutputDirectoryJsonProdutos());

            // Parse e mapeamento
            List<PositionalRecord> records = fileParserService.parsePositionalFile(path);
            List<ProdutoDTO> produtos = produtoMapperService.mapToProdutos(records);

            // Gerar JSON de produtos
            Path produtoJsonPath = produtoJsonService.generateProdutoJsonFile(produtos, outputDirJson, path.getFileName().toString());

            response.put(STATUS, "success");
            response.put(MESSAGE, "JSON de produtos gerado com sucesso");
            response.put("output_path", produtoJsonPath.toString());
            response.put("total_produtos", String.valueOf(produtos.size()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao gerar JSON de produtos: {}", e.getMessage(), e);
            response.put(STATUS, ERROR);
            response.put(MESSAGE, "Erro ao gerar JSON: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
