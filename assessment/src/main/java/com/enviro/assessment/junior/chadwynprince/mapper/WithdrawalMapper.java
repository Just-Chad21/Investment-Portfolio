package com.enviro.assessment.junior.chadwynprince.mapper;

import com.enviro.assessment.junior.chadwynprince.dto.response.WithdrawalResponse;
import com.enviro.assessment.junior.chadwynprince.entity.WithdrawalNotice;
import org.springframework.stereotype.Component;

@Component
public class WithdrawalMapper {

    public WithdrawalResponse toResponse(WithdrawalNotice notice) {
        return new WithdrawalResponse(
                notice.getId(),
                notice.getProduct().getId(),
                notice.getProduct().getName(),
                notice.getType(),
                notice.getAmount(),
                notice.getStatus(),
                notice.getRejectionReason(),
                notice.getBalanceAfter(),
                notice.getRequestedAt()
        );
    }
}
