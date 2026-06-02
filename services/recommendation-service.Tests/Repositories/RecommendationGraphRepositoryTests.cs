using FluentAssertions;
using RecommendationService.Repositories;

namespace RecommendationService.Tests.Repositories;

public sealed class RecommendationGraphRepositoryTests
{
    [Fact]
    public void UpsertDeliveredOrderQuery_ShouldUseMergeForRequiredGraphNodesAndRelations()
    {
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("MERGE (customer:Customer");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("MERGE (restaurant:Restaurant");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("MERGE (branch:Branch");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("MERGE (product:Product");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("MERGE (tag:Tag");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("ORDERED_FROM");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("ORDERED_AT");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("ORDERED_PRODUCT");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("LIKES_TAG");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("BELONGS_TO");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("HAS_TAG");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("WITH customer, restaurant");
    }

    [Fact]
    public void AddFavoriteRestaurantQuery_ShouldUseMergeToAvoidDuplicates()
    {
        RecommendationGraphRepository.AddFavoriteRestaurantQuery.Should().Contain("MERGE (customer:Customer");
        RecommendationGraphRepository.AddFavoriteRestaurantQuery.Should().Contain("MERGE (restaurant:Restaurant");
        RecommendationGraphRepository.AddFavoriteRestaurantQuery.Should().Contain("MERGE (customer)-[favorite:FAVORITE_RESTAURANT]->(restaurant)");
        RecommendationGraphRepository.AddFavoriteRestaurantQuery.Should().Contain("ON CREATE SET favorite.createdAt = $createdAt");
    }

    [Fact]
    public void AddFavoriteProductQuery_ShouldStoreBaseProductWithoutModifiers()
    {
        RecommendationGraphRepository.AddFavoriteProductQuery.Should().Contain("MERGE (product:Product {id: $productId})");
        RecommendationGraphRepository.AddFavoriteProductQuery.Should().Contain("MERGE (product)-[:SOLD_BY]->(restaurant)");
        RecommendationGraphRepository.AddFavoriteProductQuery.Should().Contain("MERGE (customer)-[favorite:FAVORITE_PRODUCT]->(product)");
        RecommendationGraphRepository.AddFavoriteProductQuery.Should().NotContain("modifier");
        RecommendationGraphRepository.AddFavoriteProductQuery.Should().NotContain("extra");
    }

    [Fact]
    public void FeaturedProductCandidatesQuery_ShouldCountEachCustomerOnce()
    {
        RecommendationGraphRepository.FeaturedProductCandidatesQuery.Should().Contain("FAVORITE_PRODUCT");
        RecommendationGraphRepository.FeaturedProductCandidatesQuery.Should().Contain("SOLD_BY");
        RecommendationGraphRepository.FeaturedProductCandidatesQuery.Should().Contain("count(DISTINCT customer)");
        RecommendationGraphRepository.FeaturedProductCandidatesQuery.Should().Contain("ORDER BY favoriteCount DESC");
    }

    [Fact]
    public void UpsertDeliveredOrderQuery_ShouldUseExpectedParameterNames()
    {
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("$customerAccountId");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("$restaurantId");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("$branchId");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("$deliveredAt");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("$products");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("productPayload.productId");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("productPayload.quantity");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("productPayload.productNameSnapshot");
        RecommendationGraphRepository.UpsertDeliveredOrderQuery.Should().Contain("productPayload.tags");
    }

    [Fact]
    public void BuildUpsertDeliveredOrderParameters_ShouldIncludeRequiredNeo4jParameterNames()
    {
        var deliveredAt = DateTime.UtcNow;
        var parameters = RecommendationGraphRepository.BuildUpsertDeliveredOrderParameters(
            new DeliveredOrderGraphUpdate
            {
                CustomerAccountId = "customer-1",
                RestaurantId = "restaurant-1",
                BranchId = "branch-1",
                DeliveredAt = deliveredAt,
                Products =
                [
                    new DeliveredProductGraphUpdate
                    {
                        ProductId = "product-1",
                        Quantity = 2,
                        ProductNameSnapshot = "Burger",
                        Tags = ["hamburguesa", "Hamburguesa", "bbq"]
                    }
                ]
            });

        parameters.Keys.Should().Contain([
            "customerAccountId",
            "restaurantId",
            "branchId",
            "deliveredAt",
            "products"
        ]);
        parameters["customerAccountId"].Should().Be("customer-1");
        parameters["restaurantId"].Should().Be("restaurant-1");
        parameters["branchId"].Should().Be("branch-1");
        parameters["deliveredAt"].Should().Be(deliveredAt);
    }

    [Fact]
    public void BuildUpsertDeliveredOrderParameters_ShouldIncludeProductsWithTagsWithoutInvalidProductIds()
    {
        var parameters = RecommendationGraphRepository.BuildUpsertDeliveredOrderParameters(
            new DeliveredOrderGraphUpdate
            {
                CustomerAccountId = "customer-1",
                RestaurantId = "restaurant-1",
                BranchId = "branch-1",
                Products =
                [
                    new DeliveredProductGraphUpdate
                    {
                        ProductId = "product-1",
                        Quantity = 0,
                        ProductNameSnapshot = "Pizza",
                        Tags = ["pizza", "", "italiano"]
                    },
                    new DeliveredProductGraphUpdate
                    {
                        ProductId = "",
                        Quantity = 1,
                        Tags = ["ignored"]
                    }
                ]
            });

        var products = parameters["products"].Should()
            .BeAssignableTo<IReadOnlyList<Dictionary<string, object?>>>()
            .Subject;

        products.Should().ContainSingle();
        products[0]["productId"].Should().Be("product-1");
        products[0]["quantity"].Should().Be(1);
        products[0]["productNameSnapshot"].Should().Be("Pizza");
        products[0]["tags"].Should().BeEquivalentTo(new[] { "pizza", "italiano" });
    }
}
