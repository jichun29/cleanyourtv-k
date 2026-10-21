# Windows version of disable.sh: disable every package listed in a debloat file on the TV
# (one package per line, # comments allowed). Packages are disabled for user 0, never uninstalled.
# Undo with tools\restore.ps1 <same file>.
#
# Usage: tools\disable.ps1 -TvHost 192.168.1.50 examples\xiaomi-mi-tv-4a\debloat.txt
param(
    [Parameter(Mandatory = $true, Position = 0)][string]$List,
    [string]$TvHost = $env:TV_HOST
)
$ErrorActionPreference = 'Stop'
# Pipe plain UTF-8 to native commands; the default here prepends a BOM that corrupts the first command.
$OutputEncoding = New-Object System.Text.UTF8Encoding $false
$repo = Split-Path -Parent $PSScriptRoot
$List = (Resolve-Path $List).Path
$py = Join-Path $repo 'tools\venv\Scripts\python.exe'
if (-not (Test-Path $py)) { throw 'run: python -m venv tools\venv; tools\venv\Scripts\python.exe -m pip install -r tools\requirements.txt' }
if (-not $TvHost) { throw 'pass -TvHost <tv ip> or set $env:TV_HOST' }
$env:TV_HOST = $TvHost

$pkgs = Get-Content $List | ForEach-Object { ($_ -replace '#.*$', '').Trim() } | Where-Object { $_ } | ForEach-Object { ($_ -split '\s+')[0] }
# Lists come from contributors and each name is pasted into a shell command on the TV (#165).
$bad = @($pkgs | Where-Object { $_ -notmatch '^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$' })
if ($bad.Count -gt 0) {
    throw "refusing $List, not a package name: $($bad -join ', ')"
}
# A Wi-Fi network saved during the first-run wizard belongs to setupwraith, and Android deletes it when
# setupwraith is disabled. Skip it while it owns the saved network; `$env:FORCE_SETUPWRAITH=1` overrides.
$setup = 'com.google.android.tungsten.setupwraith'
if ($pkgs -contains $setup -and -not $env:FORCE_SETUPWRAITH) {
    $wifi = (& $py -I (Join-Path $repo 'tools\tv.py') 'dumpsys wifi') -join "`n"
    # Be safe: if we cannot read the owners (tv.py failed, timed out, or showed no network at all),
    # do not disable setupwraith, so the TV cannot lose the network it is on.
    if ($LASTEXITCODE -ne 0 -or $wifi -notmatch 'cname=') {
        Write-Warning "Skipping ${setup}: could not check which app owns the saved Wi-Fi networks (dumpsys wifi failed or showed none). Try again, or set `$env:FORCE_SETUPWRAITH=1 if the TV uses Ethernet."
        $pkgs = @($pkgs | Where-Object { $_ -ne $setup })
    } elseif ($wifi -match 'cname=[^ ]*setupwraith') {
        Write-Warning "Skipping ${setup}: the saved Wi-Fi network belongs to it, and disabling it would delete that network and take the TV off Wi-Fi. Forget the network and join it again in Settings > Network & Internet, then run this again (or set `$env:FORCE_SETUPWRAITH=1 on Ethernet)."
        $pkgs = @($pkgs | Where-Object { $_ -ne $setup })
    }
}
# Same as disable.sh: on firmware with ro.adb.secure=0, port 5555 gives a shell to any device on the
# network without the "Allow" prompt. Read it now and say so once the list is done; a failed read only
# skips the note. Windows PowerShell turns redirected stderr into an error that 'Stop' would throw.
$ErrorActionPreference = 'Continue'
$adbSecure = ((& $py -I (Join-Path $repo 'tools\tv.py') 'getprop ro.adb.secure' 2>$null) -join '').Trim()
if ($LASTEXITCODE -ne 0) { $adbSecure = '' }
$ErrorActionPreference = 'Stop'

# Home guard (#239), the same check as disable.sh: a list that disables the app owning Home leaves the
# TV with nothing to start Home with. Skip the owner while nothing else on the TV can answer HOME;
# `$env:FORCE_HOME=1` disables it anyway, for a TV reachable another way.
if (-not $env:FORCE_HOME) {
    $homeQuery = 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME'
    $homeLine = @(& $py -I (Join-Path $repo 'tools\tv.py') $homeQuery) |
        Where-Object { $_ -match '\S' } | Select-Object -Last 1
    $homePkg = "$homeLine".Trim() -split '/' | Select-Object -First 1
    if ($homePkg -and ($pkgs -contains $homePkg)) {
        # Every package with a HOME activity the TV can start now; a disabled one does not answer.
        $claimQuery = 'cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME'
        $claims = @(& $py -I (Join-Path $repo 'tools\tv.py') $claimQuery | ForEach-Object {
            # Keep the class: a vendor fallback is named FallbackHome and is not a usable launcher
            # (TCL Android TV sets register com.tcl.keycustomfunctionservice/.FallbackHome).
            # Trim first: a trailing CR from the TV would stop the `$` anchor from matching any line.
            if ("$_".Trim() -match '^([A-Za-z0-9_.]+)/([A-Za-z0-9_.$]+)$') {
                if ($Matches[2] -notmatch 'FallbackHome$') { $Matches[1] }
            }
        } | Sort-Object -Unique)
        # The setup wizard and the settings apps answer HOME too, but they are not launchers (disable.sh).
        $notLaunchers = @('com.google.android.tungsten.setupwraith', 'com.android.tv.settings', 'com.android.settings', 'com.android.provision')
        $others = @($claims | Where-Object { $_ -ne $homePkg -and $pkgs -notcontains $_ -and $notLaunchers -notcontains $_ })
        if ($others.Count -eq 0) {
            Write-Warning "Skipping ${homePkg}: it owns Home, and no other app that can answer Home is installed and staying enabled. Make the new launcher Home first (launcher\build-and-install.ps1), then run this list again to disable the old one; or set `$env:FORCE_HOME=1 if you can still reach the TV."
            $pkgs = @($pkgs | Where-Object { $_ -ne $homePkg })
        }
    }
}
# Same as disable.sh (#321): a package that asks for android.permission.TV_INPUT_HARDWARE is the software
# layer under the HDMI, AV and tuner inputs. Disable one and the port still notices the cable but shows
# "No Signal" for good; re-enabling the pieces in batches does not bring the picture back, a full restore
# and a cold boot do. Keep them; `$env:TVINPUT_ALLOW=1` disables them anyway.
if (-not $env:TVINPUT_ALLOW -and $pkgs.Count -gt 0) {
    # Trailing `; true`: the loop's status is the last package's `grep -q`, which is nonzero whenever the
    # last listed package is not an input driver, and would look like a failed check.
    $loop = 'for p in ' + ($pkgs -join ' ') + '; do dumpsys package $p | sed -n -E ''/requested permissions:/,/(install|runtime) permissions:|User [0-9]+:/p'' | grep -q TV_INPUT_HARDWARE && echo $p; done; true'
    # Windows PowerShell turns redirected stderr into an error that 'Stop' would throw (see getprop).
    $ErrorActionPreference = 'Continue'
    $tvhw = @(& $py -I (Join-Path $repo 'tools\tv.py') $loop 2>$null)
    $checked = $LASTEXITCODE -eq 0
    $ErrorActionPreference = 'Stop'
    if (-not $checked) {
        Write-Warning 'Could not check which listed packages drive the HDMI/AV inputs (tv.py failed). Check every input after this run: a port that notices the cable and stays black means one of them was disabled, and only a full restore and a cold boot bring it back (#321).'
    } else {
        foreach ($p in ($tvhw | ForEach-Object { $_.Trim() } | Where-Object { $_ })) {
            if ($pkgs -contains $p) {
                Write-Warning "Skipping ${p}: it asks for android.permission.TV_INPUT_HARDWARE, so it is the software layer under the HDMI/AV/tuner inputs. With it disabled those inputs still notice the cable but stay black (No Signal), and re-enabling the pieces in batches does not restore the picture: a full restore plus a cold boot does (#321). Set `$env:TVINPUT_ALLOW=1 to disable it."
                $pkgs = @($pkgs | Where-Object { $_ -ne $p })
            }
        }
    }
}
# Same as disable.sh: the state undo needs is recorded before the first command reaches the TV, and
# a failed save leaves the TV untouched (#239).
& $py -I (Join-Path $repo 'tools\recovery.py') --save $List
if ($LASTEXITCODE -ne 0) { throw 'no recovery record was written, so nothing was disabled' }

$cmds = @($pkgs | ForEach-Object { "pm disable-user --user 0 $_" }) + 'pm list packages -d'
$cmds | & $py -I (Join-Path $repo 'tools\tv.py') --batch

if ($adbSecure -eq '0') {
    Write-Warning "This TV reports ro.adb.secure=0, so network ADB on port 5555 accepts any device on your network without the `"Allow`" prompt. When you are done, turn USB debugging and Network/ADB debugging off on the TV and reboot it (README: `"Isn't ADB access a security hole?`")."
}
