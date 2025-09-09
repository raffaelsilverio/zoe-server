package com.zoe.server.domain.auth.mappers;

import com.zoe.server.domain.auth.dtos.RegisterPatientRequestDto;
import com.zoe.server.domain.auth.dtos.RegisterPsychologistRequestDto;
import com.zoe.server.domain.auth.dtos.RegisterResponseDto;
import com.zoe.server.domain.user.enums.UserRole;
import com.zoe.server.domain.user.models.User;
import com.zoe.server.domain.user.models.UserCredentials;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring", imports = {UserRole.class})
public abstract class AuthMapper {

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userCredentials", expression = "java(buildUserCredentials(request.getEmail(), request.getPassword(), UserRole.PSYCHOLOGIST))")
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "psychologist", source = ".")
    @Mapping(target = "psychologist.id", ignore = true)
    @Mapping(target = "psychologist.user", ignore = true)
    public abstract User toPsychologistUser(RegisterPsychologistRequestDto request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userCredentials", expression = "java(buildUserCredentials(request.getEmail(), request.getPassword(), UserRole.PATIENT))")
    @Mapping(target = "psychologist", ignore = true)
    @Mapping(target = "patient", source = ".")
    @Mapping(target = "patient.id", ignore = true)
    @Mapping(target = "patient.cpf", ignore = true)
    @Mapping(target = "patient.user", ignore = true)
    public abstract User toPatientUser(RegisterPatientRequestDto request);

    public RegisterResponseDto toRegisterResponse(User user) {
        return new RegisterResponseDto(
                user.getUserCredentials().getEmail(),
                user.getUserCredentials().getUserRole()
        );
    }
    protected UserCredentials buildUserCredentials(String email, String password, UserRole role) {
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