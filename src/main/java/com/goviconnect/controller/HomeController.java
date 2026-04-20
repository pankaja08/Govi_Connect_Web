package com.goviconnect.controller;

import com.goviconnect.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.Map;
import com.goviconnect.entity.Blog;
import com.goviconnect.entity.User;
import com.goviconnect.service.CommentService;
import com.goviconnect.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final BlogService blogService;
    private final CommentService commentService;
    private final UserService userService;

    @GetMapping("/")
    public String home(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "season", required = false) String season,
            @RequestParam(value = "crop", required = false) String crop,
            @RequestParam(value = "method", required = false) String method,
            Model model) {
        model.addAttribute("blogs", blogService.getFilteredBlogs(keyword, location, season, crop, method));
        model.addAttribute("farmerCount", userService.getFarmerCount());
        model.addAttribute("expertCount", userService.getExpertCount());

        // Pass filter parameters back to the view to show active state
        model.addAttribute("keywordFilter", keyword);
        model.addAttribute("locationFilter", location);
        model.addAttribute("seasonFilter", season);
        model.addAttribute("cropFilter", crop);
        model.addAttribute("methodFilter", method);

        return "index";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }

    @GetMapping("/blog/{id}")
    public String viewBlog(@PathVariable("id") Long id, Model model, Authentication authentication) {
        try {
            Blog blog = blogService.getBlogById(id);
            User currentUser = null;
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                currentUser = userService.findByUsername(authentication.getName());
            }

            model.addAttribute("blog", blog);
            model.addAttribute("comments", commentService.getCommentsByBlog(blog, currentUser));
            return "blog-detail";
        } catch (Exception e) {
            return "redirect:/?error=BlogNotFound";
        }
    }

    @PostMapping("/blog/{id}/comment")
    public String addComment(@PathVariable("id") Long id,
            @RequestParam("content") String content,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            // Technically UI should catch this, but safeguard backend
            redirectAttributes.addFlashAttribute("errorMessage", "Please register or login to comment.");
            return "redirect:/register";
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            Blog blog = blogService.getBlogById(id);
            commentService.addComment(blog, user, content);
            redirectAttributes.addFlashAttribute("successMessage", "Comment posted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to post comment.");
        }

        return "redirect:/blog/" + id;
    }

    @PostMapping("/blog/{blogId}/comment/{commentId}/like")
    @ResponseBody
    public ResponseEntity<?> toggleCommentLike(@PathVariable("blogId") Long blogId,
            @PathVariable("commentId") Long commentId,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Please login to like comments"));
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            long newLikeCount = commentService.toggleLike(commentId, user);
            return ResponseEntity.ok(Map.of("likeCount", newLikeCount));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Failed to process like"));
        }
    }

    @PostMapping("/blog/{blogId}/comment/{commentId}/reply")
    public String addExpertReply(@PathVariable("blogId") Long blogId,
            @PathVariable("commentId") Long commentId,
            @RequestParam("reply") String reply,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            commentService.addExpertReply(commentId, reply, user);
            redirectAttributes.addFlashAttribute("successMessage", "Reply posted successfully!");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while posting your reply.");
        }

        return "redirect:/blog/" + blogId;
    }
}
