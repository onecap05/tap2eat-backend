using FluentAssertions;
using RecommendationService.Config;
using RecommendationService.Dtos.Responses;

namespace RecommendationService.Tests.Config;

public sealed class RecommendationSettingsTests
{
    [Fact]
    public void ExternalServiceSettings_ShouldExposeDefaults()
    {
        var settings = new ExternalServiceSettings();

        ExternalServiceSettings.SectionName.Should().Be("ExternalServices");
        settings.CatalogServiceBaseUrl.Should().Be("http://localhost:8082");
    }

    [Fact]
    public void JwtSettings_ShouldExposeDefaults()
    {
        var settings = new JwtSettings();

        JwtSettings.SectionName.Should().Be("Jwt");
        settings.PublicKeyPath.Should().Be("../api-gateway/src/main/resources/keys/public_key.pem");
    }

    [Fact]
    public void Neo4jSettings_ShouldExposeDefaults()
    {
        var settings = new Neo4jSettings();

        Neo4jSettings.SectionName.Should().Be("Neo4j");
        settings.Uri.Should().Be("bolt://localhost:7687");
        settings.Username.Should().Be("neo4j");
        settings.Password.Should().BeEmpty();
    }

    [Fact]
    public void RecommendationSettings_ShouldExposeDefaults()
    {
        var settings = new RecommendationService.Config.RecommendationSettings();

        RecommendationService.Config.RecommendationSettings.SectionName.Should().Be("Recommendations");
        settings.DefaultRadiusKm.Should().Be(5);
        settings.MaxRadiusKm.Should().Be(25);
    }

    [Fact]
    public void RecommendationWarningResponse_ShouldExposeMessage()
    {
        var response = new RecommendationWarningResponse
        {
            Message = "Location is missing."
        };

        response.Message.Should().Be("Location is missing.");
    }
}
