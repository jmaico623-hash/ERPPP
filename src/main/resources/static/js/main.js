/**
 * NanoBanana ERP - Main JavaScript
 * Funcionalidades globais compartilhadas entre páginas
 */

// ========================================
// NAMESPACE GLOBAL
// ========================================
window.NanoBanana = window.NanoBanana || {
    apiBase: '',
    tenantId: null,
    usuarioId: null,
    usuarioRole: 'VENDEDOR',
    csrfToken: '',
    csrfHeader: 'X-CSRF-TOKEN'
};

// ========================================
// UTILITÁRIOS
// ========================================

/** Seleção de elementos */
const $ = (sel, ctx = document) => ctx.querySelector(sel);
const $$ = (sel, ctx = document) => Array.from(ctx.querySelectorAll(sel));

/** Formatação de moeda MZN */
function formatMZN(value) {
    if (value == null) return '0,00 MZN';
    const num = typeof value === 'string' ? parseFloat(value.replace(/[^\d.,]/g, '').replace(',', '.')) : value;
    return new Intl.NumberFormat('pt-MZ', {
        style: 'currency',
        currency: 'MZN',
        minimumFractionDigits: 2
    }).format(num);
}

/** Parse de string MZN para número */
function parseMZN(str) {
    if (typeof str === 'number') return isFinite(str) ? str : 0;
    if (str == null) return 0;
    let s = String(str).replace(/[^\d.,-]/g, '').trim();
    if (!s) return 0;
    const temVirgula = s.includes(',');
    const temPonto = s.includes('.');
    if (temVirgula && temPonto) {
        s = s.replace(/\./g, '').replace(',', '.');
    } else if (temVirgula) {
        s = s.replace(',', '.');
    }
    const n = parseFloat(s);
    return isNaN(n) ? 0 : n;
}

/** Formatação de data */
function formatDate(date, options = {}) {
    const defaultOptions = { day: '2-digit', month: '2-digit', year: 'numeric' };
    return new Intl.DateTimeFormat('pt-MZ', { ...defaultOptions, ...options }).format(new Date(date));
}

/** Formatação de data/hora */
function formatDateTime(date) {
    return formatDate(date, { hour: '2-digit', minute: '2-digit' });
}

/** Debounce */
function debounce(fn, delay = 300) {
    let timeoutId;
    return (...args) => {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => fn(...args), delay);
    };
}

/** Throttle */
function throttle(fn, limit = 300) {
    let inThrottle;
    return (...args) => {
        if (!inThrottle) {
            fn(...args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

/** Gera UUID v4 */
function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

/** Sanitiza HTML para prevenir XSS */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/** Copia texto para clipboard */
async function copyToClipboard(text) {
    try {
        await navigator.clipboard.writeText(text);
        return true;
    } catch (e) {
        // Fallback
        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
        return true;
    }
}

// ========================================
// TOAST NOTIFICATIONS
// ========================================

/** Mostra toast notification */
function showToast(message, type = 'success', duration = 4000) {
    const container = $('#alertsContainer') || createToastContainer();
    
    const icons = { 
        success: 'fa-check-circle', 
        error: 'fa-exclamation-circle', 
        warning: 'fa-exclamation-triangle', 
        info: 'fa-info-circle' 
    };
    const classes = { 
        success: 'alert-success', 
        error: 'alert-danger', 
        warning: 'alert-warning', 
        info: 'alert-info' 
    };
    
    const toast = document.createElement('div');
    toast.className = `alert ${classes[type] || 'alert-success'} alert-dismissible`;
    toast.role = 'alert';
    toast.innerHTML = `
        <i class="fas ${icons[type] || icons.success}" aria-hidden="true"></i>
        <span>${escapeHtml(message)}</span>
        <button type="button" class="alert-close" aria-label="Fechar" onclick="this.parentElement.remove()">
            <i class="fas fa-times" aria-hidden="true"></i>
        </button>
    `;
    
    container.appendChild(toast);
    
    // Auto-remove
    setTimeout(() => {
        if (toast.parentElement) toast.remove();
    }, duration);
    
    return toast;
}

function createToastContainer() {
    const container = document.createElement('div');
    container.id = 'alertsContainer';
    container.className = 'alerts-container';
    container.setAttribute('role', 'region');
    container.setAttribute('aria-label', 'Mensagens do sistema');
    container.setAttribute('aria-live', 'polite');
    document.body.appendChild(container);
    return container;
}

/** Mostra toast de sucesso */
function showSuccess(message, duration) { return showToast(message, 'success', duration); }
/** Mostra toast de erro */
function showError(message, duration) { return showToast(message, 'error', duration); }
/** Mostra toast de aviso */
function showWarning(message, duration) { return showToast(message, 'warning', duration); }
/** Mostra toast de info */
function showInfo(message, duration) { return showToast(message, 'info', duration); }

// ========================================
// LOADING STATES
// ========================================

/** Define estado de loading em botão */
function setButtonLoading(btn, loading, originalText = '') {
    if (!btn) return;
    
    if (loading) {
        btn.dataset.originalHtml = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Carregando...';
    } else {
        btn.disabled = false;
        btn.innerHTML = btn.dataset.originalHtml || originalText;
    }
}

/** Define estado de loading em formulário */
function setFormLoading(form, loading) {
    if (!form) return;
    
    const inputs = form.querySelectorAll('input, select, textarea, button');
    inputs.forEach(el => {
        if (loading) {
            el.dataset.originalDisabled = el.disabled;
            el.disabled = true;
        } else {
            el.disabled = el.dataset.originalDisabled === 'true';
        }
    });
    
    if (loading) {
        form.classList.add('loading');
    } else {
        form.classList.remove('loading');
    }
}

// ========================================
// CONFIRMAÇÕES
// ========================================

/** Confirmação nativa */
function confirmAction(message, onConfirm, onCancel = () => {}) {
    if (window.confirm(message)) {
        onConfirm();
    } else {
        onCancel();
    }
}

/** Confirmação com modal customizado (opcional) */
function confirmModal(options) {
    // Implementar se necessário
    return confirmAction(options.message, options.onConfirm, options.onCancel);
}

// ========================================
// API FETCH COM CSRF
// ========================================

/** Fetch wrapper com CSRF e tratamento de erros */
async function apiFetch(url, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        'X-Requested-With': 'XMLHttpRequest',
        [NanoBanana.csrfHeader]: NanoBanana.csrfToken,
        ...options.headers
    };
    
    const response = await fetch(url, { 
        ...options, 
        headers, 
        credentials: 'same-origin' 
    });
    
    // Tratamento de status HTTP
    if (response.status === 401) {
        showError('Sessão expirada. Redirecionando para login...');
        setTimeout(() => window.location.href = '/login?expired=true', 1500);
        throw new Error('Unauthorized');
    }
    
    if (response.status === 403) {
        showError('Você não tem permissão para esta ação');
        throw new Error('Forbidden');
    }
    
    if (response.status === 422) {
        const data = await response.json().catch(() => ({}));
        if (data.errors) {
            const msg = Object.values(data.errors).flat().join(', ');
            showError(msg);
        } else {
            showError('Dados inválidos');
        }
        throw new Error('Validation failed');
    }
    
    if (!response.ok) {
        const text = await response.text().catch(() => '');
        throw new Error(`HTTP ${response.status}: ${text}`);
    }
    
    return response;
}

/** GET request */
async function apiGet(url) {
    return apiFetch(url, { method: 'GET' });
}

/** POST request */
async function apiPost(url, data) {
    return apiFetch(url, { 
        method: 'POST', 
        body: JSON.stringify(data) 
    });
}

/** PUT request */
async function apiPut(url, data) {
    return apiFetch(url, { 
        method: 'PUT', 
        body: JSON.stringify(data) 
    });
}

/** DELETE request */
async function apiDelete(url) {
    return apiFetch(url, { method: 'DELETE' });
}

// ========================================
// FORMULÁRIOS
// ========================================

/** Serializa formulário para objeto */
function serializeForm(form) {
    const data = {};
    const formData = new FormData(form);
    
    for (const [key, value] of formData.entries()) {
        if (data[key]) {
            if (!Array.isArray(data[key])) data[key] = [data[key]];
            data[key].push(value);
        } else {
            data[key] = value;
        }
    }
    
    return data;
}

/** Preenche formulário com dados */
function populateForm(form, data) {
    if (!form || !data) return;
    
    Object.entries(data).forEach(([key, value]) => {
        const field = form.querySelector(`[name="${key}"]`);
        if (!field) return;
        
        if (field.type === 'checkbox') {
            field.checked = Boolean(value);
        } else if (field.type === 'radio') {
            const radio = form.querySelector(`[name="${key}"][value="${value}"]`);
            if (radio) radio.checked = true;
        } else if (field.tagName === 'SELECT' && field.multiple) {
            Array.from(field.options).forEach(opt => {
                opt.selected = Array.isArray(value) ? value.includes(opt.value) : opt.value === value;
            });
        } else {
            field.value = value == null ? '' : value;
        }
    });
}

/** Valida formulário nativamente */
function validateForm(form) {
    return form.checkValidity();
}

/** Mostra erros de validação */
function showFormErrors(form, errors) {
    // Limpa erros anteriores
    form.querySelectorAll('.form-error, .is-invalid').forEach(el => {
        el.classList.remove('is-invalid');
        if (el.classList.contains('form-error')) el.remove();
    });
    
    // Adiciona novos erros
    Object.entries(errors).forEach(([field, messages]) => {
        const input = form.querySelector(`[name="${field}"]`);
        if (!input) return;
        
        input.classList.add('is-invalid');
        
        const errorDiv = document.createElement('div');
        errorDiv.className = 'form-error';
        errorDiv.textContent = Array.isArray(messages) ? messages[0] : messages;
        input.parentNode.appendChild(errorDiv);
    });
    
    // Foca no primeiro campo com erro
    const firstInvalid = form.querySelector('.is-invalid');
    if (firstInvalid) firstInvalid.focus();
}

// ========================================
// MODAIS
// ========================================

/** Abre modal */
function openModal(modalId) {
    const modal = $(`#${modalId}`);
    if (!modal) return;
    
    modal.classList.add('show');
    modal.querySelector('.modal-overlay')?.classList.add('show');
    document.body.style.overflow = 'hidden';
    
    // Foca no primeiro elemento focável
    const focusable = modal.querySelector('button, input, select, textarea, [href], [tabindex]:not([tabindex="-1"])');
    focusable?.focus();
    
    // Trap focus
    modal.dataset.lastFocus = document.activeElement?.id || '';
}

/** Fecha modal */
function closeModal(modalId) {
    const modal = $(`#${modalId}`);
    if (!modal) return;
    
    modal.classList.remove('show');
    modal.querySelector('.modal-overlay')?.classList.remove('show');
    document.body.style.overflow = '';
    
    // Restaura foco
    if (modal.dataset.lastFocus) {
        const el = $(`#${modal.dataset.lastFocus}`);
        el?.focus();
    }
}

/** Fecha modal ao clicar no overlay */
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-overlay')) {
        const modal = e.target.closest('.modal');
        if (modal) closeModal(modal.id);
    }
});

/** Fecha modal com ESC */
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        const openModal = $('.modal.show');
        if (openModal) closeModal(openModal.id);
    }
});

// ========================================
// TABELAS - ORDENAÇÃO E FILTRO
// ========================================

/** Inicializa tabela ordenável */
function initSortableTable(tableId, options = {}) {
    const table = $(`#${tableId}`);
    if (!table) return;
    
    const headers = table.querySelectorAll('th[data-sort]');
    
    headers.forEach(th => {
        th.style.cursor = 'pointer';
        th.addEventListener('click', () => {
            const column = th.dataset.sort;
            const currentDir = th.dataset.sortDir || 'asc';
            const newDir = currentDir === 'asc' ? 'desc' : 'asc';
            
            // Atualiza UI
            headers.forEach(h => { h.dataset.sortDir = ''; h.classList.remove('sort-asc', 'sort-desc'); });
            th.dataset.sortDir = newDir;
            th.classList.add(`sort-${newDir}`);
            
            // Ordena
            sortTable(table, column, newDir);
        });
    });
    
    // Ícones de ordenação
    const style = document.createElement('style');
    style.textContent = `
        th[data-sort] { user-select: none; position: relative; padding-right: 24px !important; }
        th[data-sort]::after { 
            content: ''; position: absolute; right: 8px; top: 50%; transform: translateY(-50%);
            border: 4px solid transparent; border-bottom-color: #999; opacity: 0.3;
        }
        th.sort-asc::after { border-bottom-color: var(--primary); border-top: none; transform: translateY(-50%) rotate(180deg); opacity: 1; }
        th.sort-desc::after { border-bottom-color: var(--primary); opacity: 1; }
    `;
    document.head.appendChild(style);
}

/** Ordena tabela */
function sortTable(table, column, direction) {
    const tbody = table.querySelector('tbody');
    const rows = Array.from(tbody.querySelectorAll('tr'));
    
    rows.sort((a, b) => {
        const cellA = a.querySelector(`td:nth-child(${getColumnIndex(table, column) + 1})`);
        const cellB = b.querySelector(`td:nth-child(${getColumnIndex(table, column) + 1})`);
        
        if (!cellA || !cellB) return 0;
        
        let valA = cellA.textContent.trim();
        let valB = cellB.textContent.trim();
        
        // Tenta converter para número
        const numA = parseFloat(valA.replace(/[^\d.,-]/g, '').replace(',', '.'));
        const numB = parseFloat(valB.replace(/[^\d.,-]/g, '').replace(',', '.'));
        
        if (!isNaN(numA) && !isNaN(numB)) {
            valA = numA;
            valB = numB;
        }
        
        if (valA < valB) return direction === 'asc' ? -1 : 1;
        if (valA > valB) return direction === 'asc' ? 1 : -1;
        return 0;
    });
    
    rows.forEach(row => tbody.appendChild(row));
}

function getColumnIndex(table, columnName) {
    const headers = table.querySelectorAll('th');
    for (let i = 0; i < headers.length; i++) {
        if (headers[i].dataset.sort === columnName) return i;
    }
    return -1;
}

// ========================================
// INICIALIZAÇÃO GLOBAL
// ========================================

document.addEventListener('DOMContentLoaded', () => {
    // Inicializa tooltips
    $$('[data-tooltip]').forEach(el => {
        el.addEventListener('mouseenter', showTooltip);
        el.addEventListener('mouseleave', hideTooltip);
    });
    
    // Inicializa dropdowns
    $$('.dropdown').forEach(dropdown => {
        const btn = dropdown.querySelector('[aria-expanded]');
        const menu = dropdown.querySelector('.dropdown-menu');
        
        if (btn && menu) {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const isOpen = menu.classList.toggle('show');
                btn.setAttribute('aria-expanded', isOpen);
                
                // Fecha outros
                $$('.dropdown-menu.show').forEach(m => {
                    if (m !== menu) m.classList.remove('show');
                });
                $$('[aria-expanded="true"]').forEach(b => {
                    if (b !== btn) b.setAttribute('aria-expanded', 'false');
                });
            });
        }
    });
    
    // Fecha dropdowns ao clicar fora
    document.addEventListener('click', () => {
        $$('.dropdown-menu.show').forEach(m => m.classList.remove('show'));
        $$('[aria-expanded="true"]').forEach(b => b.setAttribute('aria-expanded', 'false'));
    });
    
    // Auto-focus primeiro input com erro
    const firstError = $('input.is-invalid, select.is-invalid, textarea.is-invalid');
    firstError?.focus();
    
    // Confirmação em links/botões com data-confirm
    $$('[data-confirm]').forEach(el => {
        el.addEventListener('click', (e) => {
            if (!confirm(el.dataset.confirm)) {
                e.preventDefault();
                e.stopPropagation();
            }
        });
    });
    
    // Atualiza CSRF token periodicamente (para sessões longas)
    setInterval(async () => {
        try {
            const res = await fetch('/api/csrf', { credentials: 'same-origin' });
            if (res.ok) {
                const data = await res.json();
                NanoBanana.csrfToken = data.token;
            }
        } catch (e) {
            // Ignora erros
        }
    }, 30 * 60 * 1000); // 30 min
});

/** Tooltip simples */
let tooltipEl = null;
function showTooltip(e) {
    const text = e.target.dataset.tooltip;
    if (!text) return;
    
    tooltipEl = document.createElement('div');
    tooltipEl.className = 'tooltip show';
    tooltipEl.textContent = text;
    document.body.appendChild(tooltipEl);
    
    const rect = e.target.getBoundingClientRect();
    tooltipEl.style.left = rect.left + rect.width / 2 - tooltipEl.offsetWidth / 2 + 'px';
    tooltipEl.style.top = rect.top - tooltipEl.offsetHeight - 8 + 'px';
}

function hideTooltip() {
    if (tooltipEl) {
        tooltipEl.remove();
        tooltipEl = null;
    }
}

// ========================================
// EXPORTS GLOBAIS
// ========================================
window.NanoBanana = {
    ...window.NanoBanana,
    formatMZN,
    parseMZN,
    formatDate,
    formatDateTime,
    debounce,
    throttle,
    generateUUID,
    escapeHtml,
    copyToClipboard,
    showToast,
    showSuccess,
    showError,
    showWarning,
    showInfo,
    setButtonLoading,
    setFormLoading,
    confirmAction,
    apiFetch,
    apiGet,
    apiPost,
    apiPut,
    apiDelete,
    serializeForm,
    populateForm,
    validateForm,
    showFormErrors,
    openModal,
    closeModal,
    initSortableTable,
    sortTable
};

// Torna funções disponíveis globalmente (para compatibilidade)
window.formatMZN = formatMZN;
window.parseMZN = parseMZN;
window.showToast = showToast;
window.showSuccess = showSuccess;
window.showError = showError;
window.showWarning = showWarning;
window.showInfo = showInfo;
window.setButtonLoading = setButtonLoading;
window.confirmAction = confirmAction;
window.apiFetch = apiFetch;