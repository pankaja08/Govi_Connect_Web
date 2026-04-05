package com.goviconnect.config;

import com.goviconnect.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final CustomUserDetailsService customUserDetailsService;
        private final RoleBasedSuccessHandler roleBasedSuccessHandler;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                // Public pages
                                                 .requestMatchers("/", "/register", "/login", "/css/**", "/js/**", "/i18n/**",
                                                                "/images/**", "/uploads/**", "/webjars/**", "/error",
                                                                "/user/profile/debug/**", "/blog/**", "/market", "/market/product/**", "/forum", "/api/chatbot",
                                                                "/crop-advisory", "/api/rss-news")
                                                .permitAll()
                                                // Admin area
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                // Agri Officer area
                                                .requestMatchers("/officer/**").hasRole("AGRI_OFFICER")
                                                // Blog Moderator area
                                                .requestMatchers("/moderator/**").hasRole("BLOG_MODERATOR")
                                                // User area
                                                .requestMatchers("/user/**").hasAnyRole("USER", "AGRI_OFFICER", "ADMIN", "BLOG_MODERATOR")
                                                .anyRequest().authenticated())
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers("/api/chatbot", "/api/disease-predict"))
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .successHandler(roleBasedSuccessHandler)
                                                .failureUrl("/login?error=true")
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout=true")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())
                                .exceptionHandling(ex -> ex
                                                .accessDeniedPage("/access-denied"));

                return http.build();
        }

        @Bean
        public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
                AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
                builder.userDetailsService(customUserDetailsService).passwordEncoder(passwordEncoder());
                return builder.build();
        }
}
