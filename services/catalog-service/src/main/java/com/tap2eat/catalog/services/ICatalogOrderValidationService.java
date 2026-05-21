package com.tap2eat.catalog.services;

import com.tap2eat.catalog.dtos.request.internal.ValidateOrderRequest;
import com.tap2eat.catalog.dtos.response.internal.ValidateOrderResponse;

public interface ICatalogOrderValidationService {

    ValidateOrderResponse validateOrder(ValidateOrderRequest request);
}
