package eclosia.eclosia_organization_service.classroom.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eclosia.eclosia_organization_service.academic_level.entity.AcademicLevel;
import eclosia.eclosia_organization_service.academic_option.entity.AcademicOption;
import eclosia.eclosia_organization_service.academic_section.entity.AcademicSection;
import eclosia.eclosia_organization_service.classroom_designation.entity.ClassroomDesignation;
import eclosia.eclosia_organization_service.school.entity.School;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(
        name = "classrooms",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_school_classroom",
                        columnNames = {
                                "school_id",
                                "academic_level_id",
                                "academic_section_id",
                                "academic_option_id",
                                "classroom_designation_id"
                        }
                )
        }
)
public class Classroom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(length = 500)
    private String description;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

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

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_designation_id", nullable = false)
    private ClassroomDesignation classroomDesignation;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @JsonProperty("schoolId")
    public UUID getSchoolId() {
        return school != null ? school.getId() : null;
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

    @JsonProperty("classroomDesignationId")
    public UUID getClassroomDesignationId() {
        return classroomDesignation != null ? classroomDesignation.getId() : null;
    }

    @Transient
    @JsonProperty("displayName")
    private String displayName;
}
