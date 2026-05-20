package com.tap2eat.catalog.services.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.tap2eat.catalog.dtos.response.common.ImageMetadataResponse;
import com.tap2eat.catalog.exceptions.CatalogException;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryImageUploadServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryImageUploadService service;

    @BeforeEach
    void setUp() {
        service = new CloudinaryImageUploadService(cloudinary);
    }

    @Test
    void uploadProductImage_shouldUploadValidImage() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url", "https://cdn.tap2eat.test/product.webp",
                "public_id", "tap2eat/products/product"
        ));
        MockMultipartFile file = new MockMultipartFile("file", "product.webp", "image/webp", new byte[]{1, 2, 3});

        ImageMetadataResponse response = service.uploadProductImage(file);

        assertThat(response.url()).isEqualTo("https://cdn.tap2eat.test/product.webp");
        assertThat(response.objectKey()).isEqualTo("tap2eat/products/product");
        assertThat(response.contentType()).isEqualTo("image/webp");
    }

    @Test
    void uploadRestaurantLogo_shouldRejectEmptyOrNonImageFiles() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.webp", "image/webp", new byte[]{});
        MockMultipartFile text = new MockMultipartFile("file", "file.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> service.uploadRestaurantLogo(null)).isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> service.uploadRestaurantLogo(empty)).isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> service.uploadRestaurantLogo(text)).isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void uploadProductImage_shouldWrapIoFailures() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("network"));
        MockMultipartFile file = new MockMultipartFile("file", "product.webp", "image/webp", new byte[]{1});

        assertThatThrownBy(() -> service.uploadProductImage(file)).isInstanceOf(CatalogException.class);
    }
}
