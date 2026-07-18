package eclosia.eclosia_organization_service.academic_curriculum.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eclosia.eclosia_organization_service.academic_cycle.entity.AcademicCycle;
import eclosia.eclosia_organization_service.academic_level.entity.AcademicLevel;
import eclosia.eclosia_organization_service.academic_option.entity.AcademicOption;
import eclosia.eclosia_organization_service.academic_section.entity.AcademicSection;
import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(
        name = "academic_curriculums",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_academic_curriculum",
                        columnNames = {
                                "academic_year_id",
                                "academic_cycle_id",
                                "academic_level_id",
                                "academic_section_id",
                                "academic_option_id"
                        }
                )
        }
)
public class AcademicCurriculum {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_cycle_id", nullable = false)
    private AcademicCycle academicCycle;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_level_id", nullable = false)
    private AcademicLevel academicLevel;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_section_id")
    private AcademicSection academicSection;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_option_id")
    private AcademicOption academicOption;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("academicYearId")
    public UUID getAcademicYearId() {
        return academicYear != null ? academicYear.getId() : null;
    }

    @JsonProperty("academicCycleId")
    public UUID getAcademicCycleId() {
        return academicCycle != null ? academicCycle.getId() : null;
    }

    @JsonProperty("academicLevelId")
    public UUID getAcademicLevelId() {
        return academicLevel != null ? academicLevel.getId() : null;
    }

    @JsonProperty("academicSectionId")
    public UUID getAcademicSectionId() {
        return academicSection != null ? academicSection.getId() : null;
    }

    @JsonProperty("academicOptionId")
    public UUID getAcademicOptionId() {
        return academicOption != null ? academicOption.getId() : null;
    }
}
