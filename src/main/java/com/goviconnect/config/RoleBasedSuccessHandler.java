package com.goviconnect.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class RoleBasedSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        String redirectUrl = "/";

        for (GrantedAuthority authority : authorities) {
            switch (authority.getAuthority()) {
                case "ROLE_ADMIN"           -> redirectUrl = "/admin/dashboard";
                case "ROLE_AGRI_OFFICER"    -> redirectUrl = "/officer/dashboard";
                case "ROLE_BLOG_MODERATOR" -> redirectUrl = "/moderator/dashboard";
                case "ROLE_USER"            -> redirectUrl = "/";
            }
        }

        response.sendRedirect(redirectUrl);
    }
}
