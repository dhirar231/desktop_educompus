$tcp = Get-NetTCPConnection -LocalPort 8000 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($tcp -ne $null) { 
    $ownerPid = $tcp.OwningProcess
    Write-Host "Found listener PID: $ownerPid"
    if ($ownerPid -ne $PID) {
        try {
            Stop-Process -Id $ownerPid -Force -ErrorAction Stop
            Write-Host "Stopped process $ownerPid"
        } catch {
            Write-Host "Could not stop process: $_"
        }
    } else {
        Write-Host "Listener is current process, skipping stop."
    }
}

Write-Host "Building project..."
.\mvnw.cmd -DskipTests package
if ($LASTEXITCODE -ne 0) { Write-Host "Build failed with exit code $LASTEXITCODE"; exit $LASTEXITCODE }

$logOut = "server.log"; $logErr = "server.err"
if (Test-Path $logOut) { Remove-Item $logOut -Force }
if (Test-Path $logErr) { Remove-Item $logErr -Force }

Write-Host "Starting app (mvn spring-boot:run), logs -> $logOut / $logErr"
$startArgs = "/c .\mvnw.cmd -DskipTests spring-boot:run"
$procInfo = Start-Process -FilePath "cmd.exe" -ArgumentList $startArgs -RedirectStandardOutput $logOut -RedirectStandardError $logErr -WindowStyle Hidden -PassThru
Write-Host "Started process Id:" $procInfo.Id

# Wait for server to start (polling)
$started = $false
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 2
    if (Test-Path $logOut) {
        $log = Get-Content $logOut
        if ($log -match "Started .* in .* seconds") {
            $started = $true
            break
        }
    }
    Write-Host "Waiting for server... ($($i*2)s)"
}

if ($started) {
    $url = 'http://127.0.0.1:8000/exam/take/180'
    Write-Host "Requesting $url"
    try {
        $resp = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 15 -ErrorAction Stop
        Write-Host "HTTP Status: $($resp.StatusCode)"
        $content = $resp.Content
        Write-Host "--- RESPONSE START ---"
        Write-Host $content.Substring(0,[Math]::Min(2000,$content.Length))
        Write-Host "--- RESPONSE END ---"
    } catch {
        Write-Host "Request failed: $_"
    }
} else {
    Write-Host "Server failed to start within 60 seconds."
}

Write-Host "`n--- server.err (tail) ---"
if (Test-Path $logErr) { Get-Content $logErr -Tail 50 } else { Write-Host "<no server.err>" }
Write-Host "`n--- server.log (tail) ---"
if (Test-Path $logOut) { Get-Content $logOut -Tail 50 } else { Write-Host "<no server.log>" }
