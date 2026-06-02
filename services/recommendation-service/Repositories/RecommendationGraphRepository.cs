using Neo4j.Driver;

namespace RecommendationService.Repositories;

public sealed class RecommendationGraphRepository : IRecommendationGraphRepository
{
    public const string UpsertDeliveredOrderQuery = """
        MERGE (customer:Customer {id: $customerAccountId})
        MERGE (restaurant:Restaurant {id: $restaurantId})
        MERGE (branch:Branch {id: $branchId})
        MERGE (branch)-[:BELONGS_TO]->(restaurant)
        MERGE (customer)-[orderedFrom:ORDERED_FROM]->(restaurant)
        ON CREATE SET orderedFrom.count = 0
        SET orderedFrom.count = orderedFrom.count + 1,
            orderedFrom.lastOrderedAt = $deliveredAt
        MERGE (customer)-[orderedAt:ORDERED_AT]->(branch)
        ON CREATE SET orderedAt.count = 0
        SET orderedAt.count = orderedAt.count + 1,
            orderedAt.lastOrderedAt = $deliveredAt
        WITH customer, restaurant
        UNWIND $products AS productPayload
        MERGE (product:Product {id: productPayload.productId})
        SET product.nameSnapshot = coalesce(productPayload.productNameSnapshot, product.nameSnapshot)
        MERGE (product)-[:SOLD_BY]->(restaurant)
        MERGE (customer)-[orderedProduct:ORDERED_PRODUCT]->(product)
        ON CREATE SET orderedProduct.count = 0
        SET orderedProduct.count = orderedProduct.count + productPayload.quantity,
            orderedProduct.lastOrderedAt = $deliveredAt
        WITH customer, product, productPayload
        UNWIND productPayload.tags AS tagName
        MERGE (tag:Tag {name: tagName})
        MERGE (product)-[:HAS_TAG]->(tag)
        MERGE (customer)-[likesTag:LIKES_TAG]->(tag)
        ON CREATE SET likesTag.score = 0
        SET likesTag.score = likesTag.score + productPayload.quantity,
            likesTag.lastSeenAt = $deliveredAt
        """;

    public const string AddFavoriteRestaurantQuery = """
        MERGE (customer:Customer {id: $customerAccountId})
        MERGE (restaurant:Restaurant {id: $restaurantId})
        MERGE (customer)-[favorite:FAVORITE_RESTAURANT]->(restaurant)
        ON CREATE SET favorite.createdAt = $createdAt
        RETURN restaurant.id AS restaurantId, favorite.createdAt AS createdAt
        """;

    public const string RemoveFavoriteRestaurantQuery = """
        MATCH (:Customer {id: $customerAccountId})-[favorite:FAVORITE_RESTAURANT]->(:Restaurant {id: $restaurantId})
        DELETE favorite
        """;

    public const string AddFavoriteProductQuery = """
        MERGE (customer:Customer {id: $customerAccountId})
        MERGE (restaurant:Restaurant {id: $restaurantId})
        MERGE (product:Product {id: $productId})
        MERGE (product)-[:SOLD_BY]->(restaurant)
        MERGE (customer)-[favorite:FAVORITE_PRODUCT]->(product)
        ON CREATE SET favorite.createdAt = $createdAt
        RETURN restaurant.id AS restaurantId, product.id AS productId, favorite.createdAt AS createdAt
        """;

    public const string RemoveFavoriteProductQuery = """
        MATCH (:Customer {id: $customerAccountId})-[favorite:FAVORITE_PRODUCT]->(:Product {id: $productId})
        DELETE favorite
        """;

    public const string FavoriteRestaurantsQuery = """
        MATCH (:Customer {id: $customerAccountId})-[favorite:FAVORITE_RESTAURANT]->(restaurant:Restaurant)
        RETURN restaurant.id AS restaurantId, favorite.createdAt AS createdAt
        ORDER BY favorite.createdAt DESC
        """;

    public const string FavoriteProductsQuery = """
        MATCH (:Customer {id: $customerAccountId})-[favorite:FAVORITE_PRODUCT]->(product:Product)-[:SOLD_BY]->(restaurant:Restaurant)
        RETURN restaurant.id AS restaurantId, product.id AS productId, favorite.createdAt AS createdAt
        ORDER BY favorite.createdAt DESC
        """;

    public const string FeaturedProductCandidatesQuery = """
        MATCH (customer:Customer)-[:FAVORITE_PRODUCT]->(product:Product)-[:SOLD_BY]->(:Restaurant {id: $restaurantId})
        RETURN product.id AS productId, count(DISTINCT customer) AS favoriteCount
        ORDER BY favoriteCount DESC, product.id ASC
        LIMIT 10
        """;

    private const string PreferredTagsQuery = """
        MATCH (:Customer {id: $customerAccountId})-[likes:LIKES_TAG]->(tag:Tag)
        RETURN tag.name AS name
        ORDER BY likes.score DESC, likes.lastSeenAt DESC
        LIMIT 10
        """;

    private const string RecommendedRestaurantsByTagsQuery = """
        MATCH (restaurant:Restaurant)<-[:SOLD_BY]-(:Product)-[:HAS_TAG]->(tag:Tag)
        WHERE tag.name IN $tagNames
        RETURN restaurant.id AS id, count(tag) AS tagMatches
        ORDER BY tagMatches DESC
        LIMIT 20
        """;

    private const string AlsoOrderedRestaurantsQuery = """
        MATCH (customer:Customer {id: $customerAccountId})-[:ORDERED_FROM]->(knownRestaurant:Restaurant)
        MATCH (similarCustomer:Customer)-[:ORDERED_FROM]->(knownRestaurant)
        WHERE similarCustomer.id <> $customerAccountId
        MATCH (similarCustomer)-[alsoOrdered:ORDERED_FROM]->(recommendedRestaurant:Restaurant)
        WHERE NOT (customer)-[:ORDERED_FROM]->(recommendedRestaurant)
        RETURN recommendedRestaurant.id AS id, sum(coalesce(alsoOrdered.count, 1)) AS score
        ORDER BY score DESC
        LIMIT 20
        """;

    private readonly IDriver _driver;
    private readonly ILogger<RecommendationGraphRepository> _logger;

    public RecommendationGraphRepository(
        IDriver driver,
        ILogger<RecommendationGraphRepository> logger)
    {
        _driver = driver;
        _logger = logger;
    }

    public async Task InitializeAsync(CancellationToken cancellationToken = default)
    {
        var statements = new[]
        {
            "CREATE CONSTRAINT customer_id IF NOT EXISTS FOR (c:Customer) REQUIRE c.id IS UNIQUE",
            "CREATE CONSTRAINT restaurant_id IF NOT EXISTS FOR (r:Restaurant) REQUIRE r.id IS UNIQUE",
            "CREATE CONSTRAINT branch_id IF NOT EXISTS FOR (b:Branch) REQUIRE b.id IS UNIQUE",
            "CREATE CONSTRAINT product_id IF NOT EXISTS FOR (p:Product) REQUIRE p.id IS UNIQUE",
            "CREATE CONSTRAINT tag_name IF NOT EXISTS FOR (t:Tag) REQUIRE t.name IS UNIQUE"
        };

        try
        {
            await using var session = _driver.AsyncSession();
            foreach (var statement in statements)
            {
                await session.RunAsync(statement);
            }
        }
        catch (Exception exception)
        {
            _logger.LogWarning(exception, "Neo4j initialization failed. The service will continue and retry on later graph operations.");
        }
    }

    public async Task UpsertDeliveredOrderAsync(
        DeliveredOrderGraphUpdate update,
        CancellationToken cancellationToken = default)
    {
        var parameters = BuildUpsertDeliveredOrderParameters(update);

        await using var session = _driver.AsyncSession();
        await session.ExecuteWriteAsync(async tx =>
        {
            await tx.RunAsync(UpsertDeliveredOrderQuery, parameters);
        });
    }

    public static Dictionary<string, object?> BuildUpsertDeliveredOrderParameters(
        DeliveredOrderGraphUpdate update)
    {
        var products = update.Products
            .Where(product => !string.IsNullOrWhiteSpace(product.ProductId))
            .Select(product => new Dictionary<string, object?>
            {
                ["productId"] = product.ProductId,
                ["quantity"] = product.Quantity > 0 ? product.Quantity : 1,
                ["productNameSnapshot"] = string.IsNullOrWhiteSpace(product.ProductNameSnapshot)
                    ? null
                    : product.ProductNameSnapshot,
                ["tags"] = product.Tags
                    .Where(tag => !string.IsNullOrWhiteSpace(tag))
                    .Distinct(StringComparer.OrdinalIgnoreCase)
                    .ToArray()
            })
            .ToArray();

        return BuildUpsertDeliveredOrderParameters(update, products);
    }

    private static Dictionary<string, object?> BuildUpsertDeliveredOrderParameters(
        DeliveredOrderGraphUpdate update,
        IReadOnlyList<Dictionary<string, object?>> products)
    {
        return new Dictionary<string, object?>
        {
            ["customerAccountId"] = update.CustomerAccountId,
            ["restaurantId"] = update.RestaurantId,
            ["branchId"] = update.BranchId,
            ["deliveredAt"] = update.DeliveredAt,
            ["products"] = products
        };
    }

    public async Task<IReadOnlyList<string>> GetPreferredTagsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(customerAccountId))
        {
            return [];
        }

        try
        {
            await using var session = _driver.AsyncSession();
            var cursor = await session.RunAsync(
                PreferredTagsQuery,
                new Dictionary<string, object?>
                {
                    ["customerAccountId"] = customerAccountId
                });
            var records = await cursor.ToListAsync(record => record["name"].As<string>());

            return records;
        }
        catch (Exception exception)
        {
            _logger.LogWarning(exception, "Could not load preferred tags for customer {CustomerAccountId}.", customerAccountId);
            return [];
        }
    }

    public async Task<IReadOnlyList<string>> GetRecommendedRestaurantIdsByTagsAsync(
        IReadOnlyList<string> tagNames,
        CancellationToken cancellationToken = default)
    {
        var normalizedTags = tagNames
            .Where(tag => !string.IsNullOrWhiteSpace(tag))
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .ToArray();

        if (normalizedTags.Length == 0)
        {
            return [];
        }

        try
        {
            await using var session = _driver.AsyncSession();
            var cursor = await session.RunAsync(
                RecommendedRestaurantsByTagsQuery,
                new Dictionary<string, object?>
                {
                    ["tagNames"] = normalizedTags
                });
            var records = await cursor.ToListAsync(record => record["id"].As<string>());

            return records;
        }
        catch (Exception exception)
        {
            _logger.LogWarning(exception, "Could not load tag-based restaurant recommendations from Neo4j.");
            return [];
        }
    }

    public async Task<IReadOnlyList<string>> GetAlsoOrderedRestaurantIdsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(customerAccountId))
        {
            return [];
        }

        try
        {
            await using var session = _driver.AsyncSession();
            var cursor = await session.RunAsync(
                AlsoOrderedRestaurantsQuery,
                new Dictionary<string, object?>
                {
                    ["customerAccountId"] = customerAccountId
                });
            var records = await cursor.ToListAsync(record => record["id"].As<string>());

            return records;
        }
        catch (Exception exception)
        {
            _logger.LogWarning(exception, "Could not load also-ordered restaurant recommendations from Neo4j.");
            return [];
        }
    }

    public async Task<FavoriteRestaurantGraphRecord> AddFavoriteRestaurantAsync(
        string customerAccountId,
        string restaurantId,
        DateTimeOffset createdAt,
        CancellationToken cancellationToken = default)
    {
        await using var session = _driver.AsyncSession();
        var record = await session.ExecuteWriteAsync(async tx =>
        {
            var cursor = await tx.RunAsync(
                AddFavoriteRestaurantQuery,
                new Dictionary<string, object?>
                {
                    ["customerAccountId"] = customerAccountId,
                    ["restaurantId"] = restaurantId,
                    ["createdAt"] = ToGraphDate(createdAt)
                });

            return await cursor.SingleAsync();
        });

        return ToFavoriteRestaurantRecord(record);
    }

    public async Task RemoveFavoriteRestaurantAsync(
        string customerAccountId,
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        await using var session = _driver.AsyncSession();
        await session.ExecuteWriteAsync(async tx =>
        {
            await tx.RunAsync(
                RemoveFavoriteRestaurantQuery,
                new Dictionary<string, object?>
                {
                    ["customerAccountId"] = customerAccountId,
                    ["restaurantId"] = restaurantId
                });
        });
    }

    public async Task<FavoriteProductGraphRecord> AddFavoriteProductAsync(
        string customerAccountId,
        string restaurantId,
        string productId,
        DateTimeOffset createdAt,
        CancellationToken cancellationToken = default)
    {
        await using var session = _driver.AsyncSession();
        var record = await session.ExecuteWriteAsync(async tx =>
        {
            var cursor = await tx.RunAsync(
                AddFavoriteProductQuery,
                new Dictionary<string, object?>
                {
                    ["customerAccountId"] = customerAccountId,
                    ["restaurantId"] = restaurantId,
                    ["productId"] = productId,
                    ["createdAt"] = ToGraphDate(createdAt)
                });

            return await cursor.SingleAsync();
        });

        return ToFavoriteProductRecord(record);
    }

    public async Task RemoveFavoriteProductAsync(
        string customerAccountId,
        string productId,
        CancellationToken cancellationToken = default)
    {
        await using var session = _driver.AsyncSession();
        await session.ExecuteWriteAsync(async tx =>
        {
            await tx.RunAsync(
                RemoveFavoriteProductQuery,
                new Dictionary<string, object?>
                {
                    ["customerAccountId"] = customerAccountId,
                    ["productId"] = productId
                });
        });
    }

    public async Task<IReadOnlyList<FavoriteRestaurantGraphRecord>> GetFavoriteRestaurantsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(customerAccountId))
        {
            return [];
        }

        await using var session = _driver.AsyncSession();
        var cursor = await session.RunAsync(
            FavoriteRestaurantsQuery,
            new Dictionary<string, object?>
            {
                ["customerAccountId"] = customerAccountId
            });

        return await cursor.ToListAsync(ToFavoriteRestaurantRecord);
    }

    public async Task<IReadOnlyList<FavoriteProductGraphRecord>> GetFavoriteProductsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(customerAccountId))
        {
            return [];
        }

        await using var session = _driver.AsyncSession();
        var cursor = await session.RunAsync(
            FavoriteProductsQuery,
            new Dictionary<string, object?>
            {
                ["customerAccountId"] = customerAccountId
            });

        return await cursor.ToListAsync(ToFavoriteProductRecord);
    }

    public async Task<IReadOnlyList<FeaturedProductGraphRecord>> GetFeaturedProductCandidatesAsync(
        string restaurantId,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(restaurantId))
        {
            return [];
        }

        await using var session = _driver.AsyncSession();
        var cursor = await session.RunAsync(
            FeaturedProductCandidatesQuery,
            new Dictionary<string, object?>
            {
                ["restaurantId"] = restaurantId
            });

        return await cursor.ToListAsync(record => new FeaturedProductGraphRecord(
            record["productId"].As<string>(),
            Convert.ToInt32(record["favoriteCount"].As<long>())));
    }

    private static FavoriteRestaurantGraphRecord ToFavoriteRestaurantRecord(IRecord record)
    {
        return new FavoriteRestaurantGraphRecord(
            record["restaurantId"].As<string>(),
            FromGraphDate(record["createdAt"]));
    }

    private static FavoriteProductGraphRecord ToFavoriteProductRecord(IRecord record)
    {
        return new FavoriteProductGraphRecord(
            record["restaurantId"].As<string>(),
            record["productId"].As<string>(),
            FromGraphDate(record["createdAt"]));
    }

    private static string ToGraphDate(DateTimeOffset value)
    {
        return value.UtcDateTime.ToString("O");
    }

    private static DateTimeOffset FromGraphDate(object value)
    {
        return DateTimeOffset.TryParse(value.As<string>(), out var parsed)
            ? parsed
            : DateTimeOffset.MinValue;
    }
}
