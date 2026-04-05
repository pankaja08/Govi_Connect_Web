package com.goviconnect.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("currentUri")
    public String getCurrentUri(HttpServletRequest request) {
        if (request == null) return "";
        try {
            return request.getRequestURI();
        } catch (Exception e) {
            return "";
        }
    }
}
