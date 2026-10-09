param(
    [ValidateRange(2,10)][int]$Runs = 3,
    [ValidateRange(1,8)][int]$Workers = 2
)
$ErrorActionPreference = 'Stop'
$workspace = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $workspace
$previousUrl = $env:PARABANK_APP_URL
$previousHeadless = $env:PARABANK_HEADLESS
$previousMode = $env:PARABANK_UI_FIXTURE_MODE
$failedRuns = 0
$summary = @()
$batch = Join-Path $workspace ('target/benchmarks/' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
try {
    $env:PARABANK_APP_URL = 'http://127.0.0.1:8081/parabank/index.htm'
    $env:PARABANK_HEADLESS = 'true'
    $env:PARABANK_UI_FIXTURE_MODE = 'ui'
    New-Item -ItemType Directory -Path $batch -Force | Out-Null
    for ($run = 1; $run -le $Runs; $run++) {
        $folder = Join-Path $batch "run-$run"
        New-Item -ItemType Directory -Path $folder -Force | Out-Null
        $runStarted = [DateTime]::UtcNow
        $elapsed = [Diagnostics.Stopwatch]::StartNew()
        try {
            & ./scripts/start-parabank.ps1
            & ./mvnw.cmd -B -ntp -Pui '-DexcludedGroups=Unit' "-Dui.threads=$Workers" verify
            $code = $LASTEXITCODE
            if ($code -ne 0) { $failedRuns++ }
            $elapsed.Stop()
            $reportFile = Get-Item -LiteralPath 'target/surefire-reports/testng-results.xml'
            if ($reportFile.LastWriteTimeUtc -lt $runStarted) { throw 'No fresh report was generated for this benchmark run.' }
            [xml]$report = Get-Content -LiteralPath $reportFile.FullName -Raw
            $cases = @($report.SelectNodes("//test-method[not(@is-config='true')]"))
            $summary += [pscustomobject]@{
                Run = $run; Workers = $Workers; ExitCode = $code; StartupAndBuildSeconds = [math]::Round($elapsed.Elapsed.TotalSeconds,3)
                Cases = $cases.Count; Passed = @($cases | Where-Object status -eq 'PASS').Count
                Failed = @($cases | Where-Object status -eq 'FAIL').Count; Skipped = @($cases | Where-Object status -eq 'SKIP').Count
            }
            Copy-Item -LiteralPath 'target/surefire-reports/testng-results.xml' -Destination $folder
            Get-ChildItem -Path 'target/reports/execution-timing-*' | Where-Object { $_.LastWriteTimeUtc -ge $runStarted } | Copy-Item -Destination $folder
        } finally {
            & ./scripts/stop-parabank.ps1
        }
    }
    $summary | Export-Csv -LiteralPath (Join-Path $batch 'summary.csv') -NoTypeInformation
    $summary | Format-Table -AutoSize
    Write-Host "Reports saved to $batch. Compare matching case inventories and outcomes; startup/build time is reported separately from TestNG timing."
    if ($failedRuns -gt 0) { throw "$failedRuns benchmark run(s) failed validation; performance results do not constitute a passing suite." }
} finally {
    $env:PARABANK_APP_URL = $previousUrl
    $env:PARABANK_HEADLESS = $previousHeadless
    $env:PARABANK_UI_FIXTURE_MODE = $previousMode
    Pop-Location
}