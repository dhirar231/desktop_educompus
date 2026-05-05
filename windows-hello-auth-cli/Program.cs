using Windows.Security.Credentials.UI;

const string PromptMessage = "Authenticate to login";

if (args.Any(arg => string.Equals(arg, "--setup", StringComparison.OrdinalIgnoreCase)))
{
    try
    {
        using var process = System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
        {
            FileName = "ms-settings:signinoptions",
            UseShellExecute = true
        });
        Console.WriteLine("Windows Hello settings opened.");
        return 0;
    }
    catch (Exception ex)
    {
        Console.Error.WriteLine($"Windows Hello setup error: {ex.Message}");
        return 1;
    }
}

try
{
    var availability = await UserConsentVerifier.CheckAvailabilityAsync();
    if (args.Any(arg => string.Equals(arg, "--check", StringComparison.OrdinalIgnoreCase)))
    {
        if (availability == UserConsentVerifierAvailability.Available)
        {
            Console.WriteLine("Windows Hello available.");
            return 0;
        }

        Console.Error.WriteLine($"Windows Hello unavailable: {availability}");
        return 1;
    }

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
