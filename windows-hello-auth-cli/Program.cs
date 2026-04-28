using Windows.Security.Credentials.UI;

const string PromptMessage = "Authenticate to login";

try
{
    var availability = await UserConsentVerifier.CheckAvailabilityAsync();
    if (availability != UserConsentVerifierAvailability.Available)
    {
        Console.Error.WriteLine($"Windows Hello unavailable: {availability}");
        return 1;
    }

    var verification = await UserConsentVerifier.RequestVerificationAsync(PromptMessage);
    if (verification == UserConsentVerificationResult.Verified)
    {
        Console.WriteLine("Authentication success.");
        return 0;
    }

    Console.Error.WriteLine($"Authentication failed: {verification}");
    return 1;
}
catch (Exception ex)
{
    Console.Error.WriteLine($"Windows Hello error: {ex.Message}");
    return 1;
}
