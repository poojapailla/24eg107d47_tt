<#
.SYNOPSIS
    Builds the Aiven CA truststore used for VERIFY_IDENTITY TLS to Aiven MySQL.

.DESCRIPTION
    Converts the bundled Aiven CA certificate (src/main/resources/tls/aiven-ca.pem)
    into a PKCS12 truststore at certs/aiven-truststore.p12 and prints the
    DB_TRUST_STORE_URL value to add to the .env file.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts/setup-aiven-tls.ps1
#>
[CmdletBinding()]
param(
    [string]$StorePassword = 'changeit'
)

$ErrorActionPreference = 'Stop'

$backendRoot = Split-Path -Parent $PSScriptRoot
$caPem = Join-Path $backendRoot 'src\main\resources\tls\aiven-ca.pem'
$certDir = Join-Path $backendRoot 'certs'
$trustStore = Join-Path $certDir 'aiven-truststore.p12'

if (-not (Test-Path -LiteralPath $caPem)) {
    throw "CA certificate not found: $caPem"
}

function Resolve-Keytool {
    $candidates = New-Object System.Collections.Generic.List[string]

    if ($env:JAVA_HOME) {
        $candidates.Add((Join-Path $env:JAVA_HOME 'bin\keytool.exe'))
    }

    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $javaExe = $javaCmd.Source
        if (-not $javaExe) { $javaExe = $javaCmd.Definition }
        # java.home reported by the JVM, e.g. C:\Program Files\Java\jdk-17.0.18
        if ($javaExe) {
            # The JVM prints its settings to stderr, so the error preference is relaxed here.
            $previousPreference = $ErrorActionPreference
            $ErrorActionPreference = 'Continue'
            try {
                $settings = (& $javaExe '-XshowSettings:properties' '-version' 2>&1 | Out-String)
            } finally {
                $ErrorActionPreference = $previousPreference
            }
            $match = [regex]::Match($settings, 'java\.home\s*=\s*(.+)')
            if ($match.Success) {
                $javaHome = $match.Groups[1].Value.Trim()
                $candidates.Add((Join-Path $javaHome 'bin\keytool.exe'))
            }
        }
    }

    foreach ($root in @("$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Microsoft")) {
        if (Test-Path -LiteralPath $root) {
            Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue |
                ForEach-Object { $candidates.Add((Join-Path $_.FullName 'bin\keytool.exe')) }
        }
    }

    $onPath = Get-Command keytool -ErrorAction SilentlyContinue
    if ($onPath) {
        $candidates.Add($onPath.Source)
    }

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) {
            return $candidate
        }
    }

    throw 'keytool not found. Install a JDK (17+) or set JAVA_HOME and run the script again.'
}

$keytool = Resolve-Keytool

New-Item -ItemType Directory -Force -Path $certDir | Out-Null

if (Test-Path -LiteralPath $trustStore) {
    Remove-Item -LiteralPath $trustStore -Force
}

# keytool writes its status message to stderr, so the error preference is relaxed here.
$previousPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    $keytoolOutput = (& $keytool -importcert -noprompt -alias 'aiven-ca' -file $caPem `
            -keystore $trustStore -storetype PKCS12 -storepass $StorePassword 2>&1)
    $keytoolExitCode = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $previousPreference
}
if ($keytoolExitCode -ne 0) {
    throw "keytool failed with exit code $keytoolExitCode`n$($keytoolOutput | Out-String)"
}

$resolvedTrustStore = (Resolve-Path -LiteralPath $trustStore).Path
$trustStoreUrl = (New-Object System.Uri($resolvedTrustStore)).AbsoluteUri

Write-Output ''
Write-Output 'Truststore created.'
Write-Output "  path : $trustStore"
Write-Output '  add the following lines to backend/.env to enable verified TLS:'
Write-Output ''
Write-Output "DB_TRUST_STORE_URL=$trustStoreUrl"
Write-Output "DB_TRUST_STORE_PASSWORD=$StorePassword"
Write-Output 'DB_TRUST_STORE_TYPE=PKCS12'
Write-Output ''
Write-Output '  and switch the JDBC url to sslMode=VERIFY_IDENTITY.'
