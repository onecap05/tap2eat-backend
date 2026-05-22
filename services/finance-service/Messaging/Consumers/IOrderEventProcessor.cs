namespace FinanceService.Messaging.Consumers;

public interface IOrderEventProcessor
{
    Task ProcessAsync(string rawMessage, CancellationToken cancellationToken = default);
}
