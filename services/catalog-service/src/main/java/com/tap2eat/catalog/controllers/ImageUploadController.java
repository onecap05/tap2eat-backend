package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.dtos.response.common.ImageMetadataResponse;
import com.tap2eat.catalog.services.IImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads/images")
@RequiredArgsConstructor
public class ImageUploadController {

    private final IImageUploadService imageUploadService;

    @PostMapping(
            value = "/products",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ImageMetadataResponse> uploadProductImage(
            @RequestPart("file") MultipartFile file
    ) {
        ImageMetadataResponse response = imageUploadService.uploadProductImage(file);
        return ResponseEntity.ok(response);
    }

    @PostMapping(
            value = "/restaurants/logos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ImageMetadataResponse> uploadRestaurantLogo(
            @RequestPart("file") MultipartFile file
    ) {
        ImageMetadataResponse response = imageUploadService.uploadRestaurantLogo(file);
        return ResponseEntity.ok(response);
    }
}