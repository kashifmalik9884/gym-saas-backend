package tech.gymsaas.backend.service;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.gymsaas.backend.entity.Member;
import tech.gymsaas.backend.repository.MemberRepository;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private final MemberRepository memberRepository;

    @Value("${twilio.whatsapp-from}")
    private String fromWhatsAppNumber;

    @Value("${app.default-country-code:+91}")
    private String defaultCountryCode;

    public String sendWhatsappReminder(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (member.getPhone() == null || member.getPhone().trim().isEmpty()) {
            throw new RuntimeException("Member phone number is missing");
        }

        String toWhatsAppNumber = toWhatsappFormat(member.getPhone().trim());

        String dueDate = member.getNextDueDate() == null
                ? "soon"
                : member.getNextDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        String memberName = member.getName() == null || member.getName().trim().isEmpty()
                ? "Member"
                : member.getName().trim();

        String messageBody = "Hello " + memberName
                + ", your gym fee payment is due on "
                + dueDate
                + ". Please pay on time. Thank you.";

        Message message = Message.creator(
                new PhoneNumber(toWhatsAppNumber),
                new PhoneNumber(fromWhatsAppNumber),
                messageBody
        ).create();

        return "WhatsApp reminder sent successfully. SID: " + message.getSid();
    }

    private String toWhatsappFormat(String rawPhone) {
        String cleaned = rawPhone.replaceAll("[^\\d+]", "");

        if (!cleaned.startsWith("+")) {
            cleaned = defaultCountryCode + cleaned;
        }

        return "whatsapp:" + cleaned;
    }
}