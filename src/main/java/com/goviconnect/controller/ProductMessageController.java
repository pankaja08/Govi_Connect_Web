package com.goviconnect.controller;

import com.goviconnect.entity.ProductMessage;
import com.goviconnect.entity.User;
import com.goviconnect.service.ProductMessageService;
import com.goviconnect.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/market/api/message")
@RequiredArgsConstructor
public class ProductMessageController {

    private final ProductMessageService messageService;
    private final UserService userService;

    /**
     * POST /market/api/message/send
     * Body: { productId, receiverId, message }
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        }

        try {
            User sender = userService.findByUsername(authentication.getName());
            Long productId = Long.valueOf(body.get("productId").toString());
            Long receiverId = Long.valueOf(body.get("receiverId").toString());
            String message = body.get("message").toString();

            ProductMessage msg = messageService.sendMessage(productId, sender, receiverId, message);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm a");
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "id", msg.getId(),
                    "message", msg.getMessage(),
                    "senderName", msg.getSender().getFullName(),
                    "sentAt", msg.getSentAt().format(fmt)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to send message."));
        }
    }

    /**
     * GET /market/api/message/conversation/{productId}/{otherUserId}
     */
    @GetMapping("/conversation/{productId}/{otherUserId}")
    public ResponseEntity<?> getConversation(
            @PathVariable Long productId,
            @PathVariable Long otherUserId,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        }

        try {
            User currentUser = userService.findByUsername(authentication.getName());
            List<ProductMessage> messages = messageService.getConversation(productId, currentUser, otherUserId);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm a");
            List<Map<String, Object>> result = new ArrayList<>();
            for (ProductMessage msg : messages) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", msg.getId());
                m.put("message", msg.getMessage());
                m.put("senderId", msg.getSender().getId());
                m.put("senderName", msg.getSender().getFullName());
                m.put("sentAt", msg.getSentAt().format(fmt));
                m.put("isMine", msg.getSender().getId().equals(currentUser.getId()));
                result.add(m);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /market/api/message/inbox/seller
     * Returns seller's groupd inbox
     */
    @GetMapping("/inbox/seller")
    public ResponseEntity<?> getSellerInbox(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        }

        try {
            User seller = userService.findByUsername(authentication.getName());
            List<ProductMessageService.ConversationSummary> summaries = messageService.getSellerInbox(seller);
            return ResponseEntity.ok(buildSummaryResponse(summaries, seller));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /market/api/message/inbox/buyer
     * Returns buyer's sent messages grouped by product + seller
     */
    @GetMapping("/inbox/buyer")
    public ResponseEntity<?> getBuyerInbox(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        }

        try {
            User buyer = userService.findByUsername(authentication.getName());
            List<ProductMessageService.ConversationSummary> summaries = messageService.getBuyerSent(buyer);
            return ResponseEntity.ok(buildSummaryResponse(summaries, buyer));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /market/api/message/unread
     */
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.ok(Map.of("count", 0));
        }
        try {
            User user = userService.findByUsername(authentication.getName());
            long count = messageService.getUnreadCount(user);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("count", 0));
        }
    }

    private List<Map<String, Object>> buildSummaryResponse(
            List<ProductMessageService.ConversationSummary> summaries, User self) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM, hh:mm a");
        List<Map<String, Object>> result = new ArrayList<>();

        for (ProductMessageService.ConversationSummary s : summaries) {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", s.product.getId());
            item.put("productName", s.product.getName());
            item.put("productImage", s.product.getImageUrl());
            item.put("otherUserId", s.otherUser.getId());
            item.put("otherUserName", s.otherUser.getFullName());
            item.put("lastMessage", s.lastMessage);
            item.put("lastAt", s.lastAt != null ? s.lastAt.format(fmt) : "");
            item.put("unreadCount", s.unreadCount);
            result.add(item);
        }

        return result;
    }
}
