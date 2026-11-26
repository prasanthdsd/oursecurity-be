package com.oursecurity.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    List<User> findByActiveTrueOrderByNameAsc();

    List<User> findByRoleAndActiveTrueOrderByNameAsc(Role role);

    List<User> findByRoleInAndActiveTrue(Collection<Role> roles);

    List<User> findByRoleAndFlatNumberIgnoreCaseAndActiveTrue(Role role, String flatNumber);

    long countByRoleAndActiveTrue(Role role);
}
