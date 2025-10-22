/* ******************** Sistema de Rega Automática ********************
   Criado por: Leandro F. Moraes
   Rev.: 02a
   Data: 14.10.2025
   Observações:
   - Ajuste UMI_SENSOR_MIN/UMI_SENSOR_MAX para calibrar o seu sensor.
   - Ajuste RELAY_ACTIVE_LOW dependendo do seu módulo relé (true se o relé fecha quando pino = LOW).
*******************************************************************************/

#include <Wire.h>
#include <LiquidCrystal_I2C.h>

#define VALVE_PIN 10
#define SOIL_SENSOR_PIN A0
#define FLOW_SENSOR_PIN 2

const int DRY_THRESHOLD = 20;                 // %
const unsigned long WATERING_TIME_MS = 5000;  // 5s
const uint8_t SAMPLE_COUNT = 5;

const int SENSOR_MIN = 10;
const int SENSOR_MAX = 1000;

const bool RELAY_ACTIVE_LOW = true;
const float FLOW_FACTOR = 450.0;  // pulsos por litro

LiquidCrystal_I2C lcd(0x27, 16, 2);

volatile unsigned long pulseCount = 0;

bool watering = false;
unsigned long wateringStart = 0;
unsigned long lastReading = 0;
unsigned long lastReport = 0;

unsigned long sessionPulses = 0;
float totalVolume = 0.0;
float lastVolume = 0.0;
unsigned long lastDuration = 0;
int currentHumidity = 100;

const unsigned long READING_INTERVAL = 1000;
const unsigned long REPORT_INTERVAL = 3600000;  // 1 hora

void pulseCounter() {
  pulseCount++;
}

void enviarJSON(bool isWatering, int humidityPct, unsigned long pulsesSessao, float volumeSessao, float volumeTotal, unsigned long duracaoSessao_s) {
  char volSessBuf[12];
  char volTotBuf[12];
  dtostrf(volumeSessao, 6, 3, volSessBuf);
  dtostrf(volumeTotal, 6, 3, volTotBuf);

  unsigned long device_ts = millis();

  char jsonBuf[300];
  // regando enviado como true/false (booleano JSON)
  snprintf(jsonBuf, sizeof(jsonBuf),
           "#DATA#{\"humidity\":%d,\"device_ts_ms\":%lu,\"regando\":%s,"
           "\"rega_pulsos\":%lu,\"rega_volume_l\":%s,\"volume_total_l\":%s,"
           "\"rega_duracao_s\":%lu}",
           humidityPct,
           device_ts,
           isWatering ? "true" : "false",
           pulsesSessao,
           volSessBuf,
           volTotBuf,
           duracaoSessao_s);

  Serial.println(jsonBuf);
}

int lerUmidadeMedia(int n) {
  long soma = 0;
  for (int i = 0; i < n; i++) {
    soma += analogRead(SOIL_SENSOR_PIN);
    delay(8);
  }
  int media = (int)(soma / n);
  long pct = map(media, SENSOR_MIN, SENSOR_MAX, 100, 0);
  pct = constrain(pct, 0, 100);
  return (int)pct;
}

void startWatering() {
  watering = true;
  wateringStart = millis();
  noInterrupts();
  pulseCount = 0;
  interrupts();
  digitalWrite(VALVE_PIN, RELAY_ACTIVE_LOW ? LOW : HIGH);
}

void stopWatering() {
  noInterrupts();
  sessionPulses = pulseCount;
  interrupts();
  lastVolume = sessionPulses / FLOW_FACTOR;
  totalVolume += lastVolume;
  lastDuration = (millis() - wateringStart) / 1000UL;
  digitalWrite(VALVE_PIN, RELAY_ACTIVE_LOW ? HIGH : LOW);
  watering = false;
}

void atualizarDisplay(int humidity) {
  char line0[17] = { 0 }, line1[17] = { 0 };
  if (watering) {
    unsigned long currentPulses;
    noInterrupts();
    currentPulses = pulseCount;
    interrupts();
    float currentVolume = currentPulses / FLOW_FACTOR;
    char volBuf[8];
    dtostrf(currentVolume, 4, 2, volBuf);
    unsigned long elapsed = (millis() - wateringStart) / 1000;
    unsigned long remaining = (WATERING_TIME_MS / 1000 > elapsed) ? (WATERING_TIME_MS / 1000 - elapsed) : 0;
    snprintf(line0, sizeof(line0), "Regando V:%sL", volBuf);
    snprintf(line1, sizeof(line1), "Umid:%3d%% T:%2lus", humidity, remaining);
  } else {
    char totalBuf[8];
    dtostrf(totalVolume, 4, 2, totalBuf);
    snprintf(line0, sizeof(line0), "Estado: IDLE V:%sL", totalBuf);
    snprintf(line1, sizeof(line1), "Umid:%3d%%       ", humidity);
  }
  // preencher até 16 chars
  size_t l0 = strlen(line0);
  size_t l1 = strlen(line1);
  if (l0 < 16) memset(line0 + l0, ' ', 16 - l0);
  if (l1 < 16) memset(line1 + l1, ' ', 16 - l1);
  line0[16] = line1[16] = '\0';
  lcd.setCursor(0, 0);
  lcd.print(line0);
  lcd.setCursor(0, 1);
  lcd.print(line1);
}

void setup() {
  Serial.begin(9600);
  lcd.init();
  lcd.backlight();
  lcd.clear();
  pinMode(VALVE_PIN, OUTPUT);
  digitalWrite(VALVE_PIN, RELAY_ACTIVE_LOW ? HIGH : LOW);
  pinMode(FLOW_SENSOR_PIN, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(FLOW_SENSOR_PIN), pulseCounter, FALLING);

  lcd.setCursor(0, 0);
  lcd.print("Sistema de Rega");
  lcd.setCursor(0, 1);
  lcd.print("Inicializando...");
  delay(1000);
  lcd.clear();

  currentHumidity = lerUmidadeMedia(SAMPLE_COUNT);
  enviarJSON(false, currentHumidity, 0, 0.0, totalVolume, 0);
}

void loop() {
  unsigned long now = millis();
  if (now - lastReading >= READING_INTERVAL) {
    lastReading = now;
    currentHumidity = lerUmidadeMedia(SAMPLE_COUNT);
    atualizarDisplay(currentHumidity);
  }

  // iniciar rega
  if (!watering && currentHumidity < DRY_THRESHOLD) {
    startWatering();
    enviarJSON(true, currentHumidity, 0, 0.0, totalVolume, 0);
  }

  // parar rega
  if (watering && (now - wateringStart >= WATERING_TIME_MS)) {
    stopWatering();
    enviarJSON(false, currentHumidity, sessionPulses, lastVolume, totalVolume, lastDuration);
  }

  // relatório periódico
  if (now - lastReport >= REPORT_INTERVAL) {
    lastReport = now;
    unsigned long currentPulses;
    noInterrupts();
    currentPulses = pulseCount;
    interrupts();
    bool w = watering;
    unsigned long duration = w ? (now - wateringStart) / 1000UL : lastDuration;
    unsigned long pulses = w ? currentPulses : sessionPulses;
    float volume = pulses / FLOW_FACTOR;
    enviarJSON(w, currentHumidity, pulses, volume, totalVolume, duration);
  }
}
