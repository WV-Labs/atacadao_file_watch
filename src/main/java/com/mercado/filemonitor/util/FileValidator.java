package com.mercado.filemonitor.util;

import static com.mercado.filemonitor.util.Constants.*;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FileValidator {

  public static class ValidationResult {
        public boolean valid;
        public List<String> errors = new ArrayList<>();
        public String lineContent;
        public int lineNumber;

        public ValidationResult(int lineNumber, String lineContent) {
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
            this.valid = true;
        }

        public void addError(String error) {
            this.valid = false;
            this.errors.add(error);
        }
    }

    public ValidationResult validateLine(String line, int lineNumber) {
        ValidationResult result = new ValidationResult(lineNumber, line);

        if (line == null || line.trim().isEmpty()) {
            result.addError("Linha vazia");
            return result;
        }

        // Validar tamanho mínimo
        if (line.length() < INICIO_CAMPO_OBS) {
            result.addError("Linha deve ter pelo menos 97 caracteres. Atual: " + line.length());
            return result;
        }

        try {
            // Validar cada campo
            validateCodigo(extractField(line, INICIO_CAMPO_CODIGO, CAMPO_CODIGO), result);
            validateNome(extractField(line, INICIO_CAMPO_NOME, CAMPO_NOME), result);
            validateCategoria(extractField(line, INICIO_CAMPO_CATEGORIA, CAMPO_CATEGORIA), result);
            validateValor(extractField(line, INICIO_CAMPO_VALOR, CAMPO_VALOR), result);
            validateTipoProduto(extractField(line, INICIO_CAMPO_TIPO_PRODUTO, CAMPO_TIPO_PRODUTO), result);
            validateNumero(extractField(line, INICIO_CAMPO_DIAS_VALIDADE, CAMPO_DIAS_VALIDADE), result);
            // Observações são opcionais, não validamos

        } catch (Exception e) {
            result.addError("Erro geral na validação: " + e.getMessage());
        }

        return result;
    }

    private String extractField(String line, int start, int end) {
        if (start >= line.length()) {
            return "";
        }
        end = Math.min(end, line.length());
        return line.substring(start, end);
    }

    private void validateCodigo(String codigo, ValidationResult result) {
        if (codigo.trim().isEmpty()) {
            result.addError("Código não pode estar vazio");
        }
        if (codigo.length() != CAMPO_CODIGO) {
            result.addError("Código deve ter exatamente 10 caracteres");
        }
    }

    private void validateNome(String nome, ValidationResult result) {
        if (nome.trim().isEmpty()) {
            result.addError("Nome não pode estar vazio");
        }
        if (nome.length() != CAMPO_NOME) {
            result.addError("Nome deve ter exatamente 40 caracteres (com padding)");
        }
    }

    private void validateCategoria(String categoria, ValidationResult result) {
        if (categoria.trim().isEmpty()) {
            result.addError("Categoria não pode estar vazia");
        }
        if (categoria.length() != CAMPO_CATEGORIA) {
            result.addError("Categoria deve ter exatamente 10 caracteres");
        }
    }

    private void validateValor(String valor, ValidationResult result) {
        String valorLimpo = valor.trim();
        if (valorLimpo.isEmpty()) {
            result.addError("Valor não pode estar vazio");
        }
        if (valor.length() != CAMPO_VALOR) {
            result.addError("Valor deve ter exatamente 16 caracteres");
        }
        if (!valorLimpo.matches("\\d+")) {
            result.addError("Valor deve conter apenas números");
        }
    }

    private void validateTipoProduto(String tipoProduto, ValidationResult result) {
        if (tipoProduto.trim().isEmpty()) {
            result.addError("Tipo Produto não pode estar vazio");
        }
        if (tipoProduto.length() != CAMPO_TIPO_PRODUTO) {
            result.addError("Tipo Produto deve ter exatamente 1 caracteres");
        }
    }

    private void validateNumero(String dias, ValidationResult result) {
        if (!dias.trim().isEmpty()) {
            try {
                Integer.parseInt(dias);
            } catch (NumberFormatException e) {
                try {
                    Double.parseDouble(dias);
                } catch (NumberFormatException e2) {
                    try {
                        Long.parseLong(dias);
                    } catch (NumberFormatException e3) {
                        result.addError("Dias validade inválido");
                    }
                }
            }
        }else{
            result.addError("Dias validade não pode estar vazio");
        }
    }

    public void logValidationResult(ValidationResult result) {
        if (result.valid) {
            log.info("Linha {} válida", result.lineNumber);
        } else {
            log.error("Linha {} inválida:", result.lineNumber);
            for (String error : result.errors) {
                log.error("  - {}", error);
            }
            log.error("  Conteúdo: '{}'", result.lineContent);
        }
    }
}
