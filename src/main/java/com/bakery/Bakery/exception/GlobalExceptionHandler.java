package com.bakery.Bakery.exception;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Централізована обробка виключень для всього додатку.
 * Замість Whitelabel Error Page — зрозуміла сторінка для юзера.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Невірний аргумент — 400.
     * Наприклад: замовлення не знайдено, невірний enum тощо.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(IllegalArgumentException ex, Model model) {
        model.addAttribute("errorCode", 400);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    /**
     * Немає прав — 403.
     * Наприклад: юзер намагається скасувати чуже замовлення.
     */
    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleForbidden(SecurityException ex, Model model) {
        model.addAttribute("errorCode", 403);
        model.addAttribute("errorMessage", "Доступ заборонено: " + ex.getMessage());
        return "error";
    }

    /**
     * Некоректний стан — 409.
     * Наприклад: спроба скасувати вже виконане замовлення.
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleConflict(IllegalStateException ex, Model model) {
        model.addAttribute("errorCode", 409);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    /**
     * Будь-який необроблений виняток — 500.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception ex, Model model) {
        // У продакшні — не показувати stacktrace юзеру, логувати на сервері
        ex.printStackTrace();
        model.addAttribute("errorCode", 500);
        model.addAttribute("errorMessage", "Внутрішня помилка сервера. Спробуйте пізніше.");
        return "error";
    }
}
