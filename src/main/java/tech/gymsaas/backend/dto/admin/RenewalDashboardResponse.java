package tech.gymsaas.backend.dto.admin;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenewalDashboardResponse {
    private long totalRecords;
    private long dueSoonCount;
    private long expiredCount;
    private long renewedCount;
    private List<RenewalQueueRespons> queue;
}