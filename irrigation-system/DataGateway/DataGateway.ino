/*
   Criado por: Leandro F. Moraes
   Rev.: 03
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
#define ARDUINO_BAUD 9600
#define SERVER_URL "";

const char* WIFI_SSID = "";
const char* WIFI_PASS = "";

const unsigned long WIFI_TIMEOUT_MS = 30000;
const unsigned long HTTP_TIMEOUT_MS = 10000;
const unsigned long QUEUE_FLUSH_INTERVAL = 60000;  // 1 minuto
const unsigned long NTP_TIMEOUT_MS = 10000;

const size_t MAX_LINE_LENGTH = 384;
const uint8_t MAX_QUEUE_SIZE = 8;
const size_t MAX_PAYLOAD_SIZE = 384;
// --------------------------------

String inputBuffer;
const String DATA_PREFIX = "#DATA#";

struct Queue {
  char data[MAX_QUEUE_SIZE][MAX_PAYLOAD_SIZE + 1];
  uint8_t head = 0;
  uint8_t tail = 0;
  uint8_t count = 0;
};

Queue messageQueue;
bool isSendingHTTP = false;
bool wifiConnected = false;
unsigned long lastQueueFlush = 0;

// protótipos
bool wifiConnect();
bool syncNTPTime();
bool sendHTTPPost(const String& payload);
void addToQueue(const char* payload);
bool processQueue();
void processSerialData();
void parseSensorData(const String& jsonStr);
unsigned long getEpochMsOrMillis(bool& haveEpoch, char* isoBuf, size_t isoBufLen);

// -------------------------

void setup() {
  Serial.begin(ARDUINO_BAUD);
  Serial.setTimeout(10);

  Serial.println();
  Serial.println("=== ESP8266 Gateway Iniciando ===");

  // Conectar WiFi
  wifiConnected = wifiConnect();

  // Sincronizar NTP
  if (wifiConnected) {
    Serial.println("Sincronizando tempo NTP...");
    if (syncNTPTime()) {
      Serial.println("✓ NTP sincronizado");
    } else {
      Serial.println("✗ Falha NTP - usando tempo local");
    }
  }

  inputBuffer.reserve(MAX_LINE_LENGTH);
  Serial.println("✓ Sistema pronto - Aguardando dados...");
}

void loop() {
  static unsigned long lastWifiCheck = 0;
  const unsigned long WIFI_CHECK_INTERVAL = 30000;

  // Verificar WiFi periodicamente
  if (millis() - lastWifiCheck > WIFI_CHECK_INTERVAL) {
    lastWifiCheck = millis();
    bool currentStatus = (WiFi.status() == WL_CONNECTED);

    if (wifiConnected != currentStatus) {
      wifiConnected = currentStatus;
      if (wifiConnected) {
        Serial.println("✓ WiFi reconectado");
        processQueue();  // Tentar enviar fila
      } else {
        Serial.println("✗ WiFi desconectado");
        WiFi.reconnect();
      }
    }
  }

  // Processar dados serial
  processSerialData();

  // Processar fila periodicamente
  if (wifiConnected && messageQueue.count > 0 && !isSendingHTTP) {
    if (millis() - lastQueueFlush > QUEUE_FLUSH_INTERVAL) {
      processQueue();
      lastQueueFlush = millis();
    }
  }

  delay(5);  // Delay mínimo para estabilidade
}

// --- wifi / ntp ---

bool wifiConnect() {
  Serial.print(F("Conectando WiFi: "));
  Serial.println(WIFI_SSID);

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASS);

  unsigned long startTime = millis();
  while (WiFi.status() != WL_CONNECTED && (millis() - startTime) < WIFI_TIMEOUT_MS) {
    delay(500);
    Serial.print(".");
  }

  bool connected = (WiFi.status() == WL_CONNECTED);
  if (connected) {
    Serial.println();
    Serial.print("✓ WiFi conectado - IP: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println();
    Serial.println("✗ Falha na conexão WiFi");
  }

  return connected;
}

bool syncNTPTime() {
  configTime(0, 0, "pool.ntp.org", "time.nist.gov", "time.google.com");

  unsigned long startTime = millis();
  while (millis() - startTime < NTP_TIMEOUT_MS) {
    time_t now = time(nullptr);
    if (now > 1600000000UL) {
      return true;
    }
    delay(200);
  }
  return false;
}

// --- HTTP / fila ---

bool sendHTTPPost(const String& payload) {
  if (WiFi.status() != WL_CONNECTED || isSendingHTTP) {
    return false;
  }

  isSendingHTTP = true;
  bool success = false;

  WiFiClient client;
  HTTPClient http;

  http.setTimeout(HTTP_TIMEOUT_MS);
  http.setReuse(true);

  if (http.begin(client, SERVER_URL)) {
    http.addHeader("Content-Type", "application/json");
    http.addHeader("User-Agent", "ESP8266-Irrigation-Gateway");

    int httpCode = http.POST(payload);

    if (httpCode > 0) {
      if (httpCode >= 200 && httpCode < 300) {
        Serial.println("✓ Dados enviados com sucesso");
        success = true;
      } else {
        Serial.print("✗ HTTP Error: ");
        Serial.println(httpCode);
        String resp = http.getString();
        Serial.print(F("✗ Body: "));
        Serial.println(resp);
      }
    } else {
      Serial.print("✗ HTTP Failed: ");
      Serial.println(http.errorToString(httpCode));
    }

    http.end();
  } else {
    Serial.println("✗ HTTP begin() falhou");
  }

  isSendingHTTP = false;
  return success;
}

void addToQueue(const char* payload) {
  size_t len = strlen(payload);
  if (len == 0 || len > MAX_PAYLOAD_SIZE) return;

  if (messageQueue.count >= MAX_QUEUE_SIZE) {
    // remove mais antigo
    messageQueue.head = (messageQueue.head + 1) % MAX_QUEUE_SIZE;
    messageQueue.count--;
    Serial.println("⚠ Fila cheia - removendo mensagem mais antiga");
  }
  strncpy(messageQueue.data[messageQueue.tail], payload, MAX_PAYLOAD_SIZE);
  messageQueue.data[messageQueue.tail][MAX_PAYLOAD_SIZE] = '\0';
  messageQueue.tail = (messageQueue.tail + 1) % MAX_QUEUE_SIZE;
  messageQueue.count++;

  Serial.print(" Mensagem enfileirada (Total: ");
  Serial.print(messageQueue.count);
  Serial.println(")");
}

bool processQueue() {
  if (messageQueue.count == 0 || isSendingHTTP) return true;

  Serial.print(" Processando fila (");
  Serial.print(messageQueue.count);
  Serial.println(" itens)");

  uint8_t attempts = 0;
  const uint8_t MAX_ATTEMPTS = 3;

  while (messageQueue.count > 0 && WiFi.status() == WL_CONNECTED) {
    char* payload = messageQueue.data[messageQueue.head];
    if (sendHTTPPost(String(payload))) {
      // enviado -> remover
      messageQueue.head = (messageQueue.head + 1) % MAX_QUEUE_SIZE;
      messageQueue.count--;
      attempts = 0;
    } else {
      attempts++;
      if (attempts >= MAX_ATTEMPTS) {
        Serial.println("✗ Muitas falhas no envio; abortando flush por agora");
        break;
      }
      delay(1000);
    }
  }

  Serial.print(" Fila restante: ");
  Serial.println(messageQueue.count);
  return (messageQueue.count == 0);
}

// --- Serial / parsing ---

void processSerialData() {
  static unsigned long lastByteTime = 0;
  const unsigned long SERIAL_TIMEOUT = 100;

  while (Serial.available()) {
    char c = (char)Serial.read();
    lastByteTime = millis();
    if (c == '\r') continue;

    if (c == '\n') {
      if (inputBuffer.length() > 0) {
        inputBuffer.trim();
        if (inputBuffer.startsWith(DATA_PREFIX)) {
          String jsonData = inputBuffer.substring(DATA_PREFIX.length());
          jsonData.trim();
          parseSensorData(jsonData);
        } else if (inputBuffer.length() < 200) {
          Serial.print("📟 Arduino: ");
          Serial.println(inputBuffer);
        }
        inputBuffer = "";
      }
    } else {
      inputBuffer += c;
      if (inputBuffer.length() >= MAX_LINE_LENGTH) {
        Serial.println("⚠ Linha muito longa - descartando");
        inputBuffer = "";
      }
    }
  }

  if (inputBuffer.length() > 0 && (millis() - lastByteTime) > SERIAL_TIMEOUT) {
    Serial.println("⚠ Timeout serial - descartando linha incompleta");
    inputBuffer = "";
  }
}

void parseSensorData(const String& jsonStr) {
  StaticJsonDocument<384> inputDoc;
  DeserializationError error = deserializeJson(inputDoc, jsonStr);
  if (error) {
    Serial.print("✗ JSON inválido: ");
    Serial.println(error.c_str());
    return;
  }

  // obrigatorio
  if (!inputDoc.containsKey("humidity")) {
    Serial.println("✗ Campo 'humidity' não encontrado");
    return;
  }

  StaticJsonDocument<512> outDoc;

  // timestamp: prefer Epoch ms (NTP), fallback device millis()
  char isoBuf[32];
  bool haveEpoch = false;
  unsigned long ts = getEpochMsOrMillis(haveEpoch, isoBuf, sizeof(isoBuf));
  outDoc["timestamp"] = ts;  // ALWAYS number
  if (haveEpoch) outDoc["timestamp_iso"] = String(isoBuf);
  else outDoc["device_ts_ms"] = ts;

  // humidity (força float)
  if (inputDoc["humidity"].is<float>()) outDoc["humidity"] = inputDoc["humidity"].as<float>();
  else outDoc["humidity"] = inputDoc["humidity"].as<long>();

  // campos opcionais com conversões explícitas
  if (inputDoc.containsKey("regando")) {
    // aceita 0/1 ou boolean
    bool r = false;
    if (inputDoc["regando"].is<bool>()) r = inputDoc["regando"].as<bool>();
    else r = (inputDoc["regando"].as<int>() != 0);
    outDoc["regando"] = r;
  }
  if (inputDoc.containsKey("rega_pulsos")) outDoc["rega_pulsos"] = inputDoc["rega_pulsos"].as<unsigned long>();
  if (inputDoc.containsKey("rega_volume_l")) {
    if (inputDoc["rega_volume_l"].is<float>()) outDoc["rega_volume_l"] = inputDoc["rega_volume_l"].as<float>();
    else outDoc["rega_volume_l"] = inputDoc["rega_volume_l"].as<long>();
  }
  if (inputDoc.containsKey("volume_total_l")) {
    if (inputDoc["volume_total_l"].is<float>()) outDoc["volume_total_l"] = inputDoc["volume_total_l"].as<float>();
    else outDoc["volume_total_l"] = inputDoc["volume_total_l"].as<long>();
  }
  if (inputDoc.containsKey("rega_duracao_s")) outDoc["rega_duracao_s"] = inputDoc["rega_duracao_s"].as<unsigned long>();
  if (inputDoc.containsKey("device_ts_ms")) outDoc["device_ts_ms"] = inputDoc["device_ts_ms"].as<unsigned long>();

  // metadados do gateway
  outDoc["esp_ip"] = WiFi.localIP().toString();
  outDoc["esp_rssi"] = WiFi.RSSI();
  outDoc["gateway_ts_ms"] = millis();

  String payload;
  serializeJson(outDoc, payload);
  Serial.print(" Enviando: ");
  Serial.println(payload);

  if (!sendHTTPPost(payload)) addToQueue(payload.c_str());
}

unsigned long getEpochMsOrMillis(bool& haveEpoch, char* isoBuf, size_t isoBufLen) {
  time_t now = time(nullptr);
  if (now > 1600000000UL) {
    struct tm t;
    gmtime_r(&now, &t);
    strftime(isoBuf, isoBufLen, "%Y-%m-%dT%H:%M:%SZ", &t);
    haveEpoch = true;
    return (unsigned long)now * 1000UL;
  } else {
    haveEpoch = false;
    return millis();
  }
}
