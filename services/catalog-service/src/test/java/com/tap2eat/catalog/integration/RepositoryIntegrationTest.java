package com.tap2eat.catalog.integration;

import com.tap2eat.catalog.config.MongoIntegrationTestBase;
import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.repositories.IBranchRepository;
import com.tap2eat.catalog.repositories.ICategoryRepository;
import com.tap2eat.catalog.repositories.IProductRepository;
import com.tap2eat.catalog.repositories.IRestaurantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RepositoryIntegrationTest extends MongoIntegrationTestBase {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IRestaurantRepository restaurantRepository;

    @Autowired
    private IBranchRepository branchRepository;

    @Autowired
    private ICategoryRepository categoryRepository;

    @Autowired
    private IProductRepository productRepository;

    @Test
    void integrationTests_shouldUseIsolatedCatalogTestDatabase() {
        assertThat(mongoTemplate.getDb().getName()).isEqualTo(TEST_DATABASE_NAME);
    }

    @Test
    void repositories_shouldExcludeSoftDeletedDocumentsFromActiveQueries() {
        RestaurantDocument restaurant = CatalogTestDataFactory.restaurant("restaurant-active", "owner-active");
        RestaurantDocument deletedRestaurant = CatalogTestDataFactory.deletedRestaurant("restaurant-deleted", "owner-deleted");
        restaurantRepository.save(restaurant);
        restaurantRepository.save(deletedRestaurant);

        BranchDocument branch = CatalogTestDataFactory.branch("branch-active", "restaurant-active", null);
        BranchDocument deletedBranch = CatalogTestDataFactory.branch("branch-deleted", "restaurant-active", null);
        deletedBranch.setIsActive(Boolean.FALSE);
        deletedBranch.setDeletedAt(LocalDateTime.now());
        branchRepository.save(branch);
        branchRepository.save(deletedBranch);

        CategoryDocument category = CatalogTestDataFactory.category("category-active", "restaurant-active", null);
        CategoryDocument deletedCategory = CatalogTestDataFactory.category("category-deleted", "restaurant-active", null);
        deletedCategory.setIsActive(Boolean.FALSE);
        deletedCategory.setDeletedAt(LocalDateTime.now());
        categoryRepository.save(category);
        categoryRepository.save(deletedCategory);

        ProductDocument product = CatalogTestDataFactory.simpleProduct("product-active", "restaurant-active", "category-active");
        ProductDocument deletedProduct = CatalogTestDataFactory.simpleProduct("product-deleted", "restaurant-active", "category-active");
        deletedProduct.setIsActive(Boolean.FALSE);
        deletedProduct.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);
        productRepository.save(deletedProduct);

        assertThat(restaurantRepository.findAllByIsActiveTrueAndDeletedAtIsNull())
                .extracting(RestaurantDocument::getId)
                .containsExactly("restaurant-active");
        assertThat(branchRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-active"))
                .extracting(BranchDocument::getId)
                .containsExactly("branch-active");
        assertThat(categoryRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-active"))
                .extracting(CategoryDocument::getId)
                .containsExactly("category-active");
        assertThat(productRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-active"))
                .extracting(ProductDocument::getId)
                .containsExactly("product-active");
        assertThat(productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull("product-deleted")).isEmpty();
    }
}
