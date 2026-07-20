package eclosia.eclosia_organization_service.academic_curriculum_subject.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eclosia.eclosia_organization_service.academic_curriculum.entity.AcademicCurriculum;
import eclosia.eclosia_organization_service.subject.entity.Subject;
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
        name = "academic_curriculum_subjects",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_curriculum_subject",
                        columnNames = {
                                "academic_curriculum_id",
                                "subject_id"
                        }
                )
        }
)
public class AcademicCurriculumSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_curriculum_id", nullable = false)
    private AcademicCurriculum academicCurriculum;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal coefficient;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal maximumPoints;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private Boolean mandatory = true;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("academicCurriculumId")
    public UUID getAcademicCurriculumId() {
        return academicCurriculum != null ? academicCurriculum.getId() : null;
    }

    @JsonProperty("subjectId")
    public UUID getSubjectId() {
        return subject != null ? subject.getId() : null;
    }
}
