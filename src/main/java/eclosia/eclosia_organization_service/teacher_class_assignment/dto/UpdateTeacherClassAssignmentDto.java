package eclosia.eclosia_organization_service.teacher_class_assignment.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTeacherClassAssignmentDto {

    @Size(max = 1000)
    private String remarks;

    private Boolean active;
}
