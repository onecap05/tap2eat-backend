package com.tap2eat.catalog.services.impl;

import com.cloudinary.Cloudinary;
import com.tap2eat.catalog.dtos.response.common.ImageMetadataResponse;
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

    private final Cloudinary cloudinary;

    @Override
    public ImageMetadataResponse uploadProductImage(MultipartFile file) {
        validateImage(file);

        try {
            String publicId = PRODUCT_IMAGES_FOLDER + "/" + UUID.randomUUID();

            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    Map.of(
                            "public_id", publicId,
                            "overwrite", false,
                            "resource_type", "image"
                    )
            );

            String secureUrl = result.get("secure_url").toString();
            String objectKey = result.get("public_id").toString();

            return new ImageMetadataResponse(
                    secureUrl,
                    objectKey,
                    StorageProvider.CLOUDINARY,
                    file.getContentType(),
                    file.getSize(),
                    LocalDateTime.now()
            );

        } catch (IOException exception) {
            throw new IllegalStateException("Could not upload product image.", exception);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Product image is required.");
        }

        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed.");
        }
    }
}