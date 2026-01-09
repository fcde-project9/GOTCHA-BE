package com.gotcha.domain.user.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Converter
public class WithdrawalReasonsConverter implements AttributeConverter<List<WithdrawalReason>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<WithdrawalReason> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(reasons);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize withdrawal reasons: {}", reasons, e);
            return "[]";
        }
    }

    @Override
    public List<WithdrawalReason> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<WithdrawalReason>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize withdrawal reasons from DB: {}", dbData, e);
            return new ArrayList<>();
        }
    }
}
