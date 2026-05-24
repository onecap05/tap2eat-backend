using RecommendationService.Repositories;

namespace RecommendationService.Tests.Fakes;

public sealed class FakeGraphRepository : IRecommendationGraphRepository
{
    public List<DeliveredOrderGraphUpdate> SavedUpdates { get; } = [];

    public List<string> PreferredTags { get; } = [];

    public List<string> RecommendedRestaurantIds { get; } = [];

    public Task InitializeAsync(CancellationToken cancellationToken = default)
    {
        return Task.CompletedTask;
    }

    public Task UpsertDeliveredOrderAsync(
        DeliveredOrderGraphUpdate update,
        CancellationToken cancellationToken = default)
    {
        SavedUpdates.Add(update);

        return Task.CompletedTask;
    }

    public Task<IReadOnlyList<string>> GetPreferredTagsAsync(
        string customerAccountId,
        CancellationToken cancellationToken = default)
    {
        return Task.FromResult<IReadOnlyList<string>>(PreferredTags);
    }

    public Task<IReadOnlyList<string>> GetRecommendedRestaurantIdsByTagsAsync(
        IReadOnlyList<string> tagNames,
        CancellationToken cancellationToken = default)
    {
        return Task.FromResult<IReadOnlyList<string>>(RecommendedRestaurantIds);
    }
}
