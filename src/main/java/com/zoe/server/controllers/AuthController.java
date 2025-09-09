package com.zoe.server.controllers;

import com.zoe.server.domain.auth.dtos.*;
import com.zoe.server.domain.auth.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/psychologists/register")
    public ResponseEntity<RegisterResponseDto> registerPsychologist(
            @RequestBody RegisterPsychologistRequestDto request) {

        RegisterResponseDto response = authService.registerPsychologist(request);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/patients/register")
    public ResponseEntity<RegisterResponseDto> registerPatient(
            @RequestBody RegisterPatientRequestDto request) {

        RegisterResponseDto response = authService.registerPatient(request);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(AuthRequestDto request) {

        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponseDto> refreshToken(
            @RequestParam("refreshToken") String refreshToken) {
        AuthResponseDto response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }
}
