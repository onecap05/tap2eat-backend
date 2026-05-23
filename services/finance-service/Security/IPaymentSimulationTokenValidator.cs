namespace FinanceService.Security;

public interface IPaymentSimulationTokenValidator
{
    bool IsValid(string? token);
}
