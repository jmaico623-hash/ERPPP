package com.nanobanana.dto;

import com.nanobanana.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FuncionarioRequest(
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    String nome,

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 255, message = "Email deve ter no máximo 255 caracteres")
    String email,

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 4, max = 100, message = "Senha deve ter entre 4 e 100 caracteres")
    String senha,

    @Pattern(regexp = "^[0-9+\\-\\s()]{9,20}$", message = "Telefone inválido")
    @Size(max = 50, message = "Telefone deve ter no máximo 50 caracteres")
    String telefone,

    @NotNull(message = "Função é obrigatória")
    Usuario.Role role
) {}