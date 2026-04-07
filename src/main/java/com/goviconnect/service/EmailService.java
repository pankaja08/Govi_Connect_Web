package com.goviconnect.service;

import com.goviconnect.entity.MarketProduct;
import com.goviconnect.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Sends the approval welcome email to the approved Agri Officer.
     * The email body includes their username and raw password (sent once during approval).
     */
    public void sendApprovalEmail(User user, String rawPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Welcome to GOVI CONNECT – Account Approved");
            message.setText(
                    "Dear " + user.getFullName() + ",\n\n" +
                    "Welcome to GOVI CONNECT, thank you for joining with us to share your knowledge.\n\n" +
                    "Your account has been approved. You can now log in using the credentials below:\n\n" +
                    "  Username : " + user.getUsername() + "\n" +
                    "  Password : " + rawPassword + "\n\n" +
                    "Please change your password after your first login.\n\n" +
                    "Best regards,\n" +
                    "GOVI CONNECT Team\n" +
                    "Sri Lanka's First All-in-One Agriculture Platform"
            );
            mailSender.send(message);
            log.info("Approval email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send approval email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /**
     * Sends a rejection notification email.
     */
    public void sendRejectionEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("GOVI CONNECT – Registration Update");
            message.setText(
                    "Dear " + user.getFullName() + ",\n\n" +
                    "We regret to inform you that your registration as an Agricultural Officer " +
                    "on GOVI CONNECT has not been approved at this time.\n\n" +
                    "If you believe this is an error, please contact our support team.\n\n" +
                    "Best regards,\n" +
                    "GOVI CONNECT Team"
            );
            mailSender.send(message);
            log.info("Rejection email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send rejection email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /**
     * Sends a product approval notification email.
     */
    public void sendProductApprovalEmail(User user, MarketProduct product) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("GOVI CONNECT – Product Listing Approved");
            message.setText(
                    "Dear " + user.getFullName() + ",\n\n" +
                    "Great news! Your product listing for '" + product.getName() + "' on Govi Mart has been approved.\n" +
                    "It is now visible to all users on the marketplace.\n\n" +
                    "Best regards,\n" +
                    "GOVI CONNECT Team"
            );
            mailSender.send(message);
            log.info("Product approval email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send product approval email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /**
     * Sends a product rejection notification email.
     */
    public void sendProductRejectionEmail(User user, MarketProduct product) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("GOVI CONNECT – Product Listing Update");
            message.setText(
                    "Dear " + user.getFullName() + ",\n\n" +
                    "We regret to inform you that your product listing for '" + product.getName() + "' on Govi Mart " +
                    "has not been approved by the administrators at this time.\n\n" +
                    "If you believe this is an error or would like to know more details, please contact our support team.\n\n" +
                    "Best regards,\n" +
                    "GOVI CONNECT Team"
            );
            mailSender.send(message);
            log.info("Product rejection email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send product rejection email to {}: {}", user.getEmail(), e.getMessage());
        }
    }
    /**
     * Sends a password reset OTP email.
     */
    public void sendPasswordResetOtpEmail(User user, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("GOVI CONNECT \u2013 Password Reset OTP");
            message.setText(
                    "Dear " + user.getFullName() + ",\n\n" +
                    "You have requested to reset your password. \n" +
                    "Here is your 6-digit One-Time Password (OTP):\n\n" +
                    "  " + otp + "\n\n" +
                    "This code will expire in 10 minutes.\n" +
                    "If you did not request a password reset, please ignore this email or contact support.\n\n" +
                    "Best regards,\n" +
                    "GOVI CONNECT Team"
            );
            mailSender.send(message);
            log.info("Password reset OTP email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset OTP email to {}: {}", user.getEmail(), e.getMessage());
        }
    }
}
