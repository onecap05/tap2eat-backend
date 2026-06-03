using FluentAssertions;
using Microsoft.Extensions.Logging.Abstractions;
using Moq;
using Neo4j.Driver;
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

    [Fact]
    public async Task GetPreferredTagsAsync_WhenCustomerAccountIdIsEmpty_ShouldReturnEmptyList()
    {
        var repository = CreateRepository(Mock.Of<IDriver>());

        var tags = await repository.GetPreferredTagsAsync(" ");

        tags.Should().BeEmpty();
    }

    [Fact]
    public async Task GetPreferredTagsAsync_WhenNeo4jReturnsRecords_ShouldReturnTags()
    {
        var session = CreateSessionReturningRecords(
            Record(("name", "spicy")),
            Record(("name", "vegan")));
        var repository = CreateRepository(CreateDriver(session));

        var tags = await repository.GetPreferredTagsAsync("customer-1");

        tags.Should().Equal("spicy", "vegan");
    }

    [Fact]
    public async Task GetPreferredTagsAsync_WhenNeo4jFails_ShouldReturnEmptyList()
    {
        var session = new Mock<IAsyncSession>();
        session
            .Setup(item => item.RunAsync(
                It.IsAny<string>(),
                It.IsAny<IDictionary<string, object>>(),
                null))
            .ThrowsAsync(new InvalidOperationException("Neo4j read failed."));
        session
            .Setup(item => item.RunAsync(
                It.IsAny<string>(),
                It.IsAny<object>(),
                null))
            .ThrowsAsync(new InvalidOperationException("Neo4j read failed."));
        var repository = CreateRepository(CreateDriver(session.Object));

        var tags = await repository.GetPreferredTagsAsync("customer-1");

        tags.Should().BeEmpty();
    }

    [Fact]
    public async Task GetRecommendedRestaurantIdsByTagsAsync_WhenTagsAreEmpty_ShouldReturnEmptyList()
    {
        var repository = CreateRepository(Mock.Of<IDriver>());

        var ids = await repository.GetRecommendedRestaurantIdsByTagsAsync(["", " "]);

        ids.Should().BeEmpty();
    }

    [Fact]
    public async Task GetRecommendedRestaurantIdsByTagsAsync_WhenNeo4jReturnsRecords_ShouldReturnRestaurantIds()
    {
        var session = CreateSessionReturningRecords(
            Record(("id", "restaurant-1")),
            Record(("id", "restaurant-2")));
        var repository = CreateRepository(CreateDriver(session));

        var ids = await repository.GetRecommendedRestaurantIdsByTagsAsync(["spicy", "Spicy", "vegan"]);

        ids.Should().Equal("restaurant-1", "restaurant-2");
    }

    [Fact]
    public async Task GetRecommendedRestaurantIdsByTagsAsync_WhenNeo4jFails_ShouldReturnEmptyList()
    {
        var session = new Mock<IAsyncSession>();
        session
            .Setup(item => item.RunAsync(
                It.IsAny<string>(),
                It.IsAny<IDictionary<string, object>>(),
                null))
            .ThrowsAsync(new InvalidOperationException("Neo4j read failed."));
        session
            .Setup(item => item.RunAsync(
                It.IsAny<string>(),
                It.IsAny<object>(),
                null))
            .ThrowsAsync(new InvalidOperationException("Neo4j read failed."));
        var repository = CreateRepository(CreateDriver(session.Object));

        var ids = await repository.GetRecommendedRestaurantIdsByTagsAsync(["spicy"]);

        ids.Should().BeEmpty();
    }

    [Fact]
    public async Task GetAlsoOrderedRestaurantIdsAsync_WhenCustomerAccountIdIsEmpty_ShouldReturnEmptyList()
    {
        var repository = CreateRepository(Mock.Of<IDriver>());

        var ids = await repository.GetAlsoOrderedRestaurantIdsAsync("");

        ids.Should().BeEmpty();
    }

    [Fact]
    public async Task GetAlsoOrderedRestaurantIdsAsync_WhenNeo4jReturnsRecords_ShouldReturnRestaurantIds()
    {
        var session = CreateSessionReturningRecords(Record(("id", "restaurant-1")));
        var repository = CreateRepository(CreateDriver(session));

        var ids = await repository.GetAlsoOrderedRestaurantIdsAsync("customer-1");

        ids.Should().ContainSingle()
            .Which.Should().Be("restaurant-1");
    }

    [Fact]
    public async Task GetFavoriteRestaurantsAsync_WhenCustomerAccountIdIsEmpty_ShouldReturnEmptyList()
    {
        var repository = CreateRepository(Mock.Of<IDriver>());

        var favorites = await repository.GetFavoriteRestaurantsAsync(" ");

        favorites.Should().BeEmpty();
    }

    [Fact]
    public async Task GetFavoriteRestaurantsAsync_WhenNeo4jReturnsRecords_ShouldReturnFavorites()
    {
        var createdAt = DateTimeOffset.UtcNow;
        var session = CreateSessionReturningRecords(Record(
            ("restaurantId", "restaurant-1"),
            ("createdAt", createdAt.UtcDateTime.ToString("O"))));
        var repository = CreateRepository(CreateDriver(session));

        var favorites = await repository.GetFavoriteRestaurantsAsync("customer-1");

        favorites.Should().ContainSingle();
        favorites[0].RestaurantId.Should().Be("restaurant-1");
        favorites[0].CreatedAt.Should().BeCloseTo(createdAt, TimeSpan.FromSeconds(1));
    }

    [Fact]
    public async Task GetFavoriteProductsAsync_WhenCustomerAccountIdIsEmpty_ShouldReturnEmptyList()
    {
        var repository = CreateRepository(Mock.Of<IDriver>());

        var favorites = await repository.GetFavoriteProductsAsync(" ");

        favorites.Should().BeEmpty();
    }

    [Fact]
    public async Task GetFavoriteProductsAsync_WhenNeo4jReturnsRecords_ShouldReturnFavorites()
    {
        var createdAt = DateTimeOffset.UtcNow;
        var session = CreateSessionReturningRecords(Record(
            ("restaurantId", "restaurant-1"),
            ("productId", "product-1"),
            ("createdAt", createdAt.UtcDateTime.ToString("O"))));
        var repository = CreateRepository(CreateDriver(session));

        var favorites = await repository.GetFavoriteProductsAsync("customer-1");

        favorites.Should().ContainSingle();
        favorites[0].RestaurantId.Should().Be("restaurant-1");
        favorites[0].ProductId.Should().Be("product-1");
    }

    [Fact]
    public async Task GetFeaturedProductCandidatesAsync_WhenRestaurantIdIsEmpty_ShouldReturnEmptyList()
    {
        var repository = CreateRepository(Mock.Of<IDriver>());

        var products = await repository.GetFeaturedProductCandidatesAsync("");

        products.Should().BeEmpty();
    }

    [Fact]
    public async Task GetFeaturedProductCandidatesAsync_WhenNeo4jReturnsRecords_ShouldReturnCandidates()
    {
        var session = CreateSessionReturningRecords(Record(
            ("productId", "product-1"),
            ("favoriteCount", 3L)));
        var repository = CreateRepository(CreateDriver(session));

        var products = await repository.GetFeaturedProductCandidatesAsync("restaurant-1");

        products.Should().ContainSingle();
        products[0].ProductId.Should().Be("product-1");
        products[0].FavoriteCount.Should().Be(3);
    }

    [Fact]
    public async Task UpsertDeliveredOrderAsync_ShouldRunUpsertQueryWithParameters()
    {
        var runner = new Mock<IAsyncQueryRunner>();
        runner
            .Setup(item => item.RunAsync(
                RecommendationGraphRepository.UpsertDeliveredOrderQuery,
                It.IsAny<IDictionary<string, object>>()))
            .ReturnsAsync(new FakeResultCursor([]));
        var session = CreateWriteSession(runner.Object);
        var repository = CreateRepository(CreateDriver(session));

        await repository.UpsertDeliveredOrderAsync(new DeliveredOrderGraphUpdate
        {
            CustomerAccountId = "customer-1",
            RestaurantId = "restaurant-1",
            BranchId = "branch-1",
            Products =
            [
                new DeliveredProductGraphUpdate
                {
                    ProductId = "product-1",
                    Quantity = 2
                }
            ]
        });

        runner.Verify(
            item => item.RunAsync(
                RecommendationGraphRepository.UpsertDeliveredOrderQuery,
                It.Is<IDictionary<string, object>>(parameters => HasParameter(parameters, "customerAccountId", "customer-1"))),
            Times.Once);
    }

    [Fact]
    public async Task AddFavoriteRestaurantAsync_ShouldReturnCreatedFavorite()
    {
        var createdAt = DateTimeOffset.UtcNow;
        var runner = new Mock<IAsyncQueryRunner>();
        runner
            .Setup(item => item.RunAsync(
                RecommendationGraphRepository.AddFavoriteRestaurantQuery,
                It.IsAny<IDictionary<string, object>>()))
            .ReturnsAsync(new FakeResultCursor([
                Record(
                    ("restaurantId", "restaurant-1"),
                    ("createdAt", createdAt.UtcDateTime.ToString("O")))
            ]));
        var repository = CreateRepository(CreateDriver(CreateWriteSession(runner.Object)));

        var favorite = await repository.AddFavoriteRestaurantAsync(
            "customer-1",
            "restaurant-1",
            createdAt);

        favorite.RestaurantId.Should().Be("restaurant-1");
        favorite.CreatedAt.Should().BeCloseTo(createdAt, TimeSpan.FromSeconds(1));
    }

    [Fact]
    public async Task RemoveFavoriteRestaurantAsync_ShouldRunDeleteQuery()
    {
        var runner = new Mock<IAsyncQueryRunner>();
        runner
            .Setup(item => item.RunAsync(
                RecommendationGraphRepository.RemoveFavoriteRestaurantQuery,
                It.IsAny<IDictionary<string, object>>()))
            .ReturnsAsync(new FakeResultCursor([]));
        var repository = CreateRepository(CreateDriver(CreateWriteSession(runner.Object)));

        await repository.RemoveFavoriteRestaurantAsync("customer-1", "restaurant-1");

        runner.Verify(
            item => item.RunAsync(
                RecommendationGraphRepository.RemoveFavoriteRestaurantQuery,
                It.Is<IDictionary<string, object>>(parameters => HasParameter(parameters, "restaurantId", "restaurant-1"))),
            Times.Once);
    }

    [Fact]
    public async Task AddFavoriteProductAsync_ShouldReturnCreatedFavorite()
    {
        var createdAt = DateTimeOffset.UtcNow;
        var runner = new Mock<IAsyncQueryRunner>();
        runner
            .Setup(item => item.RunAsync(
                RecommendationGraphRepository.AddFavoriteProductQuery,
                It.IsAny<IDictionary<string, object>>()))
            .ReturnsAsync(new FakeResultCursor([
                Record(
                    ("restaurantId", "restaurant-1"),
                    ("productId", "product-1"),
                    ("createdAt", createdAt.UtcDateTime.ToString("O")))
            ]));
        var repository = CreateRepository(CreateDriver(CreateWriteSession(runner.Object)));

        var favorite = await repository.AddFavoriteProductAsync(
            "customer-1",
            "restaurant-1",
            "product-1",
            createdAt);

        favorite.RestaurantId.Should().Be("restaurant-1");
        favorite.ProductId.Should().Be("product-1");
    }

    [Fact]
    public async Task RemoveFavoriteProductAsync_ShouldRunDeleteQuery()
    {
        var runner = new Mock<IAsyncQueryRunner>();
        runner
            .Setup(item => item.RunAsync(
                RecommendationGraphRepository.RemoveFavoriteProductQuery,
                It.IsAny<IDictionary<string, object>>()))
            .ReturnsAsync(new FakeResultCursor([]));
        var repository = CreateRepository(CreateDriver(CreateWriteSession(runner.Object)));

        await repository.RemoveFavoriteProductAsync("customer-1", "product-1");

        runner.Verify(
            item => item.RunAsync(
                RecommendationGraphRepository.RemoveFavoriteProductQuery,
                It.Is<IDictionary<string, object>>(parameters => HasParameter(parameters, "productId", "product-1"))),
            Times.Once);
    }

    [Fact]
    public async Task InitializeAsync_WhenNeo4jFails_ShouldNotThrow()
    {
        var session = new Mock<IAsyncSession>();
        session
            .Setup(item => item.RunAsync(It.IsAny<string>(), null))
            .ThrowsAsync(new InvalidOperationException("Neo4j unavailable."));
        var repository = CreateRepository(CreateDriver(session.Object));

        var action = () => repository.InitializeAsync();

        await action.Should().NotThrowAsync();
    }

    private static RecommendationGraphRepository CreateRepository(IDriver driver)
    {
        return new RecommendationGraphRepository(
            driver,
            NullLogger<RecommendationGraphRepository>.Instance);
    }

    private static IDriver CreateDriver(IAsyncSession session)
    {
        var driver = new Mock<IDriver>();
        driver
            .Setup(item => item.AsyncSession())
            .Returns(session);
        driver
            .Setup(item => item.AsyncSession(It.IsAny<Action<SessionConfigBuilder>>()))
            .Returns(session);

        return driver.Object;
    }

    private static IAsyncSession CreateSessionReturningRecords(params IRecord[] records)
    {
        var session = new Mock<IAsyncSession>();
        session
            .Setup(item => item.RunAsync(
                It.IsAny<string>(),
                It.IsAny<IDictionary<string, object>>(),
                null))
            .ReturnsAsync(new FakeResultCursor(records));
        session
            .Setup(item => item.RunAsync(
                It.IsAny<string>(),
                It.IsAny<object>(),
                null))
            .ReturnsAsync(new FakeResultCursor(records));

        return session.Object;
    }

    private static IAsyncSession CreateWriteSession(IAsyncQueryRunner runner)
    {
        var session = new Mock<IAsyncSession>();
        session
            .Setup(item => item.ExecuteWriteAsync(
                It.IsAny<Func<IAsyncQueryRunner, Task>>(),
                null))
            .Returns<Func<IAsyncQueryRunner, Task>, Action<TransactionConfigBuilder>?>((work, _) => work(runner));
        session
            .Setup(item => item.ExecuteWriteAsync(
                It.IsAny<Func<IAsyncQueryRunner, Task<IRecord>>>(),
                null))
            .Returns<Func<IAsyncQueryRunner, Task<IRecord>>, Action<TransactionConfigBuilder>?>((work, _) => work(runner));

        return session.Object;
    }

    private static IRecord Record(params (string Key, object Value)[] values)
    {
        var dictionary = values.ToDictionary(item => item.Key, item => item.Value);
        var record = new Mock<IRecord>();
        record.Setup(item => item[It.IsAny<string>()])
            .Returns((string key) => dictionary[key]);
        record.Setup(item => item.Values)
            .Returns(dictionary);
        record.Setup(item => item.Keys)
            .Returns(dictionary.Keys.ToArray());

        return record.Object;
    }

    private static bool HasParameter(object parameters, string key, object expectedValue)
    {
        var dictionary = parameters.Should()
            .BeAssignableTo<IReadOnlyDictionary<string, object?>>()
            .Subject;

        return dictionary.TryGetValue(key, out var value) && Equals(value, expectedValue);
    }

    private sealed class FakeResultCursor : IResultCursor
    {
        private readonly IReadOnlyList<IRecord> _records;
        private int _index = -1;

        public FakeResultCursor(IReadOnlyList<IRecord> records)
        {
            _records = records;
        }

        public IRecord Current => _records[_index];

        public bool IsOpen => true;

        public IAsyncEnumerator<IRecord> GetAsyncEnumerator(CancellationToken cancellationToken = default)
        {
            return _records.ToAsyncEnumerable().GetAsyncEnumerator(cancellationToken);
        }

        public Task<string[]> KeysAsync()
        {
            return Task.FromResult(_records.FirstOrDefault()?.Keys.ToArray() ?? []);
        }

        public Task<IResultSummary> ConsumeAsync()
        {
            return Task.FromResult(Mock.Of<IResultSummary>());
        }

        public Task<bool> FetchAsync()
        {
            _index++;

            return Task.FromResult(_index < _records.Count);
        }

        public Task<IRecord?> PeekAsync()
        {
            return Task.FromResult(_index + 1 < _records.Count ? _records[_index + 1] : null);
        }
    }
}
