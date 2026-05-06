param(
    [switch]$SkipFetch
)

$ErrorActionPreference = "Stop"

function Invoke-Git {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $output = & git @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "git $($Arguments -join ' ') failed: $output"
    }
    return $output
}

function Write-Check {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    Write-Host "[ok] $Message"
}

function Stop-Check {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    Write-Host "[error] $Message" -ForegroundColor Red
    exit 1
}

$repoRoot = (Invoke-Git @("rev-parse", "--show-toplevel")).Trim()
Set-Location $repoRoot

Write-Host "Checking repository version state in $repoRoot"

if (-not $SkipFetch) {
    Invoke-Git @("fetch", "--prune", "origin") | Out-Null
    Write-Check "Fetched latest origin refs"
}

$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$trackedEnv = & git ls-files --error-unmatch "backend/.env" 2>$null
$trackedEnvExitCode = $LASTEXITCODE
$ErrorActionPreference = $previousErrorActionPreference

if ($trackedEnvExitCode -eq 0 -or -not [string]::IsNullOrWhiteSpace($trackedEnv)) {
    Stop-Check "backend/.env is tracked by Git. Remove it from the index before pushing secrets."
}
Write-Check "backend/.env is not tracked"

$branch = (Invoke-Git @("branch", "--show-current")).Trim()
if ($branch -ne "main") {
    Stop-Check "Current branch is '$branch'. Switch to main before treating this checkout as the latest version."
}
Write-Check "Current branch is main"

$upstream = (Invoke-Git @("rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}")).Trim()
if ($upstream -ne "origin/main") {
    Stop-Check "main is tracking '$upstream'. Expected origin/main."
}
Write-Check "main tracks origin/main"

$status = & git status --porcelain=v1
if (-not [string]::IsNullOrWhiteSpace(($status -join "`n"))) {
    Write-Host "[error] Working tree is not clean. Commit or discard changes before declaring the latest version saved." -ForegroundColor Red
    $status | ForEach-Object { Write-Host $_ }
    exit 1
}
Write-Check "Working tree is clean"

$aheadBehind = (Invoke-Git @("rev-list", "--left-right", "--count", "HEAD...@{u}")).Trim() -split "\s+"
$ahead = [int]$aheadBehind[0]
$behind = [int]$aheadBehind[1]

if ($ahead -gt 0 -or $behind -gt 0) {
    Stop-Check "main is not synchronized with origin/main. Ahead: $ahead, behind: $behind."
}
Write-Check "main is synchronized with origin/main"

$latest = (Invoke-Git @("log", "-1", "--oneline", "--decorate")).Trim()
Write-Host "Latest saved version: $latest"
Write-Host "Version state check passed."
