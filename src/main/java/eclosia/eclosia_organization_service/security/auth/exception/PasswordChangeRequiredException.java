package eclosia.eclosia_organization_service.security.auth.exception;

import lombok.Getter;

/**
 * Mot de passe temporaire Keycloak : le front doit afficher le formulaire de changement.
 */
@Getter
public class PasswordChangeRequiredException extends RuntimeException {

    public static final String CODE = "PASSWORD_CHANGE_REQUIRED";

    private final String username;

    public PasswordChangeRequiredException(String username) {
        super("Password change required before login");
        this.username = username;
    }
}
