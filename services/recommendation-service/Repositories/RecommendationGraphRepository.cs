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
        WITH customer
        UNWIND $products AS productPayload
        MERGE (product:Product {id: productPayload.productId})
        MERGE (product)-[:SOLD_BY]->(restaurant)
        MERGE (customer)-[orderedProduct:ORDERED_PRODUCT]->(product)
        ON CREATE SET orderedProduct.count = 0
        SET orderedProduct.count = orderedProduct.count + 1,
            orderedProduct.lastOrderedAt = $deliveredAt
        WITH customer, product, productPayload
        UNWIND productPayload.tags AS tagName
        MERGE (tag:Tag {name: tagName})
        MERGE (product)-[:HAS_TAG]->(tag)
        MERGE (customer)-[likesTag:LIKES_TAG]->(tag)
        ON CREATE SET likesTag.score = 0
        SET likesTag.score = likesTag.score + 1,
            likesTag.lastSeenAt = $deliveredAt
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
        var products = update.Products
            .Where(product => !string.IsNullOrWhiteSpace(product.ProductId))
            .Select(product => new Dictionary<string, object>
            {
                ["productId"] = product.ProductId,
                ["tags"] = product.Tags
                    .Where(tag => !string.IsNullOrWhiteSpace(tag))
                    .Distinct(StringComparer.OrdinalIgnoreCase)
                    .ToArray()
            })
            .ToArray();

        var parameters = new
        {
            update.CustomerAccountId,
            update.RestaurantId,
            update.BranchId,
            deliveredAt = update.DeliveredAt,
            products
        };

        await using var session = _driver.AsyncSession();
        await session.ExecuteWriteAsync(async tx =>
        {
            await tx.RunAsync(UpsertDeliveredOrderQuery, parameters);
        });
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
            var cursor = await session.RunAsync(PreferredTagsQuery, new { customerAccountId });
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
                new { tagNames = normalizedTags });
            var records = await cursor.ToListAsync(record => record["id"].As<string>());

            return records;
        }
        catch (Exception exception)
        {
            _logger.LogWarning(exception, "Could not load tag-based restaurant recommendations from Neo4j.");
            return [];
        }
    }
}
