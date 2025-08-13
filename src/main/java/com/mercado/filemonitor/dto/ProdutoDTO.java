package com.mercado.filemonitor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("nome")
    private String nome;

    @JsonProperty("descricao")
    private String descricao;

    @JsonProperty("preco")
    private BigDecimal preco;

    @JsonProperty("preco_promocao")
    private BigDecimal precoPromocao;

    @JsonProperty("codigo_barras")
    private String codigoBarras;

    @JsonProperty("estoque")
    private Integer estoque;

    @JsonProperty("importado")
    private Boolean importado;

    @JsonProperty("ativo")
    private Boolean ativo;

    @JsonProperty("unidade_medida")
    private String unidadeMedida;

    @JsonProperty("categoria")
    private CategoriaDTO categoria;

    @JsonProperty("imagem")
    private String imagem;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoriaDTO {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("nome")
        private String nome;
    }
}