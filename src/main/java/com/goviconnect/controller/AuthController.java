package com.goviconnect.controller;

import com.goviconnect.dto.RegistrationDto;
import com.goviconnect.service.TurnstileService;
import com.goviconnect.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final TurnstileService turnstileService;

    @Value("${cloudflare.turnstile.site-key}")
    private String turnstileSiteKey;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationDto", new RegistrationDto());
        model.addAttribute("turnstileSiteKey", turnstileSiteKey);
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("registrationDto") RegistrationDto dto,
                                      BindingResult result,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {

        model.addAttribute("turnstileSiteKey", turnstileSiteKey);

        // Validate bean constraints
        if (result.hasErrors()) {
            return "auth/register";
        }

        // Age validation (Minimum 16 years)
        if (dto.getDob() != null) {
            java.time.LocalDate sixteenYearsAgo = java.time.LocalDate.now().minusYears(16);
            if (dto.getDob().isAfter(sixteenYearsAgo)) {
                result.rejectValue("dob", "error.dob", "You must be at least 16 years old to register.");
                return "auth/register";
            }
        } else {
            result.rejectValue("dob", "error.dob", "Date of birth is required.");
            return "auth/register";
        }

        // Verify Turnstile token
        if (!turnstileService.verify(dto.getCfTurnstileResponse())) {
            model.addAttribute("turnstileError", "Bot verification failed. Please try again.");
            return "auth/register";
        }

        try {
            userService.registerUser(dto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }

        boolean isOfficer = dto.isAgriOfficer();
        if (isOfficer) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration submitted! Your account is pending admin approval.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration successful! You can now log in.");
        }
        return "redirect:/login";
    }
}
