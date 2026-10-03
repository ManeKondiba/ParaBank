$ErrorActionPreference = 'Stop'
$workspacePath = Split-Path -Parent $PSScriptRoot
$sourceDirectory = Join-Path $workspacePath 'target/api-environment'
$destinationDirectory = Join-Path $workspacePath 'target/api-evidence/application-logs'
$logNames = @('server.stdout.log', 'server.stderr.log')

foreach ($logName in $logNames) {
    $sourcePath = Join-Path $sourceDirectory $logName
    if (!(Test-Path -LiteralPath $sourcePath -PathType Leaf)) { continue }

    $content = [System.IO.File]::ReadAllText($sourcePath)
    if ($content.Length -gt 1048576) {
        $content = $content.Substring($content.Length - 1048576)
    }
    $content = [regex]::Replace($content, '(?i)(/login/)[^/\s?]+/[^/\s?]+', '$1{username}/{password}')
    $secretPattern = '(?i)\b(password|passwd|ssn|token|authorization|cookie)\b(\s*[:=]\s*)(?:"[^"]*"|''[^'']*''|[^\s,;&]+)'
    $content = [regex]::Replace($content, $secretPattern, '$1$2[redacted]')
    $content = [regex]::Replace($content, '(?i)(https?://)[^/@\s]+@', '$1[redacted]@')
    $content = [regex]::Replace($content, '(?i)(https?://[^/\s?]+)[^\s]*\?[^\s]*', '$1?[redacted]')
    $content = [regex]::Replace($content, '(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b', '[email redacted]')

    New-Item -ItemType Directory -Path $destinationDirectory -Force | Out-Null
    $destinationPath = Join-Path $destinationDirectory $logName
    [System.IO.File]::WriteAllText($destinationPath, $content, [System.Text.UTF8Encoding]::new($false))
}