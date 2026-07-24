package eclosia.eclosia_organization_service.security.auth.service;

import eclosia.eclosia_organization_service.role.entity.Role;
import eclosia.eclosia_organization_service.security.auth.exception.UnauthorizedException;
import eclosia.eclosia_organization_service.user.entity.User;
import eclosia.eclosia_organization_service.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Résout l'utilisateur local depuis le JWT (claim {@code sub} = keycloakId)
 * et expose le périmètre (rôles / écoles / groupe).
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    public static final String ROLE_USER_ADMIN = "USER_ADMIN";
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UnauthorizedException("Authentication required");
        }

        UUID keycloakId;
        try {
            keycloakId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid token subject");
        }

        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UnauthorizedException("Local user profile not found"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new UnauthorizedException("User account is disabled");
        }

        initializeAssociations(user);
        return user;
    }

    /**
     * Super admin / user admin : accès à tout (groupes, écoles, classes, …).
     */
    public boolean isUserAdmin(User user) {
        return hasRole(user, ROLE_SUPER_ADMIN) || hasRole(user, ROLE_USER_ADMIN);
    }

    public boolean isSuperAdmin(User user) {
        return hasRole(user, ROLE_SUPER_ADMIN);
    }

    public boolean hasRole(User user, String roleCode) {
        if (user == null || user.getRoles() == null || roleCode == null) {
            return false;
        }
        String expected = roleCode.trim().toUpperCase(Locale.ROOT);
        return user.getRoles().stream()
                .filter(role -> Boolean.TRUE.equals(role.getActive()))
                .map(Role::getCode)
                .filter(code -> code != null)
                .anyMatch(code -> code.trim().toUpperCase(Locale.ROOT).equals(expected));
    }

    public Set<UUID> schoolIds(User user) {
        if (user == null || user.getSchools() == null) {
            return Set.of();
        }
        return user.getSchools().stream()
                .map(school -> school.getId())
                .collect(Collectors.toSet());
    }

    private void initializeAssociations(User user) {
        if (user.getGroup() != null) {
            user.getGroup().getId();
        }
        user.getRoles().size();
        user.getSchools().size();
    }
}
