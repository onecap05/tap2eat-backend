namespace OrderService.Config;

public sealed class JwtSettings
{
    public const string SectionName = "Jwt";

    public string PublicKeyPath { get; set; } = "../api-gateway/src/main/resources/keys/public_key.pem";
}
