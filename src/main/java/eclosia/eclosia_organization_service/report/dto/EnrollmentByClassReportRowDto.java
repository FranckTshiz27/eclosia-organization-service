package eclosia.eclosia_organization_service.report.dto;

import lombok.Data;

@Data
public class EnrollmentByClassReportRowDto {

    private Integer rowNumber;
    private String classroomName;
    private String photoPath;
    private String studentNumber;
    private String fullName;
    private String gender;
    private String birthDate;
    private String birthPlace;
    private String tutorName;
    private String phoneNumber;
}
