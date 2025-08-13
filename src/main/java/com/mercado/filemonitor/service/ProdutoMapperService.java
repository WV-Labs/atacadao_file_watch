package com.mercado.filemonitor.service;

import com.mercado.filemonitor.dto.PositionalRecord;
import com.mercado.filemonitor.dto.ProdutoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProdutoMapperService {

    /**
     * Mapeia registros posicionais para DTOs de Produto
     * Regras de mapeamento:
     * - produto.id = positional.codigo
     * - produto.nome = positional.nome
     * - produto.categoria.nome = positional.categoria
     * - produto.preco = positional.valor
     */
    public List<ProdutoDTO> mapToProdutos(List<PositionalRecord> records) {
        log.info("Iniciando mapeamento de {} registros posicionais para produtos", records.size());

        List<ProdutoDTO> produtos = records.stream()
                .map(this::mapToProduto)
                .collect(Collectors.toList());

        log.info("Mapeamento concluído. {} produtos gerados", produtos.size());
        return produtos;
    }

    /**
     * Mapeia um registro posicional individual para ProdutoDTO
     */
    private ProdutoDTO mapToProduto(PositionalRecord record) {
        try {
            return ProdutoDTO.builder()
                    // Regra: produto.id = positional.codigo
                    .id(parseCodigoToId(record.getCodigo()))

                    // Regra: produto.nome = positional.nome
                    .nome(cleanAndValidateName(record.getNome()))

                    // Descrição padrão baseada no nome
                    .descricao(generateDescription(record.getNome(), record.getCategoria()))

                    // Regra: produto.preco = positional.valor
                    .preco(record.getValor())

                    // Campos padrão
                    .precoPromocao(BigDecimal.ZERO)
                    .codigoBarras(generateCodigoBarras(record.getCodigo()))
                    .estoque(0)
                    .importado(false)
                    .ativo(true)
                    .unidadeMedida("X") // Unidade padrão

                    // Regra: produto.categoria.nome = positional.categoria
                    .categoria(mapToCategoria(record.getCategoria()))

                    // Imagem padrão
                    .imagem("")

                    .build();

        } catch (Exception e) {
            log.error("Erro ao mapear registro posicional para produto: {}", e.getMessage());
            throw new RuntimeException("Erro no mapeamento do produto com código: " + record.getCodigo(), e);
        }
    }

    /**
     * Converte código string para ID Long
     */
    private Long parseCodigoToId(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("Código não pode estar vazio");
        }

        try {
            // Remove zeros à esquerda e converte para Long
            String codigoLimpo = codigo.trim().replaceFirst("^0+", "");
            if (codigoLimpo.isEmpty()) {
                codigoLimpo = "0";
            }
            return Long.parseLong(codigoLimpo);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Código inválido para conversão em ID: " + codigo);
        }
    }

    /**
     * Limpa e valida o nome do produto
     */
    private String cleanAndValidateName(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do produto não pode estar vazio");
        }

        String nomeLimpo = nome.trim();

        // Validar tamanho máximo (conforme Produto.java)
        if (nomeLimpo.length() > 50) {
            log.warn("Nome do produto truncado de {} para 50 caracteres: {}", nomeLimpo.length(), nomeLimpo);
            nomeLimpo = nomeLimpo.substring(0, 50);
        }

        return nomeLimpo;
    }

    /**
     * Gera descrição baseada no nome e categoria
     */
    private String generateDescription(String nome, String categoria) {
/*
        String descricao = String.format("%s - %s", nome != null ? nome.trim() : "Produto",
                categoria != null ? categoria.trim() : "Sem categoria");
*/
        String descricao = String.format("%s", nome != null ? nome.trim() : "Produto");

        // Validar tamanho máximo (conforme Produto.java)
        if (descricao.length() > 100) {
            descricao = descricao.substring(0, 100);
        }

        return descricao;
    }

    /**
     * Gera código de barras baseado no código
     */
    private String generateCodigoBarras(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return "";
        }
        String codigoAux = codigo.trim().replaceFirst("^0+", "");
        // Gera um código de barras simples baseado no código
        return "789" + codigoAux.substring(0, Math.min(10, codigoAux.length()));
    }

    /**
     * Mapeia categoria string para CategoriaDTO
     */
    private ProdutoDTO.CategoriaDTO mapToCategoria(String categoriaNome) {
        if (categoriaNome == null || categoriaNome.trim().isEmpty()) {
            return ProdutoDTO.CategoriaDTO.builder()
                    .id(1L) // ID padrão para categoria "Geral"
                    .nome("Geral")
                    .build();
        }

        return ProdutoDTO.CategoriaDTO.builder()
                .id(generateCategoriaId(categoriaNome))
                .nome(categoriaNome.trim())
                .build();
    }

    /**
     * Gera ID da categoria baseado no nome
     */
    private Long generateCategoriaId(String categoriaNome) {
        if (categoriaNome == null || categoriaNome.trim().isEmpty()) {
            return 1L;
        }

        // Gera um ID simples baseado no hash do nome da categoria
        return Math.abs((long) categoriaNome.trim().hashCode()) % 1000L + 1L;
    }
}
