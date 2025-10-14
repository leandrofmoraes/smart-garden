/* ******************** Sistema de Rega Automática ********************
   Criado por: Leandro F. Moraes
   Rev.: 01
   Data: 14.10.2025
   Observações:
   - Ajuste SENSOR_MIN/SENSOR_MAX para calibrar o seu sensor.
   - Ajuste RELAY_ACTIVE_LOW dependendo do seu módulo relé (true se o relé fecha quando pino = LOW).
*******************************************************************************/

#include <Wire.h>
#include <LiquidCrystal_I2C.h>

#define VALVULA 10
#define SENSOR A0

LiquidCrystal_I2C lcd(0x27, 16, 2);

// --- Configurações de usuário ---
const int limiarSeco = 30;                       // %: regar quando a umidade estiver abaixo deste valor.
const unsigned long tempoRegaMs = 5UL * 1000UL;  // tempo de rega em ms (5s)
const int NUM_AMOSTRAS = 5;                      // média de N leituras para reduzir ruído

// Calibração do sensor (valores brutos do ADC)
const int SENSOR_MIN = 0;    // ajustar conforme seu sensor (solo encharcado)
const int SENSOR_MAX = 900;  // ajustar conforme seu sensor (solo seco)

// módulo relé é ativo em LOW (true) ou HIGH (false)
const bool RELAY_ACTIVE_LOW = false;

// --- Variáveis de controle ---
int umidadeSoloPct = 100;
bool regando = false;
unsigned long inicioRegaMs = 0;
unsigned long ultimaLeituraMs = 0;
const unsigned long intervaloLeituraMs = 1000UL;  // intervalo entre atualizações da tela / leituras

void setup() {
  Serial.begin(9600);
  // Wire.begin(); // descomente se precisar iniciar I2C manualmente

  lcd.init();
  lcd.backlight();
  lcd.clear();

  pinMode(VALVULA, OUTPUT);
  // Inicializa relé no estado OFF
  if (RELAY_ACTIVE_LOW) digitalWrite(VALVULA, HIGH);
  else digitalWrite(VALVULA, LOW);

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
    int leitura = analogRead(SENSOR);
    soma += leitura;
    delay(10);  // curto atraso entre amostras para estabilidade
  }
  int media = (int)(soma / n);

  // Mapeia e inverte: SENSOR_MIN => 100% (molhado), SENSOR_MAX => 0% (seco)
  long pct = map(media, SENSOR_MIN, SENSOR_MAX, 100, 0);
  pct = constrain(pct, 0, 100);
  return (int)pct;
}

void iniciarRega() {
  regando = true;
  inicioRegaMs = millis();
  // Aciona o relé - respeitando polaridade
  if (RELAY_ACTIVE_LOW) digitalWrite(VALVULA, LOW);
  else digitalWrite(VALVULA, HIGH);

  Serial.println("=== Iniciando Rega ===");
}

// Para a rega e atualiza variáveis
void pararRega() {
  regando = false;
  // Desliga o relé
  if (RELAY_ACTIVE_LOW) digitalWrite(VALVULA, HIGH);
  else digitalWrite(VALVULA, LOW);

  Serial.println("=== Parando Rega ===");
}

// Atualiza o display com estado e umidade
void atualizarDisplay(int umidadePct, bool estaRegando) {
  // Linha 0: Estado
  lcd.setCursor(0, 0);
  if (estaRegando) {
    lcd.print(" REGAGANDO...   ");
  } else {
    lcd.print("Estado: IDLE      ");
  }

  // Linha 1: Umidade atual formatada
  char buf[17];
  if (estaRegando) {
    // Exibe um contador relativo (tempo desde o início) opcionalmente
    unsigned long elapsed = (millis() - inicioRegaMs) / 1000;  // segundos
    // Formato: "Umid: 045% T: 3s"
    snprintf(buf, sizeof(buf), "Umid:%3d%%  T:%2lus ", umidadePct, elapsed);
  } else {
    // Formato simples: "Umid: 045%        "
    snprintf(buf, sizeof(buf), "Umid:%3d%%            ", umidadePct);
  }
  lcd.setCursor(0, 1);
  lcd.print(buf);
}
