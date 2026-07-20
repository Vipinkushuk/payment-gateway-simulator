package com.paymentgateway.repository;

import com.paymentgateway.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    // Spring generates SQL: SELECT * FROM merchants WHERE email = ?
    Optional<Merchant> findByEmail(String email);

    // Spring generates SQL: SELECT * FROM merchants WHERE api_key = ?
    Optional<Merchant> findByApiKey(String apiKey);

    // Spring generates SQL: SELECT COUNT(*) > 0 FROM merchants WHERE email = ?
    boolean existsByEmail(String email);
}