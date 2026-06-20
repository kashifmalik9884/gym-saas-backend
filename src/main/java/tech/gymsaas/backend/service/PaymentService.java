package tech.gymsaas.backend.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.gymsaas.backend.dto.payment.PaymentRequest;
import tech.gymsaas.backend.dto.payment.PaymentResponse;
import tech.gymsaas.backend.entity.Member;
import tech.gymsaas.backend.entity.Payment;
import tech.gymsaas.backend.entity.User;
import tech.gymsaas.backend.repository.MemberRepository;
import tech.gymsaas.backend.repository.PaymentRepository;
import tech.gymsaas.backend.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          MemberRepository memberRepository,
                          UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PaymentResponse createManualInvoice(PaymentRequest request, Authentication authentication) {
        // Resolve authenticated User context to identify the gym tenant
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user context missing"));

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Build transaction record
        Payment payment = Payment.builder()
                .gym(currentUser.getGym())
                .member(member)
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now())
                .dueDate(request.getDueDate() != null ? request.getDueDate() : LocalDate.now().plusMonths(1))
                .paymentStatus(request.getPaymentStatus() != null ? request.getPaymentStatus() : "paid")
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "cash")
                .notes(request.getNotes())
                .build();

        Payment saved = paymentRepository.save(payment);

        // Optional: Update the master member entity status if they paid right now
        if ("paid".equalsIgnoreCase(saved.getPaymentStatus())) {
            member.setPaymentStatus("paid");
            member.setLastPaidDate(saved.getPaymentDate());
            member.setNextDueDate(saved.getDueDate());
            memberRepository.save(member);
        }

        return mapToResponse(saved);
    }

    public List<PaymentResponse> getAllPayments(Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return paymentRepository.findByGymIdOrderByPaymentDateDesc(currentUser.getGym().getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .memberId(payment.getMember().getId())
                .memberName(payment.getMember().getName())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .dueDate(payment.getDueDate())
                .paymentStatus(payment.getPaymentStatus())
                .paymentMethod(payment.getPaymentMethod())
                .notes(payment.getNotes())
                .build();
    }
}