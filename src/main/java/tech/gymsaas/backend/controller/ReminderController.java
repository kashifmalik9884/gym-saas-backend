package tech.gymsaas.backend.controller;

import tech.gymsaas.backend.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ReminderController {

    private final ReminderService reminderService;

    @PostMapping("/whatsapp/{memberId}")
    public ResponseEntity<?> sendWhatsappReminder(@PathVariable Long memberId) {
        try {
            String result = reminderService.sendWhatsappReminder(memberId);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to send WhatsApp reminder: " + e.getMessage()));
        }
    }
}