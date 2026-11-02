package com.nanobanana.dto;

import com.nanobanana.model.Venda;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record VendaRequest(
    @NotNull(message = "Itens são obrigatórios")
    @Size(min = 1, message = "Pelo menos 1 item é obrigatório")
    List<VendaItemRequest> itens,

    @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Desconto inválido")
    BigDecimal desconto,

    @NotNull(message = "Método de pagamento é obrigatório")
    Venda.MetodoPagamento metodoPagamento,

    @DecimalMin(value = "0.00", message = "Valor pago não pode ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Valor pago inválido")
    BigDecimal valorPago,

    @Size(max = 255, message = "Nome do cliente (fiado) deve ter no máximo 255 caracteres")
    String nomeClienteFiado,

    @Pattern(regexp = "^[0-9+\\-\\s()]{9,20}$", message = "Telefone inválido")
    @Size(max = 50, message = "Telefone deve ter no máximo 50 caracteres")
    String telefoneClienteFiado
) {}