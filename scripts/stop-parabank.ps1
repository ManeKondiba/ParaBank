$ErrorActionPreference = 'Stop'
$workspacePath = Split-Path -Parent $PSScriptRoot
$statePath = Join-Path $workspacePath 'target/api-environment/server.json'
if (!(Test-Path -LiteralPath $statePath)) { Write-Host 'No managed local ParaBank instance found.'; return }
$state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
$serverProcess = Get-Process -Id $state.processId -ErrorAction SilentlyContinue
if (!$serverProcess -or $serverProcess.StartTime.ToUniversalTime().Ticks -ne $state.processStartTicks) {
    Write-Host 'The recorded ParaBank process is no longer running.'
    Remove-Item -LiteralPath $statePath
    return
}
# Ask Tomcat to stop gracefully so HSQLDB closes its database files.
$client = [System.Net.Sockets.TcpClient]::new()
try {
    $client.Connect('127.0.0.1', [int]$state.shutdownPort)
    $bytes = [System.Text.Encoding]::ASCII.GetBytes('PARABANK_LOCAL_SHUTDOWN')
    $client.GetStream().Write($bytes, 0, $bytes.Length)
} finally { $client.Dispose() }
if (!$serverProcess.WaitForExit(30000)) { throw 'ParaBank has not stopped after 30 seconds. Inspect target/api-environment/server.stderr.log before stopping it manually.' }
Remove-Item -LiteralPath $statePath
Write-Host 'Local ParaBank stopped. Its database remains in target/api-environment.'
