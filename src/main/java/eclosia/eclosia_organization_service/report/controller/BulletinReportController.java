package eclosia.eclosia_organization_service.report.controller;

import eclosia.eclosia_organization_service.report.dto.BulletinPdfResponseDto;
import eclosia.eclosia_organization_service.report.dto.BulletinPreviewResponseDto;
import eclosia.eclosia_organization_service.report.dto.BulletinPrintRequestDto;
import eclosia.eclosia_organization_service.report.service.BulletinReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "report/bulletins")
@RequiredArgsConstructor
public class BulletinReportController {

    private final BulletinReportService service;

    @PostMapping("/preview")
    public BulletinPreviewResponseDto preview(@Valid @RequestBody BulletinPrintRequestDto request) {
        return service.preview(request);
    }

    @PostMapping("/generate")
    public BulletinPdfResponseDto generate(@Valid @RequestBody BulletinPrintRequestDto request) {
        return service.generate(request);
    }
}
