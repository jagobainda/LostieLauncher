namespace EricLostieLauncher.Services;

public interface IDownloadService
{
    
}

public class DownloadService : IDownloadService
{

}
// PROMPT FOR COPILOT AGENT:
// ...
//Context:
//This is a WPF launcher built with MVVM architecture(.NET, C#).
//The launcher downloads game ZIP files from an HTTP server (static files served by Nginx).
//The server supports HTTP Range requests(partial content).

//Task:
//Create a DownloadService class responsible for downloading files with:
//- pause
//- resume
//- progress reporting
//-cancellation
//- basic retry handling(no Polly required)
//This service will be used by ViewModels.

//Architecture requirements:
//- MVVM friendly
//-No UI logic in the service
//-Service must be injectable(constructor injection)
//- Use HttpClient with a single shared/static instance
//- Use async/await properly
//-The service must never block the UI thread
//- Must never throw unhandled exceptions to the UI layer
//-If download fails, return a meaningful result object instead of throwing

//Functional requirements:
//- Method:
//  DownloadAsync(string url, string destinationPath, IProgress<double> progress, CancellationToken ct)
//- If a partial file already exists:
//  - Get its current size
//  -Send HTTP Range request: "bytes=<existingBytes>-"
//  - Append to the existing file
//- If server responds with:
//  - 206 Partial Content → resume download
//  -200 OK → delete partial file and start from scratch
//-Support Pause:
//  - Implement pause via CancellationToken(cancel current request but keep partial file)
//- Support Resume:
//  - Calling DownloadAsync again should resume from the partial file
//- Progress reporting:
//  - Report percentage[0–100]
//  - Use Content-Length + existing bytes to compute total size
//  - Handle unknown content length gracefully
//-Cancellation:
//  - Cancel should stop immediately without corrupting the file

//Robustness:
//- Validate destination directory exists(create if needed)
//- Write to a temporary file (e.g. .part) and rename to final file on success
//-If download completes successfully:
//  - atomically move temp file to final path
//- If the server does not support Range:
//  - fall back to full re - download
//- Handle network errors and IO exceptions gracefully
//-Add minimal logging hooks(ILogger interface or simple Action<string> logger)
//- Use reasonable buffer sizes(e.g. 64 KB)

//Optional but nice:
//- Basic retry: 1–2 retries for transient network errors
//- Ability to query current download state(Idle, Downloading, Paused, Completed, Failed)

//Deliverables:
//- DownloadService class
//- DownloadResult class (Success, Cancelled, Failed, ErrorMessage)
//- Example usage from a ViewModel:
//  - StartDownloadCommand
//  - PauseCommand
//  - ResumeCommand
//- Clear inline comments explaining key decisions

//Keep the implementation simple and pragmatic.
//Do not over-engineer.
//Avoid external dependencies beyond standard.NET libraries.
