package at.fhtw.swen3.paperless.persistence;

import at.fhtw.swen3.paperless.persistence.entity.UserEntity;
import at.fhtw.swen3.paperless.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUsername_ignoresCase() {
        UserEntity user = new UserEntity();
        user.setUsername("Jarod");
        user.setEmail("jarod@example.com");
        user.setPasswordHash("$2a$10$hash");
        userRepository.saveAndFlush(user);

        assertThat(userRepository.findByUsernameIgnoreCase("jarod")).isPresent();
        assertThat(userRepository.existsByUsernameIgnoreCase("JAROD")).isTrue();
        assertThat(user.getCreatedAt()).isNotNull();
    }
}
