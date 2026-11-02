package com.nanobanana.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler global de exceções para controllers Thymeleaf (retorna views com mensagens de erro)
 * Para APIs REST, criar handler separado com @RestControllerAdvice
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Erros de validação @Valid em @ModelAttribute / @RequestBody
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ModelAndView handleValidationErrors(Exception ex, HttpServletRequest request, RedirectAttributes redirectAttrs) {
        List<String> errors;
        if (ex instanceof MethodArgumentNotValidException mave) {
            errors = mave.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());
        } else {
            errors = ((BindException) ex).getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());
        }

        String msg = String.join("; ", errors);
        log.warn("Erro de validação em {}: {}", request.getRequestURI(), msg);

        redirectAttrs.addFlashAttribute("erro", msg);
        return redirectToReferer(request);
    }

    /**
     * Erros de validação em parâmetros de método (@Validated)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ModelAndView handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request, RedirectAttributes redirectAttrs) {
        String msg = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation em {}: {}", request.getRequestURI(), msg);

        redirectAttrs.addFlashAttribute("erro", msg);
        return redirectToReferer(request);
    }

    /**
     * Erros de integridade de dados (unique constraint, foreign key, etc.)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request, RedirectAttributes redirectAttrs) {
        String msg = "Erro de integridade dos dados";
        String cause = ex.getMostSpecificCause().getMessage();

        if (cause != null) {
            if (cause.contains("uk_usuario_tenant_email") || cause.contains("email")) {
                msg = "Email já cadastrado nesta loja";
            } else if (cause.contains("uk_produto_tenant_codigo") || cause.contains("codigo_barras")) {
                msg = "Código de barras já existe nesta loja";
            } else if (cause.contains("foreign key") || cause.contains("FK_")) {
                msg = "Não é possível excluir: existem registros vinculados";
            } else if (cause.contains("check constraint") || cause.contains("chk_")) {
                msg = "Valor inválido: viola regra de negócio";
            }
        }

        log.warn("DataIntegrityViolation em {}: {} | Cause: {}", request.getRequestURI(), msg, cause);
        redirectAttrs.addFlashAttribute("erro", msg);
        return redirectToReferer(request);
    }

    /**
     * Concorrência otimista (version locking) - retry ou erro amigável
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ModelAndView handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest request, RedirectAttributes redirectAttrs) {
        log.warn("Conflito de concorrência em {}: {}", request.getRequestURI(), ex.getMessage());
        redirectAttrs.addFlashAttribute("erro", "O registro foi alterado por outro usuário. Recarregue a página e tente novamente.");
        return redirectToReferer(request);
    }

    /**
     * Acesso negado (Spring Security)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acesso negado para {}: {}", request.getRequestURI(), ex.getMessage());
        ModelAndView mav = new ModelAndView("error/403");
        mav.addObject("message", "Você não tem permissão para acessar esta página");
        mav.setStatus(HttpStatus.FORBIDDEN);
        return mav;
    }

    /**
     * Página não encontrada (404) — não deve redirecionar para o PDV
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        log.debug("Recurso não encontrado: {}", request.getRequestURI());
        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("path", request.getRequestURI());
        mav.setStatus(HttpStatus.NOT_FOUND);
        return mav;
    }

    /**
     * Exceções de negócio (RuntimeException lançadas propositalmente)
     */
    @ExceptionHandler(RuntimeException.class)
    public ModelAndView handleRuntime(RuntimeException ex, HttpServletRequest request, RedirectAttributes redirectAttrs) {
        log.warn("Erro de negócio em {}: {}", request.getRequestURI(), ex.getMessage());
        redirectAttrs.addFlashAttribute("erro", ex.getMessage());
        return redirectToReferer(request);
    }

    /**
     * Exceções genéricas não tratadas
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneric(Exception ex, HttpServletRequest request, RedirectAttributes redirectAttrs) {
        log.error("Erro inesperado em {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        redirectAttrs.addFlashAttribute("erro", "Erro interno do servidor. Tente novamente ou contacte o suporte.");
        return redirectToReferer(request);
    }

    /**
     * Redireciona para a página anterior (Referer) ou fallback
     */
    private ModelAndView redirectToReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        String fallback = "/pdv"; // PDV é a página principal para usuários logados

        String redirectUrl = (referer != null && !referer.isBlank()) ? referer : fallback;
        return new ModelAndView("redirect:" + redirectUrl);
    }
}