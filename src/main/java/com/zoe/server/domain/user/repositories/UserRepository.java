package com.zoe.server.domain.user.repositories;

import com.zoe.server.domain.user.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository <User, Long>{
    Optional<User> findByUserCredentialsEmail(String email);

    boolean existsByUserCredentials_Email(String email);
}
