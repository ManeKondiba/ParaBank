param(
    [Parameter(Mandatory = $true)][ValidateRange(1, 100000)][int]$ExpectedCount,
    [int]$AllowedDefects = 0,
    [string]$ReportPath = 'target/surefire-reports/testng-results.xml'
)
$ErrorActionPreference = 'Stop'
if (!(Test-Path -LiteralPath $ReportPath -PathType Leaf)) { throw "Missing test report: $ReportPath" }
[xml]$report = [IO.File]::ReadAllText((Resolve-Path -LiteralPath $ReportPath).Path)
$cases = @($report.SelectNodes("//test-method[not(@is-config='true')]"))
$passed = @($cases | Where-Object { $_.status -eq 'PASS' }).Count
$failed = @($cases | Where-Object { $_.status -eq 'FAIL' }).Count
$skipped = @($cases | Where-Object { $_.status -eq 'SKIP' }).Count
Write-Host "Scenario gate: expected $ExpectedCount; executed $($cases.Count); passed $passed; defects/failures $failed (allowed: $AllowedDefects); skipped $skipped."
if ($cases.Count -ne $ExpectedCount) { throw "Executed scenario count ($($cases.Count)) does not match the expected suite inventory ($ExpectedCount)." }
if ($skipped -ne 0) { throw "The suite had $skipped skipped test cases." }
if ($failed -gt $AllowedDefects) { throw "Defect count ($failed) exceeded allowed application defects ($AllowedDefects)." }
if ($passed + $failed -ne $cases.Count) { throw "Sum of passed ($passed) and defects ($failed) does not equal total executed ($($cases.Count))." }
Write-Host "Scenario gate PASSED."
