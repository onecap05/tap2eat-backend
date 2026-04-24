package com.tap2eat.catalog.dtos.response.common;

import com.tap2eat.catalog.models.enums.StorageProvider;

import java.time.LocalDateTime;

public record ImageMetadataResponse(
        String url,
        String objectKey,
        StorageProvider provider,
        String contentType,
        Long size,
        LocalDateTime uploadedAt
) {
}