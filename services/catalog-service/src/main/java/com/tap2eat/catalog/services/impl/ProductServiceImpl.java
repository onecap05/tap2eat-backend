package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.dtos.request.product.CreateProductRequest;
import com.tap2eat.catalog.dtos.request.product.UpdateProductRequest;
import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.mappers.ProductMapper;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.repositories.CategoryRepository;
import com.tap2eat.catalog.repositories.ProductRepository;
import com.tap2eat.catalog.repositories.RestaurantRepository;
import com.tap2eat.catalog.services.ProductService;
import com.tap2eat.catalog.validators.ProductValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final RestaurantRepository restaurantRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductDocument createProduct(CreateProductRequest request) {
        validateCreateRequest(request);

        validateRestaurantExists(request.restaurantId());

        CategoryDocument category = getCategoryOrThrow(request.categoryId());
        validateCategoryBelongsToRestaurant(category, request.restaurantId());

        ProductDocument product = productMapper.toDocument(request);
        ProductValidator.validate(product);

        return productRepository.save(product);
    }

    @Override
    public ProductDocument updateProduct(String restaurantId, String productId, UpdateProductRequest request) {
        if (isBlank(restaurantId) || isBlank(productId) || request == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA);
        }

        ProductDocument existingProduct = getProductOrThrow(productId);
        validateProductOwnership(existingProduct, restaurantId);

        if (!isBlank(request.categoryId())) {
            CategoryDocument category = getCategoryOrThrow(request.categoryId());
            validateCategoryBelongsToRestaurant(category, restaurantId);
        }

        productMapper.updateDocument(existingProduct, request);
        ProductValidator.validate(existingProduct);

        return productRepository.save(existingProduct);
    }

    @Override
    public ProductDocument getProductById(String productId) {
        if (isBlank(productId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA);
        }

        return getProductOrThrow(productId);
    }

    @Override
    public List<ProductDocument> getProductsByRestaurant(String restaurantId) {
        if (isBlank(restaurantId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA);
        }

        validateRestaurantExists(restaurantId);
        return productRepository.findAllByRestaurantIdAndIsActiveTrue(restaurantId);
    }

    @Override
    public List<ProductDocument> getProductsByCategory(String categoryId) {
        if (isBlank(categoryId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_CATEGORY_DATA);
        }

        getCategoryOrThrow(categoryId);
        return productRepository.findAllByCategoryIdAndIsActiveTrue(categoryId);
    }

    private void validateCreateRequest(CreateProductRequest request) {
        if (request == null
                || isBlank(request.restaurantId())
                || isBlank(request.categoryId())) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA);
        }
    }

    private void validateRestaurantExists(String restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private CategoryDocument getCategoryOrThrow(String categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND));
    }

    private ProductDocument getProductOrThrow(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validateCategoryBelongsToRestaurant(CategoryDocument category, String restaurantId) {
        if (category == null || !restaurantId.equals(category.getRestaurantId())) {
            throw new CatalogValidationException(CatalogErrorCode.UNAUTHORIZED_CATALOG_ACCESS);
        }
    }

    private void validateProductOwnership(ProductDocument product, String restaurantId) {
        if (product == null || !restaurantId.equals(product.getRestaurantId())) {
            throw new CatalogValidationException(CatalogErrorCode.UNAUTHORIZED_CATALOG_ACCESS);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}