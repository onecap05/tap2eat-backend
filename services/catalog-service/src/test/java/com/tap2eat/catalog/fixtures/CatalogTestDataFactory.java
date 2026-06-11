package com.tap2eat.catalog.fixtures;

import com.tap2eat.catalog.dtos.request.branch.CreateBranchRequest;
import com.tap2eat.catalog.dtos.request.branch.UpdateBranchRequest;
import com.tap2eat.catalog.dtos.request.category.CreateCategoryRequest;
import com.tap2eat.catalog.dtos.request.category.UpdateCategoryRequest;
import com.tap2eat.catalog.dtos.request.product.AvailabilityConfigRequest;
import com.tap2eat.catalog.dtos.request.product.CreateProductRequest;
import com.tap2eat.catalog.dtos.request.product.DailyAvailabilityRequest;
import com.tap2eat.catalog.dtos.request.product.ImageMetadataRequest;
import com.tap2eat.catalog.dtos.request.product.ModifierGroupRequest;
import com.tap2eat.catalog.dtos.request.product.ModifierOptionRequest;
import com.tap2eat.catalog.dtos.request.product.TimeRangeRequest;
import com.tap2eat.catalog.dtos.request.product.UpdateProductRequest;
import com.tap2eat.catalog.dtos.request.restaurant.CreateRestaurantRequest;
import com.tap2eat.catalog.dtos.request.restaurant.UpdateRestaurantRequest;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.embedded.DailyAvailability;
import com.tap2eat.catalog.models.embedded.ImageMetadata;
import com.tap2eat.catalog.models.embedded.ModifierGroup;
import com.tap2eat.catalog.models.embedded.ModifierOption;
import com.tap2eat.catalog.models.embedded.TimeRange;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import com.tap2eat.catalog.models.enums.ProductType;
import com.tap2eat.catalog.models.enums.SelectionType;
import com.tap2eat.catalog.models.enums.StorageProvider;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class CatalogTestDataFactory {

    public static final String OWNER_ID = "owner-1";
    public static final String RESTAURANT_ID = "restaurant-1";
    public static final String RESTAURANT_RFC = "TAP260520ABC";
    public static final String BRANCH_ID = "branch-1";
    public static final String CATEGORY_ID = "category-1";
    public static final String PRODUCT_ID = "product-1";

    private CatalogTestDataFactory() {
    }

    public static RestaurantDocument restaurant() {
        return restaurant(RESTAURANT_ID, OWNER_ID);
    }

    public static RestaurantDocument restaurant(String id, String ownerAccountId) {
        RestaurantDocument restaurant = new RestaurantDocument();
        restaurant.setId(id);
        restaurant.setOwnerAccountId(ownerAccountId);
        restaurant.setName("Demo Restaurant");
        restaurant.setDescription("Fresh food");
        restaurant.setRfc(RESTAURANT_RFC);
        restaurant.setLogo(imageMetadata());
        restaurant.setIsActive(Boolean.TRUE);
        restaurant.setDeletedAt(null);
        return restaurant;
    }

    public static RestaurantDocument deletedRestaurant(String id, String ownerAccountId) {
        RestaurantDocument restaurant = restaurant(id, ownerAccountId);
        restaurant.setIsActive(Boolean.FALSE);
        restaurant.setDeletedAt(LocalDateTime.now());
        return restaurant;
    }

    public static BranchDocument branch() {
        return branch(BRANCH_ID, RESTAURANT_ID, openAvailability());
    }

    public static BranchDocument branch(String id, String restaurantId, AvailabilityConfig availability) {
        BranchDocument branch = new BranchDocument();
        branch.setId(id);
        branch.setRestaurantId(restaurantId);
        branch.setName("Centro");
        branch.setPhoneNumber("+525512345678");
        branch.setFormattedAddress("Av Reforma 123, CDMX");
        branch.setStreet("Av Reforma");
        branch.setExteriorNumber("123");
        branch.setNeighborhood("Centro");
        branch.setCity("Ciudad de Mexico");
        branch.setState("CDMX");
        branch.setPostalCode("06000");
        branch.setCountry("Mexico");
        branch.setLatitude(19.4326);
        branch.setLongitude(-99.1332);
        branch.setGooglePlaceId("place-1");
        branch.setAvailability(availability);
        branch.setIsMainBranch(Boolean.FALSE);
        branch.setIsActive(Boolean.TRUE);
        branch.setDeletedAt(null);
        return branch;
    }

    public static CategoryDocument category() {
        return category(CATEGORY_ID, RESTAURANT_ID, openAvailability());
    }

    public static CategoryDocument category(String id, String restaurantId, AvailabilityConfig availability) {
        CategoryDocument category = new CategoryDocument();
        category.setId(id);
        category.setRestaurantId(restaurantId);
        category.setName("Tacos");
        category.setDescription("Tacos and sides");
        category.setDisplayOrder(1);
        category.setImage(imageMetadata());
        category.setAvailability(availability);
        category.setIsActive(Boolean.TRUE);
        category.setDeletedAt(null);
        return category;
    }

    public static ProductDocument simpleProduct() {
        return simpleProduct(PRODUCT_ID, RESTAURANT_ID, CATEGORY_ID);
    }

    public static ProductDocument simpleProduct(String id, String restaurantId, String categoryId) {
        ProductDocument product = baseProduct(id, restaurantId, categoryId);
        product.setProductType(ProductType.SIMPLE);
        product.setModifierGroups(new ArrayList<>());
        return product;
    }

    public static ProductDocument customizableProduct() {
        ProductDocument product = baseProduct(PRODUCT_ID, RESTAURANT_ID, CATEGORY_ID);
        product.setProductType(ProductType.CUSTOMIZABLE);
        product.setModifierGroups(List.of(modifierGroup()));
        return product;
    }

    private static ProductDocument baseProduct(String id, String restaurantId, String categoryId) {
        ProductDocument product = new ProductDocument();
        product.setId(id);
        product.setRestaurantId(restaurantId);
        product.setCategoryId(categoryId);
        product.setName("Al pastor");
        product.setDescription("Classic taco");
        product.setPrice(BigDecimal.valueOf(35));
        product.setImage(imageMetadata());
        product.setAvailability(openAvailability());
        product.setDisplayOrder(1);
        product.setFeatured(Boolean.TRUE);
        product.setTags(List.of("popular"));
        product.setDietaryFlags(List.of("spicy"));
        product.setAllergens(List.of("gluten"));
        product.setIsActive(Boolean.TRUE);
        product.setDeletedAt(null);
        return product;
    }

    public static AvailabilityConfig openAvailability() {
        return availability(AvailabilityStatus.AVAILABLE, DayOfWeek.TUESDAY, "10:00", "23:00");
    }

    public static AvailabilityConfig closedAvailability() {
        return availability(AvailabilityStatus.AVAILABLE, DayOfWeek.TUESDAY, "08:00", "10:00");
    }

    public static AvailabilityConfig availability(
            AvailabilityStatus status,
            DayOfWeek day,
            String start,
            String end
    ) {
        AvailabilityConfig availability = new AvailabilityConfig();
        availability.setStatus(status);
        availability.setWeeklySchedule(List.of(dailyAvailability(day, start, end)));
        return availability;
    }

    public static DailyAvailability dailyAvailability(DayOfWeek day, String start, String end) {
        DailyAvailability dailyAvailability = new DailyAvailability();
        dailyAvailability.setDayOfWeek(day);
        dailyAvailability.setEnabled(Boolean.TRUE);
        dailyAvailability.setTimeRanges(List.of(timeRange(start, end)));
        return dailyAvailability;
    }

    public static TimeRange timeRange(String start, String end) {
        TimeRange range = new TimeRange();
        range.setStartTime(LocalTime.parse(start));
        range.setEndTime(LocalTime.parse(end));
        return range;
    }

    public static ModifierGroup modifierGroup() {
        ModifierGroup group = new ModifierGroup();
        group.setId("group-1");
        group.setName("Salsa");
        group.setSelectionType(SelectionType.SINGLE);
        group.setRequired(Boolean.TRUE);
        group.setMinSelections(1);
        group.setMaxSelections(1);
        group.setDisplayOrder(1);
        group.setIsActive(Boolean.TRUE);
        group.setOptions(List.of(modifierOption("option-1", "Verde", true)));
        return group;
    }

    public static ModifierOption modifierOption(String id, String name, boolean active) {
        ModifierOption option = new ModifierOption();
        option.setId(id);
        option.setName(name);
        option.setAdditionalPrice(BigDecimal.valueOf(5));
        option.setIsActive(active);
        option.setDisplayOrder(1);
        return option;
    }

    public static ImageMetadata imageMetadata() {
        ImageMetadata image = new ImageMetadata();
        image.setUrl("https://cdn.tap2eat.test/image.webp");
        image.setObjectKey("tap2eat/tests/image");
        image.setProvider(StorageProvider.CLOUDINARY);
        image.setContentType("image/webp");
        image.setSize(123L);
        image.setUploadedAt(LocalDateTime.of(2026, 5, 19, 12, 0));
        return image;
    }

    public static CreateRestaurantRequest createRestaurantRequest() {
        return new CreateRestaurantRequest(OWNER_ID, "Demo Restaurant", "Fresh food", RESTAURANT_RFC, imageRequest());
    }

    public static UpdateRestaurantRequest updateRestaurantRequest() {
        return new UpdateRestaurantRequest("Updated Restaurant", "Updated description", "UPD260520ABC", imageRequest());
    }

    public static CreateBranchRequest createBranchRequest() {
        return new CreateBranchRequest(
                RESTAURANT_ID,
                "Centro",
                "+525512345678",
                "Av Reforma 123, CDMX",
                "Av Reforma",
                "123",
                null,
                "Centro",
                "Ciudad de Mexico",
                "CDMX",
                "06000",
                "Mexico",
                "Near the plaza",
                19.4326,
                -99.1332,
                "place-1",
                availabilityRequest(),
                Boolean.FALSE
        );
    }

    public static UpdateBranchRequest updateBranchRequest() {
        return new UpdateBranchRequest(
                "Roma",
                "+525587654321",
                "Calle Roma 456, CDMX",
                "Calle Roma",
                "456",
                null,
                "Roma Norte",
                "Ciudad de Mexico",
                "CDMX",
                "06700",
                "Mexico",
                "Second floor",
                19.4200,
                -99.1600,
                "place-2",
                availabilityRequest(),
                Boolean.TRUE
        );
    }

    public static CreateCategoryRequest createCategoryRequest() {
        return new CreateCategoryRequest(RESTAURANT_ID, "Tacos", "Tacos and sides", 1, imageRequest(), availabilityRequest());
    }

    public static UpdateCategoryRequest updateCategoryRequest() {
        return new UpdateCategoryRequest("Bebidas", "Drinks", 2, imageRequest(), availabilityRequest());
    }

    public static CreateProductRequest createSimpleProductRequest() {
        return new CreateProductRequest(
                RESTAURANT_ID,
                CATEGORY_ID,
                "Al pastor",
                "Classic taco",
                ProductType.SIMPLE,
                BigDecimal.valueOf(35),
                imageRequest(),
                List.of(),
                availabilityRequest(),
                Boolean.TRUE,
                1,
                Boolean.TRUE,
                List.of("popular"),
                List.of("spicy"),
                List.of("gluten")
        );
    }

    public static CreateProductRequest createCustomizableProductRequest() {
        CreateProductRequest base = createSimpleProductRequest();
        return new CreateProductRequest(
                base.restaurantId(),
                base.categoryId(),
                base.name(),
                base.description(),
                ProductType.CUSTOMIZABLE,
                base.price(),
                base.image(),
                List.of(modifierGroupRequest()),
                base.availability(),
                base.active(),
                base.displayOrder(),
                base.featured(),
                base.tags(),
                base.dietaryFlags(),
                base.allergens()
        );
    }

    public static UpdateProductRequest updateProductRequest() {
        return new UpdateProductRequest(
                CATEGORY_ID,
                "Updated taco",
                "Updated product",
                ProductType.CUSTOMIZABLE,
                BigDecimal.valueOf(40),
                imageRequest(),
                List.of(modifierGroupRequest()),
                availabilityRequest(),
                Boolean.TRUE,
                2,
                Boolean.FALSE,
                List.of("new"),
                List.of("vegan"),
                List.of("soy")
        );
    }

    public static ImageMetadataRequest imageRequest() {
        return new ImageMetadataRequest(
                "https://cdn.tap2eat.test/request.webp",
                "tap2eat/tests/request",
                StorageProvider.CLOUDINARY
        );
    }

    public static AvailabilityConfigRequest availabilityRequest() {
        return new AvailabilityConfigRequest(
                AvailabilityStatus.AVAILABLE,
                null,
                null,
                List.of(new DailyAvailabilityRequest(
                        DayOfWeek.TUESDAY,
                        Boolean.TRUE,
                        List.of(new TimeRangeRequest(LocalTime.of(10, 0), LocalTime.of(23, 0)))
                ))
        );
    }

    public static ModifierGroupRequest modifierGroupRequest() {
        return new ModifierGroupRequest(
                "group-1",
                "Salsa",
                SelectionType.SINGLE,
                1,
                1,
                Boolean.TRUE,
                Boolean.TRUE,
                1,
                List.of(new ModifierOptionRequest("option-1", "Verde", BigDecimal.valueOf(5), Boolean.TRUE, 1))
        );
    }
}
