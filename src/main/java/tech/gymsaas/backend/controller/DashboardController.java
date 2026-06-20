package tech.gymsaas.backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.gymsaas.backend.dto.dashboard.DashboardSummaryDto;
import tech.gymsaas.backend.entity.Member;
import tech.gymsaas.backend.service.DashboardService;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryDto getSummary(Authentication authentication) {
        return dashboardService.getSummary(authentication);
    }

    @GetMapping("/due-soon")
    public List<Member> getDueSoonMembers(Authentication authentication) {
        return dashboardService.getDueSoonMembers(authentication);
    }
}