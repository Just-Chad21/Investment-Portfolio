package com.enviro.assessment.junior.chadwynprince.mapper;

import com.enviro.assessment.junior.chadwynprince.entity.WithdrawalNotice;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Renders withdrawal notices as CSV text for the history export endpoint.
 *
 * <p>Output is one header row followed by one row per notice, with these columns:
 * <pre>
 * id, productName, type, amount, status, rejectionReason, balanceAfter, requestedAt
 * </pre>
 * Rows are separated by {@code \n}. Quoting follows RFC 4180 (see {@link #escape(String)}),
 * so the file opens correctly in Excel, Google Sheets and standard CSV parsers.
 *
 * <p>This class only formats; it does no filtering, sorting or I/O. The caller
 * ({@code WithdrawalService.exportCsv}) decides which notices are included and in what order,
 * and the controller sets the {@code text/csv} content type and download filename.
 */
@Component
public class WithdrawalCsvWriter {

    private static final String HEADER = "id,productName,type,amount,status,rejectionReason,balanceAfter,requestedAt";

    /**
     * Builds the full CSV (header plus one row per notice) in the order given.
     * An empty list yields just the header, so no history is a valid download, not an error.
     */
    public String write(List<WithdrawalNotice> notices) {
        StringBuilder csv = new StringBuilder(HEADER).append("\n");
        for (WithdrawalNotice notice : notices) {
            csv.append(toRow(notice)).append("\n");
        }
        return csv.toString();
    }

    /**
     * Formats one notice as a row in HEADER column order. Amounts use toPlainString (no 1E+5);
     * rejectionReason is blank when approved, balanceAfter blank when rejected.
     * Only the free-text columns are escaped; the rest can't contain commas or quotes.
     */
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

    /**
     * RFC 4180 escaping: null becomes empty; values with a comma, quote or newline are
     * wrapped in quotes with inner quotes doubled; anything else is returned unchanged.
     */
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
