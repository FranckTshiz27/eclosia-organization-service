package eclosia.eclosia_organization_service.state.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateStateDto {

    private Long id;

    private String name;

    private Long countryId;

    private String countryCode;

    private String countryName;

    private String iso2;

    private String iso3166_2;

    private String fipsCode;

    private String type;

    private String level;

    private Long parentId;

    private String nativeName;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String timezone;

    private String wikiDataId;

    private Long population;
}
