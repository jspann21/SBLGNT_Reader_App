param([string]$Owner = 'jspann21')
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$signingDirectory = Join-Path $repoRoot '.signing'
$keystorePath = Join-Path $signingDirectory 'release.p12'
$propertiesPath = Join-Path $repoRoot 'keystore.properties'
if ((Test-Path -LiteralPath $keystorePath) -or (Test-Path -LiteralPath $propertiesPath)) {
    throw 'Signing material already exists. Preserve it; do not replace your release identity.'
}
$keytool = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/keytool.exe' } else { 'keytool' }
New-Item -ItemType Directory -Path $signingDirectory -Force | Out-Null
$password = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
$env:GNT_KEYTOOL_PASSWORD = $password
try {
    & $keytool -genkeypair -noprompt -storetype PKCS12 -keystore $keystorePath `
        -storepass:env GNT_KEYTOOL_PASSWORD -keypass:env GNT_KEYTOOL_PASSWORD `
        -alias release -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=$Owner, OU=Lex Apps"
    if ($LASTEXITCODE -ne 0) { throw 'keytool failed to generate the signing key.' }
    @("storeFile=.signing/release.p12", "storePassword=$password", 'keyAlias=release', "keyPassword=$password") |
        Set-Content -LiteralPath $propertiesPath -Encoding utf8NoBOM
    Write-Output 'Created ignored .signing/release.p12 and keystore.properties. Back up both securely.'
} finally {
    Remove-Item Env:GNT_KEYTOOL_PASSWORD -ErrorAction SilentlyContinue
}
