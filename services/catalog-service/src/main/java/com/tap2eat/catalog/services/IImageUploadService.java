package com.tap2eat.catalog.services;

import com.tap2eat.catalog.dtos.response.common.ImageMetadataResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IImageUploadService {

    ImageMetadataResponse uploadProductImage(MultipartFile file);

    ImageMetadataResponse uploadRestaurantLogo(MultipartFile file);
}