/*
   Criado por: Leandro F. Moraes
   Rev.: 01
   Data: 16.10.2025
   Observações:
  - ESP8266: Recebe JSON da Serial (Arduino) e envia via HTTP POST
  - Conecte: Arduino TX -> ESP8266 RX (GPIO3)
*/

#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <ESP8266HTTPClient.h>
#include <ArduinoJson.h>

// --- Configurações ---
const char* ssid = "";       //nome da rede Wifi
const char* password = "";   //senha Wifi
const char* serverUrl = "";  //substituir pela URL do servidor HTTP
const unsigned long ARDUINO_BAUD = 9600;
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
bool isQueueEmpty() {
  return qCount == 0;
}
bool isQueueFull() {
  return qCount == MAX_QUEUE;
}

bool enqueuePayload(const char* payload) {
  size_t len = strlen(payload);
  if (len == 0 || len > MAX_PAYLOAD) {
    Serial.println("[QUEUE] Payload inválido/tamanho excede limite");
    return false;
  }

  if (isQueueFull()) {
    Serial.println("[QUEUE] Fila cheia, descartando item mais antigo");
    qHead = (qHead + 1) % MAX_QUEUE;
    qCount--;
  }

  strncpy(queueBuf[qTail], payload, MAX_PAYLOAD);
  queueBuf[qTail][MAX_PAYLOAD] = '\0';
  qTail = (qTail + 1) % MAX_QUEUE;
  qCount++;

  Serial.print("[QUEUE] Enfileirado. tamanho fila = ");
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

bool httpPostJson(const String& jsonPayload) {
  if (WiFi.status() != WL_CONNECTED) {
    wifiConnected = false;
    Serial.println("[HTTP] WiFi desconectado");
    return false;
  }

  if (enviandoHTTP) {
    Serial.println("[HTTP] Tentativa rejeitada");
    return false;
  }

  enviandoHTTP = true;
  bool success = false;

  WiFiClient client;
  HTTPClient http;
  http.setTimeout(HTTP_TIMEOUT_MS);

  if (!http.begin(client, serverUrl)) {
    Serial.println("[HTTP] begin() falhou");
    enviandoHTTP = false;
    return false;
  }

  http.addHeader("Content-Type", "application/json");
  http.addHeader("User-Agent", "ESP8266-Automacao-Rega");

  Serial.print("[HTTP] POST -> ");
  Serial.println(jsonPayload);

  int httpCode = http.POST((uint8_t*)jsonPayload.c_str(), jsonPayload.length());

  if (httpCode > 0) {
    Serial.println("[HTTP] Código - ");
    Serial.println(httpCode);
    if (httpCode >= 200 && httpCode < 300) {
      String resp = http.getString();
      Serial.print("[HTTP] Resposta: ");
      Serial.println(resp);
      success = true;
    } else {
      Serial.print("[HTTP] Código fora de 2xx: ");
      Serial.println(httpCode);
    }
  } else {
    Serial.print("[HTTP] Falha: ");
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
      Serial.println("[QUEUE] Falha ao enfileirar payload");
    }
  }
}

void flushQueue() {
  if (isQueueEmpty()) return;

  Serial.print("[QUEUE] Tentando enviar fila (itens = ");
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
      Serial.println("[QUEUE] Item enviado, removendo da fila");
      dequeuePayload();
      attempts = 0;  // Reset attempts on success
    } else {
      Serial.println("[QUEUE] Falha ao enviar item, tentando novamente...");
      attempts++;
      delay(1000);  // Aguarda 1s antes de retry
    }
  }

  Serial.print("[QUEUE] Fila após flush = ");
  Serial.println(qCount);
}

void processarLinha(String linha) {
  linha.trim();
  if (linha.length() == 0) return;

  if (linha.startsWith(prefixo)) {
    String jsonStr = linha.substring(prefixo.length());
    jsonStr.trim();

    Serial.print("[RX] Dados recebidos: ");
    Serial.println(jsonStr);

    // Valida JSON com documento menor
    StaticJsonDocument<384> doc;
    DeserializationError error = deserializeJson(doc, jsonStr);

    if (error) {
      Serial.print("[ERRO] JSON inválido: ");
      Serial.println(error.c_str());
      return;
    }

    if (!doc.containsKey("umidade") || !doc.containsKey("regando")) {
      Serial.println("[ERRO] JSON sem campos obrigatórios");
      return;
    }

    // Adiciona info do ESP
    doc["esp_ip"] = WiFi.localIP().toString();
    doc["esp_rssi"] = WiFi.RSSI();
    doc["timestamp"] = millis();

    String payload;
    serializeJson(doc, payload);

    trySendOrQueue(payload);
  } else {
    // Linha sem prefixo -> debug do Arduino
    if (linha.length() < 200) {
      Serial.print("[ARDUINO] ");
      Serial.println(linha);
    }
  }
}

void verificarConexaoWiFi() {
  static unsigned long lastCheck = 0;
  if ((unsigned long)(millis() - lastCheck) > 30000) {
    lastCheck = millis();

    bool currentlyConnected = (WiFi.status() == WL_CONNECTED);

    if (wifiConnected && !currentlyConnected) {
      Serial.println("[WIFI] Conexão perdida!");
      wifiConnected = false;
    } else if (!wifiConnected && currentlyConnected) {
      Serial.println("[WIFI] Reconectado!");
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
  Serial.println("=== ESP8266 Bridge com fila de reenvio ===");
  Serial.print("Conectando WiFi: ");
  Serial.println(ssid);

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && (unsigned long)(millis() - start) < WIFI_TIMEOUT_MS) {
    delay(500);
    Serial.print(".");
  }

  wifiConnected = (WiFi.status() == WL_CONNECTED);
  if (wifiConnected) {
    Serial.println();
    Serial.print("WiFi OK! IP: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println();
    Serial.println("Falha WiFi - modo offline ativado");
  }

  linhaBuffer.reserve(MAX_LINE_LENGTH);
  Serial.println("Aguardando dados do Arduino...");
}

void loop() {
  verificarConexaoWiFi();

  // Leitura serial com timeout
  unsigned long serialStart = millis();
  while (Serial.available() && (unsigned long)(millis() - serialStart) < 100) {
    char c = (char)Serial.read();
    if (c == '\r') continue;

    if (c == '\n') {
      processarLinha(linhaBuffer);
      linhaBuffer = "";
    } else {
      linhaBuffer += c;
      if (linhaBuffer.length() >= MAX_LINE_LENGTH) {
        Serial.println("[ERRO] Linha muito longa, descartando");
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
