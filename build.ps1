# Set Java home
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:Path = $env:JAVA_HOME + "\bin;" + $env:Path

# Run gradle clean and build
Write-Host "Starting clean build..."
& .\gradlew.bat clean assembleDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build successful!"
} else {
    Write-Host "Build failed with exit code $LASTEXITCODE"
}
