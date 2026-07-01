package eclosia.eclosia_organization_service.guardian.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eclosia.eclosia_organization_service.school.entity.School;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "guardians",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_guardian_phone",
                        columnNames = "phone_number"
                ),
                @UniqueConstraint(
                        name = "uk_school_family_code",
                        columnNames = {"school_id", "family_code"}
                )
        }
)
public class Guardian {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(length = 100)
    private String middleName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(length = 30)
    private String phoneNumber;

    @Column(length = 150)
    private String email;

    @Column(length = 500)
    private String address;

    @Column(length = 150)
    private String occupation;

    @Column(length = 150)
    private String employer;

    @Column(name = "family_code", nullable = false, length = 6)
    private String familyCode;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(length = 500)
    private String comment;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @JsonProperty("schoolId")
    public UUID getSchoolId() {
        return school != null ? school.getId() : null;
    }
}
