package eclosia.eclosia_organization_service.school.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "schools")
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "short_name", length = 100)
    private String shortName;

    private String description;

    private String motto;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "country_id")
    private UUID countryId;

    @Column(name = "state_id")
    private Long stateId;

    @Column(name = "city_id")
    private UUID cityId;

    @Column(name = "commune_id")
    private UUID communeId;

    private String address;

    @Column(precision = 12, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 12, scale = 8)
    private BigDecimal longitude;

    @Column(name = "school_type", nullable = false, length = 50)
    private String schoolType;

    private String email;

    private String phone;

    @Column(name = "alternate_phone")
    private String alternatePhone;

    private String website;

    @Column(name = "principal_name")
    private String principalName;

    @Column(name = "principal_phone")
    private String principalPhone;

    @Column(name = "principal_email")
    private String principalEmail;

    private String logo;

    @Column(name = "cover_image")
    private String coverImage;

    private Integer capacity = 0;

    @Column(name = "number_of_classrooms")
    private Integer numberOfClassrooms = 0;

    @Column(name = "establishment_date")
    private LocalDate establishmentDate;

    private Boolean active = true;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}
