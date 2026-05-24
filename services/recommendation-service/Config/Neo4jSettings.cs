namespace RecommendationService.Config;

public sealed class Neo4jSettings
{
    public const string SectionName = "Neo4j";

    public string Uri { get; set; } = "bolt://localhost:7687";

    public string Username { get; set; } = "neo4j";

    public string Password { get; set; } = string.Empty;
}
