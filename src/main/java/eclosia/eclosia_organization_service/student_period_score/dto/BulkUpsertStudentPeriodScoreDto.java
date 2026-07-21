package eclosia.eclosia_organization_service.student_period_score.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkUpsertStudentPeriodScoreDto {

    @NotEmpty(message = "Scores list is required")
    @Valid
    private List<UpsertStudentPeriodScoreDto> scores;
}
