/* ******************** Sistema de Rega Automática com MQTT ********************
 *  Criado por: Leandro F. Moraes
 *  Versão: 2.03
 *  Data: 30.04.2026
 *  Observações:
 *  - Configure Wi-Fi, broker MQTT, usuário e senha.
 *  - Calibre SENSOR_DRY e SENSOR_WET com seu solo.
 *  - Relé NC ativo-LOW: VALVE_ON=HIGH, VALVE_OFF=LOW (adequado para módulo JQC3F-05VDC-C)
 *  - Durante o boot, o pino do relé pode flutuar; se flutuar HIGH, a válvula pode abrir brevemente.
 *    O código força VALVE_OFF imediatamente após o pinMode para minimizar esse efeito.
 *
 *  DEPENDÊNCIAS:
 *    - PubSubClient       (Nick O'Leary)          >= 2.8
 *    - ArduinoJson        (Benoit Blanchon)        >= 6.x
 *    - LiquidCrystal I2C  (Frank de Brabander)    >= 1.1.2
 *
 *  ARQUITETURA:
 *    ESP32 → MQTT → integration-service → backend
 *    O ESP32 NUNCA decide irrigar – aguarda comando do backend.
 *
 *  PINAGEM:
 *    GPIO 26 -> Relé solenoide (HIGH = válvula aberta / LOW = fechada)
 *    GPIO 34 -> Sensor umidade HW-103 (ADC)
 *    GPIO 27 -> Sensor fluxo YF-S201 (interrupção FALLING)
 *    GPIO 21 -> SDA I2C (LCD)
 *    GPIO 22 -> SCL I2C (LCD)
 * ============================================================
 */

#include <WiFi.h>
#include <stdarg.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <LiquidCrystal_I2C.h>
#include <esp_task_wdt.h>
#include <esp_mac.h>
#include <esp_idf_version.h>
#include <math.h>

// CONFIGURAÇÕES
namespace Config {
constexpr const char* WIFI_SSID = "";
constexpr const char* WIFI_PASSWORD = "";
constexpr const char* MQTT_HOST = "192.168.8.11";
constexpr uint16_t MQTT_PORT = 1883;
constexpr const char* MQTT_USER = "";
constexpr const char* MQTT_PASS = "";
constexpr uint32_t WDT_TIMEOUT_S = 30;
}

// PINAGEM
namespace Pino {
constexpr uint8_t VALVE = 26;
constexpr uint8_t SOIL_SENSOR = 34;
constexpr uint8_t FLOW_SENSOR = 27;
constexpr uint8_t SDA = 21;
constexpr uint8_t SCL = 22;
}
// Lógica do relé para módulo normalmente fechado (NC) ativo LOW:
//   HIGH => bobina desligada => contato NC fecha => solenoide ativa => válvula ABRE.
//   LOW  => bobina energizada => contato NC abre => solenoide desativa => válvula FECHA.
// Se o módulo for normalmente aberto (NA), troque os valores abaixo.
constexpr uint8_t VALVE_ON = LOW;    // abre a válvula
constexpr uint8_t VALVE_OFF = HIGH;  // fecha a válvula

//  CALIBRAÇÃO
constexpr int SENSOR_DRY = 2800;       // valor ADC com solo seco
constexpr int SENSOR_WET = 1100;       // valor ADC com solo úmido
constexpr float FLOW_FACTOR = 450.0f;  // pulsos por litro (YF-S201)

//  INTERVALOS DE TEMPO (ms)
namespace Intervalo {
constexpr uint32_t LEITURA = 1000UL;         // leitura do sensor (1s)
constexpr uint32_t RELATORIO = 3600000UL;    // publicação periódica (1h)
constexpr uint32_t STATUS = 1800000UL;       // heartbeat de status (30min)
constexpr uint32_t WIFI_RETRY = 10000UL;     // reconexão WiFi
constexpr uint32_t MQTT_RETRY = 5000UL;      // reconexão MQTT
constexpr uint32_t REGA_TIMEOUT = 600000UL;  // timeout de segurança (10min)
constexpr uint32_t LCD_DEVICEID = 2000UL;    // exibe deviceId no boot
}

//  OBJETOS GLOBAIS
WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);
LiquidCrystal_I2C lcd(0x27, 16, 2);

char idDispositivo[20];
char topicoStatus[64];
char topicoLeitura[64];
char topicoComando[64];
char ultimoIp[16] = "";

uint32_t ultimaTentativaWifi = 0;
uint32_t ultimaTentativaMqtt = 0;
uint32_t ultimaLeituraMs = 0;
uint32_t ultimoRelatorioMs = 0;
uint32_t ultimoStatusMs = 0;
float umidadeAtual = 0.0f;
volatile uint32_t contagemPulsos = 0;
bool regando = false;
float pararEm = 0.0f;
uint32_t inicioRega = 0;
float volumeTotal = 0.0f;
uint32_t ultimosPulsosRega = 0;
float ultimoVolumeRega = 0.0f;
uint32_t ultimaDuracaoRega = 0;

//  PROTÓTIPO
void calcularIdDispositivo();
void construirTopicos();
bool conectarWiFi();
bool conectarMQTT();
void manterConexoes();
void atualizarIp();
float lerUmidade();
float lerUmidadeInicial();
void publicarStatus();
void publicarLeitura(bool regandoAgora, uint32_t pulsos, float volume, uint32_t duracao);
void iniciarRega(float stopHumidity);
void pararRega(const char* motivo);
void atualizarDisplay();
void lcdExibir(uint8_t col, uint8_t row, const char* fmt, ...);
void callbackMQTT(char* topic, byte* payload, unsigned int length);
void IRAM_ATTR isrFluxo();

//  IMPLEMENTAÇÕES
// Interrupção do sensor de fluxo: incrementa contagem de pulsos
void IRAM_ATTR isrFluxo() {
  contagemPulsos++;
}

// Gera ID único do dispositivo a partir do MAC WiFi
void calcularIdDispositivo() {
  uint8_t mac[6];
  esp_read_mac(mac, ESP_MAC_WIFI_STA);
  snprintf(idDispositivo, sizeof(idDispositivo), "esp-%02x%02x%02x", mac[3], mac[4], mac[5]);
}

// Constrói os tópicos MQTT usados pelo dispositivo
void construirTopicos() {
  snprintf(topicoStatus, sizeof(topicoStatus), "smartgarden/devices/%s/status", idDispositivo);
  snprintf(topicoLeitura, sizeof(topicoLeitura), "smartgarden/devices/%s/reading", idDispositivo);
  snprintf(topicoComando, sizeof(topicoComando), "smartgarden/devices/%s/command", idDispositivo);
}

// Exibe texto no LCD com padding de espaços para evitar resíduos
void lcdExibir(uint8_t col, uint8_t row, const char* fmt, ...) {
  char buf[17];
  va_list args;
  va_start(args, fmt);
  vsnprintf(buf, sizeof(buf), fmt, args);
  va_end(args);
  int len = strlen(buf);
  while (len < 16 - col) buf[len++] = ' ';
  buf[16 - col] = '\0';
  lcd.setCursor(col, row);
  lcd.print(buf);
}

// Atualiza a string global ultimoIp com o IP atual (formato textual)
void atualizarIp() {
  if (!WiFi.isConnected()) {
    ultimoIp[0] = '\0';
    return;
  }
  IPAddress ip = WiFi.localIP();
  snprintf(ultimoIp, sizeof(ultimoIp), "%u.%u.%u.%u", ip[0], ip[1], ip[2], ip[3]);
}

// Conecta ao Wi‑Fi (bloqueante no setup, usa reconexão automática depois)
bool conectarWiFi() {
  if (WiFi.isConnected()) return true;
  WiFi.persistent(false);
  WiFi.setAutoReconnect(true);
  WiFi.mode(WIFI_STA);
  WiFi.begin(Config::WIFI_SSID, Config::WIFI_PASSWORD);
  Serial.printf("[WiFi] Conectando a %s", Config::WIFI_SSID);
  uint32_t start = millis();
  while (!WiFi.isConnected() && millis() - start < 20000UL) {
    delay(500);
    Serial.print('.');
    esp_task_wdt_reset();
  }
  Serial.println();
  if (WiFi.isConnected()) {
    atualizarIp();
    Serial.printf("[WiFi] Conectado. IP: %s | RSSI: %d\n", ultimoIp, WiFi.RSSI());
    return true;
  }
  Serial.println("[WiFi] Falha na conexão.");
  return false;
}

// Conecta ao broker MQTT
bool conectarMQTT() {
  if (mqttClient.connected()) return true;
  const char lwtPayload[] = "{\"ip\":\"\",\"rssi\":0,\"online\":false}";
  char clientId[32];
  snprintf(clientId, sizeof(clientId), "smartgarden-%s", idDispositivo);
  bool ok;
  if (strlen(Config::MQTT_USER) > 0) {
    ok = mqttClient.connect(clientId, Config::MQTT_USER, Config::MQTT_PASS,
                            topicoStatus, 1, true, lwtPayload);
  } else {
    ok = mqttClient.connect(clientId, nullptr, nullptr,
                            topicoStatus, 1, true, lwtPayload);
  }
  if (ok) {
    Serial.println("[MQTT] Conectado.");
    if (mqttClient.subscribe(topicoComando, 1))
      Serial.printf("[MQTT] Subscrito: %s\n", topicoComando);
    else
      Serial.println("[MQTT] Falha ao subscrever topicoComando.");
    publicarStatus();
  } else {
    Serial.printf("[MQTT] Falha. Estado: %d\n", mqttClient.state());
  }
  return ok;
}

// Reconecta Wi‑Fi e MQTT quando necessário
void manterConexoes() {
  uint32_t agora = millis();
  if (!WiFi.isConnected()) {
    if (agora - ultimaTentativaWifi >= Intervalo::WIFI_RETRY) {
      ultimaTentativaWifi = agora;
      Serial.println("[WiFi] Desconectado – tentando reconectar...");
      lcdExibir(0, 0, "WiFi perdido");
      lcdExibir(0, 1, "Reconectando...");
      WiFi.reconnect();
    }
    return;
  }
  if (!mqttClient.connected()) {
    if (agora - ultimaTentativaMqtt >= Intervalo::MQTT_RETRY) {
      ultimaTentativaMqtt = agora;
      Serial.println("[MQTT] Desconectado – reconectando...");
      conectarMQTT();
    }
  }
}

// Leitura do sensor de umidade com média de 5 amostras (delay não‑bloqueante curto)
float lerUmidade() {
  const int amostras = 5;
  long soma = 0;
  for (int i = 0; i < amostras; i++) {
    soma += analogRead(Pino::SOIL_SENSOR);
    delayMicroseconds(500);
  }
  long adcMedio = soma / amostras;
  long pct = map(adcMedio, SENSOR_DRY, SENSOR_WET, 0, 100);
  pct = constrain(pct, 0, 100);
  Serial.printf("[Sensor] ADC: %ld → Umidade: %ld %%\n", adcMedio, pct);
  return (float)pct;
}

// Garante valores corretos antes do loop
float lerUmidadeInicial() {
  const int amostras = 5;
  long soma = 0;
  for (int i = 0; i < amostras; i++) {
    soma += analogRead(Pino::SOIL_SENSOR);
    delayMicroseconds(500);
  }
  long adcMedio = soma / amostras;
  long pct = map(adcMedio, SENSOR_DRY, SENSOR_WET, 0, 100);
  pct = constrain(pct, 0, 100);
  Serial.printf("[Sensor] Leitura inicial: ADC: %ld → Umidade: %ld %%\n", adcMedio, pct);
  return (float)pct;
}

// Publica o status do dispositivo
void publicarStatus() {
  if (!mqttClient.connected()) return;
  atualizarIp();
  StaticJsonDocument<128> doc;
  doc["ip"] = ultimoIp;
  doc["rssi"] = WiFi.RSSI();
  doc["online"] = true;
  char buf[128];
  serializeJson(doc, buf);
  mqttClient.publish(topicoStatus, buf, true);
  Serial.printf("[MQTT] Status: %s\n", buf);
}

// Publica uma leitura completa (periódica ou ao fim da rega)
void publicarLeitura(bool regandoAgora, uint32_t pulsos, float volume, uint32_t duracao) {
  if (!mqttClient.connected()) return;
  char ipBuf[16];
  IPAddress ip = WiFi.localIP();
  snprintf(ipBuf, sizeof(ipBuf), "%u.%u.%u.%u", ip[0], ip[1], ip[2], ip[3]);
  StaticJsonDocument<256> doc;
  doc["humidity"] = (int)umidadeAtual;
  doc["device_ts_ms"] = (uint32_t)millis();
  doc["regando"] = regandoAgora;
  doc["rega_pulsos"] = pulsos;
  doc["rega_volume_l"] = roundf(volume * 1000.0f) / 1000.0f;
  doc["volume_total_l"] = roundf(volumeTotal * 1000.0f) / 1000.0f;
  doc["rega_duracao_s"] = duracao;
  doc["esp_ip"] = ipBuf;
  doc["esp_rssi"] = WiFi.RSSI();
  char buf[256];
  serializeJson(doc, buf);
  mqttClient.publish(topicoLeitura, buf, false);
  Serial.printf("[MQTT] Leitura: %s\n", buf);
}

// Inicia a rega: abre válvula, zera pulsos e publica início da sessão
void iniciarRega(float stopHumidity) {
  if (regando) return;
  pararEm = stopHumidity;
  inicioRega = millis();
  noInterrupts();
  contagemPulsos = 0;
  interrupts();
  digitalWrite(Pino::VALVE, VALVE_ON);
  regando = true;
  Serial.printf("[Rega] INICIADA. Parar quando umidade >= %.1f %%\n", pararEm);
  // publicarLeitura(true, 0, 0.0f, 0);
  publicarLeitura(true, ultimosPulsosRega, ultimoVolumeRega, ultimaDuracaoRega);
}

// Finaliza a rega: atualiza métricas, fecha válvula e publica resultado
void pararRega(const char* motivo) {
  if (!regando) return;
  uint32_t pulsosSessao;
  noInterrupts();
  pulsosSessao = contagemPulsos;
  interrupts();
  ultimosPulsosRega = pulsosSessao;
  ultimoVolumeRega = (FLOW_FACTOR > 0.0f) ? (float)pulsosSessao / FLOW_FACTOR : 0.0f;
  volumeTotal += ultimoVolumeRega;
  ultimaDuracaoRega = (millis() - inicioRega) / 1000UL;
  digitalWrite(Pino::VALVE, VALVE_OFF);
  regando = false;
  Serial.printf("[Rega] PARADA (%s). Pulsos:%u Volume:%.3fL Duração:%us Total:%.3fL\n",
                motivo, ultimosPulsosRega, ultimoVolumeRega, ultimaDuracaoRega, volumeTotal);
  publicarLeitura(false, ultimosPulsosRega, ultimoVolumeRega, ultimaDuracaoRega);
}

// Atualiza o LCD com umidade atual e estado do dispositivo
void atualizarDisplay() {
  // Linha 0: umidade sempre atualizada a cada ciclo de leitura
  lcdExibir(0, 0, "Umid: %3d%%", (int)umidadeAtual);

  // Linha 1: varia conforme o estado
  if (regando) {
    uint32_t pulsosAgora;
    noInterrupts();
    pulsosAgora = contagemPulsos;
    interrupts();

    float volumeAtual = (FLOW_FACTOR > 0.0f)
                          ? (float)pulsosAgora / FLOW_FACTOR
                          : 0.0f;

    lcdExibir(0, 1, "V:%5.2fL S:%3d", volumeAtual, (int)pararEm);
  } else if (WiFi.isConnected()) {
    lcdExibir(0, 1, "%s", ultimoIp);
  } else {
    lcdExibir(0, 1, "Sem WiFi");
  }
}

// Callback das mensagens MQTT recebidas – processa apenas comando IRRIGATE
void callbackMQTT(char* topic, byte* payload, unsigned int length) {
  if (length == 0 || length >= 512) return;
  if (strcmp(topic, topicoComando) != 0) return;
  char buf[512];
  memcpy(buf, payload, length);
  buf[length] = '\0';
  StaticJsonDocument<512> doc;
  DeserializationError err = deserializeJson(doc, buf);
  if (err) return;
  const char* type = doc["type"] | "";
  if (strcmp(type, "IRRIGATE") != 0) return;
  if (regando) return;
  if (!doc.containsKey("stopAtHumidity")) return;
  float stop = doc["stopAtHumidity"];
  if (stop < 1 || stop > 100) return;
  iniciarRega(stop);
}

// Configuração inicial
void setup() {
  Serial.begin(115200);
  Serial.println("\n=== SmartGarden Boot ===");

  // Watchdog: reinicia se o loop travar
#if ESP_IDF_VERSION_MAJOR >= 5
  esp_task_wdt_config_t wdt_config = { .timeout_ms = Config::WDT_TIMEOUT_S * 1000, .trigger_panic = true };
  esp_task_wdt_init(&wdt_config);
#else
  esp_task_wdt_init(Config::WDT_TIMEOUT_S, true);
#endif
  esp_task_wdt_add(NULL);

  Wire.begin(Pino::SDA, Pino::SCL);
  lcd.init();
  lcd.backlight();
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("SmartGarden");
  lcd.setCursor(0, 1);
  lcd.print("Inicializando...");

  // Inicializa pino do relé e garante válvula fechada
  pinMode(Pino::VALVE, OUTPUT);
  digitalWrite(Pino::VALVE, VALVE_OFF);
  pinMode(Pino::SOIL_SENSOR, INPUT);
  pinMode(Pino::FLOW_SENSOR, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(Pino::FLOW_SENSOR), isrFluxo, FALLING);

  calcularIdDispositivo();
  construirTopicos();
  Serial.printf("deviceId: %s\n", idDispositivo);
  lcd.setCursor(0, 1);
  lcd.print("Conectando WiFi ");
  conectarWiFi();
  mqttClient.setServer(Config::MQTT_HOST, Config::MQTT_PORT);
  mqttClient.setCallback(callbackMQTT);
  mqttClient.setBufferSize(512);
  conectarMQTT();

  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Device ID:");
  lcd.setCursor(0, 1);
  lcd.print(idDispositivo);
  delay(Intervalo::LCD_DEVICEID);
  esp_task_wdt_reset();

  umidadeAtual = lerUmidadeInicial();
  publicarLeitura(false, ultimosPulsosRega, ultimoVolumeRega, ultimaDuracaoRega);
  uint32_t agora = millis();
  ultimaLeituraMs = agora;
  ultimoRelatorioMs = agora;
  ultimoStatusMs = agora;
  Serial.println("Boot concluído.\n");
}

// Loop principal – não‑bloqueante
void loop() {
  uint32_t agora = millis();
  manterConexoes();
  if (mqttClient.connected()) mqttClient.loop();

  // Leitura periódica do sensor (1s)
  if (agora - ultimaLeituraMs >= Intervalo::LEITURA) {
    ultimaLeituraMs = agora;
    umidadeAtual = lerUmidade();
    atualizarDisplay();
  }

  // Verifica condições de parada da rega
  if (regando) {
    if (umidadeAtual >= pararEm) {
      pararRega("HUMIDITY_TARGET");
    } else if (millis() - inicioRega >= Intervalo::REGA_TIMEOUT) {
      pararRega("TIMEOUT");
    }
  }

  // Publicação periódica de leitura (1h)
  if (agora - ultimoRelatorioMs >= Intervalo::RELATORIO) {
    ultimoRelatorioMs = agora;
    publicarLeitura(regando, ultimosPulsosRega, ultimoVolumeRega, ultimaDuracaoRega);
  }

  // Verifica mudança de IP e heartbeat de status
  if (WiFi.isConnected()) {
    char ipAtual[16];
    IPAddress ip = WiFi.localIP();
    snprintf(ipAtual, sizeof(ipAtual), "%u.%u.%u.%u", ip[0], ip[1], ip[2], ip[3]);
    bool ipMudou = (strcmp(ipAtual, ultimoIp) != 0);
    if (ipMudou || (agora - ultimoStatusMs >= Intervalo::STATUS)) {
      ultimoStatusMs = agora;
      publicarStatus();
    }
  } else {
    if (agora - ultimoStatusMs >= Intervalo::STATUS) {
      ultimoStatusMs = agora;
      publicarStatus();
    }
  }

  esp_task_wdt_reset();
}
