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

    function atualizarBotao() {
        var btn = document.getElementById('nbThemeToggle');
        if (!btn) return;
        var escuro = temaAtual() === 'dark';
        btn.innerHTML = escuro
            ? '<span aria-hidden="true" style="font-size:20px;line-height:1">☀️</span>'
            : '<span aria-hidden="true" style="font-size:20px;line-height:1">🌙</span>';
        btn.setAttribute('aria-label', escuro ? 'Ativar tema claro' : 'Ativar tema escuro');
        btn.setAttribute('title', escuro ? 'Tema claro' : 'Tema escuro');
    }

    function criarBotao() {
        if (document.getElementById('nbThemeToggle')) return;
        var btn = document.createElement('button');
        btn.id = 'nbThemeToggle';
        btn.type = 'button';
        btn.className = 'nb-theme-toggle';
        btn.addEventListener('click', function () {
            aplicar(temaAtual() === 'dark' ? 'light' : 'dark');
        });
        document.body.appendChild(btn);
        atualizarBotao();
    }

    // Aplica o tema salvo o mais cedo possível (evita "flash" branco)
    try {
        var salvo = localStorage.getItem(STORAGE_KEY);
        if (salvo === 'dark' || salvo === 'light') {
            root.setAttribute('data-theme', salvo);
        } else if (prefereEscuro()) {
            root.setAttribute('data-theme', 'dark');
        }
    } catch (e) { /* ignora */ }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', criarBotao);
    } else {
        criarBotao();
    }
})();
