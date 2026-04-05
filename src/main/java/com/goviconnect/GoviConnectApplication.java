package com.goviconnect;

import com.goviconnect.entity.User;
import com.goviconnect.enums.AccountStatus;
import com.goviconnect.enums.Role;
import com.goviconnect.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableScheduling
public class GoviConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoviConnectApplication.class, args);
    }

    /**
     * Runs FIRST — patches the `role` ENUM column in MySQL to include BLOG_MODERATOR.
     * Hibernate ddl-auto=update does NOT modify existing ENUM column definitions,
     * so we do it manually once here. Safe to run multiple times.
     */
    @Bean
    @Order(1)
    CommandLineRunner fixRoleEnum(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute(
                    "ALTER TABLE users MODIFY COLUMN role " +
                    "ENUM('USER','AGRI_OFFICER','ADMIN','BLOG_MODERATOR') NOT NULL"
                );
                System.out.println("✅  users.role ENUM column updated to include BLOG_MODERATOR.");
            } catch (Exception e) {
                // Already includes BLOG_MODERATOR or column is VARCHAR — safe to ignore
                System.out.println("ℹ️  users.role ENUM column already up-to-date: " + e.getMessage());
            }
        };
    }

    /**
     * Seeds a default ADMIN user on first startup if none exists.
     * Username: admin | Password: Admin@123
     */
    @Bean
    @Order(2)
    CommandLineRunner seedAdmin(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepo.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setFullName("GOVI CONNECT Admin");
                admin.setUsername("admin");
                admin.setEmail("admin@goviconnect.lk");
                admin.setPassword(passwordEncoder.encode("Admin@123"));
                admin.setRole(Role.ADMIN);
                admin.setAccountStatus(AccountStatus.APPROVED);
                admin.setNic("000000000V");
                admin.setContactNumber("0000000000");
                admin.setAddress("Colombo");
                admin.setDistrict("Colombo");
                admin.setProvince("Western");
                userRepo.save(admin);
                System.out.println("✅  Default admin seeded → username: admin  password: Admin@123");
            }
        };
    }

    /**
     * Seeds the default Blog Moderator account on first startup.
     * Username: moderator002 | Password: mode@002
     */
    @Bean
    @Order(3)
    CommandLineRunner seedModerator(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepo.findByUsername("moderator002").isEmpty()) {
                User moderator = new User();
                moderator.setFullName("Blog Moderator");
                moderator.setUsername("moderator002");
                moderator.setEmail("moderator002@goviconnect.lk");
                moderator.setPassword(passwordEncoder.encode("mode@002"));
                moderator.setRole(Role.BLOG_MODERATOR);
                moderator.setAccountStatus(AccountStatus.APPROVED);
                moderator.setNic("000000001M");
                moderator.setContactNumber("0000000001");
                moderator.setAddress("Colombo");
                moderator.setDistrict("Colombo");
                moderator.setProvince("Western");
                userRepo.save(moderator);
                System.out.println("✅  Default moderator seeded → username: moderator002  password: mode@002");
            }
        };
    }

    @Bean
    @Order(4)
    CommandLineRunner checkUser(UserRepository userRepo,
            com.goviconnect.repository.AgriOfficerDetailsRepository officerRepo) {
        return args -> {
            userRepo.findByUsername("sesali211").ifPresent(user -> {
                System.out.println("\n\n--- DEBUG OUTPUT ---");
                System.out.println("User found: " + user.getUsername() + ", Role: " + user.getRole());

                officerRepo.findByUser(user).ifPresentOrElse(
                        details -> System.out
                                .println("Officer Details Found. Registration: " + details.getRegistrationNumber()
                                        + " Specialization: " + details.getSpecializationArea()),
                        () -> System.out
                                .println("NO OFFICER DETAILS FOUND IN DB FOR USER " + user.getUsername() + "!"));
                System.out.println("--------------------\n\n");
            });
        };
    }
}
