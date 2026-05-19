package com.tap2eat.catalog.services.impl;

import com.cloudinary.Cloudinary;
import com.tap2eat.catalog.dtos.response.common.ImageMetadataResponse;
import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogException;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.models.enums.StorageProvider;
import com.tap2eat.catalog.services.IImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryImageUploadService implements IImageUploadService {

    private static final String PRODUCT_IMAGES_FOLDER = "tap2eat/products";
    private static final String RESTAURANT_LOGOS_FOLDER = "tap2eat/restaurants/logos";

    private final Cloudinary cloudinary;

    @Override
    public ImageMetadataResponse uploadProductImage(MultipartFile file) {
        return uploadImage(file, PRODUCT_IMAGES_FOLDER, CatalogErrorCode.INVALID_PRODUCT_DATA);
    }

    @Override
    public ImageMetadataResponse uploadRestaurantLogo(MultipartFile file) {
        return uploadImage(file, RESTAURANT_LOGOS_FOLDER, CatalogErrorCode.INVALID_RESTAURANT_DATA);
    }

    private ImageMetadataResponse uploadImage(
            MultipartFile file,
            String folder,
            CatalogErrorCode validationErrorCode
    ) {
        validateImage(file, validationErrorCode);

        try {
            String publicId = folder + "/" + UUID.randomUUID();

            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    Map.of(
                            "public_id", publicId,
                            "overwrite", false,
                            "resource_type", "image"
                    )
            );

            return new ImageMetadataResponse(
                    result.get("secure_url").toString(),
                    result.get("public_id").toString(),
                    StorageProvider.CLOUDINARY,
                    file.getContentType(),
                    file.getSize(),
                    LocalDateTime.now()
            );
        } catch (IOException exception) {
            throw new CatalogException(CatalogErrorCode.INTERNAL_ERROR);
        }
    }

    private void validateImage(MultipartFile file, CatalogErrorCode validationErrorCode) {
        if (file == null || file.isEmpty()) {
            throw new CatalogValidationException(validationErrorCode);
        }

        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CatalogValidationException(validationErrorCode);
        }
    }
}