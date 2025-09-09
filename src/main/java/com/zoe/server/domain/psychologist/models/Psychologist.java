package com.zoe.server.domain.psychologist.models;

import com.zoe.server.domain.user.models.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Psychologist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String crpNumber;
    private String phoneNumber;
    @Column(nullable = true)
    private String practiceAddress;
    @Column(nullable = true)
    private String state;
    @Column(nullable = true)
    private String City;
    @Column(nullable = true)
    private String zipCode;
    private boolean agreedTermServiceAndPrivacyPolicy;
    private boolean agreedHippaCompliance;
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
}