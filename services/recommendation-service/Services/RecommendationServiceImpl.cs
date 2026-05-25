using RecommendationService.Dtos.Requests;
using RecommendationService.Dtos.Responses;
using RecommendationService.Exceptions;
using RecommendationService.Integrations.Catalog;
using RecommendationService.Repositories;

namespace RecommendationService.Services;

public sealed class RecommendationServiceImpl : IRecommendationService
{
    public const string NoLocationWarning =
        "No pudimos acceder a tu ubicación. Seleccionamos una sucursal disponible automáticamente, pero puedes cambiarla antes de ordenar.";

    private const string NearReason = "Cerca de ti";
    private const string SimilarTagsReason = "Porque has pedido productos con etiquetas similares";
    private const string SimilarCustomersReason = "Personas con gustos parecidos también pidieron aquí";
    private const string AutomaticReason = "Sucursal disponible seleccionada automáticamente";
    private const string NearbyRecommendationType = "NEARBY";
    private const string AlsoOrderedRecommendationType = "ALSO_ORDERED";
    private const string TasteBasedRecommendationType = "TASTE_BASED";
    private const int MaxRecommendationsPerSection = 6;

    private readonly ICatalogClient _catalogClient;
    private readonly IRecommendationGraphRepository _graphRepository;
    private readonly ILocationDistanceService _distanceService;

    public RecommendationServiceImpl(
        ICatalogClient catalogClient,
        IRecommendationGraphRepository graphRepository,
        ILocationDistanceService distanceService)
    {
        _catalogClient = catalogClient;
        _graphRepository = graphRepository;
        _distanceService = distanceService;
    }

    public async Task<IReadOnlyList<BranchRecommendationResponse>> GetNearbyAsync(
        RecommendationQueryRequest query,
        CancellationToken cancellationToken = default)
    {
        var candidates = await LoadCandidatesAsync(cancellationToken);

        return BuildNearbyRecommendations(candidates, query);
    }

    public async Task<IReadOnlyList<BranchRecommendationResponse>> GetForCustomerAsync(
        string customerAccountId,
        RecommendationQueryRequest query,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(customerAccountId))
        {
            throw new RecommendationValidationException("Customer account id is required.");
        }

        var candidates = await LoadCandidatesAsync(cancellationToken);
        var preferredTags = await _graphRepository.GetPreferredTagsAsync(customerAccountId, cancellationToken);
        var tasteBased = await BuildTasteBasedRecommendationsAsync(
            candidates,
            preferredTags,
            query,
            excludedRestaurantIds: new HashSet<string>(StringComparer.OrdinalIgnoreCase),
            cancellationToken);

        return tasteBased.Count == 0 ? BuildNearbyRecommendations(candidates, query) : tasteBased;
    }

    public async Task<CustomerRecommendationSectionsResponse> GetCustomerSectionsAsync(
        string customerAccountId,
        RecommendationQueryRequest query,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(customerAccountId))
        {
            throw new RecommendationValidationException("Customer account id is required.");
        }

        var candidates = await LoadCandidatesAsync(cancellationToken);
        var nearby = BuildNearbyRecommendations(candidates, query)
            .Take(MaxRecommendationsPerSection)
            .ToList();

        var usedRestaurantIds = nearby
            .Select(recommendation => recommendation.RestaurantId)
            .ToHashSet(StringComparer.OrdinalIgnoreCase);

        var alsoOrdered = await BuildAlsoOrderedRecommendationsAsync(
            candidates,
            customerAccountId,
            query,
            usedRestaurantIds,
            cancellationToken);

        foreach (var recommendation in alsoOrdered)
        {
            usedRestaurantIds.Add(recommendation.RestaurantId);
        }

        var preferredTags = await _graphRepository.GetPreferredTagsAsync(customerAccountId, cancellationToken);
        var tasteBased = await BuildTasteBasedRecommendationsAsync(
            candidates,
            preferredTags,
            query,
            usedRestaurantIds,
            cancellationToken);

        return new CustomerRecommendationSectionsResponse
        {
            Nearby = nearby,
            AlsoOrdered = alsoOrdered,
            TasteBased = tasteBased
        };
    }

    public async Task<RecommendedBranchResponse> GetNearestBranchAsync(
        string restaurantId,
        RecommendationQueryRequest query,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(restaurantId))
        {
            throw new RecommendationValidationException("Restaurant id is required.");
        }

        var restaurant = await _catalogClient.GetRestaurantAsync(restaurantId, cancellationToken)
            ?? throw new RecommendationNotFoundException($"Restaurant '{restaurantId}' was not found.");

        var branches = await _catalogClient.GetBranchesByRestaurantAsync(restaurantId, cancellationToken);
        var branch = PickBranch(branches, query, out var distanceKm)
            ?? throw new RecommendationNotFoundException($"Restaurant '{restaurantId}' has no open branches.");

        var hasLocation = _distanceService.HasValidLocation(query.Lat, query.Lng);

        var response = ToResponse(
            restaurant,
            branch,
            distanceKm,
            hasLocation ? NearReason : AutomaticReason,
            distanceKm is null ? 1 : Math.Max(1, 100 - distanceKm.Value),
            hasLocation ? null : NoLocationWarning,
            NearbyRecommendationType);

        return new RecommendedBranchResponse
        {
            RestaurantId = response.RestaurantId,
            RestaurantName = response.RestaurantName,
            RestaurantImageUrl = response.RestaurantImageUrl,
            BranchId = response.BranchId,
            BranchName = response.BranchName,
            BranchAddress = response.BranchAddress,
            Latitude = response.Latitude,
            Longitude = response.Longitude,
            DistanceKm = response.DistanceKm,
            Reason = response.Reason,
            Score = response.Score,
            Warning = response.Warning
        };
    }

    private IReadOnlyList<BranchRecommendationResponse> BuildNearbyRecommendations(
        IReadOnlyList<RestaurantBranchCandidate> candidates,
        RecommendationQueryRequest query)
    {
        var hasLocation = _distanceService.HasValidLocation(query.Lat, query.Lng);
        var radiusKm = _distanceService.NormalizeRadiusKm(query.RadiusKm);
        var recommendations = new List<BranchRecommendationResponse>();

        foreach (var candidate in candidates)
        {
            var branch = PickBranch(candidate.Branches, query, out var distanceKm);
            if (branch is null)
            {
                continue;
            }

            if (hasLocation && distanceKm is not null && distanceKm > radiusKm)
            {
                continue;
            }

            var score = distanceKm is null ? 1 : Math.Max(1, 100 - distanceKm.Value);

            recommendations.Add(ToResponse(
                candidate.Restaurant,
                branch,
                distanceKm,
                hasLocation ? NearReason : AutomaticReason,
                score,
                hasLocation ? null : NoLocationWarning,
                NearbyRecommendationType));
        }

        return recommendations
            .OrderBy(recommendation => recommendation.DistanceKm ?? double.MaxValue)
            .ThenByDescending(recommendation => recommendation.Score)
            .DistinctBy(recommendation => recommendation.RestaurantId, StringComparer.OrdinalIgnoreCase)
            .ToList();
    }

    private async Task<IReadOnlyList<BranchRecommendationResponse>> BuildAlsoOrderedRecommendationsAsync(
        IReadOnlyList<RestaurantBranchCandidate> candidates,
        string customerAccountId,
        RecommendationQueryRequest query,
        ISet<string> excludedRestaurantIds,
        CancellationToken cancellationToken)
    {
        var recommendedRestaurantIds = await _graphRepository.GetAlsoOrderedRestaurantIdsAsync(
            customerAccountId,
            cancellationToken);
        var recommendedRestaurantSet = recommendedRestaurantIds.ToHashSet(StringComparer.OrdinalIgnoreCase);

        if (recommendedRestaurantSet.Count == 0)
        {
            return [];
        }

        var recommendations = BuildRankedRecommendations(
            candidates,
            recommendedRestaurantIds,
            recommendedRestaurantSet,
            query,
            excludedRestaurantIds,
            SimilarCustomersReason,
            AlsoOrderedRecommendationType);

        return recommendations.Count == 0 && excludedRestaurantIds.Count > 0
            ? BuildRankedRecommendations(
                candidates,
                recommendedRestaurantIds,
                recommendedRestaurantSet,
                query,
                new HashSet<string>(StringComparer.OrdinalIgnoreCase),
                SimilarCustomersReason,
                AlsoOrderedRecommendationType)
            : recommendations;
    }

    private async Task<IReadOnlyList<BranchRecommendationResponse>> BuildTasteBasedRecommendationsAsync(
        IReadOnlyList<RestaurantBranchCandidate> candidates,
        IReadOnlyList<string> preferredTags,
        RecommendationQueryRequest query,
        ISet<string> excludedRestaurantIds,
        CancellationToken cancellationToken)
    {
        if (preferredTags.Count == 0)
        {
            return [];
        }

        var recommendedRestaurantIds = await _graphRepository.GetRecommendedRestaurantIdsByTagsAsync(
            preferredTags,
            cancellationToken);
        var recommendedRestaurantSet = recommendedRestaurantIds.ToHashSet(StringComparer.OrdinalIgnoreCase);

        if (recommendedRestaurantSet.Count == 0)
        {
            return [];
        }

        var recommendations = BuildRankedRecommendations(
            candidates,
            recommendedRestaurantIds,
            recommendedRestaurantSet,
            query,
            excludedRestaurantIds,
            BuildTasteReason(preferredTags),
            TasteBasedRecommendationType);

        return recommendations.Count == 0 && excludedRestaurantIds.Count > 0
            ? BuildRankedRecommendations(
                candidates,
                recommendedRestaurantIds,
                recommendedRestaurantSet,
                query,
                new HashSet<string>(StringComparer.OrdinalIgnoreCase),
                BuildTasteReason(preferredTags),
                TasteBasedRecommendationType)
            : recommendations;
    }

    private IReadOnlyList<BranchRecommendationResponse> BuildRankedRecommendations(
        IReadOnlyList<RestaurantBranchCandidate> candidates,
        IReadOnlyList<string> rankedRestaurantIds,
        ISet<string> rankedRestaurantSet,
        RecommendationQueryRequest query,
        ISet<string> excludedRestaurantIds,
        string reason,
        string recommendationType)
    {
        var hasLocation = _distanceService.HasValidLocation(query.Lat, query.Lng);
        var radiusKm = _distanceService.NormalizeRadiusKm(query.RadiusKm);
        var recommendations = new List<BranchRecommendationResponse>();

        foreach (var candidate in candidates)
        {
            if (excludedRestaurantIds.Contains(candidate.Restaurant.Id)
                || !rankedRestaurantSet.Contains(candidate.Restaurant.Id))
            {
                continue;
            }

            var branch = PickBranch(candidate.Branches, query, out var distanceKm);
            if (branch is null)
            {
                continue;
            }

            if (hasLocation && distanceKm is not null && distanceKm > radiusKm)
            {
                continue;
            }

            var distanceScore = distanceKm is null ? 0 : Math.Max(0, radiusKm - distanceKm.Value);
            var ranking = rankedRestaurantIds
                .Select((id, index) => new { id, index })
                .FirstOrDefault(item => string.Equals(item.id, candidate.Restaurant.Id, StringComparison.OrdinalIgnoreCase))
                ?.index ?? rankedRestaurantIds.Count;

            recommendations.Add(ToResponse(
                candidate.Restaurant,
                branch,
                distanceKm,
                reason,
                (rankedRestaurantIds.Count - ranking) * 10 + distanceScore,
                warning: hasLocation ? null : NoLocationWarning,
                recommendationType));
        }

        return recommendations
            .OrderByDescending(recommendation => recommendation.Score)
            .ThenBy(recommendation => recommendation.DistanceKm ?? double.MaxValue)
            .DistinctBy(recommendation => recommendation.RestaurantId, StringComparer.OrdinalIgnoreCase)
            .Take(MaxRecommendationsPerSection)
            .ToList();
    }

    private async Task<IReadOnlyList<RestaurantBranchCandidate>> LoadCandidatesAsync(
        CancellationToken cancellationToken)
    {
        var restaurants = await _catalogClient.GetRestaurantsAsync(cancellationToken);
        var candidates = new List<RestaurantBranchCandidate>();

        foreach (var restaurant in restaurants.Where(IsVisibleRestaurant))
        {
            var branches = await _catalogClient.GetBranchesByRestaurantAsync(restaurant.Id, cancellationToken);
            var openBranches = branches.Where(IsOpenBranch).ToList();

            if (openBranches.Count > 0)
            {
                candidates.Add(new RestaurantBranchCandidate(restaurant, openBranches));
            }
        }

        return candidates;
    }

    private CatalogBranchResponse? PickBranch(
        IReadOnlyList<CatalogBranchResponse> branches,
        RecommendationQueryRequest query,
        out double? distanceKm)
    {
        distanceKm = null;
        var openBranches = branches.Where(IsOpenBranch).ToList();

        if (openBranches.Count == 0)
        {
            return null;
        }

        if (_distanceService.HasValidLocation(query.Lat, query.Lng))
        {
            var nearest = openBranches
                .Where(branch => branch.Latitude.HasValue && branch.Longitude.HasValue)
                .Select(branch => new
                {
                    Branch = branch,
                    DistanceKm = _distanceService.CalculateDistanceKm(
                        query.Lat!.Value,
                        query.Lng!.Value,
                        branch.Latitude!.Value,
                        branch.Longitude!.Value)
                })
                .OrderBy(item => item.DistanceKm)
                .FirstOrDefault();

            distanceKm = nearest?.DistanceKm;
            return nearest?.Branch;
        }

        return openBranches.FirstOrDefault(branch => branch.IsMainBranch == true)
            ?? openBranches.First();
    }

    private static BranchRecommendationResponse ToResponse(
        CatalogRestaurantResponse restaurant,
        CatalogBranchResponse branch,
        double? distanceKm,
        string reason,
        double score,
        string? warning,
        string recommendationType)
    {
        return new BranchRecommendationResponse
        {
            RestaurantId = restaurant.Id,
            RestaurantName = restaurant.Name,
            RestaurantImageUrl = restaurant.Logo?.Url,
            BranchId = branch.Id,
            BranchName = branch.Name,
            BranchAddress = branch.FormattedAddress,
            Latitude = branch.Latitude,
            Longitude = branch.Longitude,
            DistanceKm = distanceKm is null ? null : Math.Round(distanceKm.Value, 2, MidpointRounding.AwayFromZero),
            Reason = reason,
            Score = Math.Round(score, 2, MidpointRounding.AwayFromZero),
            Warning = warning,
            RecommendationType = recommendationType
        };
    }

    private static string BuildTasteReason(IReadOnlyList<string> preferredTags)
    {
        var preferredTag = preferredTags.FirstOrDefault(tag => !string.IsNullOrWhiteSpace(tag));

        return string.IsNullOrWhiteSpace(preferredTag)
            ? SimilarTagsReason
            : $"Hemos visto que te gusta {preferredTag}";
    }

    private static bool IsVisibleRestaurant(CatalogRestaurantResponse restaurant)
    {
        return !string.IsNullOrWhiteSpace(restaurant.Id) && restaurant.Active != false;
    }

    private static bool IsOpenBranch(CatalogBranchResponse branch)
    {
        return !string.IsNullOrWhiteSpace(branch.Id)
            && branch.Active != false
            && branch.Open != false;
    }

    private sealed record RestaurantBranchCandidate(
        CatalogRestaurantResponse Restaurant,
        IReadOnlyList<CatalogBranchResponse> Branches);
}
