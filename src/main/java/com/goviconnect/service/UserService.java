package com.goviconnect.service;

import com.goviconnect.dto.RegistrationDto;
import com.goviconnect.entity.AgriOfficerDetails;
import com.goviconnect.entity.PasswordResetToken;
import com.goviconnect.entity.User;
import com.goviconnect.enums.AccountStatus;
import com.goviconnect.enums.Role;
import com.goviconnect.repository.AgriOfficerDetailsRepository;
import com.goviconnect.repository.PasswordResetTokenRepository;
import com.goviconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AgriOfficerDetailsRepository agriOfficerDetailsRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Registers a new user (normal user or agri officer).
     * Agri officers start with PENDING status; normal users start as APPROVED.
     */
    @Transactional
    public User registerUser(RegistrationDto dto) {
        // Validate uniqueness
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username already taken.");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already registered.");
        }
        if (userRepository.existsByNic(dto.getNic())) {
            throw new IllegalArgumentException("NIC already registered.");
        }

        boolean isOfficer = dto.isAgriOfficer();
        String rawPassword = dto.getPassword();

        validateNIC(dto.getNic());
        User user = User.builder()
                .fullName(dto.getFullName())
                .nic(dto.getNic())
                .contactNumber(dto.getContactNumber())
                .address(dto.getAddress())
                .district(dto.getDistrict())
                .province(dto.getProvince())
                .dob(dto.getDob())
                .email(dto.getEmail())
                .username(dto.getUsername())
                .password(passwordEncoder.encode(rawPassword))
                .role(isOfficer ? Role.AGRI_OFFICER : Role.USER)
                .accountStatus(isOfficer ? AccountStatus.PENDING : AccountStatus.APPROVED)
                .build();

        user = userRepository.save(user);

        if (isOfficer) {
            // Validate mandatory officer fields
            if (dto.getRegistrationNumber() == null || dto.getRegistrationNumber().trim().isEmpty()) {
                throw new RuntimeException("Registration number is required for Agri Officers.");
            }
            if (dto.getDesignation() == null || dto.getDesignation().trim().isEmpty()) {
                throw new RuntimeException("Designation is required for Agri Officers.");
            }
            if (dto.getOfficialEmail() == null || dto.getOfficialEmail().trim().isEmpty()) {
                throw new RuntimeException("Official email is required for Agri Officers.");
            }

            AgriOfficerDetails details = AgriOfficerDetails.builder()
                    .user(user)
                    .registrationNumber(dto.getRegistrationNumber())
                    .designation(dto.getDesignation())
                    .specializationArea(dto.getSpecializationArea())
                    .assignedArea(dto.getAssignedArea())
                    .officialEmail(dto.getOfficialEmail())
                    .build();
            agriOfficerDetailsRepository.save(details);
            log.info("Agri Officer '{}' registered — pending approval.", user.getUsername());
        } else {
            log.info("Normal user '{}' registered and approved.", user.getUsername());
        }

        return user;
    }

    /**
     * Returns all AGRI_OFFICER accounts with PENDING status.
     */
    public List<User> getPendingOfficers() {
        return userRepository.findByRoleAndAccountStatus(Role.AGRI_OFFICER, AccountStatus.PENDING);
    }

    /**
     * Returns all users in the system.
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Retrieves the AgriOfficerDetails for a given user.
     */
    public AgriOfficerDetails getAgriOfficerDetails(User user) {
        return agriOfficerDetailsRepository.findByUser(user).orElse(null);
    }

    /**
     * Approves an agri officer and sends the welcome email.
     */
    @Transactional
    public void approveOfficer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setAccountStatus(AccountStatus.APPROVED);
        userRepository.save(user);
        // We send a welcome email; password is not stored raw so we notify with a reset
        // nudge
        emailService.sendApprovalEmail(user, "[Use your registered password]");
        log.info("Officer '{}' approved.", user.getUsername());
    }

    /**
     * Rejects an agri officer registration and notifies by email.
     */
    @Transactional
    public void rejectOfficer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setAccountStatus(AccountStatus.REJECTED);
        userRepository.save(user);
        emailService.sendRejectionEmail(user);
        log.info("Officer '{}' rejected.", user.getUsername());
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    /**
     * Returns a user by ID.
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    /**
     * Returns all users with a specific role.
     */
    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    /**
     * Returns only APPROVED agri officers.
     */
    public List<User> getApprovedOfficers() {
        return userRepository.findByRoleAndAccountStatus(Role.AGRI_OFFICER, AccountStatus.APPROVED);
    }

    /**
     * Updates user details (no username/password change).
     */
    @Transactional
    public void updateUser(Long userId, String fullName, String nic, String email,
            String contactNumber, String address, String district,
            String province, String dob, String accountStatus) {
        User user = getUserById(userId);
        validateNIC(nic);
        user.setFullName(fullName);
        user.setNic(nic);
        user.setEmail(email);
        user.setContactNumber(contactNumber);
        user.setAddress(address);
        user.setDistrict(district);
        user.setProvince(province);
        if (dob != null && !dob.isBlank()) {
            user.setDob(LocalDate.parse(dob));
        }
        if (accountStatus != null && !accountStatus.isBlank()) {
            user.setAccountStatus(AccountStatus.valueOf(accountStatus));
        }
        userRepository.save(user);
        log.info("User '{}' updated by admin.", user.getUsername());
    }

    private void validateNIC(String nic) {
        if (nic == null || !nic.matches("^([0-9]{9}[a-zA-Z]|[0-9]{12})$")) {
            throw new IllegalArgumentException("Invalid NIC format. Must be 12 digits or 10 characters (9 digits + 1 letter).");
        }
    }

    /**
     * Updates user details by the user themselves (no role/status/username/password
     * change).
     */
    @Transactional
    public void updateUserProfile(Long userId, String fullName, String nic, String email,
            String contactNumber, String address, String district,
            String province, String dob) {
        User user = getUserById(userId);

        // Prevent changing NIC and Email if they already exist for another user
        if (!user.getNic().equals(nic) && userRepository.existsByNic(nic)) {
            throw new IllegalArgumentException("NIC already in use by another account.");
        }
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use by another account.");
        }

        validateNIC(nic);
        user.setFullName(fullName);
        user.setNic(nic);
        user.setEmail(email);
        user.setContactNumber(contactNumber);
        user.setAddress(address);
        user.setDistrict(district);
        user.setProvince(province);
        if (dob != null && !dob.isBlank()) {
            user.setDob(LocalDate.parse(dob));
        }
        userRepository.save(user);
        log.info("User profile '{}' updated by the user.", user.getUsername());
    }

    /**
     * Deletes a user by ID.
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserById(userId);
        // Delete officer details first if they exist
        if (user.getRole() == Role.AGRI_OFFICER) {
            agriOfficerDetailsRepository.findByUser(user)
                    .ifPresent(agriOfficerDetailsRepository::delete);
        }
        userRepository.delete(user);
        log.info("User '{}' deleted by admin.", user.getUsername());
    }

    /**
     * Returns the total count of registered farmers (Role.USER).
     */
    public long getFarmerCount() {
        return userRepository.countByRole(Role.USER);
    }

    /**
     * Returns the total count of registered agri experts (Role.AGRI_OFFICER).
     */
    public long getExpertCount() {
        return userRepository.countByRoleAndAccountStatus(Role.AGRI_OFFICER, AccountStatus.APPROVED);
    }

    /**
     * Returns a distribution of farmers by district.
     */
    public java.util.Map<String, Long> getFarmerCountByDistrict() {
        List<User> farmers = userRepository.findByRole(Role.USER);
        java.util.Map<String, Long> distribution = new java.util.HashMap<>();
        for (User f : farmers) {
            String dist = (f.getDistrict() == null || f.getDistrict().isBlank()) ? "Unknown" : f.getDistrict();
            distribution.put(dist, distribution.getOrDefault(dist, 0L) + 1);
        }
        return distribution;
    }

    /**
     * Administrative user creation (bypasses bot checks, auto-appproves).
     */
    @Transactional
    public User createUser(String fullName, String username, String email, String nic, String password, Role role,
            String contactNumber, String address, String district, String province, LocalDate dob,
            String registrationNumber, String designation, String specializationArea, String assignedArea, String officialEmail) {
        
        // Validate uniqueness
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered.");
        }
        if (userRepository.existsByNic(nic)) {
            throw new IllegalArgumentException("NIC already registered.");
        }

        validateNIC(nic);

        User user = User.builder()
                .fullName(fullName)
                .username(username)
                .email(email)
                .nic(nic)
                .password(passwordEncoder.encode(password))
                .role(role)
                .contactNumber(contactNumber)
                .address(address)
                .district(district)
                .province(province)
                .dob(dob)
                .accountStatus(AccountStatus.APPROVED) // Admin created are approved by default
                .build();

        user = userRepository.save(user);

        if (role == Role.AGRI_OFFICER) {
            // Validate mandatory officer fields
            if (registrationNumber == null || registrationNumber.trim().isEmpty()) {
                throw new RuntimeException("Registration number is required for Agri Officers.");
            }
            if (designation == null || designation.trim().isEmpty()) {
                throw new RuntimeException("Designation is required for Agri Officers.");
            }
            if (officialEmail == null || officialEmail.trim().isEmpty()) {
                throw new RuntimeException("Official email is required for Agri Officers.");
            }

            AgriOfficerDetails details = AgriOfficerDetails.builder()
                    .user(user)
                    .registrationNumber(registrationNumber)
                    .designation(designation)
                    .specializationArea(specializationArea)
                    .assignedArea(assignedArea)
                    .officialEmail(officialEmail)
                    .build();
            agriOfficerDetailsRepository.save(details);
        }

        log.info("Administrative user creation successful for: {}", username);
        return user;
    }

    /**
     * Returns a unique list of all districts currently assigned to users.
     */
    public List<String> getAllDistricts() {
        return userRepository.findAll().stream()
                .map(User::getDistrict)
                .filter(d -> d != null && !d.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Returns a unique list of all provinces currently assigned to users.
     */
    public List<String> getAllProvinces() {
        return userRepository.findAll().stream()
                .map(User::getProvince)
                .filter(p -> p != null && !p.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Generates a 6-digit OTP for password reset, saves it, and emails the user.
     */
    @Transactional
    public void generatePasswordResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No account found with that email address."));

        // Generate 6-digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        // Update existing token if it exists, otherwise create a new one
        // This avoids "Duplicate Entry" errors on the user_id unique constraint
        PasswordResetToken token = passwordResetTokenRepository.findByUser(user)
                .orElse(new PasswordResetToken());

        token.setUser(user);
        token.setToken(otp);
        token.setExpiryDate(java.time.LocalDateTime.now().plusMinutes(10)); // 10 minutes expiry

        passwordResetTokenRepository.save(token);

        emailService.sendPasswordResetOtpEmail(user, otp);
        log.info("Password reset OTP generated for '{}'.", email);
    }

    /**
     * Verifies if the provided OTP is valid and not expired.
     */
    public boolean verifyOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email."));

        PasswordResetToken token = passwordResetTokenRepository.findByUser(user)
                .orElse(null);

        if (token == null) {
            return false;
        }

        if (token.isExpired()) {
            passwordResetTokenRepository.delete(token);
            return false;
        }

        return token.getToken().equals(otp);
    }

    /**
     * Updates the password using an OTP, then invalidates the OTP.
     */
    @Transactional
    public void updatePasswordWithOtp(String email, String otp, String newPassword) {
        if (!verifyOtp(email, otp)) {
            throw new IllegalArgumentException("Invalid or expired OTP.");
        }

        User user = userRepository.findByEmail(email).get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Delete the token so it can't be reused
        passwordResetTokenRepository.deleteByUser(user);
        
        log.info("Password successfully reset for user '{}'", email);
    }
}
