package com.tap2eat.catalog.services;

import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.embedded.AvailabilityConfig;

public interface IAvailabilityEvaluator {

    boolean isBranchOpen(BranchDocument branch);

    boolean isCategoryAvailable(CategoryDocument category);

    boolean isProductAvailable(ProductDocument product);

    boolean isAvailableNow(AvailabilityConfig availability, boolean emptyScheduleAvailable);
}
