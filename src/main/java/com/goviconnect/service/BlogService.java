package com.goviconnect.service;

import com.goviconnect.entity.Blog;
import com.goviconnect.entity.User;
import com.goviconnect.enums.BlogStatus;
import com.goviconnect.repository.BlogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogService {

    private final BlogRepository blogRepository;
    private final BlogImageService blogImageService;

    @Value("${app.upload.dir:uploads/blog-images}")
    private String uploadDir;

    /**
     * Saves a blog post. Starts in PENDING state awaiting moderator approval.
     */
    @Transactional
    public Blog createBlog(User author, String heading, String textContent, MultipartFile image,
            String locationTag, String seasonTag, String cropTag, String farmingMethodTag) throws IOException {
        String imageUrl = null;

        if (image != null && !image.isEmpty()) {
            imageUrl = saveImage(image);
        }

        Blog blog = Blog.builder()
                .author(author)
                .heading(heading)
                .textContent(textContent)
                .imageUrl(imageUrl)
                .locationTag(locationTag)
                .seasonTag(seasonTag)
                .cropTag(cropTag)
                .farmingMethodTag(farmingMethodTag)
                .approvalStatus(BlogStatus.PENDING)
                .build();

        return blogRepository.save(blog);
    }

    /**
     * Returns the latest 6 APPROVED blogs for the home page.
     */
    public List<Blog> getLatestBlogs() {
        return blogRepository.findTop6ByApprovalStatusOrderByCreatedDateDesc(BlogStatus.APPROVED);
    }

    /**
     * Returns all APPROVED blogs ordered by newest first (public listing).
     */
    public List<Blog> getAllBlogs() {
        return blogRepository.findByApprovalStatusOrderByCreatedDateDesc(BlogStatus.APPROVED);
    }

    /**
     * Returns filtered APPROVED blogs based on optional tags.
     */
    public List<Blog> getFilteredBlogs(String keyword, String location, String season, String crop, String method) {
        String pattern = (keyword == null || keyword.trim().isEmpty()) ? null : "%" + keyword.trim().toLowerCase() + "%";
        return blogRepository.findFilteredBlogs(BlogStatus.APPROVED, pattern, location, season, crop, method);
    }

    /**
     * Returns all blogs by a specific author (all statuses — for the officer's own dashboard).
     */
    public List<Blog> getBlogsByAuthor(User author) {
        return blogRepository.findByAuthor(author);
    }

    /**
     * Finds a blog by ID and Author.
     */
    public Blog getBlogByIdAndAuthor(Long id, User author) {
        return blogRepository.findByIdAndAuthor(id, author)
                .orElseThrow(() -> new IllegalArgumentException("Blog not found or unauthorized"));
    }

    /**
     * Finds a blog by ID for public viewing.
     */
    public Blog getBlogById(Long id) {
        return blogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Blog not found"));
    }

    /**
     * Updates an existing blog post (resets to PENDING so moderator re-reviews).
     */
    @Transactional
    public void updateBlog(Long id, User author, String heading, String textContent, MultipartFile image,
            String locationTag, String seasonTag, String cropTag, String farmingMethodTag)
            throws IOException {
        Blog blog = getBlogByIdAndAuthor(id, author);
        blog.setHeading(heading);
        blog.setTextContent(textContent);
        blog.setLocationTag(locationTag);
        blog.setSeasonTag(seasonTag);
        blog.setCropTag(cropTag);
        blog.setFarmingMethodTag(farmingMethodTag);
        blog.setApprovalStatus(BlogStatus.PENDING);  // re-review after edit
        blog.setRejectionReason(null);

        if (image != null && !image.isEmpty()) {
            String imageUrl = saveImage(image);
            blog.setImageUrl(imageUrl);
        }

        blogRepository.save(blog);
    }

    /**
     * Deletes a blog post.
     */
    @Transactional
    public void deleteBlog(Long id, User author) {
        Blog blog = getBlogByIdAndAuthor(id, author);
        blogRepository.delete(blog);
    }

    // ====================== MODERATION METHODS ======================

    /**
     * Returns all blogs awaiting moderation.
     */
    public List<Blog> getPendingBlogs() {
        return blogRepository.findByApprovalStatus(BlogStatus.PENDING);
    }

    /**
     * Returns all approved blogs (for moderator dashboard).
     */
    public List<Blog> getApprovedBlogs() {
        return blogRepository.findByApprovalStatusOrderByCreatedDateDesc(BlogStatus.APPROVED);
    }

    /**
     * Returns all rejected blogs (for moderator dashboard).
     */
    public List<Blog> getRejectedBlogs() {
        return blogRepository.findByApprovalStatusOrderByCreatedDateDesc(BlogStatus.REJECTED);
    }

    /**
     * Approves a pending blog post.
     */
    @Transactional
    public void approveBlog(Long id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Blog not found: " + id));
        blog.setApprovalStatus(BlogStatus.APPROVED);
        blog.setRejectionReason(null);
        blogRepository.save(blog);
        log.info("Blog '{}' approved by moderator.", blog.getHeading());
    }

    /**
     * Rejects a pending blog post with optional reason.
     */
    @Transactional
    public void rejectBlog(Long id, String reason) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Blog not found: " + id));
        blog.setApprovalStatus(BlogStatus.REJECTED);
        blog.setRejectionReason(reason);
        blogRepository.save(blog);
        log.info("Blog '{}' rejected by moderator. Reason: {}", blog.getHeading(), reason);
    }

    /**
     * Count stats for the moderator dashboard.
     */
    public long countPending() {
        return blogRepository.countByApprovalStatus(BlogStatus.PENDING);
    }

    public long countApproved() {
        return blogRepository.countByApprovalStatus(BlogStatus.APPROVED);
    }

    public long countRejected() {
        return blogRepository.countByApprovalStatus(BlogStatus.REJECTED);
    }

    private String saveImage(MultipartFile file) throws IOException {
        com.goviconnect.entity.BlogImage blogImage = blogImageService.uploadImage(file);
        return blogImage.getFilePath();
    }
}
