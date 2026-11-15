/**
 * NanoBanana ERP - Alternador de tema claro/escuro.
 * Independente de outros scripts para evitar conflitos de escopo global.
 */
(function () {
    'use strict';

    var STORAGE_KEY = 'nanobanana-theme';
    var root = document.documentElement;

    function prefereEscuro() {
        return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    }

    function temaAtual() {
        return root.getAttribute('data-theme') === 'dark' ? 'dark' : 'light';
    }

    function aplicar(tema) {
        root.setAttribute('data-theme', tema);
        try {
            localStorage.setItem(STORAGE_KEY, tema);
        } catch (e) { /* armazenamento indisponível */ }
        atualizarBotao();
    }

    function alternar() {
        aplicar(temaAtual() === 'dark' ? 'light' : 'dark');
    }

    function atualizarBotao() {
        var botoes = document.querySelectorAll('#nbThemeToggle, .nb-theme-toggle');
        var escuro = temaAtual() === 'dark';
        Array.prototype.forEach.call(botoes, function (btn) {
            btn.innerHTML = escuro
                ? '<i class="fas fa-sun" aria-hidden="true"></i>'
                : '<i class="fas fa-moon" aria-hidden="true"></i>';
            btn.setAttribute('aria-label', escuro ? 'Ativar tema claro' : 'Ativar tema escuro');
            btn.setAttribute('title', escuro ? 'Tema claro' : 'Tema escuro');
        });
    }

    function iniciar() {
        var botoes = document.querySelectorAll('#nbThemeToggle, .nb-theme-toggle');
        if (botoes.length === 0 && !document.querySelector('.dash-topbar')) {
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'nb-theme-toggle';
            btn.id = 'nbThemeToggle';
            document.body.appendChild(btn);
            botoes = [btn];
        }
        Array.prototype.forEach.call(botoes, function (btn) {
            btn.addEventListener('click', alternar);
        });
        atualizarBotao();
    }

    // Aplica o tema guardado o mais cedo possível (evita "flash" branco)
    try {
        var salvo = localStorage.getItem(STORAGE_KEY);
        if (salvo === 'dark' || salvo === 'light') {
            root.setAttribute('data-theme', salvo);
        } else if (prefereEscuro()) {
            root.setAttribute('data-theme', 'dark');
        }
    } catch (e) { /* ignora */ }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', iniciar);
    } else {
        iniciar();
    }
})();
