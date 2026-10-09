param(
    [Parameter(Mandatory = $true)][ValidateRange(1, 100000)][int]$ExpectedCount,
    [string]$ReportPath = 'target/surefire-reports/testng-results.xml'
)
$ErrorActionPreference = 'Stop'
if (!(Test-Path -LiteralPath $ReportPath -PathType Leaf)) { throw "Missing test report: $ReportPath" }
[xml]$report = [IO.File]::ReadAllText((Resolve-Path -LiteralPath $ReportPath).Path)
$cases = @($report.SelectNodes("//test-method[not(@is-config='true')]"))
$passed = @($cases | Where-Object { $_.status -eq 'PASS' }).Count
$failed = @($cases | Where-Object { $_.status -eq 'FAIL' }).Count
$skipped = @($cases | Where-Object { $_.status -eq 'SKIP' }).Count
Write-Host "Scenario gate: expected $ExpectedCount; executed $($cases.Count); passed $passed; failed $failed; skipped $skipped."
if ($cases.Count -ne $ExpectedCount) { throw 'Executed scenario count does not match the reviewed suite inventory.' }
if ($passed -ne $ExpectedCount -or $failed -ne 0 -or $skipped -ne 0) { throw 'The suite did not pass every expected scenario without skips.' }