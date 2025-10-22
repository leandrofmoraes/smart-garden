#!/usr/bin/env bash
# send_all.sh
# Envia uma sequência de leituras simuladas para o backend.
# Endpoint e valores podem ser ajustados via variáveis abaixo.

SERVER="http://192.168.8.11:3000/reading"
CONTENT_TYPE="Content-Type: application/json"

# Pequeno delay entre requisições (segundos)
DELAY=0.15

payloads=(
  '{"humidity":32,"timestamp":"2025-10-21T18:10:00Z","esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":30,"timestamp":"2025-10-21T19:10:00Z","esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":28,"timestamp":"2025-10-21T20:10:00Z","esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":24,"timestamp":"2025-10-21T21:10:00Z","esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":19,"timestamp":"2025-10-21T22:10:00Z","esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":18,"timestamp":"2025-10-21T22:30:00Z","regando":true,"rega_pulsos":0,"rega_volume_l":0.0,"volume_total_l":0.0,"device_ts_ms":0,"esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":42,"timestamp":"2025-10-21T22:30:05Z","regando":false,"rega_pulsos":45,"rega_volume_l":0.10,"volume_total_l":0.10,"rega_duracao_s":5,"esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":40,"timestamp":"2025-10-21T23:10:00Z","esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":38,"timestamp":"2025-10-22T00:10:00Z","esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":36,"timestamp":"2025-10-22T01:10:00Z","esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":34,"timestamp":"2025-10-22T02:10:00Z","esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":30,"timestamp":"2025-10-22T03:10:00Z","esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":29,"timestamp":"2025-10-22T03:30:00Z","regando":true,"rega_pulsos":0,"rega_volume_l":0.0,"volume_total_l":0.10,"device_ts_ms":0,"esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":55,"timestamp":"2025-10-22T03:30:05Z","regando":false,"rega_pulsos":45,"rega_volume_l":0.10,"volume_total_l":0.20,"rega_duracao_s":5,"esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":53,"timestamp":"2025-10-22T04:10:00Z","esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":50,"timestamp":"2025-10-22T05:10:00Z","esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":48,"timestamp":"2025-10-22T06:10:00Z","esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":45,"timestamp":"2025-10-22T07:10:00Z","esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":42,"timestamp":"2025-10-22T08:10:00Z","esp_ip":"192.168.8.50","esp_rssi":-62}'
  '{"humidity":40,"timestamp":"2025-10-22T09:00:00Z","esp_ip":"192.168.8.50","esp_rssi":-62}'
)

echo "Enviando ${#payloads[@]} leituras para $SERVER"
echo

i=0
for p in "${payloads[@]}"; do
  i=$((i+1))
  echo "[$i/${#payloads[@]}] POST -> $p"
  # mostra código HTTP e corpo de resposta
  httpCode=$(curl -sS -w "%{http_code}" -o /tmp/send_all_response -X POST "$SERVER" -H "$CONTENT_TYPE" -d "$p")
  resp=$(cat /tmp/send_all_response)
  echo "Resposta HTTP: $httpCode"
  if [ -n "$resp" ]; then
    echo "Body: $resp"
  fi
  echo "-----------------------------"
  sleep "$DELAY"
done

echo "Concluído."
