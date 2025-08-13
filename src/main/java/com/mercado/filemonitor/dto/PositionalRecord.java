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
public class PositionalRecord {

    @JsonProperty("codigo")
    private String codigo;           // Posição 5-11

    @JsonProperty("nome")
    private String nome;             // Posição 20-45

    @JsonProperty("categoria")
    private String categoria;        // Posição 0-2

    @JsonProperty("valor")
    private BigDecimal valor;        // Posição 11-17

    @JsonProperty("dias_validade")
    private int diasValidade;        // Posição 17-20

    @JsonProperty("tipo_Produto")
    private String tipoProduto;      // Posição 4-5

    @JsonProperty("observacoes")
    private String observacoes;      // Posição 120-170
}
