package eclosia.eclosia_organization_service.school.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class UpdateSchoolDto {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    private String shortName;
    private String description;
    private String motto;
    private UUID groupId;
    private UUID countryId;
    private Long stateId;
    private UUID cityId;
    private UUID communeId;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;

    @NotBlank(message = "School type is required")
    private String schoolType;

    private String email;
    private String phone;
    private String alternatePhone;
    private String website;
    private String principalName;
    private String principalPhone;
    private String principalEmail;
    private String logo;
    private String coverImage;
    private Integer capacity;
    private Integer numberOfClassrooms;
    private LocalDate establishmentDate;
    private Boolean active;
}
