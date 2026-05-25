using FluentAssertions;
using Microsoft.Extensions.Options;
using RecommendationService.Config;
using RecommendationService.Dtos.Requests;
using RecommendationService.Integrations.Catalog;
using RecommendationService.Services;
using RecommendationService.Tests.Fakes;

namespace RecommendationService.Tests.Services;

public sealed class RecommendationServiceImplTests
{
    [Fact]
    public async Task GetNearestBranchAsync_WithLocation_ShouldChooseNearestOpenBranch()
    {
        var (service, catalog) = CreateService();
        SeedRestaurantWithBranches(catalog);

        var response = await service.GetNearestBranchAsync(
            "restaurant-1",
            new RecommendationQueryRequest { Lat = 19.4326, Lng = -99.1332 });

        response.BranchId.Should().Be("branch-near");
        response.DistanceKm.Should().NotBeNull();
        response.Warning.Should().BeNull();
    }

    [Fact]
    public async Task GetNearestBranchAsync_WithoutLocation_ShouldChooseMainOpenBranch()
    {
        var (service, catalog) = CreateService();
        SeedRestaurantWithBranches(catalog);

        var response = await service.GetNearestBranchAsync(
            "restaurant-1",
            new RecommendationQueryRequest());

        response.BranchId.Should().Be("branch-main");
        response.Warning.Should().Be(RecommendationServiceImpl.NoLocationWarning);
    }

    [Fact]
    public async Task GetNearestBranchAsync_WithoutMainOpenBranch_ShouldChooseFirstOpenBranch()
    {
        var (service, catalog) = CreateService();
        catalog.Restaurants.Add(Restaurant("restaurant-1"));
        catalog.BranchesByRestaurantId["restaurant-1"] =
        [
            Branch("branch-first", isMain: false),
            Branch("branch-closed-main", isMain: true, open: false)
        ];

        var response = await service.GetNearestBranchAsync(
            "restaurant-1",
            new RecommendationQueryRequest());

        response.BranchId.Should().Be("branch-first");
        response.Warning.Should().Be(RecommendationServiceImpl.NoLocationWarning);
    }

    [Fact]
    public async Task GetNearbyAsync_ShouldFilterByRadiusAndSortByDistance()
    {
        var (service, catalog) = CreateService();
        SeedRestaurantWithBranches(catalog);

        var response = await service.GetNearbyAsync(new RecommendationQueryRequest
        {
            Lat = 19.4326,
            Lng = -99.1332,
            RadiusKm = 5
        });

        response.Should().ContainSingle();
        response[0].BranchId.Should().Be("branch-near");
        response[0].Reason.Should().Be("Cerca de ti");
        response[0].RecommendationType.Should().Be("NEARBY");
    }

    [Fact]
    public async Task GetForCustomerAsync_WithHistory_ShouldUseTagBasedRestaurantRecommendations()
    {
        var (service, catalog, graph) = CreateServiceWithGraph();
        SeedRestaurantWithBranches(catalog);
        graph.PreferredTags.Add("vegan");
        graph.RecommendedRestaurantIds.Add("restaurant-1");

        var response = await service.GetForCustomerAsync(
            "customer-1",
            new RecommendationQueryRequest());

        response.Should().ContainSingle();
        response[0].Reason.Should().Be("Hemos visto que te gusta vegan");
        response[0].RecommendationType.Should().Be("TASTE_BASED");
    }

    [Fact]
    public async Task GetCustomerSectionsAsync_WithSimilarCustomers_ShouldReturnAlsoOrderedSection()
    {
        var (service, catalog, graph) = CreateServiceWithGraph();
        SeedRestaurant(catalog, "restaurant-1");
        SeedRestaurant(catalog, "restaurant-2");
        graph.AlsoOrderedRestaurantIds.Add("restaurant-2");

        var response = await service.GetCustomerSectionsAsync(
            "customer-1",
            new RecommendationQueryRequest());

        response.AlsoOrdered.Should().ContainSingle();
        response.AlsoOrdered[0].RestaurantId.Should().Be("restaurant-2");
        response.AlsoOrdered[0].RecommendationType.Should().Be("ALSO_ORDERED");
        response.AlsoOrdered[0].Reason.Should().Be("Personas con gustos parecidos también pidieron aquí");
    }

    [Fact]
    public async Task GetCustomerSectionsAsync_WithPreferredTags_ShouldReturnTasteBasedSection()
    {
        var (service, catalog, graph) = CreateServiceWithGraph();
        SeedRestaurant(catalog, "restaurant-1");
        SeedRestaurant(catalog, "restaurant-2");
        graph.PreferredTags.Add("hamburguesa");
        graph.RecommendedRestaurantIds.Add("restaurant-2");

        var response = await service.GetCustomerSectionsAsync(
            "customer-1",
            new RecommendationQueryRequest());

        response.TasteBased.Should().ContainSingle();
        response.TasteBased[0].RestaurantId.Should().Be("restaurant-2");
        response.TasteBased[0].RecommendationType.Should().Be("TASTE_BASED");
        response.TasteBased[0].Reason.Should().Be("Hemos visto que te gusta hamburguesa");
    }

    [Fact]
    public async Task GetCustomerSectionsAsync_WithoutHistory_ShouldReturnEmptyHistorySections()
    {
        var (service, catalog) = CreateService();
        SeedRestaurantWithBranches(catalog);

        var response = await service.GetCustomerSectionsAsync(
            "customer-1",
            new RecommendationQueryRequest());

        response.Nearby.Should().NotBeEmpty();
        response.AlsoOrdered.Should().BeEmpty();
        response.TasteBased.Should().BeEmpty();
    }

    [Fact]
    public async Task GetCustomerSectionsAsync_ShouldNotDuplicateRestaurantWithinSection()
    {
        var (service, catalog, graph) = CreateServiceWithGraph();
        SeedRestaurant(catalog, "restaurant-1");
        graph.AlsoOrderedRestaurantIds.Add("restaurant-1");
        graph.AlsoOrderedRestaurantIds.Add("restaurant-1");

        var response = await service.GetCustomerSectionsAsync(
            "customer-1",
            new RecommendationQueryRequest());

        response.AlsoOrdered.Select(item => item.RestaurantId).Should().OnlyHaveUniqueItems();
    }

    [Fact]
    public async Task GetNearbyAsync_WhenRadiusExceedsMaximum_ShouldLimitToTwentyFiveKm()
    {
        var (service, catalog) = CreateService();
        SeedRestaurant(catalog, "restaurant-near", latitude: 19.4328, longitude: -99.1334);
        SeedRestaurant(catalog, "restaurant-mid", latitude: 19.60, longitude: -99.1334);
        SeedRestaurant(catalog, "restaurant-far", latitude: 19.90, longitude: -99.1334);

        var response = await service.GetNearbyAsync(new RecommendationQueryRequest
        {
            Lat = 19.4326,
            Lng = -99.1332,
            RadiusKm = 999
        });

        response.Select(item => item.RestaurantId).Should().Contain("restaurant-mid");
        response.Select(item => item.RestaurantId).Should().NotContain("restaurant-far");
    }

    private static (RecommendationServiceImpl Service, FakeCatalogClient Catalog) CreateService()
    {
        var (service, catalog, _) = CreateServiceWithGraph();
        return (service, catalog);
    }

    private static (RecommendationServiceImpl Service, FakeCatalogClient Catalog, FakeGraphRepository Graph) CreateServiceWithGraph()
    {
        var catalog = new FakeCatalogClient();
        var graph = new FakeGraphRepository();
        var distanceService = new HaversineDistanceService(Options.Create(new RecommendationSettings()));

        return (new RecommendationServiceImpl(catalog, graph, distanceService), catalog, graph);
    }

    private static void SeedRestaurantWithBranches(FakeCatalogClient catalog)
    {
        catalog.Restaurants.Add(Restaurant("restaurant-1"));
        catalog.BranchesByRestaurantId["restaurant-1"] =
        [
            Branch("branch-main", latitude: 19.50, longitude: -99.20, isMain: true),
            Branch("branch-near", latitude: 19.4328, longitude: -99.1334, isMain: false)
        ];
    }

    private static void SeedRestaurant(
        FakeCatalogClient catalog,
        string restaurantId,
        double latitude = 19.43,
        double longitude = -99.13)
    {
        catalog.Restaurants.Add(Restaurant(restaurantId));
        catalog.BranchesByRestaurantId[restaurantId] =
        [
            Branch($"branch-{restaurantId}", restaurantId, latitude, longitude, isMain: true)
        ];
    }

    private static CatalogRestaurantResponse Restaurant(string id)
    {
        return new CatalogRestaurantResponse
        {
            Id = id,
            Name = $"Restaurant {id}",
            Active = true,
            Open = true
        };
    }

    private static CatalogBranchResponse Branch(
        string id,
        string restaurantId = "restaurant-1",
        double latitude = 19.43,
        double longitude = -99.13,
        bool isMain = false,
        bool open = true)
    {
        return new CatalogBranchResponse
        {
            Id = id,
            RestaurantId = restaurantId,
            Name = $"Branch {id}",
            Latitude = latitude,
            Longitude = longitude,
            IsMainBranch = isMain,
            Active = true,
            Open = open
        };
    }
}
