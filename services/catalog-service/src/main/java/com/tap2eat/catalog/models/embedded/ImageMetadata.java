package com.tap2eat.catalog.models.embedded;

import com.tap2eat.catalog.models.enums.StorageProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageMetadata {

    private String url;
    private String objectKey;
    private StorageProvider provider;
    private String contentType;
    private Long size;
    private LocalDateTime uploadedAt;
}