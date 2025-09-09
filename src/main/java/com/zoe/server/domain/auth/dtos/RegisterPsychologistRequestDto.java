package com.zoe.server.domain.auth.dtos;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterPsychologistRequestDto {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotBlank(message = "CRP number is required")
    @Pattern(regexp = "^\\d{2}/\\d{6}$", message = "CRP number must be in format XX/XXXXXX")
    private String crpNumber;
    @NotBlank
    private String phoneNumber;

    private String practiceAddress;

    private String state;

    private String City;

    private String zipCode;
    @AssertTrue
    private boolean agreedTermServiceAndPrivacyPolicy;
    @AssertTrue
    private boolean agreedHippaCompliance;
}
