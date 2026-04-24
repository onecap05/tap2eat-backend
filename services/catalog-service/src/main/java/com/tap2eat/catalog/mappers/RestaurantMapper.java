package com.tap2eat.catalog.mappers;

import com.tap2eat.catalog.dtos.request.product.ImageMetadataRequest;
import com.tap2eat.catalog.dtos.request.restaurant.CreateRestaurantRequest;
import com.tap2eat.catalog.dtos.request.restaurant.UpdateRestaurantRequest;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.models.embedded.ImageMetadata;
import org.springframework.stereotype.Component;

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
        document.setLogo(mapImage(request.logo()));
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