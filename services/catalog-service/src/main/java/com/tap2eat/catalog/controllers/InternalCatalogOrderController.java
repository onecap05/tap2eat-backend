package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.dtos.request.internal.ValidateOrderRequest;
import com.tap2eat.catalog.dtos.response.internal.ValidateOrderResponse;
import com.tap2eat.catalog.services.ICatalogOrderValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/catalog/orders")
@RequiredArgsConstructor
public class InternalCatalogOrderController {

    private final ICatalogOrderValidationService catalogOrderValidationService;

    @PostMapping("/validate")
    public ValidateOrderResponse validateOrder(@RequestBody ValidateOrderRequest request) {
        return catalogOrderValidationService.validateOrder(request);
    }
}
