package eclosia.eclosia_organization_service.report.controller;

import eclosia.eclosia_organization_service.report.service.RevenueStatementReportService;
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
@RequestMapping("report/revenue-statement")
@RequiredArgsConstructor
public class RevenueStatementReportController {

    private final RevenueStatementReportService service;

    @GetMapping
    public ResponseEntity<byte[]> generateRevenueStatement(
            @RequestParam UUID schoolId,
            @RequestParam UUID academicYearId,
            @RequestParam(required = false) List<UUID> cycleIds,
            @RequestParam(required = false) List<UUID> classroomIds
    ) {
        byte[] pdf = service.generateRevenueStatement(
                schoolId,
                academicYearId,
                cycleIds,
                classroomIds
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.inline().filename("etat-recettes.pdf").build()
        );

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
