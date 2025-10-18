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

#define VALVULA 10
#define UMI_SENSOR A0
#define FLOW_SENSOR 2

// DEBUG: comente a linha abaixo para DESLIGAR prints de debug
// #define DEBUG
#ifdef DEBUG
#define DPRINT(...) Serial.print(__VA_ARGS__)
#define DPRINTLN(...) Serial.println(__VA_ARGS__)
#else
#define DPRINT(...) (void)0
#define DPRINTLN(...) (void)0
#endif

LiquidCrystal_I2C lcd(0x27, 16, 2);

// ----------------- CONFIG -----------------
const int limiarSeco = 20;                       // %: regar quando a umidade estiver abaixo deste valor.
const unsigned long tempoRegaMs = 5UL * 1000UL;  // tempo de rega em ms (5s)
const int NUM_AMOSTRAS = 5;                      // média de N leituras para reduzir ruído

const int UMI_SENSOR_MIN = 10;    // solo encharcado (calibrar)
const int UMI_SENSOR_MAX = 1000;  // solo seco (calibrar)

// módulo relé é ativo em LOW (true) ou HIGH (false)
const bool RELAY_ACTIVE_LOW = false;
// -----------------------------------------

// Variáveis de controle
int umidadeSoloPct = 100;
bool regando = false;
unsigned long inicioRegaMs = 0;
unsigned long ultimaLeituraMs = 0;
const unsigned long intervaloLeituraMs = 1000UL;  // intervalo entre atualizações da tela / leituras

// Intervalo de envio de relatório via Serial (1 hora)
const unsigned long intervaloRelatorioMs = 3600000UL;  // 1 hora = 3.600.000 ms
unsigned long ultimaEnvioRelatorioMs = 0;

// Variáveis do sensor de fluxo
volatile unsigned long pulseCount = 0;  // Contador de pulsos
unsigned long pulsesDuringRega = 0;     // pulsos medidos na sessão
float volumeTotal = 0.0;                // volume acumulado (L)
float lastSessionVolume = 0.0;          // volume da última rega em
unsigned long lastSessionDuration_s = 0;
const float factorK = 450.0;  // Fator de calibração (pulsos/L)

// ISR - incrementa contador de pulsos
void contadorDePulsos() {
  pulseCount++;
}

// Enviar JSON via Serial
void enviarJSON(bool estaRegando,
                int humidityPct,
                unsigned long pulsesSessao,
                float volumeSessao,
                float volumeAcumulado,
                unsigned long duracaoSessao_s) {
  // Converte floats para string (AVR-safe)
  char volSessBuf[12];
  char volTotBuf[12];
  dtostrf(volumeSessao, 6, 3, volSessBuf);  // largura 6, 3 decimais
  dtostrf(volumeAcumulado, 6, 3, volTotBuf);

  unsigned long device_ts = millis();

  char jsonBuf[300];
  // Prefixo #DATA# facilita filtro no receptor
  // Ex: #DATA#{"umidade":45,"regando":1,"rega_duracao_s":3,"rega_pulsos":123,"rega_volume_l":0.123,"volume_total_l":1.234,"ts":123456}
  snprintf(jsonBuf, sizeof(jsonBuf),
           "#DATA#{\"humidity\":%d,\"device_ts_ms\":%lu,\"regando\":%d,"
           "\"rega_pulsos\":%lu,\"rega_volume_l\":%s,\"volume_total_l\":%s}",
           humidityPct,
           device_ts,
           estaRegando ? 1 : 0,
           pulsesSessao,
           volSessBuf,
           volTotBuf);

  Serial.println(jsonBuf);
}

void enviarRelatorioHorario() {
  // captura valores atômicos
  unsigned long pulsesNow;
  noInterrupts();
  pulsesNow = pulseCount;
  interrupts();

  bool estaRegando = regando;
  unsigned long duracao_s;
  unsigned long pulsesSessao;
  float volumeSessao;
  if (estaRegando) {
    // durante rega: relatórios mostram valores correntes
    duracao_s = (millis() - inicioRegaMs) / 1000UL;
    pulsesSessao = pulsesNow;
    volumeSessao = pulsesNow / factorK;
  } else {
    // não regando: reporta última sessão (se houver)
    duracao_s = lastSessionDuration_s;
    pulsesSessao = pulsesDuringRega;
    volumeSessao = lastSessionVolume;
  }

  // envia
  enviarJSON(estaRegando, umidadeSoloPct, pulsesSessao, volumeSessao, volumeTotal, duracao_s);
  DPRINTLN("Relatorio horario enviado.");
}


void setup() {
  Serial.begin(9600);
  // Wire.begin(); // descomente para iniciar I2C manualmente

  lcd.init();
  lcd.backlight();
  lcd.clear();

  pinMode(VALVULA, OUTPUT);
  // Inicializa relé no estado OFF
  if (RELAY_ACTIVE_LOW) digitalWrite(VALVULA, HIGH);
  else digitalWrite(VALVULA, LOW);

  pinMode(FLOW_SENSOR, INPUT);  // ou INPUT_PULLUP se sensor exigir
  attachInterrupt(digitalPinToInterrupt(FLOW_SENSOR), contadorDePulsos, RISING);

  // enviar relatório imediato ao ligar (opcional)
  ultimaEnvioRelatorioMs = millis();
  enviarRelatorioHorario();

  // Mensagem inicial
  lcd.setCursor(0, 0);
  lcd.print("Sistema de Rega");
  lcd.setCursor(0, 1);
  lcd.print("Inicializando...");
  delay(800);
  lcd.clear();
}

void loop() {
  unsigned long now = millis();

  // Faz leituras periodicamente (cada intervaloLeituraMs)
  if (now - ultimaLeituraMs >= intervaloLeituraMs) {
    ultimaLeituraMs = now;
    umidadeSoloPct = lerUmidadeMedia(NUM_AMOSTRAS);
    atualizarDisplay(umidadeSoloPct, regando);

    DPRINT("Umidade: ");
    DPRINTLN(umidadeSoloPct);
  }

  // Não está regando e umidade abaixo do limiar? iniciar rega
  if (!regando && umidadeSoloPct < limiarSeco) {
    iniciarRega();
    enviarJSON(true, umidadeSoloPct, 0, 0.0, volumeTotal, 0);
    atualizarDisplay(umidadeSoloPct, regando);
  }

  // Se estiver regando, verificar tempo de término
  if (regando) {
    if (now - inicioRegaMs >= tempoRegaMs) {
      pararRega();
      enviarJSON(false, umidadeSoloPct, pulsesDuringRega, lastSessionVolume, volumeTotal, lastSessionDuration_s);
      atualizarDisplay(umidadeSoloPct, regando);
    }
  }

  // Envia relatório horário
  if (now - ultimaEnvioRelatorioMs >= intervaloRelatorioMs) {
    ultimaEnvioRelatorioMs = now;
    enviarRelatorioHorario();
  }
}

// ----------------- Funções auxiliares -----------------

int lerUmidadeMedia(int n) {
  long soma = 0;
  for (int i = 0; i < n; i++) {
    int leitura = analogRead(UMI_SENSOR);
    soma += leitura;
    delay(10);  // curto atraso entre amostras para estabilidade
  }
  int media = (int)(soma / n);

  // Mapeia e inverte: UMI_SENSOR_MIN => 100% (molhado), UMI_SENSOR_MAX => 0% (seco)
  long pct = map(media, UMI_SENSOR_MIN, UMI_SENSOR_MAX, 100, 0);
  pct = constrain(pct, 0, 100);
  return (int)pct;
}

void iniciarRega() {
  regando = true;
  inicioRegaMs = millis();

  noInterrupts();
  pulseCount = 0;  // Zera o contador após a leitura
  interrupts();

  // Aciona o relé - respeitando polaridade
  if (RELAY_ACTIVE_LOW) digitalWrite(VALVULA, LOW);
  else digitalWrite(VALVULA, HIGH);

  DPRINTLN("=== Iniciando Rega ===");
}

// Para a rega e atualiza variáveis
void pararRega() {

  // Leitura de pulsos da sessão
  noInterrupts();
  pulsesDuringRega = pulseCount;
  interrupts();

  // Calcula volume da sessão e acumula
  lastSessionVolume = pulsesDuringRega / factorK;  // litros
  volumeTotal += lastSessionVolume;
  lastSessionDuration_s = (millis() - inicioRegaMs) / 1000UL;

  // Desliga o relé
  if (RELAY_ACTIVE_LOW) digitalWrite(VALVULA, HIGH);
  else digitalWrite(VALVULA, LOW);

  regando = false;

  DPRINTLN("=== Parando Rega ===");
  DPRINT("Pulsos na sessao: ");
  DPRINTLN(pulsesDuringRega);
  DPRINT("Volume sessao (L): ");
  DPRINTLN(lastSessionVolume, 4);
  DPRINT("Volume total (L): ");
  DPRINTLN(volumeTotal, 4);
}

// Atualiza o display com estado e umidade
void atualizarDisplay(int umidadePct, bool estaRegando) {
  lcd.setCursor(0, 0);
  char linha0[18];
  char linha1[18];

  if (estaRegando) {
    // Exibir estado e volume da sessão (atualiza volume parcial lendo pulseCount)
    unsigned long currentPulses;

    noInterrupts();
    currentPulses = pulseCount;
    interrupts();

    float currentVolume = currentPulses / factorK;  // litros (parcial)
    // converte float para string: dtostrf
    char volBuf[8];
    dtostrf(currentVolume, 4, 2, volBuf);  // largura 4, 2 casas decimais
    snprintf(linha0, sizeof(linha0), "Regando V:%sL", volBuf);

    // tempo restante
    unsigned long elapsed = (millis() - inicioRegaMs) / 1000;
    unsigned long remaining = (tempoRegaMs / 1000 > elapsed) ? (tempoRegaMs / 1000 - elapsed) : 0;
    snprintf(linha1, sizeof(linha1), "Umid:%3d%% Rem:%2lus", umidadePct, remaining);

  } else {
    // Estado IDLE + total acumulado
    char volTotalBuf[8];
    dtostrf(volumeTotal, 4, 2, volTotalBuf);
    snprintf(linha0, sizeof(linha0), "Estado: IDLE V:%sL", volTotalBuf);
    snprintf(linha1, sizeof(linha1), "Umid:%3d%%", umidadePct);
  }

  //garante 16 colunas
  for (int i = strlen(linha0); i < 16; i++) linha0[i] = ' ';
  linha0[16] = '\0';
  for (int i = strlen(linha1); i < 16; i++) linha1[i] = ' ';
  linha1[16] = '\0';

  lcd.print(linha0);
  lcd.setCursor(0, 1);
  lcd.print(linha1);
}
