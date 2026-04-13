package com.goviconnect.controller;

import com.goviconnect.entity.BlogImage;
import com.goviconnect.exception.DuplicateImageException;
import com.goviconnect.service.BlogImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/blog-images")
@RequiredArgsConstructor
public class BlogImageController {

    private final BlogImageService blogImageService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }
        
        try {
            BlogImage savedImage = blogImageService.uploadImage(file);
            return ResponseEntity.ok(Map.of(
                    "message", "Image uploaded successfully",
                    "url", savedImage.getFilePath(),
                    "id", savedImage.getId()
            ));
        } catch (DuplicateImageException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to upload image: " + e.getMessage()
            ));
        }
    }
}
