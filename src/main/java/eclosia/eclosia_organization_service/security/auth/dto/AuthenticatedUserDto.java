package eclosia.eclosia_organization_service.security.auth.dto;

import eclosia.eclosia_organization_service.feature.entity.Feature;
import eclosia.eclosia_organization_service.role.entity.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AuthenticatedUserDto {

    private UUID id;
    private UUID keycloakId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private UUID groupId;
    private List<UUID> schoolIds;
    private Boolean active;
    private List<Role> roles;
    private List<Feature> permissions;
}
