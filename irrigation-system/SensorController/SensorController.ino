/* ******************** Sistema de Rega Automática ********************
   Criado por: Leandro F. Moraes
   Rev.: 02
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

LiquidCrystal_I2C lcd(0x27, 16, 2);

// Configurações
const int limiarSeco = 30;                       // %: regar quando a umidade estiver abaixo deste valor.
const unsigned long tempoRegaMs = 5UL * 1000UL;  // tempo de rega em ms (5s)
const int NUM_AMOSTRAS = 5;                      // média de N leituras para reduzir ruído

// Calibração do sensor (valores brutos do ADC)
const int UMI_SENSOR_MIN = 10;    // solo encharcado
const int UMI_SENSOR_MAX = 1000;  // solo seco

// módulo relé é ativo em LOW (true) ou HIGH (false)
const bool RELAY_ACTIVE_LOW = false;

// Variáveis de controle
int umidadeSoloPct = 100;
bool regando = false;
unsigned long inicioRegaMs = 0;
unsigned long ultimaLeituraMs = 0;
const unsigned long intervaloLeituraMs = 1000UL;  // intervalo entre atualizações da tela / leituras

// Variáveis do sensor de fluxo
volatile unsigned long pulseCount = 0;  // Contador de pulsos
unsigned long pulsesDuringRega = 0;     // pulsos medidos na sessão
float volumeTotal = 0.0;                // volume acumulado (L)
float lastSessionVolume = 0.0;          // volume da última rega em L
const float factorK = 450.0;            // Fator de calibração (pulsos/L)

// ISR - incrementa contador de pulsos
void contadorDePulsos() {
  pulseCount++;
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

    Serial.print("Umidade: ");
    Serial.print(umidadeSoloPct);
    Serial.println(" %");
  }

  // Não está regando e umidade abaixo do limiar? iniciar rega
  if (!regando && umidadeSoloPct < limiarSeco) {
    iniciarRega();
    atualizarDisplay(umidadeSoloPct, regando);
  }

  // Se estiver regando, verificar tempo de término
  if (regando) {
    if (now - inicioRegaMs >= tempoRegaMs) {
      pararRega();
      atualizarDisplay(umidadeSoloPct, regando);
    }
  }
}

// Lê N amostras e retorna porcentagem (0..100)
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

  Serial.println("=== Iniciando Rega ===");
}

// Para a rega e atualiza variáveis
void pararRega() {
  // Ler pulsos contabilizados durante a rega (proteger com noInterrupts)
  noInterrupts();
  pulsesDuringRega = pulseCount;
  interrupts();

  // Calcula volume da sessão e acumula
  lastSessionVolume = pulsesDuringRega / factorK;  // litros
  volumeTotal += lastSessionVolume;

  // Desliga o relé
  if (RELAY_ACTIVE_LOW) digitalWrite(VALVULA, HIGH);
  else digitalWrite(VALVULA, LOW);

  regando = false;

  Serial.println("=== Parando Rega ===");
  Serial.print("Pulsos na sessao: ");
  Serial.println(pulsesDuringRega);
  Serial.print("Volume sessao (L): ");
  Serial.println(lastSessionVolume, 4);
  Serial.print("Volume total (L): ");
  Serial.println(volumeTotal, 4);
}

// Atualiza o display com estado e umidade
void atualizarDisplay(int umidadePct, bool estaRegando) {
  lcd.setCursor(0, 0);
  char linha0[17];
  char linha1[17];

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
    snprintf(linha1, sizeof(linha1), "Umid:%3d%%            ", umidadePct);
  }

  lcd.print(linha0);
  lcd.setCursor(0, 1);
  lcd.print(linha1);
}
