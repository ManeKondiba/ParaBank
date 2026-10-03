param(
    [ValidateRange(1024, 65535)][int]$Port = 8081,
    [ValidateRange(1024, 65535)][int]$DatabasePort = 9001,
    [ValidateRange(1024, 65535)][int]$ShutdownPort = 8006
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$workspacePath = Split-Path -Parent $PSScriptRoot
$environmentPath = Join-Path $workspacePath 'target/api-environment'
$sourceRevision = '98c1c9ab4889eb92c7798da63bb417e27261d3d5'
$sourcePath = Join-Path $environmentPath "parabank-$sourceRevision"
$jdkPath = Join-Path $environmentPath 'jdk-21.0.12.1+1'
$tomcatPath = Join-Path $environmentPath 'apache-tomcat-11.0.26'
$statePath = Join-Path $environmentPath 'server.json'
$databaseMarker = Join-Path $environmentPath 'database-initialized.txt'
$apiUrl = "http://127.0.0.1:$Port/parabank/services/bank"
New-Item -ItemType Directory -Path $environmentPath -Force | Out-Null

if (Test-Path -LiteralPath $statePath) {
    $state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    $existing = Get-Process -Id $state.processId -ErrorAction SilentlyContinue
    if ($existing -and $existing.StartTime.ToUniversalTime().Ticks -eq $state.processStartTicks) {
        throw "Managed ParaBank is already running at $($state.apiUrl). Stop it before starting a fresh test environment."
    }
    Remove-Item -LiteralPath $statePath
}

function Get-VerifiedArchive([string]$Url, [string]$FileName, [string]$ExpectedHash, [string]$Algorithm = 'SHA256') {
    $archivePath = Join-Path $environmentPath $FileName
    if (!(Test-Path -LiteralPath $archivePath)) {
        Write-Host "Downloading $FileName..."
        Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $archivePath
    }
    if ((Get-FileHash -LiteralPath $archivePath -Algorithm $Algorithm).Hash -ne $ExpectedHash) {
        throw "Checksum mismatch for $archivePath. Remove that archive and retry."
    }
    return $archivePath
}

$sourceArchive = Get-VerifiedArchive "https://codeload.github.com/parasoft/parabank/zip/$sourceRevision" 'parabank-source.zip' '5f994a976e057232a58caaa53e49c68b885b7724a2a03fb4636cb87678e519c2'
$jdkArchive = Get-VerifiedArchive 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_x64_windows_hotspot_21.0.12.1_1.zip' 'jdk.zip' 'f9d6e191ab098c0d416e7d588a24420a8621cd2f4720dab2459b8b7b2d2d8b4e'
$tomcatArchive = Get-VerifiedArchive 'https://repo.maven.apache.org/maven2/org/apache/tomcat/tomcat/11.0.26/tomcat-11.0.26.zip' 'tomcat.zip' 'd5994ebbc654fb401c1dfc8521000fee8deea811c5a9bbefca4178a26ec87c9024757274c80120c607148b6133b7427d35500c14e021d33aec7b8ed79d66105a' 'SHA512'
if (!(Test-Path -LiteralPath $sourcePath)) { Expand-Archive -LiteralPath $sourceArchive -DestinationPath $environmentPath }
if (!(Test-Path -LiteralPath $jdkPath)) { Expand-Archive -LiteralPath $jdkArchive -DestinationPath $environmentPath }
if (!(Test-Path -LiteralPath $tomcatPath)) { Expand-Archive -LiteralPath $tomcatArchive -DestinationPath $environmentPath }

$warPath = Join-Path $sourcePath 'target/parabank-6.0.0-SNAPSHOT.war'
if (!(Test-Path -LiteralPath $warPath)) {
    $settingsPath = Join-Path $environmentPath 'maven-settings.xml'
    '<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"><mirrors><mirror><id>central-only</id><url>https://repo.maven.apache.org/maven2</url><mirrorOf>*</mirrorOf></mirror></mirrors></settings>' | Set-Content -LiteralPath $settingsPath -Encoding UTF8
    $previousJavaHome = $env:JAVA_HOME
    $previousMavenUserHome = $env:MAVEN_USER_HOME
    try {
        $env:JAVA_HOME = $jdkPath
        $env:MAVEN_USER_HOME = Join-Path $workspacePath '.maven-cache'
        Write-Host 'Building pinned ParaBank source. First build downloads Maven dependencies...'
        & (Join-Path $workspacePath 'mvnw.cmd') -B -ntp -s $settingsPath -f (Join-Path $sourcePath 'pom.xml') '-Dmaven.test.skip=true' "-Dmaven.repo.local=$(Join-Path $workspacePath '.maven-cache/repository')" package *> (Join-Path $environmentPath 'build.log')
        if ($LASTEXITCODE -ne 0) { throw "ParaBank build failed. See $environmentPath/build.log" }
    } finally {
        $env:JAVA_HOME = $previousJavaHome
        $env:MAVEN_USER_HOME = $previousMavenUserHome
    }
}

$applicationPath = Join-Path $tomcatPath 'webapps/parabank'
if (!(Test-Path -LiteralPath $applicationPath)) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::ExtractToDirectory($warPath, $applicationPath)
}
# Keep the intentionally vulnerable demo and its database on loopback.
$databaseConfigPath = Join-Path $applicationPath 'WEB-INF/classes/applicationContext-hsqldb.xml'
[xml]$databaseConfig = Get-Content -LiteralPath $databaseConfigPath -Raw
$databaseProperties = $databaseConfig.SelectSingleNode("//*[local-name()='bean' and @id='hsqldb']/*[local-name()='property' and @name='serverProperties']/*[local-name()='props']")
foreach ($entry in @{ 'server.address' = '127.0.0.1'; 'server.port' = "$DatabasePort" }.GetEnumerator()) {
    $property = $databaseProperties.SelectSingleNode("*[local-name()='prop' and @key='$($entry.Key)']")
    if (!$property) {
        $property = $databaseConfig.CreateElement('prop', $databaseProperties.NamespaceURI)
        $property.SetAttribute('key', $entry.Key)
        $null = $databaseProperties.AppendChild($property)
    }
    $property.InnerText = $entry.Value
}
$databaseConfig.Save($databaseConfigPath)
foreach ($propertiesFile in @('jdbc.properties', 'jdbcBookstore.properties')) {
    $propertiesPath = Join-Path $applicationPath "WEB-INF/classes/$propertiesFile"
    $content = (Get-Content -LiteralPath $propertiesPath -Raw) -replace 'hsql://[^/]+/', "hsql://127.0.0.1:$DatabasePort/"
    [System.IO.File]::WriteAllText($propertiesPath, $content)
}
# Exclude default manager/demo applications; do not record credential-bearing request URLs.
$serverXml = @"
<?xml version="1.0" encoding="UTF-8"?>
<Server port="$ShutdownPort" address="127.0.0.1" shutdown="PARABANK_LOCAL_SHUTDOWN">
  <Service name="Catalina">
    <Connector address="127.0.0.1" port="$Port" protocol="HTTP/1.1" connectionTimeout="20000" />
    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="api-apps" unpackWARs="false" autoDeploy="false">
        <Context path="/parabank" docBase="$($applicationPath.Replace('\', '/'))" reloadable="false" />
      </Host>
    </Engine>
  </Service>
</Server>
"@
[System.IO.File]::WriteAllText((Join-Path $tomcatPath 'conf/server.xml'), $serverXml)
New-Item -ItemType Directory -Path (Join-Path $tomcatPath 'api-apps') -Force | Out-Null
foreach ($localPort in @($Port, $DatabasePort, $ShutdownPort)) {
    $portProbe = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $localPort)
    try { $portProbe.Start() } catch { throw "Local port $localPort is already in use. Choose another port or stop the existing process." } finally { $portProbe.Stop() }
}
$arguments = @(
    '-Xms128m', '-Xmx768m', '-Duser.timezone=UTC', '-Duser.language=en', '-Duser.country=US',
    "`"-Dcatalina.home=$tomcatPath`"", "`"-Dcatalina.base=$tomcatPath`"",
    "`"-Djava.io.tmpdir=$(Join-Path $tomcatPath 'temp')`"",
    '-cp', "`"$(Join-Path $tomcatPath 'bin/bootstrap.jar');$(Join-Path $tomcatPath 'bin/tomcat-juli.jar')`"",
    'org.apache.catalina.startup.Bootstrap', 'start'
)
$serverProcess = Start-Process -FilePath (Join-Path $jdkPath 'bin/java.exe') -ArgumentList $arguments -WorkingDirectory $environmentPath -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $environmentPath 'server.stdout.log') -RedirectStandardError (Join-Path $environmentPath 'server.stderr.log')
$state = [ordered]@{
    processId = $serverProcess.Id
    processStartTicks = $serverProcess.StartTime.ToUniversalTime().Ticks
    apiUrl = $apiUrl
    sourceRevision = $sourceRevision
    version = '6.0.0-SNAPSHOT'
    javaVersion = 'Temurin 21.0.12.1+1'
    tomcatVersion = '11.0.26'
    shutdownPort = $ShutdownPort
    databasePort = $DatabasePort
    timezone = 'UTC'
    initialBalance = '1000.00'
    minimumBalance = '100.00'
    loanProvider = 'local'
    loanProcessor = 'down'
    loanProcessorThreshold = '20'
}
$state | ConvertTo-Json | Set-Content -LiteralPath $statePath -Encoding UTF8
$ready = $false
for ($attempt = 0; $attempt -lt 90; $attempt++) {
    if ($serverProcess.HasExited) { throw "ParaBank process exited. See $environmentPath/server.stderr.log" }
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri "$apiUrl/openapi.json" -TimeoutSec 3
        if ($response.StatusCode -eq 200) { $ready = $true; break }
    } catch { }
    Start-Sleep -Seconds 2
}
if (!$ready) { throw "ParaBank did not become ready. See $environmentPath/server.stderr.log; stop it using scripts/stop-parabank.ps1." }
# This script owns the loopback-only database. Reset it on every fresh start so local
# runs have the same baseline as ephemeral CI environments.
$null = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$apiUrl/initializeDB" -TimeoutSec 30
foreach ($name in @('initialBalance', 'minimumBalance', 'loanProvider', 'loanProcessor', 'loanProcessorThreshold')) {
    $null = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$apiUrl/setParameter/$name/$($state[$name])" -TimeoutSec 15
}
Set-Content -LiteralPath $databaseMarker -Value "Pinned local database reset; source $sourceRevision" -Encoding UTF8
Write-Host "ParaBank running at $apiUrl (PID $($serverProcess.Id), UTC)."
Write-Host 'Initial balance: 1000.00; account funding: 100.00; local loan provider with 20% down-payment threshold.'
Write-Host 'Stop with: powershell -ExecutionPolicy Bypass -File scripts/stop-parabank.ps1'
