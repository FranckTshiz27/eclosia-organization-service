package eclosia.eclosia_organization_service.country.dto;

import lombok.Data;

@Data
public class UpdateCountryDto {

    private String iso2;

    private String iso3;

    private String nameFr;

    private String nameEn;

    private String phoneCode;

    private String currencyCode;
}
