package eclosia.eclosia_organization_service.student_grade.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eclosia.eclosia_organization_service.academic_curriculum_subject.entity.AcademicCurriculumSubject;
import eclosia.eclosia_organization_service.academic_period.entity.AcademicPeriod;
import eclosia.eclosia_organization_service.enrollment.entity.Enrollment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(
        name = "student_grades",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_student_period_subject",
                        columnNames = {
                                "student_enrollment_id",
                                "academic_period_id",
                                "academic_curriculum_subject_id"
                        }
                )
        }
)
public class StudentGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_enrollment_id", nullable = false)
    private Enrollment studentEnrollment;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_period_id", nullable = false)
    private AcademicPeriod academicPeriod;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_curriculum_subject_id", nullable = false)
    private AcademicCurriculumSubject academicCurriculumSubject;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal score;

    @Column(length = 500)
    private String observation;

    @Column(nullable = false)
    private Boolean published = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("studentEnrollmentId")
    public UUID getStudentEnrollmentId() {
        return studentEnrollment != null ? studentEnrollment.getId() : null;
    }

    @JsonProperty("academicPeriodId")
    public UUID getAcademicPeriodId() {
        return academicPeriod != null ? academicPeriod.getId() : null;
    }

    @JsonProperty("academicCurriculumSubjectId")
    public UUID getAcademicCurriculumSubjectId() {
        return academicCurriculumSubject != null ? academicCurriculumSubject.getId() : null;
    }
}
