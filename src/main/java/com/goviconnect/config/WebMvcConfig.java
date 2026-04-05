package com.goviconnect.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/blog-images}")
    private String uploadDir;

    /**
     * Serve uploaded blog images statically at /uploads/blog-images/**
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve the parent 'uploads/' directory as the resource location
        String uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize().getParent().toUri().toString();
        // Spring requires resource locations to end with '/'
        if (!uploadPath.endsWith("/")) {
            uploadPath += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);

        // Also serve default static resources
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**").addResourceLocations("classpath:/static/images/");
    }
}
