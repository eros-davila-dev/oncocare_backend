# Levanta OncoCare completo en esta PC para una demostracion.
#
#   cd oncocare_backend\docker
#   powershell -ExecutionPolicy Bypass -File .\levantar-demo.ps1
#
# Que hace:
#   1. Arranca los contenedores (base de datos, backend, portal, intranet, n8n).
#   2. Abre un tunel de cloudflared hacia n8n. La URL del tunel rapido cambia
#      cada vez que se abre, asi que...
#   3. ...la guarda en N8N_WEBHOOK_URL del .env y reinicia n8n, y
#   4. vuelve a registrar el webhook del bot de Telegram con la URL nueva (sin
#      esto, los mensajes y los botones del bot no llegan).
#   5. Comprueba que todo responda e imprime las direcciones.
#
# El token del bot no se imprime ni se guarda: se lee de la credencial de n8n.

$ErrorActionPreference = 'Stop'
$carpeta = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $carpeta

# Ruta del webhook del router de Telegram (fija: webhookId del trigger en
# n8n/workflows/telegram-router.json) y secreto que n8n exige en cada llamada.
$WORKFLOW_ROUTER = 'Rpcfz8i9xVGAMqX0'
$RUTA_WEBHOOK = 'webhook/c7d1f3a2-5b8e-4c6a-9f10-2e4b7a9d3c51/webhook'
$NODO_TRIGGER = '4a246eb1-9b0d-5481-b374-b41e2e10041e'
$CLOUDFLARED = 'C:\Program Files (x86)\cloudflared\cloudflared.exe'

function Paso($texto) { Write-Host "`n==> $texto" -ForegroundColor Cyan }
function Ok($texto) { Write-Host "    OK  $texto" -ForegroundColor Green }
function Falla($texto) { Write-Host "    !!  $texto" -ForegroundColor Red }

function Esperar($descripcion, [scriptblock]$prueba, $segundos = 180) {
    $limite = (Get-Date).AddSeconds($segundos)
    while ((Get-Date) -lt $limite) {
        try { if (& $prueba) { Ok $descripcion; return $true } } catch { }
        Start-Sleep -Seconds 3
    }
    Falla "$descripcion (no respondio en $segundos s)"
    return $false
}

function Valor-Env($clave, $porDefecto) {
    $linea = Get-Content .env -Encoding UTF8 | Where-Object { $_ -match "^$clave=" } | Select-Object -First 1
    if ($linea) { return ($linea -split '=', 2)[1].Trim() } else { return $porDefecto }
}

# 1. Docker -------------------------------------------------------------------
Paso 'Docker'
# En Windows PowerShell 5.1, redirigir el error de un comando externo con
# "*> $null" lo convierte en una excepcion: se consulta a traves de cmd.
function DockerListo { cmd /c "docker info >nul 2>&1"; return ($LASTEXITCODE -eq 0) }
if (-not (DockerListo)) {
    Write-Host '    Iniciando Docker Desktop...'
    Start-Process 'C:\Program Files\Docker\Docker\Docker Desktop.exe'
    if (-not (Esperar 'Docker Desktop listo' { DockerListo } 300)) { exit 1 }
}
docker compose up -d
if ($LASTEXITCODE -ne 0) { Falla 'docker compose up fallo'; exit 1 }

$puertoBackend = Valor-Env 'BACKEND_HOST_PORT' '8080'
$puertoPortal = Valor-Env 'PORTAL_HOST_PORT' '4200'
$puertoIntranet = Valor-Env 'INTRANET_HOST_PORT' '4300'
$puertoN8n = Valor-Env 'N8N_HOST_PORT' '5678'

Esperar 'Backend' { (Invoke-WebRequest "http://localhost:$puertoBackend/actuator/health" -UseBasicParsing -TimeoutSec 5).StatusCode -eq 200 } | Out-Null

# 2. Tunel --------------------------------------------------------------------
Paso 'Tunel de cloudflared hacia n8n'
Get-Process cloudflared -ErrorAction SilentlyContinue | Stop-Process -Force
$log = Join-Path $env:TEMP 'oncocare-cloudflared.log'
$logSalida = Join-Path $env:TEMP 'oncocare-cloudflared-salida.log'
Remove-Item $log, $logSalida -ErrorAction SilentlyContinue
# Ambas salidas a archivo: asi cloudflared no retiene la consola de este script.
Start-Process -FilePath $CLOUDFLARED -ArgumentList "tunnel --no-autoupdate --url http://localhost:$puertoN8n" `
    -RedirectStandardError $log -RedirectStandardOutput $logSalida -WindowStyle Hidden
$url = $null
$limite = (Get-Date).AddSeconds(60)
while (-not $url -and (Get-Date) -lt $limite) {
    Start-Sleep -Seconds 2
    if (Test-Path $log) {
        $m = Select-String -Path $log -Pattern 'https://[a-z0-9-]+\.trycloudflare\.com' | Select-Object -First 1
        if ($m) { $url = $m.Matches[0].Value }
    }
}
if (-not $url) { Falla "No se obtuvo la URL del tunel (revise $log)"; exit 1 }
Ok "Tunel: $url"

# 3. n8n con la URL nueva ------------------------------------------------------
Paso 'n8n'
$rutaEnv = Join-Path $carpeta '.env'
$utf8 = New-Object System.Text.UTF8Encoding $false   # sin BOM: docker compose no lo entiende
$contenido = [System.IO.File]::ReadAllLines($rutaEnv, $utf8)
$contenido = $contenido -replace '^N8N_WEBHOOK_URL=.*$', "N8N_WEBHOOK_URL=$url/"
[System.IO.File]::WriteAllLines($rutaEnv, $contenido, $utf8)
docker compose up -d n8n | Out-Null
Esperar 'n8n' { (Invoke-WebRequest "http://localhost:$puertoN8n/healthz" -UseBasicParsing -TimeoutSec 5).StatusCode -eq 200 } | Out-Null
# El tunel tarda unos segundos en enrutar despues de crearse.
Esperar 'Tunel publico respondiendo' { (Invoke-WebRequest "$url/healthz" -UseBasicParsing -TimeoutSec 10).StatusCode -eq 200 } 90 | Out-Null

# 4. Webhook de Telegram -------------------------------------------------------
Paso 'Bot de Telegram'
$env:MSYS_NO_PATHCONV = '1'
cmd /c "docker exec oncocare-n8n-1 n8n export:credentials --all --decrypted --output=/tmp/cr.json >nul 2>&1"
$token = docker exec oncocare-n8n-1 node -e "const c=require('/tmp/cr.json');const t=c.find(x=>x.type==='telegramApi');process.stdout.write(t?t.data.accessToken:'')"
docker exec oncocare-n8n-1 rm -f /tmp/cr.json | Out-Null
if (-not $token) {
    Falla 'No hay credencial de Telegram en n8n'
} else {
    $cuerpo = @{
        url             = "$url/$RUTA_WEBHOOK"
        secret_token    = "${WORKFLOW_ROUTER}_$NODO_TRIGGER"
        allowed_updates = @('message', 'callback_query')
    } | ConvertTo-Json
    $r = Invoke-RestMethod -Method Post -Uri "https://api.telegram.org/bot$token/setWebhook" -ContentType 'application/json' -Body $cuerpo
    if ($r.ok) { Ok 'Webhook registrado' } else { Falla "Telegram respondio: $($r.description)" }
    $info = (Invoke-RestMethod "https://api.telegram.org/bot$token/getWebhookInfo").result
    Write-Host "    URL en Telegram: $($info.url)"
    Write-Host "    Mensajes pendientes: $($info.pending_update_count)"
    $token = $null
}

# 5. Resumen -------------------------------------------------------------------
Paso 'Comprobacion final'
Esperar 'Portal del paciente' { (Invoke-WebRequest "http://localhost:$puertoPortal/" -UseBasicParsing -TimeoutSec 5).StatusCode -eq 200 } 30 | Out-Null
Esperar 'Intranet' { (Invoke-WebRequest "http://localhost:$puertoIntranet/" -UseBasicParsing -TimeoutSec 5).StatusCode -eq 200 } 30 | Out-Null

Write-Host ''
Write-Host '  Portal del paciente : ' -NoNewline; Write-Host "http://localhost:$puertoPortal" -ForegroundColor Yellow
Write-Host '  Intranet            : ' -NoNewline; Write-Host "http://localhost:$puertoIntranet" -ForegroundColor Yellow
Write-Host '  n8n (editor)        : ' -NoNewline; Write-Host "http://localhost:$puertoN8n" -ForegroundColor Yellow
Write-Host '  Bot de Telegram     : ' -NoNewline; Write-Host "https://t.me/$(Valor-Env 'TELEGRAM_BOT_USERNAME' '')" -ForegroundColor Yellow
Write-Host ''
Write-Host '  Deje esta PC encendida y sin suspender: el tunel vive mientras cloudflared este abierto.'
