package com.goviconnect.repository;

import com.goviconnect.entity.ProductMessage;
import com.goviconnect.entity.User;
import com.goviconnect.entity.MarketProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductMessageRepository extends JpaRepository<ProductMessage, Long> {

    // Get full conversation between two users about a specific product
    @Query("SELECT m FROM ProductMessage m WHERE m.product = :product " +
           "AND ((m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1)) " +
           "ORDER BY m.sentAt ASC")
    List<ProductMessage> findConversation(
            @Param("product") MarketProduct product,
            @Param("user1") User user1,
            @Param("user2") User user2);

    // For seller: get all messages about my products (listings)
    @Query("SELECT m FROM ProductMessage m WHERE m.product.seller = :seller " +
           "ORDER BY m.sentAt DESC")
    List<ProductMessage> findBySeller(@Param("seller") User seller);

    // For buyer: get all messages I'm involved in for products I don't own
    @Query("SELECT m FROM ProductMessage m WHERE m.product.seller <> :user " +
           "AND (m.sender = :user OR m.receiver = :user) " +
           "ORDER BY m.sentAt DESC")
    List<ProductMessage> findByBuyerSent(@Param("user") User user);

    // Check if conversation exists
    @Query("SELECT COUNT(m) > 0 FROM ProductMessage m WHERE m.product = :product " +
           "AND ((m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1))")
    boolean conversationExists(
            @Param("product") MarketProduct product,
            @Param("user1") User user1,
            @Param("user2") User user2);

    // Get unread count for a user (as receiver)
    long countByReceiverAndReadByReceiverFalse(User receiver);

    // Mark messages as read
    @Query("UPDATE ProductMessage m SET m.readByReceiver = true WHERE m.product = :product " +
           "AND m.receiver = :receiver")
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void markConversationAsRead(
            @Param("product") MarketProduct product,
            @Param("receiver") User receiver);
}
