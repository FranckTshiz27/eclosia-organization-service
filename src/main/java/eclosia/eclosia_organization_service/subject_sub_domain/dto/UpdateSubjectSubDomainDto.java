package eclosia.eclosia_organization_service.subject_sub_domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateSubjectSubDomainDto {

    @NotNull(message = "Subject domain id is required")
    private UUID subjectDomainId;

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    private Integer displayOrder;

    private Boolean active;
}
