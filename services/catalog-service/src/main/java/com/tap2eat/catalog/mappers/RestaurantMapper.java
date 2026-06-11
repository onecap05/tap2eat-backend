package com.tap2eat.catalog.mappers;

import com.tap2eat.catalog.dtos.request.product.ImageMetadataRequest;
import com.tap2eat.catalog.dtos.request.restaurant.CreateRestaurantRequest;
import com.tap2eat.catalog.dtos.request.restaurant.UpdateRestaurantRequest;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.models.embedded.ImageMetadata;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class RestaurantMapper {

    public RestaurantDocument toDocument(CreateRestaurantRequest request) {
        if (request == null) {
            return null;
        }

        RestaurantDocument document = new RestaurantDocument();
        document.setOwnerAccountId(request.ownerAccountId());
        document.setName(request.name());
        document.setDescription(request.description());
        document.setRfc(normalizeRfc(request.rfc()));
        document.setLogo(mapImage(request.logo()));
        document.setIsActive(Boolean.TRUE);

        return document;
    }

    public void updateDocument(RestaurantDocument document, UpdateRestaurantRequest request) {
        if (document == null || request == null) {
            return;
        }

        document.setName(request.name());
        document.setDescription(request.description());
        document.setRfc(normalizeRfc(request.rfc()));
        document.setLogo(mapImage(request.logo()));
    }

    private String normalizeRfc(String rfc) {
        if (rfc == null) {
            return null;
        }

        String normalized = rfc.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private ImageMetadata mapImage(ImageMetadataRequest request) {
        if (request == null) {
            return null;
        }

        ImageMetadata image = new ImageMetadata();
        image.setUrl(request.url());
        image.setObjectKey(request.objectKey());
        image.setProvider(request.provider());

        return image;
    }
}
