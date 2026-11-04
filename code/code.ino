#include <WiFi.h>
#include <PubSubClient.h>
#include <DHT.h>
#include <Preferences.h>

const char* WIFI_SSID = "GIAHUNG-4225TB-404";
const char* WIFI_PASSWORD = "GIAHUNG1368";
const char* MQTT_SERVER = "192.168.44.177"; // IP máy tính

const int DHT_PIN = 4;
const int LIGHT_PIN = 34;
const int LED_PINS[] = {16, 17, 18};

const int MQTT_PORT = 1884;
const char* MQTT_USER = "iotuser";
const char* MQTT_PASSWORD = "b23dccn086";

const char* COMMAND_TOPICS[] = {
  "iot/led1/set",
  "iot/led2/set",
  "iot/led3/set",
  "iot/ledall/set"
};

const char* STATE_TOPICS[] = {
  "iot/led1/state",
  "iot/led2/state",
  "iot/led3/state"
};

bool ledStates[] = {false, false, false};

DHT dht(DHT_PIN, DHT11);
WiFiClient wifiClient;
PubSubClient mqtt(wifiClient);
Preferences prefs;

unsigned long lastSend = 0;
unsigned long lastConnectAttempt = 0;

// Nhận lệnh điều khiển từ MQTT
void onMessage(char* topic, byte* payload, unsigned int length) {
  String command;

  // Chuyển dữ liệu nhận được thành chuỗi
  for (unsigned int i = 0; i < length; i++) {
    command += (char)payload[i];
  }
  command.trim();
  command.toUpperCase();

  // Chỉ chấp nhận lệnh ON hoặc OFF
  if (command != "ON" && command != "OFF") {
    return;
  }

  for (int i = 0; i < 3; i++) {
    if (strcmp(topic, COMMAND_TOPICS[i]) == 0) {

      // Cập nhật trạng thái LED, điều khiển chân GPIO, báo lại trạng thái thiết bị
      if (command == "ON") {
        ledStates[i] = true;
        digitalWrite(LED_PINS[i], HIGH);
        mqtt.publish(STATE_TOPICS[i], "ON", true);
      } else {
        ledStates[i] = false;
        digitalWrite(LED_PINS[i], LOW);
        mqtt.publish(STATE_TOPICS[i], "OFF", true);
      }

      // Lưu trạng thái vào bộ nhớ Flash NVS để phục hồi ngay lập tức khi mất nguồn
      char key[8];
      snprintf(key, sizeof(key), "led%d", i + 1);
      prefs.putBool(key, ledStates[i]);

      // Báo lại trạng thái GPIO đã đặt
      Serial.printf("LED %d: %s (da luu Flash)\n", i + 1, command.c_str());
      return;
    }
  }

  if (strcmp(topic, COMMAND_TOPICS[3]) == 0) {
    if (command == "ON") {
      for (int i = 0; i < 3; i++) {
        ledStates[i] = true;
        digitalWrite(LED_PINS[i], HIGH);
        mqtt.publish(STATE_TOPICS[i], "ON", true);
        char key[8];
        snprintf(key, sizeof(key), "led%d", i + 1);
        prefs.putBool(key, true);
      }
    } else {
      for (int i = 0; i < 3; i++) {
        ledStates[i] = false;
        digitalWrite(LED_PINS[i], LOW);
        mqtt.publish(STATE_TOPICS[i], "OFF", true);
        char key[8];
        snprintf(key, sizeof(key), "led%d", i + 1);
        prefs.putBool(key, false);
      }
    }
    Serial.printf("LED ALL: %s (da luu Flash)\n", command.c_str());
  }
}

// Gửi một giá trị và in kết quả lên Serial
void publishValue(const char* topic, const String& value) {
  bool sent = mqtt.publish(topic, value.c_str());
  if (sent == true) {
    Serial.printf("%s = %s [Da gui]\n", topic, value.c_str());
  } else {
    Serial.printf("%s = %s [Gui that bai]\n", topic, value.c_str());
  }
}

// Kết nối MQTT và đồng bộ trạng thái
void connectMQTT() {
  if (WiFi.status() != WL_CONNECTED) return;

  String clientId = "ESP32-" + WiFi.macAddress();
  Serial.println("Dang ket noi MQTT...");

  if (mqtt.connect(clientId.c_str(), MQTT_USER, MQTT_PASSWORD)) {
    Serial.println("MQTT da ket noi thanh cong!");
    for (int i = 0; i < 4; i++) {
      mqtt.subscribe(COMMAND_TOPICS[i]);
    }

    // Báo trạng thái hiện tại lên MQTT để Backend đồng bộ
    for (int i = 0; i < 3; i++) {
      mqtt.publish(STATE_TOPICS[i], ledStates[i] ? "ON" : "OFF", true);
    }

    mqtt.loop();
  } else {
    Serial.print("Loi MQTT, ma: ");
    Serial.println(mqtt.state());
  }
}

void setup() {
  Serial.begin(115200);

  // 1. Cấu hình chân LED
  for (int i = 0; i < 3; i++) {
    pinMode(LED_PINS[i], OUTPUT);
  }

  // 2. KHÔI PHỤC NGAY LẬP TỨC TRẠNG THÁI TỪ FLASH NVS (< 10ms)
  // Giúp đèn BẬT SÁNG NGAY KHI CẮM NGUỒN, không cần phải chờ Wi-Fi hay MQTT!
  prefs.begin("iot_state", false);
  ledStates[0] = prefs.getBool("led1", false);
  ledStates[1] = prefs.getBool("led2", false);
  ledStates[2] = prefs.getBool("led3", false);

  for (int i = 0; i < 3; i++) {
    digitalWrite(LED_PINS[i], ledStates[i] ? HIGH : LOW);
  }
  Serial.printf("Khoi phuc ngay tu Flash: LED1=%s, LED2=%s, LED3=%s\n",
                ledStates[0] ? "ON" : "OFF",
                ledStates[1] ? "ON" : "OFF",
                ledStates[2] ? "ON" : "OFF");

  pinMode(LIGHT_PIN, INPUT);
  dht.begin();

  mqtt.setServer(MQTT_SERVER, MQTT_PORT);
  mqtt.setCallback(onMessage);

  // 3. Kết nối Wi-Fi (tắt chế độ sleep để kết nối nhanh và ổn định nhất)
  WiFi.mode(WIFI_STA);
  WiFi.setAutoReconnect(true);
  WiFi.setSleep(false);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  Serial.print("Dang ket noi Wi-Fi");

  while (WiFi.status() != WL_CONNECTED) {
    delay(100);
    Serial.print(".");
  }

  Serial.println("\nWi-Fi da ket noi!");
  Serial.print("IP ESP32: ");
  Serial.println(WiFi.localIP());

  // 4. Kết nối MQTT ngay trong setup()
  connectMQTT();
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    delay(10);
    return;
  }

  // Kết nối lại nếu mất kết nối MQTT (thử lại mỗi 2 giây)
  if (!mqtt.connected()) {
    unsigned long now = millis();
    if (now - lastConnectAttempt >= 2000) {
      lastConnectAttempt = now;
      connectMQTT();
    }
    return;
  }

  // Xử lý các tin nhắn điều khiển ngay lập tức
  mqtt.loop();

  // Đọc và gửi dữ liệu mỗi 2 giây
  if (millis() - lastSend >= 2000) {
    lastSend = millis();

    float temperature = dht.readTemperature();
    float humidity = dht.readHumidity();
    float light = analogRead(LIGHT_PIN);

    if (!isnan(temperature) && !isnan(humidity)) {
      publishValue(
        "iot/sensor/temperature",
        String(temperature, 1)
      );

      publishValue(
        "iot/sensor/humidity",
        String(humidity, 1)
      );
    } else {
      Serial.println("Khong doc duoc DHT11!");
    }

    publishValue("iot/sensor/light", String(light));
  }
}
