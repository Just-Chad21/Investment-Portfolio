package com.enviro.assessment.junior.chadwynprince.controller;

import com.enviro.assessment.junior.chadwynprince.dto.request.WithdrawalRequest;
import com.enviro.assessment.junior.chadwynprince.dto.response.WithdrawalResponse;
import com.enviro.assessment.junior.chadwynprince.entity.WithdrawalStatus;
import com.enviro.assessment.junior.chadwynprince.entity.WithdrawalType;
import com.enviro.assessment.junior.chadwynprince.service.WithdrawalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/investors/{investorId}/withdrawals")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    @PostMapping
    public ResponseEntity<WithdrawalResponse> submitWithdrawal(@PathVariable Long investorId,
                                                                 @Valid @RequestBody WithdrawalRequest request) {
        WithdrawalResponse response = withdrawalService.submitWithdrawal(investorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<WithdrawalResponse> getHistory(@PathVariable Long investorId,
                                                @RequestParam(required = false) WithdrawalType type,
                                                @RequestParam(required = false) WithdrawalStatus status,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return withdrawalService.getHistory(investorId, type, status, from, to);
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportCsv(@PathVariable Long investorId,
                                             @RequestParam(required = false) WithdrawalType type,
                                             @RequestParam(required = false) WithdrawalStatus status,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var export = withdrawalService.exportCsv(investorId, type, status, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.filename() + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(export.content());
    }
}
