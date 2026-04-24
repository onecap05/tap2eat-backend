package com.tap2eat.catalog.dtos.request.product;

import com.tap2eat.catalog.models.enums.StorageProvider;

public record ImageMetadataRequest(
        String url,
        String objectKey,
        StorageProvider provider
) {
}