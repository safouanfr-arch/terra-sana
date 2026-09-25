[CmdletBinding()]
param(
    [switch]$NoMessage
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$desktop = [Environment]::GetFolderPath('Desktop')
$powerShell = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
$icon = Join-Path $PSScriptRoot 'terra-sana.ico'
$startScript = Join-Path $PSScriptRoot 'Start-TerraSana.ps1'
$stopScript = Join-Path $PSScriptRoot 'Stop-TerraSana.ps1'
$stopShortcutName = 'Arr' + [char]0x00EA + 'ter Terra Sana'

if (-not (Test-Path $icon)) {
    throw "Icone introuvable : $icon"
}

$shell = New-Object -ComObject WScript.Shell
$legacyStopShortcut = Join-Path $desktop 'Arreter Terra Sana.lnk'
$misencodedStopShortcut = Join-Path $desktop ('Arr' + [char]0x00C3 + [char]0x00AA + 'ter Terra Sana.lnk')

foreach ($oldShortcut in @($legacyStopShortcut, $misencodedStopShortcut)) {
    if (Test-Path $oldShortcut) {
        Remove-Item -LiteralPath $oldShortcut -Force
    }
}

function New-TerraSanaShortcut {
    param(
        [string]$Name,
        [string]$Script,
        [string]$Description
    )

    $shortcutPath = Join-Path $desktop "$Name.lnk"
    $shortcut = $shell.CreateShortcut($shortcutPath)
    $shortcut.TargetPath = $powerShell
    $shortcut.Arguments = "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$Script`""
    $shortcut.WorkingDirectory = $projectRoot
    $shortcut.IconLocation = "$icon,0"
    $shortcut.Description = $Description
    $shortcut.Save()
}

New-TerraSanaShortcut `
    -Name 'Terra Sana' `
    -Script $startScript `
    -Description 'Demarrer Terra Sana et ouvrir l application'

New-TerraSanaShortcut `
    -Name $stopShortcutName `
    -Script $stopScript `
    -Description 'Arreter les services Terra Sana'

if (-not $NoMessage) {
    [void]$shell.Popup('Les raccourcis Terra Sana ont ete installes sur le Bureau.', 0, 'Terra Sana', 64)
}
