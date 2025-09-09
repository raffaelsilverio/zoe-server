package com.zoe.server.domain.user.models;

import com.zoe.server.domain.patient.models.Patient;
import com.zoe.server.domain.psychologist.models.Psychologist;
import com.zoe.server.domain.user.enums.UserRole;
import jakarta.persistence.*;

public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private UserRole role;
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Psychologist psychologist;
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Patient patient;
}
