package com.goviconnect.dto;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationDto {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "NIC is required")
    private String nic;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^0\\d{9}$", message = "Contact number must be 10 digits starting with 0")
    private String contactNumber;

    private String address;
    private String district;
    private String province;

    @Past(message = "Date of birth must be in the past")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 30, message = "Username must be 4–30 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    // Agri Officer fields
    private boolean agriOfficer;
    private String registrationNumber;
    private String designation;
    private String specializationArea;
    private String assignedArea;
    private String officialEmail;

    // Turnstile token sent from the form
    @NotBlank(message = "Bot verification is required")
    private String cfTurnstileResponse;

    // Explicit Getters and Setters to ensure compilation if Lombok fails
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isAgriOfficer() { return agriOfficer; }
    public void setAgriOfficer(boolean agriOfficer) { this.agriOfficer = agriOfficer; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getSpecializationArea() { return specializationArea; }
    public void setSpecializationArea(String specializationArea) { this.specializationArea = specializationArea; }

    public String getAssignedArea() { return assignedArea; }
    public void setAssignedArea(String assignedArea) { this.assignedArea = assignedArea; }

    public String getOfficialEmail() { return officialEmail; }
    public void setOfficialEmail(String officialEmail) { this.officialEmail = officialEmail; }

    public String getCfTurnstileResponse() { return cfTurnstileResponse; }
    public void setCfTurnstileResponse(String cfTurnstileResponse) { this.cfTurnstileResponse = cfTurnstileResponse; }
}
