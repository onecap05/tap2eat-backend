package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.config.CatalogImageProperties;
import com.tap2eat.catalog.dtos.request.product.CreateProductRequest;
import com.tap2eat.catalog.dtos.request.product.UpdateProductRequest;
import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.mappers.ProductMapper;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.embedded.ImageMetadata;
import com.tap2eat.catalog.models.enums.StorageProvider;
import com.tap2eat.catalog.repositories.ICategoryRepository;
import com.tap2eat.catalog.repositories.IProductRepository;
import com.tap2eat.catalog.repositories.IRestaurantRepository;
import com.tap2eat.catalog.dtos.request.product.PauseProductRequest;
import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import com.tap2eat.catalog.services.IProductService;
import com.tap2eat.catalog.validators.ProductValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {

    private final IProductRepository IProductRepository;
    private final IRestaurantRepository IRestaurantRepository;
    private final ICategoryRepository ICategoryRepository;
    private final ProductMapper productMapper;
    private final CatalogImageProperties catalogImageProperties;

    @Override
    public ProductDocument createProduct(CreateProductRequest request) {
        validateCreateRequest(request);

        validateRestaurantExists(request.restaurantId());

        CategoryDocument category = getCategoryOrThrow(request.categoryId());
        validateCategoryBelongsToRestaurant(category, request.restaurantId());

        ProductDocument product = productMapper.toDocument(request);
        product.setImage(resolveProductImage(product.getImage()));

        ProductValidator.validate(product);

        return IProductRepository.save(product);
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
        existingProduct.setImage(resolveProductImage(existingProduct.getImage()));

        ProductValidator.validate(existingProduct);

        return IProductRepository.save(existingProduct);
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

        return IProductRepository.findAllByRestaurantIdAndIsActiveTrue(restaurantId);
    }

    @Override
    public List<ProductDocument> getProductsByCategory(String categoryId) {
        if (isBlank(categoryId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_CATEGORY_DATA);
        }

        getCategoryOrThrow(categoryId);

        return IProductRepository.findAllByCategoryIdAndIsActiveTrue(categoryId);
    }

    @Override
    public ProductDocument deactivateProduct(String restaurantId, String productId) {
        if (isBlank(restaurantId) || isBlank(productId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA);
        }

        ProductDocument product = getProductOrThrow(productId);
        validateProductOwnership(product, restaurantId);

        product.setIsActive(Boolean.FALSE);
        product.setDeletedAt(LocalDateTime.now());

        return IProductRepository.save(product);
    }

    @Override
    public ProductDocument activateProduct(String restaurantId, String productId) {
        if (isBlank(restaurantId) || isBlank(productId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA);
        }

        ProductDocument product = getProductOrThrow(productId);
        validateProductOwnership(product, restaurantId);

        product.setIsActive(Boolean.TRUE);
        product.setDeletedAt(null);

        return IProductRepository.save(product);
    }

    @Override
    public ProductDocument pauseProduct(String restaurantId, String productId, PauseProductRequest request) {
        if (isBlank(restaurantId) || isBlank(productId) || request == null || request.temporaryReason() == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_AVAILABILITY);
        }

        ProductDocument product = getProductOrThrow(productId);
        validateProductOwnership(product, restaurantId);

        AvailabilityConfig availability = product.getAvailability();

        if (availability == null) {
            availability = new AvailabilityConfig();
        }

        availability.setStatus(AvailabilityStatus.TEMPORARILY_UNAVAILABLE);
        availability.setTemporaryReason(request.temporaryReason());
        availability.setTemporaryReasonDetail(
                isBlank(request.temporaryReasonDetail()) ? null : request.temporaryReasonDetail().trim()
        );

        product.setAvailability(availability);

        ProductValidator.validate(product);

        return IProductRepository.save(product);
    }

    @Override
    public ProductDocument resumeProduct(String restaurantId, String productId) {
        if (isBlank(restaurantId) || isBlank(productId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA);
        }

        ProductDocument product = getProductOrThrow(productId);
        validateProductOwnership(product, restaurantId);

        AvailabilityConfig availability = product.getAvailability();

        if (availability == null) {
            availability = new AvailabilityConfig();
        }

        availability.setStatus(AvailabilityStatus.AVAILABLE);
        availability.setTemporaryReason(null);
        availability.setTemporaryReasonDetail(null);

        product.setAvailability(availability);

        ProductValidator.validate(product);

        return IProductRepository.save(product);
    }

    @Override
    public ProductDocument deleteProduct(String restaurantId, String productId) {
        return deactivateProduct(restaurantId, productId);
    }

    @Override
    public ProductDocument restoreProduct(String restaurantId, String productId) {
        return activateProduct(restaurantId, productId);
    }

    private ImageMetadata resolveProductImage(ImageMetadata image) {
        if (image != null && image.getUrl() != null && !image.getUrl().isBlank()) {
            return image;
        }

        ImageMetadata defaultImage = new ImageMetadata();
        defaultImage.setUrl(catalogImageProperties.getDefaultProductUrl());
        defaultImage.setObjectKey(catalogImageProperties.getDefaultProductObjectKey());
        defaultImage.setProvider(StorageProvider.CLOUDINARY);
        defaultImage.setContentType("image/webp");
        defaultImage.setSize(null);
        defaultImage.setUploadedAt(null);

        return defaultImage;
    }

    private void validateCreateRequest(CreateProductRequest request) {
        if (request == null
                || isBlank(request.restaurantId())
                || isBlank(request.categoryId())) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA);
        }
    }

    private void validateRestaurantExists(String restaurantId) {
        if (!IRestaurantRepository.existsById(restaurantId)) {
            throw new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private CategoryDocument getCategoryOrThrow(String categoryId) {
        return ICategoryRepository.findById(categoryId)
                .orElseThrow(() -> new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND));
    }

    private ProductDocument getProductOrThrow(String productId) {
        return IProductRepository.findById(productId)
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