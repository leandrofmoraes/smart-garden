#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h> // Para criar o payload JSON
#include <NTPClient.h>   // Para obter a hora correta via NTP
#include <WiFiUdp.h>     // Necessário para o NTPClient
#include <TimeLib.h>

const char *ssid = "06853@SmartNet";
const char *password = "j56fj2b8";
const String serverName = "http://192.168.100.8:3000"; // Ou seu domínio

Adafruit_BME280 bme; // I2C

// Define o objeto NTPClient
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP);

void setup(){
  Serial.begin(115200);
  Serial.println("Iniciando...");

  WiFi.begin(ssid, password);
  Serial.print("Conectando ao WiFi");
  while (WiFi.status() != WL_CONNECTED){
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.println("Wi-Fi conectado");
  Serial.print("Endereço IP: ");
  Serial.println(WiFi.localIP());

  if (!bme.begin(0x76)){ // Endereço I2C do BME280
    Serial.println("Nao foi possivel encontrar o sensor BME280!");
    while (1)
      ;
  }
  Serial.println("Sensor BME280 encontrado.");

  // Inicializa o cliente NTP
  timeClient.begin();
  // Define o fuso horário para o Brasil (GMT -3)
  timeClient.setTimeOffset(-3 * 3600);
}

void loop()
{
  float temperatura = bme.readTemperature();
  float pressao = bme.readPressure() / 100.0F; // Converter para hPa
  float umidade = bme.readHumidity();

  // Atualiza as informações de hora do servidor NTP
  // Atualiza e obtém a data e hora atuais
  timeClient.update();
  time_t epochTime = timeClient.getEpochTime();
  String formattedDate = getFormattedDate(epochTime);

  JsonDocument jsonDocument;
  jsonDocument["temperature"] = temperatura;
  jsonDocument["pressure"] = pressao;
  jsonDocument["humidity"] = umidade;
  jsonDocument["timestamp"] = formattedDate;

  String jsonString;
  serializeJson(jsonDocument, jsonString);

  WiFiClient client; // Cria um objeto WiFiClient
  HTTPClient http;
  String serverPath = serverName + "/reading";
  http.begin(serverPath);
  http.addHeader("Content-Type", "application/json");
  Serial.println(jsonString);

  int httpResponseCode = http.POST(jsonString);

  if (httpResponseCode > 0)
  {
    Serial.printf("Resposta HTTP: %d\n", httpResponseCode);
    String response = http.getString();
    Serial.println(response);
  }
  else
  {
    Serial.println(httpResponseCode);
    Serial.printf("Erro na requisicao HTTP: %s\n", http.errorToString(httpResponseCode).c_str());
  }
  http.end();

  delay(5000); // Enviar dados a cada 5 segundos (ajuste conforme necessario)
}

// Função para formatar a data e hora no formato ISO 8601
String getFormattedDate(time_t epochTime){
  struct tm *ptm = gmtime((time_t *)&epochTime);
  char buffer[25];
  snprintf(buffer, sizeof(buffer), "%04d-%02d-%02dT%02d:%02d:%02dZ", year(epochTime), month(epochTime), day(epochTime), hour(epochTime), minute(epochTime), second(epochTime));
  snprintf(buffer, sizeof(buffer), "%04d-%02d-%02dT%02d:%02d:%02dZ",
           ptm->tm_year + 1900,
           ptm->tm_mon + 1,
           ptm->tm_mday,
           ptm->tm_hour,
           ptm->tm_min,
           ptm->tm_sec);
  return String(buffer);
}
