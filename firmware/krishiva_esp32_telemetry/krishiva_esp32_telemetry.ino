/*
 * KRISHIVA 360 — Real ESP32 DevKit Soil-Moisture Telemetry Firmware
 * Target Board: ESP32 Dev Module (Classic ESP32, NOT ESP32-S3)
 * Sensor: Analog Resistive Soil-Moisture Module (3.3V supply, AO -> GPIO34 / ADC1_CH6)
 * Transport: Local 2.4 GHz Wi-Fi HTTP POST -> Android Phone Receiver (Port 8080)
 *
 * WIRING TABLE (Classic ESP32 DevKit):
 *   Sensor VCC  -> ESP32 3V3   (Recommended so AO never exceeds 3.3V on GPIO34)
 *   Sensor GND  -> ESP32 GND
 *   Sensor AO   -> ESP32 GPIO34 (ADC1_CH6 — input-only pin, works reliably while Wi-Fi is active)
 *   Sensor DO   -> Not connected (leave floating)
 *
 * NOTE: Do NOT use ADC2 pins (GPIO0, 2, 4, 12–15, 25–27) for analogRead() while Wi-Fi is active on ESP32.
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <esp_adc_cal.h>

// ============================================================================
// USER-REPLACEABLE CONFIGURATION (Edit these 4 values before flashing)
// ============================================================================
const char* WIFI_SSID     = "REPLACE_WITH_2_4GHZ_WIFI_SSID";
const char* WIFI_PASSWORD = "REPLACE_WITH_WIFI_PASSWORD";
const char* PHONE_IP      = "192.168.43.1"; // Copy from KRISHIVA 360 -> ESP32 Setup screen
const char* DEVICE_TOKEN  = "REPLACE_WITH_X_DEVICE_TOKEN_FROM_APP";

// ============================================================================
// HARDWARE & PROTOCOL CONSTANTS
// ============================================================================
static const uint16_t SERVER_PORT          = 8080;
static const int      SOIL_ADC_PIN         = 34;     // GPIO34 = ADC1_CH6
static const int      ADC_SAMPLES          = 16;     // Multi-sample averaging count
static const uint32_t TELEMETRY_INTERVAL_MS = 2000;  // Send telemetry every 2 seconds
static const uint32_t WIFI_RETRY_INTERVAL_MS = 5000;
static const uint32_t HTTP_TIMEOUT_MS      = 2500;

static const char*    DEVICE_ID            = "krishiva-esp32-01";
static const char*    FIRMWARE_VERSION     = "1.0.0";

// Runtime state
static uint64_t g_sequenceNumber   = 0;
static uint32_t g_lastTelemetryMs  = 0;
static uint32_t g_lastWifiRetryMs  = 0;

// Read and average 16 samples from GPIO34 (12-bit 0..4095)
int readAveragedSoilMoistureRaw() {
  long sum = 0;
  for (int i = 0; i < ADC_SAMPLES; i++) {
    int sample = analogRead(SOIL_ADC_PIN);
    if (sample < 0) sample = 0;
    if (sample > 4095) sample = 4095;
    sum += sample;
    delay(2);
  }
  int avg = (int)(sum / ADC_SAMPLES);
  if (avg < 0) avg = 0;
  if (avg > 4095) avg = 4095;
  return avg;
}

void ensureWiFiConnected() {
  if (WiFi.status() == WL_CONNECTED) {
    return;
  }
  uint32_t now = millis();
  if (now - g_lastWifiRetryMs < WIFI_RETRY_INTERVAL_MS) {
    return;
  }
  g_lastWifiRetryMs = now;

  Serial.println("[KRISHIVA] Connecting to 2.4 GHz Wi-Fi...");
  WiFi.disconnect();
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
}

void sendTelemetryPacket() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("[KRISHIVA] Wi-Fi not connected; skipping telemetry tick.");
    return;
  }

  int moistureRaw = readAveragedSoilMoistureRaw();
  int rssiDbm     = WiFi.RSSI();
  uint32_t uptime = millis();
  g_sequenceNumber++;

  // Build endpoint URL: http://<PHONE_IP>:8080/api/telemetry
  String url = String("http://") + PHONE_IP + ":" + String(SERVER_PORT) + "/api/telemetry";

  // Build JSON payload matching KRISHIVA 360 protocol v1.0
  String payload = "{";
  payload += "\"deviceId\":\"" + String(DEVICE_ID) + "\",";
  payload += "\"sequence\":" + String((unsigned long)g_sequenceNumber) + ",";
  payload += "\"uptimeMs\":" + String(uptime) + ",";
  payload += "\"moistureRaw\":" + String(moistureRaw) + ",";
  payload += "\"wifiRssiDbm\":" + String(rssiDbm) + ",";
  payload += "\"firmwareVersion\":\"" + String(FIRMWARE_VERSION) + "\"";
  payload += "}";

  HTTPClient http;
  http.setTimeout(HTTP_TIMEOUT_MS);
  if (!http.begin(url)) {
    Serial.println("[KRISHIVA] Failed to initialize HTTPClient.");
    return;
  }

  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Device-Token", DEVICE_TOKEN);

  int httpCode = http.POST(payload);
  if (httpCode > 0) {
    String response = http.getString();
    Serial.printf(
      "[KRISHIVA] seq=%lu rawADC=%d rssi=%ddBm HTTP=%d resp=%s\n",
      (unsigned long)g_sequenceNumber,
      moistureRaw,
      rssiDbm,
      httpCode,
      response.c_str()
    );
  } else {
    Serial.printf(
      "[KRISHIVA] seq=%lu rawADC=%d POST failed: %s\n",
      (unsigned long)g_sequenceNumber,
      moistureRaw,
      http.errorToString(httpCode).c_str()
    );
  }

  http.end();
}

void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println("\n====================================================");
  Serial.println("KRISHIVA 360 — ESP32 Soil Moisture Telemetry Node");
  Serial.println("====================================================");

  // Configure 12-bit ADC resolution (0..4095) and 11dB attenuation (~0..3.3V full-scale)
  analogReadResolution(12);
  analogSetPinAttenuation(SOIL_ADC_PIN, ADC_11db);
  pinMode(SOIL_ADC_PIN, INPUT);

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  g_lastWifiRetryMs = millis();

  Serial.print("[KRISHIVA] Waiting for Wi-Fi connection");
  uint32_t startAttempt = millis();
  while (WiFi.status() != WL_CONNECTED && (millis() - startAttempt) < 10000) {
    delay(400);
    Serial.print(".");
  }
  Serial.println();

  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("[KRISHIVA] Connected! ESP32 IP: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("[KRISHIVA] Initial Wi-Fi connect timed out; will retry automatically in loop.");
  }
}

void loop() {
  ensureWiFiConnected();

  uint32_t now = millis();
  if (now - g_lastTelemetryMs >= TELEMETRY_INTERVAL_MS) {
    g_lastTelemetryMs = now;
    sendTelemetryPacket();
  }
}
