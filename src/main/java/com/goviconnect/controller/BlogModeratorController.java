package com.goviconnect.controller;

import com.goviconnect.entity.User;
import com.goviconnect.service.BlogService;
import com.goviconnect.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/moderator")
@PreAuthorize("hasRole('BLOG_MODERATOR')")
@RequiredArgsConstructor
@Slf4j
public class BlogModeratorController {

    private final BlogService blogService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User moderator = userService.findByUsername(authentication.getName());
        model.addAttribute("moderator", moderator);

        model.addAttribute("pendingBlogs", blogService.getPendingBlogs());
        model.addAttribute("approvedBlogs", blogService.getApprovedBlogs());
        model.addAttribute("rejectedBlogs", blogService.getRejectedBlogs());

        model.addAttribute("pendingCount", blogService.countPending());
        model.addAttribute("approvedCount", blogService.countApproved());
        model.addAttribute("rejectedCount", blogService.countRejected());
        model.addAttribute("totalCount",
                blogService.countPending() + blogService.countApproved() + blogService.countRejected());

        return "moderator/dashboard";
    }

    @PostMapping("/blog/{id}/approve")
    public String approveBlog(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        log.info("Moderator approving blog ID: {}", id);
        try {
            blogService.approveBlog(id);
            redirectAttributes.addFlashAttribute("successMessage", "Blog approved and is now publicly visible.");
        } catch (Exception e) {
            log.error("Failed to approve blog ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to approve: " + e.getMessage());
        }
        return "redirect:/moderator/dashboard";
    }

    @PostMapping("/blog/{id}/reject")
    public String rejectBlog(@PathVariable("id") Long id,
            @RequestParam(value = "reason", required = false) String reason,
            RedirectAttributes redirectAttributes) {
        log.info("Moderator rejecting blog ID: {}", id);
        try {
            blogService.rejectBlog(id, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Blog rejected successfully.");
        } catch (Exception e) {
            log.error("Failed to reject blog ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject: " + e.getMessage());
        }
        return "redirect:/moderator/dashboard";
    }
}
