package com.oursecurity.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    @Query("select d.token from DeviceToken d where d.user.id in :userIds and d.user.active = true")
    List<String> findTokensForUsers(Collection<Long> userIds);

    @Modifying
    @Query("delete from DeviceToken d where d.token in :tokens")
    int deleteByTokens(Collection<String> tokens);
}
