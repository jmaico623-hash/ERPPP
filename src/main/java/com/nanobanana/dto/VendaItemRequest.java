package com.nanobanana.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Item recebido do PDV ao finalizar uma venda.
 */
public record VendaItemRequest(
        @NotNull(message = "ID do produto é obrigatório")
        UUID produtoId,

        @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser positiva")
        Integer quantidade
) {
    public int quantidadeInt() {
        return quantidade != null ? quantidade : 0;
    }
}
