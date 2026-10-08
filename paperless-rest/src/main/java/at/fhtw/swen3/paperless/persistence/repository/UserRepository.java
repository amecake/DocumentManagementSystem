package at.fhtw.swen3.paperless.persistence.repository;

import at.fhtw.swen3.paperless.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);
}
