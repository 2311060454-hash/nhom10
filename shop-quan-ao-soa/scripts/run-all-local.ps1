# Script khoi chay toan bo cac dich vu backend SOA local
param([string]$Java = 'java.exe')
$ErrorActionPreference = 'Stop'
$startScript = Join-Path $PSScriptRoot 'Start-Local.ps1'
& $startScript -Java $Java
