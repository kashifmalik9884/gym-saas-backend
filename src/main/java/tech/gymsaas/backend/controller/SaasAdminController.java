package tech.gymsaas.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.gymsaas.backend.dto.admin.CreateGymRequest;
import tech.gymsaas.backend.dto.admin.GymResponse;
import tech.gymsaas.backend.dto.admin.RenewGymRequest;
import tech.gymsaas.backend.dto.admin.RenewalDashboardResponse;
import tech.gymsaas.backend.service.SaasAdminService;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/saas-admin") //
public class SaasAdminController {

    private final SaasAdminService saasAdminService;

    public SaasAdminController(SaasAdminService saasAdminService) {
        this.saasAdminService = saasAdminService;
    }

    @GetMapping("/dashboard-summary")
    public ResponseEntity<tech.gymsaas.backend.dto.admin.RenewalDashboardResponse> getDashboardSummary() {

        return ResponseEntity.ok(saasAdminService.getRenewalDashboardMetrics());
    }


    @GetMapping("/gyms")
    public ResponseEntity<List<GymResponse>> getAllGyms() {
        return ResponseEntity.ok(saasAdminService.getAllGyms());
    }

    @GetMapping("/gyms/expired")
    public ResponseEntity<List<GymResponse>> getExpiredGyms() {
        return ResponseEntity.ok(saasAdminService.getExpiredGyms());
    }

    @GetMapping("/gyms/{gymId}")
    public ResponseEntity<GymResponse> getGymById(@PathVariable Long gymId) {
        return ResponseEntity.ok(saasAdminService.getGymById(gymId));
    }

    @PostMapping("/gyms")
    public ResponseEntity<GymResponse> createGym(@Valid @RequestBody CreateGymRequest request) {
        return ResponseEntity.ok(saasAdminService.createGym(request));
    }

    @PutMapping("/gyms/{gymId}")
    public ResponseEntity<GymResponse> updateGym(@PathVariable Long gymId, @Valid @RequestBody CreateGymRequest request) {
        return ResponseEntity.ok(saasAdminService.updateGym(gymId, request));
    }
    @PutMapping("/profile")
    public ResponseEntity<String> updateAdminProfile(
            @jakarta.validation.Valid @RequestBody tech.gymsaas.backend.dto.admin.AdminProfileRequest request,
            java.security.Principal principal) {

        String currentAdminEmail = principal.getName();
        // Forward parameters to your service layer to handle the DB update logic
        saasAdminService.updateAdminProfileDetails(currentAdminEmail, request);

        return ResponseEntity.ok("Profile settings successfully saved to cloud infrastructure.");
    }
    @PostMapping("/gyms/{gymId}/renew")
    public ResponseEntity<GymResponse> renewGym(
            @PathVariable Long gymId,
            @Valid @RequestBody RenewGymRequest request,
            Principal principal) {
        String adminEmail = principal != null ? principal.getName() : "system-admin@gymsaas.com";
        return ResponseEntity.ok(saasAdminService.renewGym(gymId, request, adminEmail));
    }

    @PostMapping("/gyms/{gymId}/suspend")
    public ResponseEntity<GymResponse> suspendGym(@PathVariable Long gymId) {
        return ResponseEntity.ok(saasAdminService.suspendGym(gymId));
    }

    @PostMapping("/gyms/{gymId}/activate")
    public ResponseEntity<GymResponse> activateGym(@PathVariable Long gymId) {
        return ResponseEntity.ok(saasAdminService.activateGym(gymId));
    }


    @GetMapping("/renewals")
    public ResponseEntity<RenewalDashboardResponse> getRenewalsDashboard() {
        return ResponseEntity.ok(saasAdminService.getRenewalDashboardMetrics());
    }
}