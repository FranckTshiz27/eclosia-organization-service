package eclosia.eclosia_organization_service.report.dto;

import eclosia.eclosia_organization_service.report.enums.BulletinFormat;
import eclosia.eclosia_organization_service.report.enums.BulletinPrintMode;
import eclosia.eclosia_organization_service.report.enums.BulletinSortBy;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BulletinPrintRequestDto {

    @NotNull(message = "Mode is required")
    private BulletinPrintMode mode;

    @NotNull(message = "School id is required")
    private UUID schoolId;

    @NotNull(message = "Academic year id is required")
    private UUID academicYearId;

    private BulletinFormat format = BulletinFormat.OFFICIEL;

    private BulletinSortBy sortBy = BulletinSortBy.CLASS_THEN_ALPHABETICAL;

    private Boolean includeCoverPage = false;

    private Boolean includeSignatures = true;

    private Boolean includeStudentRank = true;

    private Boolean includeClassAverages = false;

    private List<UUID> classroomIds;

    private UUID academicCycleId;

    private UUID studentEnrollmentId;
}
