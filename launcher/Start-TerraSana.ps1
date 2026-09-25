[CmdletBinding()]
param(
    [switch]$NoBrowser
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$backendDirectory = Join-Path $projectRoot 'Back End Java'
$frontendDirectory = Join-Path $projectRoot 'Front End VUE3'
$runtimeDirectory = Join-Path $projectRoot '.terra-sana-runtime'
$stateFile = Join-Path $runtimeDirectory 'processes.json'
$launcherLog = Join-Path $runtimeDirectory 'launcher.log'
$backendUrl = 'http://127.0.0.1:8081/api/auth/me'
$frontendHealthUrl = 'http://127.0.0.1:5173'
$frontendBrowserUrl = 'http://localhost:5173'

New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null

function Write-LauncherLog {
    param([string]$Message)

    $timestamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
    Add-Content -Path $launcherLog -Value "[$timestamp] $Message" -Encoding UTF8
}

function Show-LauncherMessage {
    param(
        [string]$Message,
        [int]$Icon = 64
    )

    try {
        $shell = New-Object -ComObject WScript.Shell
        [void]$shell.Popup($Message, 0, 'Terra Sana', $Icon)
    } catch {
        Write-LauncherLog "Impossible d'afficher le message Windows : $($_.Exception.Message)"
    }
}

function Test-HttpEndpoint {
    param([string]$Url)

    try {
        $request = [System.Net.HttpWebRequest]::Create($Url)
        $request.Method = 'GET'
        $request.Timeout = 1500
        $request.AllowAutoRedirect = $false
        $response = $request.GetResponse()
        $response.Close()
        return $true
    } catch [System.Net.WebException] {
        if ($null -ne $_.Exception.Response) {
            $_.Exception.Response.Close()
            return $true
        }
        return $false
    } catch {
        return $false
    }
}

function Wait-ForEndpoint {
    param(
        [string]$Url,
        [int]$TimeoutSeconds,
        [System.Diagnostics.Process]$StartedProcess
    )

    $timer = [System.Diagnostics.Stopwatch]::StartNew()
    while ($timer.Elapsed.TotalSeconds -lt $TimeoutSeconds) {
        if (Test-HttpEndpoint -Url $Url) {
            return $true
        }

        if ($null -ne $StartedProcess) {
            $StartedProcess.Refresh()
            if ($StartedProcess.HasExited) {
                return $false
            }
        }

        Start-Sleep -Seconds 1
    }

    return $false
}

function Start-HiddenCommand {
    param(
        [string]$WorkingDirectory,
        [string]$Command,
        [string]$StandardOutput,
        [string]$StandardError
    )

    return Start-Process -FilePath $env:ComSpec `
        -ArgumentList @('/d', '/c', $Command) `
        -WorkingDirectory $WorkingDirectory `
        -WindowStyle Hidden `
        -RedirectStandardOutput $StandardOutput `
        -RedirectStandardError $StandardError `
        -PassThru
}

function Stop-ProcessTree {
    param([System.Diagnostics.Process]$Process)

    if ($null -eq $Process) {
        return
    }

    try {
        $Process.Refresh()
        if (-not $Process.HasExited) {
            Start-Process -FilePath (Join-Path $env:SystemRoot 'System32\taskkill.exe') `
                -ArgumentList @('/PID', $Process.Id.ToString(), '/T', '/F') `
                -WindowStyle Hidden `
                -Wait | Out-Null
        }
    } catch {
        Write-LauncherLog "Nettoyage impossible pour le processus $($Process.Id) : $($_.Exception.Message)"
    }
}

function Read-LauncherState {
    $state = [ordered]@{
        BackendPid = $null
        FrontendPid = $null
        BackendLog = $null
        FrontendLog = $null
        StartedAt = $null
    }

    if (-not (Test-Path $stateFile)) {
        return $state
    }

    try {
        $savedState = Get-Content -Path $stateFile -Raw -Encoding UTF8 | ConvertFrom-Json
        foreach ($key in $state.Keys) {
            if ($savedState.PSObject.Properties.Name -contains $key) {
                $state[$key] = $savedState.$key
            }
        }
    } catch {
        Write-LauncherLog 'Etat precedent illisible, il sera remplace.'
    }

    return $state
}

function Save-LauncherState {
    param([System.Collections.IDictionary]$State)

    $State | ConvertTo-Json | Set-Content -Path $stateFile -Encoding UTF8
}

$mutex = New-Object System.Threading.Mutex($false, 'Local\TerraSanaLauncher')
$hasMutex = $false
$backendProcess = $null
$frontendProcess = $null
$startedBackendNow = $false
$startedFrontendNow = $false

try {
    $hasMutex = $mutex.WaitOne(0)
    if (-not $hasMutex) {
        exit 0
    }

    if (-not (Test-Path (Join-Path $backendDirectory 'gradlew.bat'))) {
        throw "Le backend Terra Sana est introuvable dans : $backendDirectory"
    }
    if (-not (Test-Path (Join-Path $frontendDirectory 'package.json'))) {
        throw "Le frontend Terra Sana est introuvable dans : $frontendDirectory"
    }
    if ($null -eq (Get-Command 'npm.cmd' -ErrorAction SilentlyContinue)) {
        throw 'Node.js/npm est introuvable. Installez Node.js puis relancez Terra Sana.'
    }

    $state = Read-LauncherState
    $timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'

    if (-not (Test-Path (Join-Path $frontendDirectory 'node_modules\.bin\vite.cmd'))) {
        $npmOutput = Join-Path $runtimeDirectory "npm-install-$timestamp.out.log"
        $npmError = Join-Path $runtimeDirectory "npm-install-$timestamp.err.log"
        Write-LauncherLog 'Installation initiale des dependances frontend.'
        $npmInstall = Start-HiddenCommand `
            -WorkingDirectory $frontendDirectory `
            -Command 'call npm.cmd ci' `
            -StandardOutput $npmOutput `
            -StandardError $npmError
        $npmInstall.WaitForExit()
        if ($npmInstall.ExitCode -ne 0) {
            throw "L'installation frontend a echoue. Consultez : $npmError"
        }
    }

    if (-not (Test-HttpEndpoint -Url $backendUrl)) {
        $backendOutput = Join-Path $runtimeDirectory "backend-$timestamp.out.log"
        $backendError = Join-Path $runtimeDirectory "backend-$timestamp.err.log"
        Write-LauncherLog 'Demarrage du backend Spring Boot.'
        $backendProcess = Start-HiddenCommand `
            -WorkingDirectory $backendDirectory `
            -Command 'call gradlew.bat bootRun --console=plain' `
            -StandardOutput $backendOutput `
            -StandardError $backendError
        $startedBackendNow = $true
        $state.BackendPid = $backendProcess.Id
        $state.BackendLog = $backendError
    }

    if (-not (Test-HttpEndpoint -Url $frontendHealthUrl)) {
        $frontendOutput = Join-Path $runtimeDirectory "frontend-$timestamp.out.log"
        $frontendError = Join-Path $runtimeDirectory "frontend-$timestamp.err.log"
        Write-LauncherLog 'Demarrage du frontend Vue.'
        $frontendProcess = Start-HiddenCommand `
            -WorkingDirectory $frontendDirectory `
            -Command 'call npm.cmd run dev -- --host 127.0.0.1 --port 5173 --strictPort' `
            -StandardOutput $frontendOutput `
            -StandardError $frontendError
        $startedFrontendNow = $true
        $state.FrontendPid = $frontendProcess.Id
        $state.FrontendLog = $frontendError
    }

    if ($startedBackendNow -or $startedFrontendNow) {
        $state.StartedAt = (Get-Date).ToString('o')
        Save-LauncherState -State $state
    }

    if (-not (Wait-ForEndpoint -Url $backendUrl -TimeoutSeconds 180 -StartedProcess $backendProcess)) {
        throw "Le backend n'a pas pu demarrer. Consultez les journaux dans : $runtimeDirectory"
    }
    if (-not (Wait-ForEndpoint -Url $frontendHealthUrl -TimeoutSeconds 90 -StartedProcess $frontendProcess)) {
        throw "Le frontend n'a pas pu demarrer. Consultez les journaux dans : $runtimeDirectory"
    }

    Write-LauncherLog 'Terra Sana est disponible.'
    if (-not $NoBrowser) {
        Start-Process $frontendBrowserUrl | Out-Null
    }
    Write-Output 'READY'
} catch {
    Write-LauncherLog "ERREUR : $($_.Exception.Message)"
    if ($startedFrontendNow) {
        Stop-ProcessTree -Process $frontendProcess
    }
    if ($startedBackendNow) {
        Stop-ProcessTree -Process $backendProcess
    }
    Remove-Item -Path $stateFile -Force -ErrorAction SilentlyContinue
    if (-not $NoBrowser) {
        Show-LauncherMessage -Message $_.Exception.Message -Icon 16
    }
    exit 1
} finally {
    if ($hasMutex) {
        $mutex.ReleaseMutex()
    }
    $mutex.Dispose()
}
