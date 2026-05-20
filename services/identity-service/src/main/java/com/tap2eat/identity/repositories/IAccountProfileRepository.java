package com.tap2eat.identity.repositories;

import com.tap2eat.identity.models.AccountProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IAccountProfileRepository extends JpaRepository<AccountProfile, UUID> {

    Optional<AccountProfile> findByAccount_Id(UUID accountId);
}