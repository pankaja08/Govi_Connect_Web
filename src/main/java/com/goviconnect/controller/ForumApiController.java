package com.goviconnect.controller;

import com.goviconnect.entity.User;
import com.goviconnect.service.ForumService;
import com.goviconnect.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumApiController {

    private final ForumService forumService;
    private final UserService userService;

    @PostMapping("/answer/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long id, Authentication authentication) {
        System.out.println("DEBUG: Toggling like for answer ID: " + id + " by user: " + (authentication != null ? authentication.getName() : "anonymous"));
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            java.util.Map<String, Object> response = forumService.toggleLikeAnswer(id, user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", e.getMessage()));
        }
    }
}
