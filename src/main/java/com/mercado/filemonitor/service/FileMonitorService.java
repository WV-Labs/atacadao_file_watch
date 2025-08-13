package com.mercado.filemonitor.service;

import com.mercado.filemonitor.config.FileMonitorConfig;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileMonitorService {
    private final FileMonitorConfig config;
    private final FileProcessingService fileProcessingService;

    private WatchService watchService;
    private final Set<String> processingFiles = ConcurrentHashMap.newKeySet();

    @EventListener(ApplicationReadyEvent.class)
    public void initializeFileMonitoring() {
        try {
            setupDirectories();
            startWatchService();
            scanExistingFiles();
            log.info("Monitoramento de arquivos iniciado para: {}", config.getInputDirectory());
        } catch (Exception e) {
            log.error("Erro ao inicializar monitoramento de arquivos: {}", e.getMessage(), e);
        }
    }

    private void setupDirectories() throws IOException {
        Path inputDir = Paths.get(config.getInputDirectory());
        Path outputDir = Paths.get(config.getOutputDirectory());

        Files.createDirectories(inputDir);
        Files.createDirectories(outputDir);

        log.info("Diretórios configurados - Input: {}, Output: {}", inputDir, outputDir);
    }

    private void startWatchService() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        Path inputDir = Paths.get(config.getInputDirectory());

        inputDir.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY);

    }

    @Scheduled(fixedDelayString = "${file.monitor.polling-interval:5000}")
    public void pollForFileChanges() {
        if (watchService == null) {
            return;
        }

        WatchKey key = watchService.poll();
        if (key != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path filename = ev.context();
                Path filePath = Paths.get(config.getInputDirectory()).resolve(filename);

                if (shouldProcessFile(filePath)) {
                    processFileAsync(filePath);
                }
            }

            key.reset();
        }
    }

    private void scanExistingFiles() {
        try {
            Path inputDir = Paths.get(config.getInputDirectory());
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + config.getFilePattern());

            Files.walkFileTree(inputDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (matcher.matches(file.getFileName()) && shouldProcessFile(file)) {
                        log.info("Arquivo existente encontrado: {}", file);
                        processFileAsync(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Erro ao escanear arquivos existentes: {}", e.getMessage(), e);
        }
    }

    private boolean shouldProcessFile(Path filePath) {
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            return false;
        }

        String fileName = filePath.getFileName().toString();
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + config.getFilePattern());

        if (!matcher.matches(filePath.getFileName())) {
            return false;
        }

        // Evitar processamento duplo
        if (processingFiles.contains(fileName)) {
            log.debug("Arquivo {} já está sendo processado", fileName);
            return false;
        }

        // Verificar se o arquivo deve ser processado baseado no histórico
        return fileProcessingService.shouldProcessFile(filePath);
    }

    private void processFileAsync(Path filePath) {
        String fileName = filePath.getFileName().toString();

        if (processingFiles.add(fileName)) {
            log.info("Iniciando processamento assíncrono do arquivo: {}", filePath);

            try {
                fileProcessingService.processFile(filePath);
            } finally {
                processingFiles.remove(fileName);
            }
        }
    }
}
