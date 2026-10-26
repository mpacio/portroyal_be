param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile,

    [Parameter(Mandatory = $true)]
    [string]$TargetDatabase,

    [Parameter(Mandatory = $true)]
    [switch]$ConfirmSeparateEmptyDatabase
)

$ErrorActionPreference = "Stop"

foreach ($name in @("PGHOST", "PGUSER", "PGPASSFILE", "PGDATABASE")) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "$name must be configured in the protected environment."
    }
}
if (-not (Test-Path -LiteralPath $env:PGPASSFILE -PathType Leaf)) {
    throw "PGPASSFILE must point to a protected password file."
}
if (-not $ConfirmSeparateEmptyDatabase) {
    throw "Confirm that TargetDatabase is a separate, empty restore database."
}
if ($TargetDatabase -eq $env:PGDATABASE) {
    throw "Restore target must differ from PGDATABASE; never restore over the source database."
}

$backupPathInfo = Resolve-Path -LiteralPath $BackupFile
if (-not (Test-Path -LiteralPath $backupPathInfo.Path -PathType Leaf)) {
    throw "BackupFile must be an existing archive file."
}
$pgRestore = Get-Command pg_restore -ErrorAction Stop
$port = if ([string]::IsNullOrWhiteSpace($env:PGPORT)) { "5432" } else { $env:PGPORT }

& $pgRestore.Source --list $backupPathInfo.Path *> $null
if ($LASTEXITCODE -ne 0) {
    throw "BackupFile is not a valid PostgreSQL archive."
}

& $pgRestore.Source `
    "--exit-on-error" `
    "--no-owner" `
    "--no-acl" `
    "--host=$env:PGHOST" `
    "--port=$port" `
    "--username=$env:PGUSER" `
    "--dbname=$TargetDatabase" `
    $backupPathInfo.Path
if ($LASTEXITCODE -ne 0) {
    throw "pg_restore failed with exit code $LASTEXITCODE."
}

Write-Output "Restore completed to the separate database: $TargetDatabase"
Write-Warning "Reapply account-deletion records from the operator's durable deletion ledger before exposing this database."
