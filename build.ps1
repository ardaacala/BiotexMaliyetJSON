param([switch]$SkipPackage)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
New-Item -ItemType Directory -Force -Path 'build/classes','build/tests','dist/input/assets','dist/input/sources' | Out-Null
& javac -encoding UTF-8 -cp 'lib/*' -d build/classes src/biotex/*.java
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
& javac -encoding UTF-8 -cp 'build/classes;lib/*' -d build/tests tests/JsonAcceptanceTests.java
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
& java -cp 'build/classes;build/tests;lib/*' JsonAcceptanceTests 'sources/maliyet-hesaplamalari.zip' 'assets/biotex-logo.jpg'
if ($LASTEXITCODE -ne 0) { throw 'Acceptance tests failed' }
$manifest = "Manifest-Version: 1.0`r`nMain-Class: biotex.App`r`nClass-Path: gson.jar flatlaf.jar pdfbox-app.jar`r`n`r`n"
[IO.File]::WriteAllText((Join-Path $PSScriptRoot 'build/manifest.mf'),$manifest,[Text.Encoding]::ASCII)
& jar --create --file dist/input/biotex-json.jar --manifest build/manifest.mf -C build/classes .
if ($LASTEXITCODE -ne 0) { throw 'JAR creation failed' }
Copy-Item -LiteralPath 'lib/gson.jar','lib/flatlaf.jar','lib/pdfbox-app.jar' -Destination 'dist/input' -Force
Copy-Item -LiteralPath 'assets/biotex-logo.jpg' -Destination 'dist/input/assets/biotex-logo.jpg' -Force
Copy-Item -LiteralPath 'sources/maliyet-hesaplamalari.zip' -Destination 'dist/input/sources/maliyet-hesaplamalari.zip' -Force
if (-not $SkipPackage) {
    $distPath=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'dist'))
    $imagePath=[IO.Path]::GetFullPath((Join-Path $distPath 'BiotexMaliyetJSON'))
    if (-not $imagePath.StartsWith($distPath+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe package path' }
    if (Test-Path -LiteralPath $imagePath) { Remove-Item -LiteralPath $imagePath -Recurse -Force }
    & jpackage --type app-image --name BiotexMaliyetJSON --input dist/input --main-jar biotex-json.jar --main-class biotex.App --dest dist --java-options '-Dfile.encoding=UTF-8'
    if ($LASTEXITCODE -ne 0) { throw 'Windows packaging failed' }
}
