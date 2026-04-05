package com.goviconnect.repository;

import com.goviconnect.entity.User;
import com.goviconnect.enums.AccountStatus;
import com.goviconnect.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByNic(String nic);

    List<User> findByRoleAndAccountStatus(Role role, AccountStatus accountStatus);

    List<User> findByRole(Role role);

    long countByRole(Role role);

    long countByRoleAndAccountStatus(Role role, AccountStatus accountStatus);
}
