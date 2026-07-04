package eclosia.eclosia_organization_service.academic_fee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eclosia.eclosia_organization_service.academic_cycle.entity.AcademicCycle;
import eclosia.eclosia_organization_service.academic_level.entity.AcademicLevel;
import eclosia.eclosia_organization_service.academic_option.entity.AcademicOption;
import eclosia.eclosia_organization_service.academic_section.entity.AcademicSection;
import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.fee_category.entity.FeeCategory;
import eclosia.eclosia_organization_service.payment_installment.entity.PaymentInstallment;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.student_category.entity.StudentCategory;
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
        name = "academic_fees",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_academic_fee",
                        columnNames = {
                                "school_id",
                                "academic_year_id",
                                "fee_category_id",
                                "academic_cycle_id",
                                "academic_level_id",
                                "academic_section_id",
                                "academic_option_id",
                                "student_category_id",
                                "payment_installment_id",
                                "code"
                        }
                )
        }
)
public class AcademicFee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Boolean payableByInstallment = false;

    @Column(nullable = false)
    private Boolean active = true;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_category_id", nullable = false)
    private FeeCategory feeCategory;

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

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_category_id", nullable = false)
    private StudentCategory studentCategory;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_installment_id")
    private PaymentInstallment paymentInstallment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("schoolId")
    public UUID getSchoolId() {
        return school != null ? school.getId() : null;
    }

    @JsonProperty("academicYearId")
    public UUID getAcademicYearId() {
        return academicYear != null ? academicYear.getId() : null;
    }

    @JsonProperty("feeCategoryId")
    public UUID getFeeCategoryId() {
        return feeCategory != null ? feeCategory.getId() : null;
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

    @JsonProperty("studentCategoryId")
    public UUID getStudentCategoryId() {
        return studentCategory != null ? studentCategory.getId() : null;
    }

    @JsonProperty("paymentInstallmentId")
    public UUID getPaymentInstallmentId() {
        return paymentInstallment != null ? paymentInstallment.getId() : null;
    }
}
