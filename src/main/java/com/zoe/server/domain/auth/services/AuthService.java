package com.zoe.server.domain.auth.services;

import com.zoe.server.domain.auth.dtos.*;
import com.zoe.server.domain.auth.exceptions.AuthenticationException;
import com.zoe.server.domain.auth.exceptions.InvalidCredentialsException;
import com.zoe.server.domain.auth.mappers.AuthMapper;
import com.zoe.server.domain.user.enums.UserRole;
import com.zoe.server.domain.user.models.CustomUserDetails;
import com.zoe.server.domain.user.models.User;
import com.zoe.server.domain.user.models.UserCredentials;
import com.zoe.server.domain.user.repositories.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthMapper authMapper;

    public AuthService(
            AuthenticationManager authManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            AuthMapper authMapper
    ) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.authMapper = authMapper;
    }

    @Transactional
    public AuthResponseDto refreshToken(String oldToken) {
        if (oldToken == null || oldToken.trim().isEmpty()) {
            throw new AuthenticationException("Refresh token cannot be null or empty");
        }
        String email = jwtService.extractEmailFromRefreshToken(oldToken);
        Long userId = refreshTokenService.extractUserIdFromRefreshToken(oldToken);
        UserRole userRole = refreshTokenService.extractUserRoleFromRefreshToken(oldToken);
        String newAccessToken = jwtService.generateAccessToken(email);
        String newRefreshToken = refreshTokenService.createRefreshToken(userId, userRole);
        return new AuthResponseDto(newAccessToken, newRefreshToken);
    }

    @Transactional
    public AuthResponseDto login(AuthRequestDto request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new AuthenticationException("Email and password cannot be null");
        }
        try {
            var authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail() , request.getPassword() )
            );
            Object principal = authentication.getPrincipal();
            String userEmail;
            Long id;
            UserRole userRole;
            if (principal instanceof CustomUserDetails userDetails) {
                userEmail = userDetails.getUsername();
                id = userDetails.getUser().getId();
                userRole = userDetails.getUser().getUserCredentials().getUserRole();
            } else {
                throw new AuthenticationException("Invalid principal type");
            }
            String accessToken = jwtService.generateAccessToken(userEmail);
            String refreshToken = refreshTokenService.createRefreshToken(id, userRole);
            return new AuthResponseDto(accessToken, refreshToken);
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password", e);
        }
    }

    @Transactional
    public RegisterResponseDto registerPsychologist(RegisterPsychologistRequestDto request) {
        if (userRepository.existsByUserCredentials_Email(request.getEmail())) {
            throw new IllegalArgumentException("Email já registrado");
        }
        if(!Objects.equals(request.getPassword(), request.getConfirmPassword())) throw new IllegalArgumentException("Passwords do not match!");
        User user = authMapper.toPsychologistUser(request);
        user.getPsychologist().setUser(user);
        userRepository.save(user);
        return authMapper.toRegisterResponse(user);
    }

    @Transactional
    public RegisterResponseDto registerPatient(RegisterPatientRequestDto request) {
        if (userRepository.existsByUserCredentials_Email(request.getEmail())) {
            throw new IllegalArgumentException("Email already register");
        }
        if(!Objects.equals(request.getPassword(), request.getConfirmPassword())) throw new IllegalArgumentException("Passwords do not match!");
        User user = authMapper.toPatientUser(request);
        user.getPatient().setUser(user);
        userRepository.save(user);
        return authMapper.toRegisterResponse(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new AuthenticationException("Refresh token cannot be null or empty");
        }
        refreshTokenService.revokeRefreshToken(refreshToken, "User logout");
    }
    private UserCredentials buildUserCredentials(String email, String password, UserRole role) {
        UserCredentials credentials = new UserCredentials();
        credentials.setEmail(email);
        credentials.setPasswordHash(passwordEncoder.encode(password));
        credentials.setUserRole(role);
        credentials.setActive(true);
        credentials.setLocked(false);
        credentials.setPasswordLastChanged(LocalDateTime.now());
        return credentials;
    }

}
