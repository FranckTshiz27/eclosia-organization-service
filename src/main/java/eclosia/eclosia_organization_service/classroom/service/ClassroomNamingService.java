package eclosia.eclosia_organization_service.classroom.service;

import eclosia.eclosia_organization_service.academic_level.entity.AcademicLevel;
import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import org.springframework.stereotype.Service;

@Service
public class ClassroomNamingService {

    public String build(Classroom classroom) {
        AcademicLevel level = classroom.getAcademicLevel();

        StringBuilder sb = new StringBuilder();

        if (level != null) {
            sb.append(level.getName());

            boolean hasOption = Boolean.TRUE.equals(level.getRequiresOption())
                    && classroom.getAcademicOption() != null;
            boolean hasSection = Boolean.TRUE.equals(level.getRequiresSection())
                    && classroom.getAcademicSection() != null;

            // If both exist, option has priority in the display name.
            if (hasOption) {
                sb.append(" ").append(classroom.getAcademicOption().getName());
            } else if (hasSection) {
                sb.append(" ").append(classroom.getAcademicSection().getName());
            }
        }

        if (classroom.getClassroomDesignation() != null) {
            sb.append(" ").append(classroom.getClassroomDesignation().getName());
        }

        return sb.toString().trim();
    }
}
