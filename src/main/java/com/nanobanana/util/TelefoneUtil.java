package com.nanobanana.util;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Normalização de números de telefone para login/registo.
 *
 * O utilizador pode escrever o número de várias formas:
 *   "84 555 0001", "+258 84 555 0001", "00258 845550001", "845550001"
 * Todas devem identificar a mesma conta. Guardamos apenas dígitos e, na
 * pesquisa, testamos também a variante sem o indicativo do país.
 */
public final class TelefoneUtil {

    private TelefoneUtil() {
    }

    /** Indicativos internacionais dos países lusófonos. */
    private static final String[] CODIGOS_PAIS = {
            "258", // Moçambique
            "244", // Angola
            "238", // Cabo Verde
            "245", // Guiné-Bissau
            "239", // São Tomé e Príncipe
            "670", // Timor-Leste
            "351", // Portugal
            "55"   // Brasil
    };

    /**
     * Devolve o número apenas com dígitos, removendo o prefixo internacional
     * "00" quando presente. O indicativo do país é mantido (ex.: 258845550001)
     * para não alterar o número que o utilizador indicou.
     */
    public static String normalizar(String telefone) {
        if (telefone == null) {
            return "";
        }
        String digitos = telefone.replaceAll("\\D", "");
        while (digitos.startsWith("00") && digitos.length() > 2) {
            digitos = digitos.substring(2);
        }
        return digitos;
    }

    /**
     * Variantes de pesquisa para um telefone. Inclui o número normalizado, a
     * versão sem indicativo do país e a versão com indicativo (para quem
     * registou o número local e tenta entrar com o indicativo).
     */
    public static Set<String> variantes(String telefone) {
        Set<String> variantes = new LinkedHashSet<>();
        String digitos = normalizar(telefone);
        if (digitos.isEmpty()) {
            return variantes;
        }
        variantes.add(digitos);

        for (String codigo : CODIGOS_PAIS) {
            if (digitos.startsWith(codigo)) {
                String nacional = digitos.substring(codigo.length());
                if (tamanhoNacionalValido(nacional) && !(codigo.equals("55") && nacional.length() < 10)) {
                    // Brasil tem sempre 10/11 dígitos (DDD + número). O código "55"
                    // não deve ser removido de um número nacional que começa por 55.
                    variantes.add(nacional);
                }
            }
        }

        if (tamanhoNacionalValido(digitos)) {
            for (String codigo : CODIGOS_PAIS) {
                variantes.add(codigo + digitos);
            }
        }
        return variantes;
    }

    /** Considera válido um número nacional entre 7 e 11 dígitos. */
    private static boolean tamanhoNacionalValido(String numero) {
        return numero != null && numero.length() >= 7 && numero.length() <= 11;
    }
}
