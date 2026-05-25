namespace RecommendationService.Repositories;

public interface IRecommendationGraphRepository
{
    Task InitializeAsync(CancellationToken cancellationToken = default);

    Task UpsertDeliveredOrderAsync(
        DeliveredOrderGraphUpdate update,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<string>> GetPreferredTagsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<string>> GetRecommendedRestaurantIdsByTagsAsync(
        IReadOnlyList<string> tagNames,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<string>> GetAlsoOrderedRestaurantIdsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default);
}
