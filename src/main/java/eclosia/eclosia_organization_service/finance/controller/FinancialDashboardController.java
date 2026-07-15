package eclosia.eclosia_organization_service.finance.controller;

import eclosia.eclosia_organization_service.finance.dto.FinancialDashboardDto;
import eclosia.eclosia_organization_service.finance.service.FinancialDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("finance")
@RequiredArgsConstructor
public class FinancialDashboardController {

    private final FinancialDashboardService financialDashboardService;

    @GetMapping("/dashboard")
    public FinancialDashboardDto getDashboard(
            @RequestParam UUID schoolId,
            @RequestParam UUID academicYearId
    ) {
        return financialDashboardService.getDashboard(schoolId, academicYearId);
    }
}
