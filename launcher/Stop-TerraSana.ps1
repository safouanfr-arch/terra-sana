[CmdletBinding()]
param(
    [switch]$NoMessage
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$runtimeDirectory = Join-Path $projectRoot '.terra-sana-runtime'
$stateFile = Join-Path $runtimeDirectory 'processes.json'

function Show-LauncherMessage {
    param(
        [string]$Message,
        [int]$Icon = 64
    )

    try {
        $shell = New-Object -ComObject WScript.Shell
        [void]$shell.Popup($Message, 0, 'Terra Sana', $Icon)
    } catch {
        Write-Output $Message
    }
}

function Stop-TrackedProcess {
    param(
        [object]$ProcessId,
        [string]$ExpectedCommand
    )

    if ($null -eq $ProcessId) {
        return $false
    }

    $numericId = 0
    if (-not [int]::TryParse($ProcessId.ToString(), [ref]$numericId)) {
        return $false
    }

    $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $numericId" -ErrorAction SilentlyContinue
    if ($null -eq $processInfo -or $processInfo.CommandLine -notlike "*$ExpectedCommand*") {
        return $false
    }

    Start-Process -FilePath (Join-Path $env:SystemRoot 'System32\taskkill.exe') `
        -ArgumentList @('/PID', $numericId.ToString(), '/T', '/F') `
        -WindowStyle Hidden `
        -Wait | Out-Null
    return $true
}

if (-not (Test-Path $stateFile)) {
    if (-not $NoMessage) {
        Show-LauncherMessage "Aucun service Terra Sana lance par le raccourci n'a ete trouve."
    }
    exit 0
}

try {
    $state = Get-Content -Path $stateFile -Raw -Encoding UTF8 | ConvertFrom-Json
    $frontendStopped = Stop-TrackedProcess -ProcessId $state.FrontendPid -ExpectedCommand 'npm.cmd run dev'
    $backendStopped = Stop-TrackedProcess -ProcessId $state.BackendPid -ExpectedCommand 'gradlew.bat bootRun'
    Remove-Item -Path $stateFile -Force -ErrorAction SilentlyContinue

    if (-not $NoMessage) {
        if ($frontendStopped -or $backendStopped) {
            Show-LauncherMessage 'Terra Sana a ete arretee.'
        } else {
            Show-LauncherMessage 'Les processus Terra Sana etaient deja arretes.'
        }
    }
} catch {
    if (-not $NoMessage) {
        Show-LauncherMessage -Message "Impossible d'arreter Terra Sana : $($_.Exception.Message)" -Icon 16
    }
    exit 1
}
