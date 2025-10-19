/*
   Criado por: Leandro F. Moraes
   Rev.: 02
   Data: 16.10.2025
   Observações:
  - ESP8266: Recebe JSON vinda do Arduino via Srial e envia via HTTP POST
  - Conecte: Arduino TX -> ESP8266 RX (GPIO3)
*/

#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <ESP8266HTTPClient.h>
#include <ArduinoJson.h>
#include <time.h>

// --- Configurações ---
#define ARDUINO_BAUD 9600  // Velocidade Serial com Arduino

const char* ssid = "";       //nome da rede Wifi
const char* password = "";   //senha Wifi
const char* serverUrl = "";  //substituir pela URL do servidor HTTP
const unsigned long WIFI_TIMEOUT_MS = 30000;
const unsigned long HTTP_TIMEOUT_MS = 10000;
const size_t MAX_LINE_LENGTH = 384;

// Para Fila
const int MAX_QUEUE = 8;
const int MAX_PAYLOAD = 384;
// --------------------------------

String linhaBuffer = "";
const String prefixo = "#DATA#";
bool wifiConnected = false;
bool enviandoHTTP = false;

// Fila circular
char queueBuf[MAX_QUEUE][MAX_PAYLOAD + 1];
int qHead = 0;
int qTail = 0;
int qCount = 0;

// --- Funções de fila ---

// Verifica se a fila está vazia
bool isQueueEmpty() {
  return qCount == 0;
}
// Verifica se a fila está cheia
bool isQueueFull() {
  return qCount == MAX_QUEUE;
}

bool enqueuePayload(const char* payload) {
  size_t len = strlen(payload);
  if (len == 0 || len > MAX_PAYLOAD) {
    Serial.println(F("[QUEUE] Payload inválido/tamanho excede limite"));
    return false;
  }

  if (isQueueFull()) {
    Serial.println(F("[QUEUE] Fila cheia, descartando item mais antigo"));
    qHead = (qHead + 1) % MAX_QUEUE;
    qCount--;
  }

  strncpy(queueBuf[qTail], payload, MAX_PAYLOAD);
  queueBuf[qTail][MAX_PAYLOAD] = '\0';
  qTail = (qTail + 1) % MAX_QUEUE;
  qCount++;

  Serial.print(F("[QUEUE] Enfileirado. tamanho fila = "));
  Serial.println(qCount);
  return true;
}

bool peekQueue(char* outBuf) {
  if (isQueueEmpty()) return false;
  strncpy(outBuf, queueBuf[qHead], MAX_PAYLOAD);
  outBuf[MAX_PAYLOAD] = '\0';
  return true;
}

bool dequeuePayload() {
  if (isQueueEmpty()) return false;
  qHead = (qHead + 1) % MAX_QUEUE;
  qCount--;
  return true;
}

bool getIsoTimestamp(char* buf, size_t buflen) {
  time_t now = time(nullptr);
  if (now < 1600000000UL) return false;  // tempo inválido (NTP não sincronizado)
  struct tm t;
  gmtime_r(&now, &t);
  strftime(buf, buflen, "%Y-%m-%dT%H:%M:%SZ", &t);
  return true;
}

bool httpPostJson(const String& jsonPayload) {
  if (WiFi.status() != WL_CONNECTED) {
    wifiConnected = false;
    Serial.println(F("[HTTP] WiFi desconectado"));
    return false;
  }

  if (enviandoHTTP) {
    Serial.println(F("[HTTP] Tentativa rejeitada"));
    return false;
  }

  enviandoHTTP = true;
  bool success = false;

  WiFiClient client;
  HTTPClient http;
  http.setTimeout(HTTP_TIMEOUT_MS);

  if (!http.begin(client, serverUrl)) {
    Serial.println(F("[HTTP] begin() falhou"));
    enviandoHTTP = false;
    return false;
  }

  http.addHeader("Content-Type", "application/json");
  http.addHeader("User-Agent", "ESP8266-Automacao-Rega");

  Serial.print(F("[HTTP] POST -> "));
  Serial.println(jsonPayload);

  int httpCode = http.POST((uint8_t*)jsonPayload.c_str(), jsonPayload.length());

  if (httpCode > 0) {
    Serial.print(F("[HTTP] Código - "));
    Serial.println(httpCode);
    if (httpCode >= 200 && httpCode < 300) {
      String resp = http.getString();
      Serial.print(F("[HTTP] Resposta: "));
      Serial.println(resp);
      success = true;
    } else {
      Serial.print(F("[HTTP] Código fora de 2xx: "));
      Serial.println(httpCode);
    }
  } else {
    Serial.print(F("[HTTP] Falha: "));
    Serial.println(http.errorToString(httpCode).c_str());
  }

  http.end();
  enviandoHTTP = false;
  return success;
}

void trySendOrQueue(const String& payload) {
  if (payload.length() == 0 || payload.length() > MAX_PAYLOAD) return;

  bool ok = httpPostJson(payload);
  if (!ok) {
    if (!enqueuePayload(payload.c_str())) {
      Serial.println(F("[QUEUE] Falha ao enfileirar payload"));
    }
  }
}

void flushQueue() {
  if (isQueueEmpty()) return;

  Serial.print(F("[QUEUE] Tentando enviar fila (itens = "));
  Serial.print(qCount);
  Serial.println(" )...");

  char tmp[MAX_PAYLOAD + 1];
  int attempts = 0;
  const int MAX_ATTEMPTS = 3;  // Evita loop infinito

  while (!isQueueEmpty() && WiFi.status() == WL_CONNECTED && attempts < MAX_ATTEMPTS) {
    if (enviandoHTTP) break;

    if (!peekQueue(tmp)) break;

    String payload = String(tmp);
    bool ok = httpPostJson(payload);

    if (ok) {
      Serial.println(F("[QUEUE] Item enviado, removendo da fila"));
      dequeuePayload();
      attempts = 0;  // Reset attempts on success
    } else {
      Serial.println(F("[QUEUE] Falha ao enviar item, tentando novamente..."));
      attempts++;
      delay(1000);  // Aguarda 1s antes de retry
    }
  }

  Serial.print(F("[QUEUE] Fila após flush = "));
  Serial.println(qCount);
}

void processarLinha(String linha) {
  linha.trim();
  if (linha.length() == 0) return;

  if (!linha.startsWith(prefixo)) {
    // linha sem prefixo = debug do Arduino
    if (linha.length() < 200) {
      Serial.print(F("[ARDUINO] "));
      Serial.println(linha);
    }
    return;
  }

  String jsonStr = linha.substring(prefixo.length());
  jsonStr.trim();

  Serial.print(F("[RX] Bruto: "));
  Serial.println(jsonStr);

  // Validar JSON
  StaticJsonDocument<384> inDoc;
  DeserializationError err = deserializeJson(inDoc, jsonStr);
  if (err) {
    Serial.print(F("[ERRO] JSON inválido: "));
    Serial.println(err.c_str());
    return;
  }

  // extrai humidity (prioriza "humidity", fallback "umidade")
  if (!inDoc.containsKey("humidity") && !inDoc.containsKey("umidade")) {
    Serial.println(F("[ERRO] Campo 'humidity' ausente no JSON recebido"));
    return;
  }

  float humidityVal = 0.0;
  if (inDoc.containsKey("humidity")) {
    // aceita inteiro ou float
    if (inDoc["humidity"].is<float>()) humidityVal = inDoc["humidity"].as<float>();
    else humidityVal = (float)inDoc["humidity"].as<long>();
  } else {
    if (inDoc["umidade"].is<float>()) humidityVal = inDoc["umidade"].as<float>();
    else humidityVal = (float)inDoc["umidade"].as<long>();
  }

  bool hasRegando = false;
  bool regandoVal = false;
  if (inDoc.containsKey("regando")) {
    hasRegando = true;
    if (inDoc["regando"].is<bool>()) regandoVal = inDoc["regando"].as<bool>();
    else regandoVal = (inDoc["regando"].as<int>() != 0);
  }

  long rega_pulsos = 0;
  bool hasPulsos = false;
  if (inDoc.containsKey("rega_pulsos")) {
    hasPulsos = true;
    rega_pulsos = inDoc["rega_pulsos"].as<long>();
  }

  float rega_volume_l = 0.0;
  bool hasRegaVolume = false;
  if (inDoc.containsKey("rega_volume_l")) {
    hasRegaVolume = true;
    rega_volume_l = inDoc["rega_volume_l"].is<float>() ? inDoc["rega_volume_l"].as<float>() : (float)inDoc["rega_volume_l"].as<long>();
  }

  float volume_total_l = 0.0;
  bool hasVolTotal = false;
  if (inDoc.containsKey("volume_total_l")) {
    hasVolTotal = true;
    volume_total_l = inDoc["volume_total_l"].is<float>() ? inDoc["volume_total_l"].as<float>() : (float)inDoc["volume_total_l"].as<long>();
  }

  long rega_duracao_s = 0;
  bool hasDuracao = false;
  if (inDoc.containsKey("rega_duracao_s")) {
    hasDuracao = true;
    rega_duracao_s = inDoc["rega_duracao_s"].as<long>();
  }

  unsigned long device_ts_ms = 0;
  bool hasDeviceTs = false;
  if (inDoc.containsKey("device_ts_ms")) {
    hasDeviceTs = true;
    device_ts_ms = inDoc["device_ts_ms"].as<unsigned long>();
  }

  // monta JSON final contendo só os campos "esperados" + opcionais
  StaticJsonDocument<384> outDoc;
  // obtem timestamp ISO (NTP) se possível
  char isoBuf[32];
  bool haveIso = getIsoTimestamp(isoBuf, sizeof(isoBuf));
  if (haveIso) {
    // epoch em ms a partir do time() (NTP)
    unsigned long epochMs = (unsigned long)time(nullptr) * 1000UL;
    outDoc["timestamp"] = epochMs;             // number: epoch ms
    outDoc["timestamp_iso"] = String(isoBuf);  // string ISO (opcional)
  } else {
    // fallback: sem consenso real de hora -> enviar device_ms (millis desde boot)
    unsigned long devMs = millis();
    outDoc["timestamp"] = devMs;     // number: dispositivo pode usar isso (marcador relativo)
    outDoc["device_ts_ms"] = devMs;  // explícito que é tempo do device
    // não incluir timestamp_iso porque não há NTP
  }

  outDoc["humidity"] = humidityVal;
  outDoc["timestamp"] = String(isoBuf);  // ISO string ou epoch-ms string fallback

  if (hasRegando) outDoc["regando"] = regandoVal;
  if (hasPulsos) outDoc["rega_pulsos"] = inDoc["rega_pulsos"].as<long>();
  if (hasRegaVolume) outDoc["rega_volume_l"] = inDoc["rega_volume_l"].is<float>() ? inDoc["rega_volume_l"].as<float>() : (float)inDoc["rega_volume_l"].as<long>();
  if (hasVolTotal) outDoc["volume_total_l"] = inDoc["volume_total_l"].is<float>() ? inDoc["volume_total_l"].as<float>() : (float)inDoc["volume_total_l"].as<long>();
  if (hasDuracao) outDoc["rega_duracao_s"] = inDoc["rega_duracao_s"].as<long>();
  if (hasDeviceTs) outDoc["device_ts_ms"] = device_ts_ms;

  // adiciona metadados do ESP
  outDoc["esp_ip"] = WiFi.localIP().toString();
  outDoc["esp_rssi"] = WiFi.RSSI();

  String payload;
  serializeJson(outDoc, payload);

  Serial.print("[OUT] Payload para backend: ");
  Serial.println(payload);

  trySendOrQueue(payload);
}

void verificarConexaoWiFi() {
  static unsigned long lastCheck = 0;
  if (millis() - lastCheck > 30000) {
    lastCheck = millis();

    bool currentlyConnected = (WiFi.status() == WL_CONNECTED);

    if (wifiConnected && !currentlyConnected) {
      Serial.println(F("[WIFI] Conexão perdida!"));
      wifiConnected = false;
    } else if (!wifiConnected && currentlyConnected) {
      Serial.println(F("[WIFI] Reconectado!"));
      wifiConnected = true;
      flushQueue();
    }

    if (!wifiConnected) {
      WiFi.reconnect();
    }
  }
}

void setup() {
  Serial.begin(ARDUINO_BAUD);
  Serial.setTimeout(10);

  Serial.println();
  Serial.println(F("=== ESP8266 Bridge iniciando ==="));
  Serial.print(F("Conectando WiFi: "));
  Serial.println(ssid);

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && (millis() - start) < WIFI_TIMEOUT_MS) {
    delay(500);
    Serial.print(".");
  }

  wifiConnected = (WiFi.status() == WL_CONNECTED);
  if (wifiConnected) {
    Serial.println();
    Serial.print(F("WiFi OK! IP: "));
    Serial.println(WiFi.localIP());
  } else {
    Serial.println();
    Serial.println(F("Falha WiFi - modo offline ativado"));
  }

  // inicializa NTP para obter hora em UTC
  configTime(0, 0, "pool.ntp.org", "time.google.com");
  Serial.println(F("Inicializando NTP..."));
  unsigned long t0 = millis();
  bool haveTime = false;
  while (millis() - t0 < 10000UL) {  // aguarda até 10s
    if (time(nullptr) > 1600000000UL) {
      haveTime = true;
      break;
    }
    delay(200);
  }
  if (haveTime) Serial.println(F("NTP sincronizado"));
  else Serial.println(F("NTP: sem sincronizacao (fallback para millis)"));

  linhaBuffer.reserve(MAX_LINE_LENGTH);
  Serial.println(F("Aguardando dados do Arduino..."));
}

void loop() {
  verificarConexaoWiFi();

  // Leitura serial com timeout
  unsigned long serialStart = millis();
  while (Serial.available() && (millis() - serialStart) < 100) {
    char c = (char)Serial.read();
    if (c == '\r') continue;
    if (c == '\n') {
      processarLinha(linhaBuffer);
      linhaBuffer = "";
    } else {
      linhaBuffer += c;
      if (linhaBuffer.length() >= MAX_LINE_LENGTH) {
        Serial.println(F("[ERRO] Linha muito longa, descartando"));
        linhaBuffer = "";
      }
    }
  }

  // Tenta esvaziar fila se conectado
  if (WiFi.status() == WL_CONNECTED && !isQueueEmpty() && !enviandoHTTP) {
    flushQueue();
  }

  delay(10);
}
