package com.mercado.filemonitor.service;

import com.mercado.filemonitor.dto.PositionalRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static com.mercado.filemonitor.util.Constants.*;

@Service
@Slf4j
public class FileParserService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public List<PositionalRecord> parsePositionalFile(Path filePath) throws IOException {
        log.info("Iniciando parse do arquivo: {}", filePath);

        List<String> lines = Files.readAllLines(filePath);
        List<PositionalRecord> records = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            try {
                PositionalRecord record = parseLine(line, i + 1);
                if (record != null) {
                    records.add(record);
                }
            } catch (Exception e) {
                log.error("Erro ao processar linha {} do arquivo {}: {}", i + 1, filePath, e.getMessage());
                throw new RuntimeException("Erro na linha " + (i + 1) + ": " + e.getMessage(), e);
            }
        }

        log.info("Parse concluído. {} registros processados", records.size());
        return records;
    }

    private PositionalRecord parseLine(String line, int lineNumber) {
        if (line == null || line.trim().isEmpty()) {
            log.debug("Linha {} vazia, ignorando", lineNumber);
            return null;
        }

        // Validar tamanho mínimo da linha (agora com campo categoria)
        if (line.length() < 97) {
            throw new IllegalArgumentException("Linha deve ter pelo menos 97 caracteres. Atual: " + line.length());
        }

        // Debug: mostrar campos extraídos
        if (log.isDebugEnabled()) {
            log.debug("Linha {}: '{}'", lineNumber, line);
            log.debug("  TipoProduto: '{}'", extractField(line, INICIO_CAMPO_TIPO_PRODUTO, INICIO_CAMPO_TIPO_PRODUTO + CAMPO_TIPO_PRODUTO));
            log.debug("  Código: '{}'", extractField(line, INICIO_CAMPO_CODIGO, INICIO_CAMPO_CODIGO + CAMPO_CODIGO));
            log.debug("  Nome: '{}'", extractField(line, INICIO_CAMPO_NOME, INICIO_CAMPO_NOME + CAMPO_NOME));
            log.debug("  Valor: '{}'", extractField(line, INICIO_CAMPO_VALOR, INICIO_CAMPO_VALOR + CAMPO_VALOR));
            log.debug("  Categoria: '{}'", extractField(line, INICIO_CAMPO_CATEGORIA, INICIO_CAMPO_CATEGORIA + CAMPO_CATEGORIA));
            log.debug("  DiasValidade: '{}'", extractField(line, INICIO_CAMPO_DIAS_VALIDADE, INICIO_CAMPO_DIAS_VALIDADE + CAMPO_DIAS_VALIDADE));
            log.debug("  Obs: '{}'", line.length() > INICIO_CAMPO_OBS ? extractField(line, INICIO_CAMPO_OBS, Math.min(line.length(), INICIO_CAMPO_OBS + CAMPO_OBS)) : "");
       }

        try {
            return PositionalRecord.builder()
                    .codigo(extractField(line, INICIO_CAMPO_CODIGO, INICIO_CAMPO_CODIGO + CAMPO_CODIGO).trim())           // pos 1-10
                    .nome(extractField(line, INICIO_CAMPO_NOME, INICIO_CAMPO_NOME + CAMPO_NOME).trim())            // pos 11-50
                    .categoria(extractField(line, INICIO_CAMPO_CATEGORIA, INICIO_CAMPO_CATEGORIA + CAMPO_CATEGORIA).trim())       // pos 51-60 (NOVO)
                    .valor(parseDecimal(extractField(line, INICIO_CAMPO_VALOR, INICIO_CAMPO_VALOR + CAMPO_VALOR).trim()))       // pos 80-95 (AJUSTADO)
                    .tipoProduto(extractField(line, INICIO_CAMPO_TIPO_PRODUTO, INICIO_CAMPO_TIPO_PRODUTO + CAMPO_TIPO_PRODUTO).trim())          // pos 96-97 (AJUSTADO)
                    .observacoes(line.length() > INICIO_CAMPO_OBS ? extractField(line, INICIO_CAMPO_OBS, Math.min(line.length(), INICIO_CAMPO_OBS + CAMPO_OBS)).trim() : "") // pos 98-160 (AJUSTADO)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao parsear campos da linha: " + e.getMessage(), e);
        }
    }

    private String extractField(String line, int start, int end) {
        if (start >= line.length()) {
            return "";
        }
        end = Math.min(end, line.length());
        return line.substring(start, end);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida: " + dateStr + ". Formato esperado: yyyyMMdd");
        }
    }

    private BigDecimal parseDecimal(String valueStr) {
        if (valueStr.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            // Remove espaços e converte assumindo 2 casas decimais implícitas
            String cleanValue = valueStr.replaceAll("\\s", "");
            if (cleanValue.isEmpty()) {
                return BigDecimal.ZERO;
            }

            // Se não tem ponto decimal, assume que os 2 últimos dígitos são decimais
            if (!cleanValue.contains(".") && cleanValue.length() > 2) {
                int len = cleanValue.length();
                cleanValue = cleanValue.substring(0, len - 2) + "." + cleanValue.substring(len - 2);
            }

            return new BigDecimal(cleanValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor numérico inválido: " + valueStr);
        }
    }
}
