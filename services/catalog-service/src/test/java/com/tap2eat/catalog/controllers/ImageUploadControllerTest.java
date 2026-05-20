package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.dtos.response.common.ImageMetadataResponse;
import com.tap2eat.catalog.models.enums.StorageProvider;
import com.tap2eat.catalog.services.IImageUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageUploadControllerTest {

    private final IImageUploadService service = mock(IImageUploadService.class);
    private final ImageUploadController controller = new ImageUploadController(service);

    @Test
    void uploadEndpoints_shouldDelegateToService() {
        MockMultipartFile file = new MockMultipartFile("file", "image.webp", "image/webp", new byte[]{1});
        ImageMetadataResponse response = new ImageMetadataResponse(
                "https://cdn.tap2eat.test/image.webp",
                "tap2eat/tests/image",
                StorageProvider.CLOUDINARY,
                "image/webp",
                1L,
                LocalDateTime.now()
        );
        when(service.uploadProductImage(file)).thenReturn(response);
        when(service.uploadRestaurantLogo(file)).thenReturn(response);

        assertThat(controller.uploadProductImage(file).getBody()).isEqualTo(response);
        assertThat(controller.uploadRestaurantLogo(file).getBody()).isEqualTo(response);
    }
}
