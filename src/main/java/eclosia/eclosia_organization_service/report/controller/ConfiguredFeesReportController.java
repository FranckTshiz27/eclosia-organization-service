package eclosia.eclosia_organization_service.report.controller;

import eclosia.eclosia_organization_service.report.service.ConfiguredFeesReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("report/configured-fees")
@RequiredArgsConstructor
public class ConfiguredFeesReportController {

    private final ConfiguredFeesReportService service;

    @GetMapping
    public ResponseEntity<byte[]> generateConfiguredFeesReport(
            @RequestParam UUID schoolId,
            @RequestParam UUID academicYearId,
            @RequestParam(required = false) List<UUID> cycleIds,
            @RequestParam(required = false) List<UUID> classroomIds
    ) {
        byte[] pdf = service.generateConfiguredFeesReport(
                schoolId,
                academicYearId,
                cycleIds,
                classroomIds
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.inline().filename("etat-frais-configures.pdf").build()
        );

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
