package eclosia.eclosia_organization_service.teacher.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class TeacherRoleDto {

    private UUID id;
    private String code;
    private String name;
}
