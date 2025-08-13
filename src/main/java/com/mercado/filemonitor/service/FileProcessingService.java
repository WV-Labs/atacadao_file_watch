package com.mercado.filemonitor.service;

import com.mercado.filemonitor.config.ClientConfig;
import com.mercado.filemonitor.config.FileMonitorConfig;
import com.mercado.filemonitor.dto.PositionalRecord;
import com.mercado.filemonitor.dto.ProdutoDTO;
import com.mercado.filemonitor.entity.FileRecord;
import com.mercado.filemonitor.repository.FileRecordRepository;
import com.mercado.filemonitor.util.ProdutoWebClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileProcessingService {
  private final FileParserService fileParserService;
  private final JsonGeneratorService jsonGeneratorService;
  private final FileRecordRepository fileRecordRepository;
  private final ProdutoMapperService produtoMapperService;
  private final ProdutoJsonService produtoJsonService;
  private final FileMonitorConfig config;
  private final ClientConfig clientConfig;

  @Async
  @Transactional
  public void processFile(Path filePath) {
    log.info("Iniciando processamento do arquivo: {}", filePath);

    FileRecord fileRecord = createFileRecord(filePath);
    fileRecord.setStatus(FileRecord.ProcessingStatus.PROCESSING);
    fileRecord = fileRecordRepository.save(fileRecord);

    try {
      // Parse do arquivo posicional
      List<PositionalRecord> records = fileParserService.parsePositionalFile(filePath);

      // Geração do JSON
      Path outputDir = Paths.get(config.getOutputDirectory());
      Path jsonPath =
          jsonGeneratorService.generateJsonFile(
              records, outputDir, filePath.getFileName().toString());

      // Mapeamento para produtos
      List<ProdutoDTO> produtos = produtoMapperService.mapToProdutos(records);

      // Geração do JSON de produtos
      Path outputDirJson = Paths.get(config.getOutputDirectoryJsonProdutos());
      Path produtoJsonPath =
          produtoJsonService.generateProdutoJsonFile(
              produtos, outputDirJson, filePath.getFileName().toString());

      // Atualizar registro de sucesso
      fileRecord.setStatus(FileRecord.ProcessingStatus.COMPLETED);
      fileRecord.setProcessedAt(LocalDateTime.now());
      fileRecord.setOutputPath(jsonPath.toString() + "; " + produtoJsonPath.toString());
      fileRecord.setRecordsCount(records.size());
      fileRecord.setErrorMessage(null);

      log.info("Arquivo processado com sucesso: ");
      log.info("  - Entrada: {}", filePath);
      log.info("  - JSON Original: {}", jsonPath);
      log.info("  - JSON Produtos: {}", produtoJsonPath);
      log.info("  - Registros: {}", records.size());

      try {
        if (produtos.size() > 0) {
          log.info("  Chamando o adm para atualizar BD com {} registros", produtos.size());
          ProdutoWebClient webClient = new ProdutoWebClient(clientConfig);
          webClient.enviarProdutos(produtos);
        }
      } catch (WebClientRequestException e) {
        log.error("Erro ao executar o chamado remoto para o adm {}", e.getMessage(), e);
        // Atualizar registro de erro
        fileRecord.setStatus(FileRecord.ProcessingStatus.ERROR);
        fileRecord.setProcessedAt(LocalDateTime.now());
        fileRecord.setErrorMessage(e.getMessage());
      } catch (Exception e) {
        log.error("Erro no adm remoto{}", e.getMessage(), e);
        // Atualizar registro de erro
        fileRecord.setStatus(FileRecord.ProcessingStatus.ERROR);
        fileRecord.setProcessedAt(LocalDateTime.now());
        fileRecord.setErrorMessage(e.getMessage());
      }
    } catch (Exception e) {
      log.error("Erro ao processar arquivo {}: {}", filePath, e.getMessage(), e);

      // Atualizar registro de erro
      fileRecord.setStatus(FileRecord.ProcessingStatus.ERROR);
      fileRecord.setProcessedAt(LocalDateTime.now());
      fileRecord.setErrorMessage(e.getMessage());
    } finally {
      fileRecordRepository.save(fileRecord);
    }
  }

  private FileRecord createFileRecord(Path filePath) {
    FileRecord record = new FileRecord();
    record.setFileName(filePath.getFileName().toString());
    record.setFilePath(filePath.toString());
    record.setStatus(FileRecord.ProcessingStatus.PENDING);

    try {
      record.setFileSize(Files.size(filePath));
      record.setLastModified(
          LocalDateTime.ofInstant(
              Files.getLastModifiedTime(filePath).toInstant(), ZoneId.systemDefault()));
    } catch (IOException e) {
      log.warn("Erro ao obter informações do arquivo {}: {}", filePath, e.getMessage());
    }

    return record;
  }

  public boolean shouldProcessFile(Path filePath) {
    try {
      LocalDateTime lastModified =
          LocalDateTime.ofInstant(
              Files.getLastModifiedTime(filePath).toInstant(), ZoneId.systemDefault());

      return fileRecordRepository
          .findByFilePathAndLastModified(filePath.toString(), lastModified)
          .isEmpty();
    } catch (IOException e) {
      log.error("Erro ao verificar se arquivo deve ser processado: {}", e.getMessage());
      return false;
    }
  }
}
