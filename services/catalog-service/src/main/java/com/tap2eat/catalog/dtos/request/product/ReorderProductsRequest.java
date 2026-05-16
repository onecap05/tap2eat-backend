package com.tap2eat.catalog.dtos.request.product;

import java.util.List;

public record ReorderProductsRequest(
        String restaurantId,
        String categoryId,
        List<ProductReorderItemRequest> products
) {
}