package eclosia.eclosia_organization_service.user.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.group.entity.Group;
import eclosia.eclosia_organization_service.group.repository.GroupRepository;
import eclosia.eclosia_organization_service.role.entity.Role;
import eclosia.eclosia_organization_service.role.repository.RoleRepository;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import eclosia.eclosia_organization_service.user.dto.CreateUserDto;
import eclosia.eclosia_organization_service.user.dto.UpdateUserDto;
import eclosia.eclosia_organization_service.user.entity.User;
import eclosia.eclosia_organization_service.user.repository.UserRepository;
import eclosia.eclosia_organization_service.security.keycloak.KeycloakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final SchoolRepository schoolRepository;
    private final GroupRepository groupRepository;
    private final KeycloakService keycloakService;

    @Transactional
    public User create(CreateUserDto dto) {
        String username = normalizeUsername(dto.getUsername());
        String email = normalizeEmail(dto.getEmail());

        validateLocalUniqueness(username, email);

        ResolvedScope scope = resolveScope(dto.getGroupId(), dto.getSchoolIds());
        Set<Role> roles = resolveRoles(dto.getRoleIds());

        UUID keycloakId = keycloakService.createUser(
                username,
                email,
                dto.getFirstName(),
                dto.getLastName(),
                dto.getPassword(),
                Boolean.TRUE.equals(dto.getTemporaryPassword())
        );

        try {
            User user = new User();
            user.setKeycloakId(keycloakId);
            user.setUsername(username);
            user.setEmail(email);
            user.setFirstName(dto.getFirstName());
            user.setLastName(dto.getLastName());
            user.setGroup(scope.group());
            user.setRoles(roles);
            user.setSchools(scope.schools());
            user.setActive(dto.getActive() != null ? dto.getActive() : true);
            User saved = repository.save(user);
            initializeAssociations(saved);
            return saved;
        } catch (RuntimeException ex) {
            try {
                keycloakService.deleteUser(keycloakId);
            } catch (Exception cleanupError) {
                log.warn("Failed to rollback Keycloak user {}: {}", keycloakId, cleanupError.getMessage());
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        List<User> users = repository.findAllByOrderByUsernameAsc();
        users.forEach(this::initializeAssociations);
        return users;
    }

    @Transactional(readOnly = true)
    public List<User> findByGroupId(UUID groupId) {
        List<User> users = repository.findByGroup_IdOrderByUsernameAsc(groupId);
        users.forEach(this::initializeAssociations);
        return users;
    }

    @Transactional(readOnly = true)
    public List<User> findBySchoolId(UUID schoolId) {
        List<User> users = repository.findBySchools_IdOrderByUsernameAsc(schoolId);
        users.forEach(this::initializeAssociations);
        return users;
    }

    @Transactional(readOnly = true)
    public List<User> findByRoleId(UUID roleId) {
        List<User> users = repository.findByRoles_IdOrderByUsernameAsc(roleId);
        users.forEach(this::initializeAssociations);
        return users;
    }

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        initializeAssociations(user);
        return user;
    }

    @Transactional(readOnly = true)
    public User findByKeycloakId(UUID keycloakId) {
        User user = repository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for Keycloak id"));
        initializeAssociations(user);
        return user;
    }

    @Transactional
    public User update(UUID id, UpdateUserDto dto) {
        User user = findById(id);
        String email = normalizeEmail(dto.getEmail());

        if (email != null && repository.existsByEmailAndIdNot(email, id)) {
            throw new BadRequestException("Email already exists");
        }

        ResolvedScope scope = resolveScope(dto.getGroupId(), dto.getSchoolIds());
        Set<Role> roles = resolveRoles(dto.getRoleIds());
        Boolean active = dto.getActive() != null ? dto.getActive() : user.getActive();

        keycloakService.updateUser(
                user.getKeycloakId(),
                email,
                dto.getFirstName(),
                dto.getLastName(),
                active
        );

        user.setEmail(email);
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setGroup(scope.group());
        user.setRoles(roles);
        user.setSchools(scope.schools());
        user.setActive(active);

        User saved = repository.save(user);
        initializeAssociations(saved);
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        User user = findById(id);
        keycloakService.deleteUser(user.getKeycloakId());
        repository.delete(user);
    }

    private ResolvedScope resolveScope(UUID groupId, Set<UUID> schoolIds) {
        boolean hasSchools = schoolIds != null && !schoolIds.isEmpty();

        if (!hasSchools) {
            if (groupId == null) {
                throw new BadRequestException(
                        "Provide groupId, or at least one schoolId"
                );
            }
            return new ResolvedScope(resolveGroup(groupId), new HashSet<>());
        }

        List<School> schools = schoolRepository.findAllById(schoolIds);
        if (schools.size() != schoolIds.size()) {
            throw new ResourceNotFoundException("One or more schools were not found");
        }

        Set<UUID> schoolGroupIds = schools.stream()
                .map(School::getGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (schoolGroupIds.isEmpty()) {
            throw new BadRequestException("Selected schools must belong to a group");
        }
        if (schoolGroupIds.size() > 1) {
            throw new BadRequestException("All schools must belong to the same group");
        }

        UUID derivedGroupId = schoolGroupIds.iterator().next();
        if (groupId != null && !groupId.equals(derivedGroupId)) {
            throw new BadRequestException("groupId does not match the group of the selected schools");
        }

        return new ResolvedScope(resolveGroup(derivedGroupId), new HashSet<>(schools));
    }

    private Set<Role> resolveRoles(Set<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BadRequestException("At least one role is required");
        }

        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new ResourceNotFoundException("One or more roles were not found");
        }
        return new HashSet<>(roles);
    }

    private Group resolveGroup(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
    }

    private void validateLocalUniqueness(String username, String email) {
        if (repository.existsByUsername(username)) {
            throw new BadRequestException("Username already exists");
        }
        if (email != null && repository.existsByEmail(email)) {
            throw new BadRequestException("Email already exists");
        }
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private void initializeAssociations(User user) {
        if (user.getGroup() != null) {
            user.getGroup().getId();
        }
        user.getRoles().size();
        user.getSchools().size();
    }

    private record ResolvedScope(Group group, Set<School> schools) {
    }
}
