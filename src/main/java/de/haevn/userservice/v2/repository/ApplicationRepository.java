package de.haevn.userservice.v2.repository;

import de.haevn.userservice.v2.model.SsoApplication;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<SsoApplication, String> {
    public Optional<SsoApplication> findByClientId(String clientId);
}
