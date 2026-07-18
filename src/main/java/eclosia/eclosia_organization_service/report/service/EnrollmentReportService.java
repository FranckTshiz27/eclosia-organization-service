package eclosia.eclosia_organization_service.report.service;

import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.academic_year.repository.AcademicYearRepository;
import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.common.validation.AcademicYearCountryValidator;
import eclosia.eclosia_organization_service.enrollment.entity.Enrollment;
import eclosia.eclosia_organization_service.enrollment.repository.EnrollmentRepository;
import eclosia.eclosia_organization_service.report.dto.EnrollmentByClassReportRowDto;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentReportService {

    private static final String REPORT_PATH = "report/eleves_eclosia.jrxml";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final EnrollmentRepository enrollmentRepository;
    private final SchoolRepository schoolRepository;
    private final AcademicYearRepository academicYearRepository;

    @Transactional(readOnly = true)
    public byte[] generateStudentsByClassReport(UUID schoolId, UUID academicYearId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        AcademicYear academicYear = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));

        AcademicYearCountryValidator.requireSameCountry(school, academicYear);

        List<Enrollment> enrollments = enrollmentRepository.findForSchoolAndAcademicYearReport(academicYearId, schoolId);
        List<EnrollmentByClassReportRowDto> rows = mapRows(enrollments);
        long totalBoys = enrollments.stream()
                .filter(enrollment -> "MALE".equalsIgnoreCase(enrollment.getStudent().getGender()))
                .count();
        long totalGirls = enrollments.stream()
                .filter(enrollment -> "FEMALE".equalsIgnoreCase(enrollment.getStudent().getGender()))
                .count();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("SCHOOL_NAME", resolveSchoolName(school));
        parameters.put("SCHOOL_ADDRESS", school.getAddress() != null ? school.getAddress() : "-");
        parameters.put("SCHOOL_CONTACT", buildContact(school));
        parameters.put("SCHOOL_LOGO_PATH", school.getLogo());
        parameters.put("ACADEMIC_YEAR_LABEL", academicYear.getCode());
        parameters.put("PRINT_DATE", DATE_FORMATTER.format(LocalDate.now()));
        parameters.put("PRINT_TIME", TIME_FORMATTER.format(LocalTime.now()));
        parameters.put("CLASS_LABEL", "Toutes classes");
        parameters.put("TOTAL_STUDENTS", rows.size());
        parameters.put("TOTAL_BOYS", totalBoys);
        parameters.put("TOTAL_GIRLS", totalGirls);

        try (InputStream reportStream = new ClassPathResource(REPORT_PATH).getInputStream()) {
            JasperReport report = JasperCompileManager.compileReport(reportStream);
            JasperPrint print = JasperFillManager.fillReport(
                    report,
                    parameters,
                    new JRBeanCollectionDataSource(rows)
            );
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException | IOException exception) {
            throw new IllegalStateException("Unable to generate enrollment report", exception);
        }
    }

    private List<EnrollmentByClassReportRowDto> mapRows(List<Enrollment> enrollments) {
        AtomicInteger counter = new AtomicInteger(1);

        return enrollments.stream().map(enrollment -> {
            EnrollmentByClassReportRowDto row = new EnrollmentByClassReportRowDto();
            row.setRowNumber(counter.getAndIncrement());
            row.setClassroomName(buildClassroomName(enrollment.getClassroom()));
            row.setPhotoPath(buildPhotoPath(enrollment));
            row.setStudentNumber(enrollment.getStudent().getStudentNumber());
            row.setFullName((enrollment.getStudent().getLastName() + " " + enrollment.getStudent().getFirstName()).trim());
            row.setGender(toShortGender(enrollment.getStudent().getGender()));
            row.setBirthDate(formatDate(enrollment.getStudent().getBirthDate()));
            row.setBirthPlace(resolveBirthPlace(enrollment));
            row.setTutorName((enrollment.getGuardian().getLastName() + " " + enrollment.getGuardian().getFirstName()).trim());
            row.setPhoneNumber(enrollment.getGuardian().getPhoneNumber());
            return row;
        }).collect(Collectors.toList());
    }

    private String buildClassroomName(Classroom classroom) {
        String level = classroom.getAcademicLevel() != null ? classroom.getAcademicLevel().getName() : "";
        String section = classroom.getAcademicSection() != null ? classroom.getAcademicSection().getName() : "";
        String option = classroom.getAcademicOption() != null ? classroom.getAcademicOption().getName() : "";
        String designation = classroom.getClassroomDesignation() != null ? classroom.getClassroomDesignation().getName() : "";

        return (level + " " + section + " " + option + " " + designation).trim().replaceAll("\\s+", " ");
    }

    private String buildPhotoPath(Enrollment enrollment) {
        if (enrollment.getPhoto() == null) {
            return null;
        }
        return java.nio.file.Paths.get(enrollment.getPhoto().getPath(), enrollment.getPhoto().getFileName()).toString();
    }

    private String toShortGender(String gender) {
        if (gender == null) {
            return "-";
        }
        return "FEMALE".equalsIgnoreCase(gender) ? "F" : "M";
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return DATE_FORMATTER.format(date);
    }

    private String resolveBirthPlace(Enrollment enrollment) {
        if (enrollment.getStudent().getBirthCity() != null) {
            return enrollment.getStudent().getBirthCity().getName();
        }
        if (enrollment.getStudent().getBirthCommune() != null) {
            return enrollment.getStudent().getBirthCommune().getName();
        }
        if (enrollment.getStudent().getBirthCountry() != null) {
            return enrollment.getStudent().getBirthCountry().getNameFr();
        }
        return "-";
    }

    private String buildContact(School school) {
        String phone = school.getPhone() != null ? school.getPhone() : "-";
        String email = school.getEmail() != null ? school.getEmail() : "-";
        return phone + " | " + email;
    }

    private String resolveSchoolName(School school) {
        if (school.getName() != null && !school.getName().isBlank()) {
            return school.getName();
        }
        if (school.getShortName() != null && !school.getShortName().isBlank()) {
            return school.getShortName();
        }
        if (school.getCode() != null && !school.getCode().isBlank()) {
            return school.getCode();
        }
        return "-";
    }
}
