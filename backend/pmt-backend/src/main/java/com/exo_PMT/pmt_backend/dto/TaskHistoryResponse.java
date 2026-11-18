package com.exo_PMT.pmt_backend.dto;

import com.exo_PMT.pmt_backend.entity.TaskHistory;

import java.time.LocalDateTime;

public record TaskHistoryResponse(
        Long id,
        String fieldName,
        String oldValue,
        String newValue,
        Long changedByUserId,
        String changedByName,
        LocalDateTime changedAt
) {
    public static TaskHistoryResponse from(TaskHistory history, String changedByName) {
        return new TaskHistoryResponse(
                history.getId(),
                history.getFieldName(),
                history.getOldValue(),
                history.getNewValue(),
                history.getChangedByUserId(),
                changedByName,
                history.getChangedAt()
        );
    }
}
