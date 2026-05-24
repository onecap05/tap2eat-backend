using FluentAssertions;
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
    }
}
