param(
    [string]$Password = "admin123",
    [string]$Owner = "Nil"
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

$Jdk25 = "C:\Users\nilbe\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2"
if (Test-Path $Jdk25) {
    $env:JAVA_HOME = $Jdk25
}

Write-Host "Resetting database tables in imdbselector-postgres..."
docker exec imdbselector-postgres psql -U imdb -d imdbselector -c "TRUNCATE TABLE watchlist_subscriptions, watchlist_items, user_seen_media, watchlists, users, media_item_genres, media_items RESTART IDENTITY CASCADE;"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$env:DB_URL = "jdbc:postgresql://localhost:5433/imdbselector"

Write-Host "Re-importing baseline legacy dataset from test-data/legacy..."
.\backend\gradlew.bat -p backend migrateLegacy --args="--data `"$RepoRoot\test-data\legacy`" --owner $Owner --password $Password" --console=plain --no-daemon
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Baseline ready! Log in as '$Owner' / '$Password' and test updating with test-data/latest_watchlist.csv."
