package tech.gymsaas.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.gymsaas.backend.entity.Payment;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // Fetch payments belonging to a specific gym for tenant isolation
    List<Payment> findByGymIdOrderByPaymentDateDesc(Long gymId);

    // Fetch specific member history
    List<Payment> findByMemberIdOrderByPaymentDateDesc(Long memberId);
}