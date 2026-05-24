using FluentAssertions;
using Microsoft.Extensions.Options;
using RecommendationService.Config;
using RecommendationService.Services;

namespace RecommendationService.Tests.Services;

public sealed class HaversineDistanceServiceTests
{
    private readonly HaversineDistanceService _service = new(
        Options.Create(new RecommendationSettings()));

    [Fact]
    public void CalculateDistanceKm_ShouldCalculateReasonableDistance()
    {
        var distance = _service.CalculateDistanceKm(
            19.4326,
            -99.1332,
            19.4270,
            -99.1677);

        distance.Should().BeApproximately(3.67, 0.2);
    }

    [Fact]
    public void NormalizeRadiusKm_WhenMissing_ShouldReturnDefaultFive()
    {
        _service.NormalizeRadiusKm(null).Should().Be(5);
    }

    [Fact]
    public void NormalizeRadiusKm_WhenGreaterThanMax_ShouldReturnTwentyFive()
    {
        _service.NormalizeRadiusKm(100).Should().Be(25);
    }

    [Theory]
    [InlineData(-91.0, -99.0)]
    [InlineData(19.0, -181.0)]
    [InlineData(null, -99.0)]
    [InlineData(19.0, null)]
    public void HasValidLocation_WhenInvalid_ShouldReturnFalse(double? lat, double? lng)
    {
        _service.HasValidLocation(lat, lng).Should().BeFalse();
    }
}
