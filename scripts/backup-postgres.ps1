param(
    [Parameter(Mandatory = $true)]
    [string]$BackupDirectory,

    [Parameter(Mandatory = $true)]
    [switch]$EncryptedTargetConfirmed
)

$ErrorActionPreference = "Stop"

foreach ($name in @("PGHOST", "PGDATABASE", "PGUSER", "PGPASSFILE")) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "$name must be configured in the protected environment."
    }
}

if (-not (Test-Path -LiteralPath $env:PGPASSFILE -PathType Leaf)) {
    throw "PGPASSFILE must point to a protected password file."
}
if (-not $EncryptedTargetConfirmed) {
    throw "Confirm that BackupDirectory is encrypted and access-controlled."
}

$database = $env:PGDATABASE
if ($database -notmatch '^[A-Za-z0-9_-]+$') {
    throw "PGDATABASE contains unsupported characters for the backup filename."
}

$backupPathInfo = Resolve-Path -LiteralPath $BackupDirectory
$backupItem = Get-Item -LiteralPath $backupPathInfo.Path
if (-not $backupItem.PSIsContainer) {
    throw "BackupDirectory must be an existing directory."
}

$pgDump = Get-Command pg_dump -ErrorAction Stop
$pgRestore = Get-Command pg_restore -ErrorAction Stop
$timestamp = [DateTime]::UtcNow.ToString("yyyyMMdd-HHmmss")
$backupPath = Join-Path $backupItem.FullName "portroyal-$database-$timestamp.dump"
$partialPath = "$backupPath.partial"
$port = if ([string]::IsNullOrWhiteSpace($env:PGPORT)) { "5432" } else { $env:PGPORT }

try {
    & $pgDump.Source `
        "--format=custom" `
        "--compress=9" `
        "--no-owner" `
        "--no-acl" `
        "--file=$partialPath" `
        "--host=$env:PGHOST" `
        "--port=$port" `
        "--username=$env:PGUSER" `
        "--dbname=$database"
    if ($LASTEXITCODE -ne 0) {
        throw "pg_dump failed with exit code $LASTEXITCODE."
    }

    & $pgRestore.Source --list $partialPath *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "The generated PostgreSQL archive failed verification."
    }
    Move-Item -LiteralPath $partialPath -Destination $backupPath
}
catch {
    if (Test-Path -LiteralPath $partialPath -PathType Leaf) {
        Remove-Item -LiteralPath $partialPath -Force
    }
    throw
}

$cutoff = [DateTime]::UtcNow.AddDays(-30)
Get-ChildItem -LiteralPath $backupItem.FullName -File -Filter "portroyal-$database-*.dump" |
    Where-Object { $_.LastWriteTimeUtc -lt $cutoff } |
    ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force }

Write-Output "Created verified PostgreSQL backup: $backupPath"
