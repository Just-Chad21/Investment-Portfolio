package com.enviro.assessment.junior.chadwynprince.mapper;

import com.enviro.assessment.junior.chadwynprince.entity.WithdrawalNotice;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WithdrawalCsvWriter {

    private static final String HEADER = "id,productName,type,amount,status,rejectionReason,balanceAfter,requestedAt";

    // No rows beyond the header is a valid, non-error result — an investor with no
    // withdrawal history still gets a downloadable (empty) CSV, not an error.
    public String write(List<WithdrawalNotice> notices) {
        StringBuilder csv = new StringBuilder(HEADER).append("\n");
        for (WithdrawalNotice notice : notices) {
            csv.append(toRow(notice)).append("\n");
        }
        return csv.toString();
    }

    private String toRow(WithdrawalNotice notice) {
        return String.join(",",
                String.valueOf(notice.getId()),
                escape(notice.getProduct().getName()),
                notice.getType().name(),
                notice.getAmount().toPlainString(),
                notice.getStatus().name(),
                escape(notice.getRejectionReason()),
                notice.getBalanceAfter() == null ? "" : notice.getBalanceAfter().toPlainString(),
                notice.getRequestedAt().toString());
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
