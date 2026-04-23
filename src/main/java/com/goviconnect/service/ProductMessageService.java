package com.goviconnect.service;

import com.goviconnect.entity.MarketProduct;
import com.goviconnect.entity.ProductMessage;
import com.goviconnect.entity.User;
import com.goviconnect.repository.MarketProductRepository;
import com.goviconnect.repository.ProductMessageRepository;
import com.goviconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductMessageService {

    private final ProductMessageRepository messageRepository;
    private final MarketProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Send a message from sender to receiver about a product.
     */
    @Transactional
    public ProductMessage sendMessage(Long productId, User sender, Long receiverId, String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty.");
        }
        if (text.length() > 2000) {
            throw new IllegalArgumentException("Message too long (max 2000 characters).");
        }

        MarketProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found."));

        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("You cannot message yourself.");
        }

        ProductMessage msg = ProductMessage.builder()
                .product(product)
                .sender(sender)
                .receiver(receiver)
                .message(text.trim())
                .build();

        return messageRepository.save(msg);
    }

    /**
     * Get full conversation between two users about a product.
     */
    @Transactional
    public List<ProductMessage> getConversation(Long productId, User currentUser, Long otherUserId) {
        MarketProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // Mark incoming messages as read
        messageRepository.markConversationAsRead(product, currentUser);

        return messageRepository.findConversation(product, currentUser, otherUser);
    }

    /**
     * Get the seller's inbox: grouped by product, then by buyer (the unique 'other'
     * person).
     * Returns a list of unique conversations (product + buyer) with the latest
     * message.
     */
    public List<ConversationSummary> getSellerInbox(User seller) {
        List<ProductMessage> allMessages = messageRepository.findBySeller(seller);
        return buildConversationSummaries(allMessages, seller);
    }

    /**
     * Get the buyer's sent messages (and seller's responses) for products I don't own.
     */
    public List<ConversationSummary> getBuyerSent(User buyer) {
        List<ProductMessage> allMessages = messageRepository.findByBuyerSent(buyer);
        return buildConversationSummaries(allMessages, buyer);
    }

    /**
     * Unread count for a user.
     */
    public long getUnreadCount(User user) {
        return messageRepository.countByReceiverAndReadByReceiverFalse(user);
    }

    /**
     * Build unique conversation summaries from a list of messages.
     */
    private List<ConversationSummary> buildConversationSummaries(List<ProductMessage> messages, User self) {
        // Key: productId + ":" + otherId
        Map<String, ConversationSummary> map = new LinkedHashMap<>();

        for (ProductMessage msg : messages) {
            User other = msg.getSender().getId().equals(self.getId()) ? msg.getReceiver() : msg.getSender();
            String key = msg.getProduct().getId() + ":" + other.getId();

            map.computeIfAbsent(key, k -> new ConversationSummary(msg.getProduct(), other));
            map.get(key).addMessage(msg);
        }

        return new ArrayList<>(map.values());
    }

    /**
     * DTO for a conversation summary (latest message, product, other user).
     */
    public static class ConversationSummary {
        public final MarketProduct product;
        public final User otherUser;
        public String lastMessage;
        public java.time.LocalDateTime lastAt;
        public int unreadCount = 0;
        public List<ProductMessage> messages = new ArrayList<>();

        public ConversationSummary(MarketProduct product, User otherUser) {
            this.product = product;
            this.otherUser = otherUser;
        }

        public void addMessage(ProductMessage msg) {
            messages.add(msg);
            if (lastAt == null || msg.getSentAt().isAfter(lastAt)) {
                lastAt = msg.getSentAt();
                lastMessage = msg.getMessage();
            }
            if (!msg.isReadByReceiver() && msg.getReceiver().getId().equals(otherUser.getId())) {
                // don't count; this is messages from self
            }
        }
    }
}
