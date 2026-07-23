package eclosia.eclosia_organization_service.teacher.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.group.entity.Group;
import eclosia.eclosia_organization_service.group.repository.GroupRepository;
import eclosia.eclosia_organization_service.role.entity.Role;
import eclosia.eclosia_organization_service.role.repository.RoleRepository;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import eclosia.eclosia_organization_service.security.keycloak.KeycloakService;
import eclosia.eclosia_organization_service.security.auth.service.CurrentUserService;
import eclosia.eclosia_organization_service.teacher.dto.CreateTeacherDto;
import eclosia.eclosia_organization_service.teacher.dto.TeacherResponseDto;
import eclosia.eclosia_organization_service.teacher.dto.UpdateTeacherDto;
import eclosia.eclosia_organization_service.teacher.entity.Teacher;
import eclosia.eclosia_organization_service.teacher.mapper.TeacherMapper;
import eclosia.eclosia_organization_service.teacher.repository.TeacherRepository;
import eclosia.eclosia_organization_service.user.entity.User;
import eclosia.eclosia_organization_service.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherService {

    private static final String PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int TEMP_PASSWORD_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;
    private final KeycloakService keycloakService;
    private final TeacherMapper teacherMapper;
    private final CurrentUserService currentUserService;

    @Transactional
    public TeacherResponseDto create(CreateTeacherDto dto) {
        String email = normalizeEmail(dto.getEmail());
        String phone = normalizePhone(dto.getPhone());
        String registrationNumber = dto.getRegistrationNumber().trim();

        validateUniquenessForCreate(email, phone, registrationNumber);

        School school = resolveSchool(dto.getSchoolId());
        Group group = resolveGroupFromSchool(school);
        Set<Role> roles = resolveRoles(dto.getRoleIds());

        String username = resolveUsername(dto.getUsername(), dto.getFirstName(), dto.getLastName());
        String temporaryPassword = resolveInitialPassword(dto.getPassword());

        UUID keycloakId = keycloakService.createUser(
                username,
                email,
                dto.getFirstName().trim(),
                dto.getLastName().trim(),
                temporaryPassword,
                true
        );

        try {
            keycloakService.assignRealmRoles(
                    keycloakId,
                    roles.stream().map(Role::getCode).toList()
            );

            User user = new User();
            user.setKeycloakId(keycloakId);
            user.setUsername(username);
            user.setEmail(email);
            user.setFirstName(dto.getFirstName().trim());
            user.setLastName(dto.getLastName().trim());
            user.setMiddleName(trimToNull(dto.getMiddleName()));
            user.setPhone(phone);
            user.setGender(dto.getGender());
            user.setBirthDate(dto.getBirthDate());
            user.setAddress(trimToNull(dto.getAddress()));
            user.setGroup(group);
            user.setRoles(roles);
            user.setSchools(new HashSet<>(Set.of(school)));
            user.setActive(true);
            User savedUser = userRepository.save(user);

            Teacher teacher = new Teacher();
            teacher.setSecurityUser(savedUser);
            teacher.setSchool(school);
            teacher.setRegistrationNumber(registrationNumber);
            teacher.setHiringDate(dto.getHiringDate());
            teacher.setLeavingDate(dto.getLeavingDate());
            teacher.setQualification(trimToNull(dto.getQualification()));
            teacher.setSpecialty(trimToNull(dto.getSpecialty()));
            teacher.setGrade(trimToNull(dto.getGrade()));
            teacher.setSignature(trimToNull(dto.getSignature()));
            teacher.setTitular(Boolean.TRUE.equals(dto.getTitular()));
            teacher.setActive(true);
            teacher.setRemarks(trimToNull(dto.getRemarks()));

            Teacher savedTeacher = teacherRepository.save(teacher);
            return teacherMapper
                    .toResponse(requireDetailed(savedTeacher.getId()), temporaryPassword)
                    .toBuilder()
                    .username(username)
                    .temporaryPassword(temporaryPassword)
                    .build();
        } catch (RuntimeException ex) {
            rollbackKeycloakUser(keycloakId);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<TeacherResponseDto> findAll() {
        return teacherRepository.findAllDetailed().stream()
                .map(teacherMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherResponseDto> findBySchoolId(UUID schoolId) {
        resolveSchool(schoolId);
        return teacherRepository.findDetailedBySchoolId(schoolId).stream()
                .map(teacherMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherResponseDto findById(UUID id) {
        return teacherMapper.toResponse(requireDetailed(id));
    }

    /** Fiche enseignant de l'utilisateur connecté (404 si non enseignant). */
    @Transactional(readOnly = true)
    public TeacherResponseDto findCurrentTeacher() {
        User currentUser = currentUserService.requireCurrentUser();
        Teacher teacher = teacherRepository.findBySecurityUser_IdAndActiveTrue(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found for current user"));
        return teacherMapper.toResponse(requireDetailed(teacher.getId()));
    }

    @Transactional
    public TeacherResponseDto update(UUID id, UpdateTeacherDto dto) {
        Teacher teacher = requireDetailed(id);
        User user = teacher.getSecurityUser();

        String email = normalizeEmail(dto.getEmail());
        String phone = normalizePhone(dto.getPhone());
        String registrationNumber = dto.getRegistrationNumber().trim();

        if (userRepository.existsByEmailAndIdNot(email, user.getId())) {
            throw new BadRequestException("Email already exists");
        }
        if (userRepository.existsByPhoneAndIdNot(phone, user.getId())) {
            throw new BadRequestException("Phone already exists");
        }
        if (teacherRepository.existsByRegistrationNumberAndIdNot(registrationNumber, id)) {
            throw new BadRequestException("Teacher registration number already exists");
        }

        School school = resolveSchool(dto.getSchoolId());
        Group group = resolveGroupFromSchool(school);
        Set<Role> newRoles = resolveRoles(dto.getRoleIds());

        syncKeycloakRoles(user.getKeycloakId(), user.getRoles(), newRoles);

        Boolean active = dto.getActive() != null ? dto.getActive() : teacher.getActive();
        keycloakService.updateUser(
                user.getKeycloakId(),
                email,
                dto.getFirstName().trim(),
                dto.getLastName().trim(),
                active
        );

        user.setEmail(email);
        user.setFirstName(dto.getFirstName().trim());
        user.setLastName(dto.getLastName().trim());
        user.setMiddleName(trimToNull(dto.getMiddleName()));
        user.setPhone(phone);
        user.setGender(dto.getGender());
        user.setBirthDate(dto.getBirthDate());
        user.setAddress(trimToNull(dto.getAddress()));
        user.setGroup(group);
        user.setRoles(newRoles);
        user.setSchools(new HashSet<>(Set.of(school)));
        user.setActive(active);
        userRepository.save(user);

        teacher.setSchool(school);
        teacher.setRegistrationNumber(registrationNumber);
        teacher.setHiringDate(dto.getHiringDate());
        teacher.setLeavingDate(dto.getLeavingDate());
        teacher.setQualification(trimToNull(dto.getQualification()));
        teacher.setSpecialty(trimToNull(dto.getSpecialty()));
        teacher.setGrade(trimToNull(dto.getGrade()));
        teacher.setSignature(trimToNull(dto.getSignature()));
        teacher.setTitular(Boolean.TRUE.equals(dto.getTitular()));
        teacher.setActive(active);
        teacher.setRemarks(trimToNull(dto.getRemarks()));
        teacherRepository.save(teacher);

        return teacherMapper.toResponse(requireDetailed(id));
    }

    @Transactional
    public void delete(UUID id) {
        Teacher teacher = requireDetailed(id);
        User user = teacher.getSecurityUser();
        UUID keycloakId = user.getKeycloakId();

        teacherRepository.delete(teacher);
        userRepository.delete(user);

        try {
            keycloakService.deleteUser(keycloakId);
        } catch (Exception ex) {
            log.error("Teacher local delete succeeded but Keycloak cleanup failed for {}: {}",
                    keycloakId, ex.getMessage());
            throw ex;
        }
    }

    private void validateUniquenessForCreate(String email, String phone, String registrationNumber) {
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already exists");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new BadRequestException("Phone already exists");
        }
        if (teacherRepository.existsByRegistrationNumber(registrationNumber)) {
            throw new BadRequestException("Teacher registration number already exists");
        }
    }

    private String resolveUsername(String requestedUsername, String firstName, String lastName) {
        String username;
        if (requestedUsername != null && !requestedUsername.isBlank()) {
            username = requestedUsername.trim().toLowerCase(Locale.ROOT);
        } else {
            username = buildUsernameBase(firstName, lastName);
        }

        if (!userRepository.existsByUsername(username)) {
            return username;
        }

        int suffix = 1;
        String candidate;
        do {
            candidate = username + suffix;
            suffix++;
        } while (userRepository.existsByUsername(candidate));

        return candidate;
    }

    private String buildUsernameBase(String firstName, String lastName) {
        String first = sanitizeUsernamePart(firstName);
        String last = sanitizeUsernamePart(lastName);
        if (first.isBlank() && last.isBlank()) {
            throw new BadRequestException("Unable to generate username from first/last name");
        }
        if (first.isBlank()) {
            return last;
        }
        if (last.isBlank()) {
            return first;
        }
        return first + "." + last;
    }

    private String sanitizeUsernamePart(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9]", "");
        return normalized;
    }

    private String resolveInitialPassword(String requestedPassword) {
        String password = trimToNull(requestedPassword);
        if (password != null) {
            return password;
        }
        return generateTemporaryPassword();
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }

    private void syncKeycloakRoles(UUID keycloakId, Set<Role> currentRoles, Set<Role> newRoles) {
        Set<String> currentCodes = currentRoles == null
                ? Set.of()
                : currentRoles.stream().map(Role::getCode).collect(Collectors.toSet());
        Set<String> newCodes = newRoles.stream().map(Role::getCode).collect(Collectors.toSet());

        for (String code : newCodes) {
            if (!currentCodes.contains(code)) {
                keycloakService.assignRealmRole(keycloakId, code);
            }
        }
        for (String code : currentCodes) {
            if (!newCodes.contains(code)) {
                keycloakService.removeRealmRole(keycloakId, code);
            }
        }
    }

    private void rollbackKeycloakUser(UUID keycloakId) {
        try {
            keycloakService.deleteUser(keycloakId);
        } catch (Exception cleanupError) {
            log.warn("Failed to rollback Keycloak user {}: {}", keycloakId, cleanupError.getMessage());
        }
    }

    private Teacher requireDetailed(UUID id) {
        return teacherRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
    }

    private School resolveSchool(UUID schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
    }

    private Group resolveGroupFromSchool(School school) {
        if (school.getGroupId() == null) {
            throw new BadRequestException("School must belong to a group");
        }
        return groupRepository.findById(school.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group not found for school"));
    }

    private Set<Role> resolveRoles(Set<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BadRequestException("At least one role is required");
        }
        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new ResourceNotFoundException("One or more roles were not found");
        }
        boolean inactive = roles.stream().anyMatch(role -> !Boolean.TRUE.equals(role.getActive()));
        if (inactive) {
            throw new BadRequestException("One or more roles are inactive");
        }
        return new HashSet<>(roles);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        return phone.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
