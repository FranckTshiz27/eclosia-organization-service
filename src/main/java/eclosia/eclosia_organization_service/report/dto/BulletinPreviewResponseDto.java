package eclosia.eclosia_organization_service.report.dto;

import eclosia.eclosia_organization_service.report.enums.BulletinPrintMode;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class BulletinPreviewResponseDto {

    private BulletinPrintMode mode;
    private UUID schoolId;
    private UUID academicYearId;
    private int totalStudents;
    private int totalClassrooms;
    private List<ClassroomPreviewDto> classrooms = new ArrayList<>();

    @Data
    public static class ClassroomPreviewDto {
        private UUID classroomId;
        private String classroomName;
        private int studentCount;
    }
}
