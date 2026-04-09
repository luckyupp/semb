$ErrorActionPreference = "Stop"

New-Item -ItemType Directory -Force out | Out-Null
$sources = Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d out $sources

Write-Host "Compilation OK. Starting GUI..."
java -cp out cz.andel.Main
