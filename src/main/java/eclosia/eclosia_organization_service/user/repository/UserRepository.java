package eclosia.eclosia_organization_service.user.repository;

import eclosia.eclosia_organization_service.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    List<User> findAllByOrderByUsernameAsc();

    List<User> findByGroup_IdOrderByUsernameAsc(UUID groupId);

    List<User> findBySchools_IdOrderByUsernameAsc(UUID schoolId);

    List<User> findByRoles_IdOrderByUsernameAsc(UUID roleId);

    Optional<User> findByKeycloakId(UUID keycloakId);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, UUID id);

    boolean existsByKeycloakId(UUID keycloakId);
}
