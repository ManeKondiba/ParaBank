param(
    [string]$TimingCsv = '',
    [double]$BottleneckThresholdSeconds = 3.0
)

$ErrorActionPreference = 'Stop'
$workspace = Split-Path -Parent $PSScriptRoot

if (-not $TimingCsv) {
    $latest = Get-ChildItem -Path (Join-Path $workspace 'target/reports/execution-timing-*.csv') -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1

    if (-not $latest) {
        Write-Warning "No execution timing CSV found in target/reports/. Run a test suite with ExecutionTimingListener enabled first."
        exit 0
    }
    $TimingCsv = $latest.FullName
}

Write-Host "Analyzing timing report: $TimingCsv" -ForegroundColor Cyan

$data = Import-Csv -LiteralPath $TimingCsv | ForEach-Object {
    [PSCustomObject]@{
        Context  = $_.context
        Method   = $_.method
        Kind     = $_.kind
        Status   = [int]$_.status
        Duration = [double]$_.duration_seconds
    }
}

if ($data.Count -eq 0) {
    Write-Warning "Timing report contains no entries."
    exit 0
}

$sorted = $data | Sort-Object Duration -Descending
$durations = $data | Select-Object -ExpandProperty Duration | Sort-Object

$totalCount = $data.Count
$totalSeconds = ($durations | Measure-Object -Sum).Sum
$average = [math]::Round(($durations | Measure-Object -Average).Average, 3)
$min = [math]::Round($durations[0], 3)
$max = [math]::Round($durations[-1], 3)

function Get-Percentile([array]$arr, [double]$pct) {
    $idx = [math]::Floor(($arr.Count - 1) * ($pct / 100.0))
    return [math]::Round($arr[$idx], 3)
}

$p50 = Get-Percentile $durations 50
$p90 = Get-Percentile $durations 90
$p95 = Get-Percentile $durations 95

Write-Host "`n================ PERFORMANCE SUMMARY ================" -ForegroundColor Green
Write-Host "Total Executions: $totalCount"
Write-Host "Total Duration:   $([math]::Round($totalSeconds, 2)) seconds"
Write-Host "Min Latency:      $min s"
Write-Host "Average Latency:  $average s"
Write-Host "p50 (Median):     $p50 s"
Write-Host "p90:              $p90 s"
Write-Host "p95:              $p95 s"
Write-Host "Max Latency:      $max s"
Write-Host "====================================================="

Write-Host "`nTop 10 Slowest Executions:" -ForegroundColor Yellow
$top10 = $sorted | Select-Object -First 10
$top10 | Format-Table -Property Method, Kind, Duration, Context -AutoSize

$bottlenecks = $sorted | Where-Object { $_.Duration -ge $BottleneckThresholdSeconds }
if ($bottlenecks.Count -gt 0) {
    Write-Host "`n[ALERT] Detected $($bottlenecks.Count) executions exceeding bottleneck threshold (${BottleneckThresholdSeconds}s):" -ForegroundColor Red
    $bottlenecks | Format-Table -Property Method, Kind, Duration -AutoSize
} else {
    Write-Host "`n[OK] No executions exceeded the bottleneck threshold (${BottleneckThresholdSeconds}s)." -ForegroundColor Green
}
