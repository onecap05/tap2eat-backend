package com.tap2eat.catalog.dtos.request.product;

public record ProductReorderItemRequest(
        String productId,
        Integer displayOrder
) {
}