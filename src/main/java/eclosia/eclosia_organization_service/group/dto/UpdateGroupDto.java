package eclosia.eclosia_organization_service.group.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eclosia.eclosia_organization_service.common.jackson.StatusBooleanDeserializer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateGroupDto {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @Email(message = "Invalid email")
    private String email;

    private String phone;

    private String address;

    @JsonDeserialize(using = StatusBooleanDeserializer.class)
    private Boolean status;
}