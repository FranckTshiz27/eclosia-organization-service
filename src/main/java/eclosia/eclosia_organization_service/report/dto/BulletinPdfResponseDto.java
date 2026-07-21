package eclosia.eclosia_organization_service.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulletinPdfResponseDto {

    private String fileName;
    private String contentType;
    private String contentBase64;
}
