package com.nanobanana.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProdutoRequest(
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    String nome,

    @Size(max = 100, message = "Categoria deve ter no máximo 100 caracteres")
    String categoria,

    @Size(max = 50, message = "Código de barras deve ter no máximo 50 caracteres")
    String codigoBarras,

    @NotNull(message = "Preço de venda é obrigatório")
    @DecimalMin(value = "0.01", message = "Preço de venda deve ser maior que zero")
    @Digits(integer = 8, fraction = 2, message = "Preço de venda inválido (máx 8 dígitos inteiros, 2 decimais)")
    BigDecimal precoVenda,

    @DecimalMin(value = "0.00", message = "Preço de custo não pode ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Preço de custo inválido")
    BigDecimal precoCusto,

    @NotNull(message = "Stock mínimo é obrigatório")
    @Positive(message = "Stock mínimo deve ser positivo")
    Integer stockMinimo,

    @Positive(message = "Stock máximo deve ser positivo")
    Integer stockMaximo
) {}