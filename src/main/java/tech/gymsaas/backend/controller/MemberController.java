package tech.gymsaas.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.gymsaas.backend.dto.member.MemberRequest;
import tech.gymsaas.backend.dto.member.MemberResponse;
import tech.gymsaas.backend.service.MemberService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<List<MemberResponse>> getAllMembers(Authentication authentication) {
        return ResponseEntity.ok(memberService.getAllMembers(authentication));
    }

    @GetMapping("/attendance")
    public ResponseEntity<List<MemberResponse>> getAttendanceMembers(Authentication authentication) {
        return ResponseEntity.ok(memberService.getAllMembers(authentication));
    }

    @GetMapping("/due-soon")
    public ResponseEntity<List<MemberResponse>> getDueSoonMembers(Authentication authentication) {
        return ResponseEntity.ok(memberService.getDueSoonMembers(authentication));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemberResponse> getMemberById(@PathVariable Long id,
                                                        Authentication authentication) {
        return ResponseEntity.ok(memberService.getMemberById(id, authentication));
    }

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MemberResponse> createMember(
            @RequestParam("name") String name,
            @RequestParam("phone") String phone,
            @RequestParam("monthlyFee") Double monthlyFee,
            @RequestParam("joiningDate") String joiningDate,
            @RequestParam("billingDay") Integer billingDay,
            @RequestParam("paymentStatus") String paymentStatus,
            @RequestParam("active") Boolean active,
            @RequestParam(value = "nextDueDate", required = false) String nextDueDate,
            @RequestParam(value = "lastPaidDate", required = false) String lastPaidDate,
            @RequestParam(value = "photoFileId", required = false) String photoFileId,
            @RequestParam(value = "biometricUserId", required = false) String biometricUserId,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            Authentication authentication
    ) {
        // Construct the request mapping DTO programmatically to ensure perfect type isolation
        MemberRequest request = MemberRequest.builder()
                .name(name)
                .phone(phone)
                .monthlyFee(monthlyFee)
                .joiningDate(LocalDate.parse(joiningDate))
                .billingDay(billingDay)
                .paymentStatus(paymentStatus)
                .active(active)
                .photoFileId(photoFileId)
                .biometricUserId(biometricUserId)
                .nextDueDate(nextDueDate != null && !nextDueDate.isBlank() ? LocalDate.parse(nextDueDate) : null)
                .lastPaidDate(lastPaidDate != null && !lastPaidDate.isBlank() ? LocalDate.parse(lastPaidDate) : null)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memberService.createMember(request, photo, authentication));
    }

    @PutMapping(value = "/{id}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MemberResponse> updateMember(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("phone") String phone,
            @RequestParam("monthlyFee") Double monthlyFee,
            @RequestParam("joiningDate") String joiningDate,
            @RequestParam("billingDay") Integer billingDay,
            @RequestParam("paymentStatus") String paymentStatus,
            @RequestParam("active") Boolean active,
            @RequestParam(value = "nextDueDate", required = false) String nextDueDate,
            @RequestParam(value = "lastPaidDate", required = false) String lastPaidDate,
            @RequestParam(value = "photoFileId", required = false) String photoFileId,
            @RequestParam(value = "biometricUserId", required = false) String biometricUserId,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            Authentication authentication
    ) {
        MemberRequest request = MemberRequest.builder()
                .name(name)
                .phone(phone)
                .monthlyFee(monthlyFee)
                .joiningDate(LocalDate.parse(joiningDate))
                .billingDay(billingDay)
                .paymentStatus(paymentStatus)
                .active(active)
                .photoFileId(photoFileId)
                .biometricUserId(biometricUserId)
                .nextDueDate(nextDueDate != null && !nextDueDate.isBlank() ? LocalDate.parse(nextDueDate) : null)
                .lastPaidDate(lastPaidDate != null && !lastPaidDate.isBlank() ? LocalDate.parse(lastPaidDate) : null)
                .build();

        return ResponseEntity.ok(memberService.updateMember(id, request, photo, authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMember(@PathVariable Long id,
                                                            Authentication authentication) {
        memberService.deleteMember(id, authentication);
        return ResponseEntity.ok(Map.of(
                "message", "Member deleted successfully",
                "id", id
        ));
    }
}