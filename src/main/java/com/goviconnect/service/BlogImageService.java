package com.goviconnect.service;

import com.goviconnect.entity.BlogImage;
import com.goviconnect.exception.DuplicateImageException;
import com.goviconnect.repository.BlogImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogImageService {

    private final BlogImageRepository blogImageRepository;

    @Value("${app.upload.dir:uploads/blog-images}")
    private String uploadDir;

    public BlogImage uploadImage(MultipartFile file) throws IOException {
        try {
            // Generate SHA-256 hash
            String hash = calculateSha256(file.getInputStream());

            // Check for duplicates
            if (blogImageRepository.existsBySha256Hash(hash)) {
                log.warn("Duplicate image upload attempt. Hash: {}", hash);
                throw new DuplicateImageException("This image has already been uploaded.");
            }

            // Create directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg");
            String fileExtension = "";
            if (originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
            
            // Save file locally
            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Save to database
            BlogImage blogImage = new BlogImage();
            blogImage.setFileName(originalFileName);
            blogImage.setFilePath("/" + uploadDir + "/" + uniqueFileName);
            blogImage.setSha256Hash(hash);
            
            return blogImageRepository.save(blogImage);

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            throw new RuntimeException("Error processing image hash", e);
        }
    }

    private String calculateSha256(InputStream is) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }
        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
