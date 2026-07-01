package eclosia.eclosia_organization_service.classroom.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateClassroomDto {

    @NotNull(message = "Capacity is required")
    private Integer capacity;

    private Boolean active;

    private String description;

    @NotNull(message = "School id is required")
    private UUID schoolId;

    @NotNull(message = "Academic level id is required")
    private UUID academicLevelId;

    private UUID academicSectionId;

    private UUID academicOptionId;

    @NotNull(message = "Classroom designation id is required")
    private UUID classroomDesignationId;
}
