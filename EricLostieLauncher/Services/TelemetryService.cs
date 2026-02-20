namespace EricLostieLauncher.Services;

public interface ITelemetryService
{
    
}

public class TelemetryService : ITelemetryService
{

}

// PROMPT FOR COPILOT AGENT:
// ...
//Context:
//This is a WPF launcher built with MVVM architecture(.NET, C#).
//The launcher downloads and updates fangames.
//Telemetry is a non-critical “nice to have” feature and must never block downloads or core flows.

//Task:
//Create a TelemetryService class (and any small supporting classes if needed) that sends anonymous hardware and usage telemetry to a backend API.

//Architecture requirements:
//- MVVM friendly
//-Telemetry logic must be isolated in a service(no UI logic)
//- Service should be injectable(constructor injection)
//- Must be easy to disable via config flag(e.g.EnableTelemetry = true / false)
//- Must never throw unhandled exceptions to the UI layer
//- Telemetry sending must be fire-and - forget(non - blocking)
//- If telemetry fails, silently ignore

//HTTP client requirements:
//- Use HttpClient with a single static/shared instance
//-Set short timeouts(e.g. 2–3 seconds)
//- Send JSON
//-Add required headers
//- Handle network failures gracefully

//Endpoint documentation:

//POST https://your-domain.com/api/telemetry

//Headers:
//- Content-Type: application/json
//- X-Launcher-Key: <API_KEY>

//Payload JSON:
//{
//  "gameId": "string (max 64)",
//  "gameVersion": "X.Y.Z",
//  "launcherVersion": "X.Y.Z",
//  "os": "Windows 10 | Windows 11",
//  "cpuName": "string",
//  "cpuCores": number,
//  "gpuName": "string",
//  "ramGb": number
//}

//Behavior rules:
//-Telemetry must be sent:
//  -when a game download starts
//- The service must gather hardware info:
//  -CPU name
//  - number of cores
//  -GPU name
//  - RAM in GB
//  - OS version normalized to "Windows 10" or "Windows 11"
//- Version strings must be semver-like (X.Y.Z)
//- gameId must come from launcher configuration (not hardcoded in the service)
//- The service should normalize values before sending
//- The service should not send telemetry more than once per X minutes for the same gameId (simple in-memory cooldown to avoid spam)

//Security / hygiene:
//-API key must be read from config (not hardcoded)
//- No IP handling on client side
//- No user-identifying data
//- Telemetry must be opt-in or opt-out configurable

//Deliverables:
//-TelemetryService class
//-Data model class for payload
//- Example usage from a ViewModel (e.g.OnDownloadStarted / OnDownloadFinished)
//- Comments explaining design decisions

//Keep the implementation simple and pragmatic.
//Do not over - engineer.
//Avoid external dependencies beyond standard.NET libraries.
