# Script for commit with automatic version update
# USAGE: .\commit-with-version.ps1 [-Message "Commit message"]

param(
    [string]$Message = "update: changes"
)

# Path to version file
$VERSION_FILE = "app/src/main/java/l1c/RunYBase.java"

# Get current version
$content = Get-Content $VERSION_FILE -Raw
if ($content -match 'private static final String VERSION\s*=\s*"(\d{4}\.\d{2}\.\d{2}\.\d{3})"') {
    $currentVersion = $matches[1]
    Write-Host "Current version: $currentVersion" -ForegroundColor Cyan

    # Parse version (YYYY.MM.DD.PPP)
    $parts = $currentVersion -split '\.'
    $year = $parts[0]
    $month = $parts[1]
    $day = $parts[2]
    $patch = [int]$parts[3]

    # Increment patch
    $patch++
    $newVersion = "$year.$month.$day.$($patch.ToString('000'))"

    Write-Host "New version: $newVersion" -ForegroundColor Green

    # Update version in file
    $newContent = $content -replace 'private static final String VERSION\s*=\s*"\d{4}\.\d{2}\.\d{2}\.\d{3}"', "private static final String VERSION = `"$newVersion`""
    $newContent | Set-Content $VERSION_FILE -Encoding UTF8

    Write-Host "Version updated in file" -ForegroundColor Green

    # Add and commit
    Write-Host "`nAdding files to git..." -ForegroundColor Yellow
    git add -A

    Write-Host "Creating commit..." -ForegroundColor Yellow
    git commit -m "$Message`n`nchore: bump version $currentVersion -> $newVersion"

    Write-Host "`nDone! Version updated: $newVersion" -ForegroundColor Green
}
else {
    Write-Host "Error: could not find version in file $VERSION_FILE" -ForegroundColor Red
    exit 1
}
