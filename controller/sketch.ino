/**
 * EnvironmentController
 *
 * @version v1.0
 * @author maxrt
 */

#include <LiquidCrystal_I2C.h>
#include <ArduinoWebsockets.h>
#include <ArduinoJson.h>
#include <ESP32Servo.h>
#include <HTTPClient.h>
#include <EEPROM.h>
#include <WiFi.h>
#include "DHTesp.h"

#define DISPLAY_PORT 0x27
#define DHT_PIN 13
#define WIFI_SSID "Wokwi-GUEST"
#define WIFI_PASS ""
#define DEFAULT_DEVICE_NAME "controller-1"
#define DEFAULT_SERVER_IP "3.88.139.144"
#define DEFAULT_SERVER_PORT "8080"

#define EEPROM_SIZE 128
#define EEPROM_IS_EEPROM_USED_START 1
#define EEPROM_IS_EEPROM_USED_END (EEPROM_IS_EEPROM_USED_START + 1)
#define EEPROM_DEVICE_NAME_START (EEPROM_IS_EEPROM_USED_END + 1)
#define EEPROM_DEVICE_NAME_SIZE 16
#define EEPROM_DEVICE_NAME_END (EEPROM_DEVICE_NAME_START + EEPROM_DEVICE_NAME_SIZE)
#define EEPROM_SERVER_IP_START (EEPROM_DEVICE_NAME_END + 1)
#define EEPROM_SERVER_IP_SIZE 16
#define EEPROM_SERVER_IP_END (EEPROM_SERVER_IP_START + EEPROM_SERVER_IP_SIZE)
#define EEPROM_SERVER_PORT_START (EEPROM_SERVER_IP_END + 1)
#define EEPROM_SERVER_PORT_SIZE 5
#define EEPROM_SERVER_PORT_END (EEPROM_SERVER_PORT_START + EEPROM_SERVER_PORT_SIZE)
#define EEPROM_FAN_THRESHOLD_START (EEPROM_SERVER_PORT_END + 1)
#define EEPROM_FAN_THRESHOLD_END (EEPROM_FAN_THRESHOLD_START + 1)
#define EEPROM_UPDATE_TIMEOUT_START (EEPROM_FAN_THRESHOLD_END + 1)
#define EEPROM_UPDATE_TIMEOUT_END (EEPROM_UPDATE_TIMEOUT_START + 1)

using namespace websockets;

void IRAM_ATTR onTimer();
void onMessageCallback(WebsocketsMessage message);
void onEventsCallback(WebsocketsEvent event, String data);

volatile bool dataHasToUpdate = false;

struct State {
  DHTesp dht;
  LiquidCrystal_I2C lcd;
  Servo fan;
  WebsocketsClient webSocket;
  hw_timer_t* timer;

  char deviceName[EEPROM_DEVICE_NAME_SIZE+1];
  char serverIp[EEPROM_SERVER_IP_SIZE+1];
  char serverPort[EEPROM_SERVER_PORT_SIZE+1];
  byte fanThresholdTemp = 30; // Degrees
  byte dataUpdateTimeout = 5; // Seconds
  bool fanEnabled = false;
  bool fanAutoEnabled = false;
  byte fanSpeed = 75; // Percent

  State() : lcd(DISPLAY_PORT, 16, 2) {}

  void init() {
    Serial.begin(115200);

    for (int i = 0; i < EEPROM_DEVICE_NAME_SIZE+1; i++) deviceName[i] = '\0';
    for (int i = 0; i < EEPROM_SERVER_IP_SIZE+1;   i++) serverIp[i] = '\0';
    for (int i = 0; i < EEPROM_SERVER_PORT_SIZE+1; i++) serverPort[i] = '\0';

    initEEPROM();

    if (deviceName[0] == '\0') strcpy(deviceName, DEFAULT_DEVICE_NAME);
    if (serverIp[0]   == '\0') strcpy(serverIp, DEFAULT_SERVER_IP);
    if (serverPort[0] == '\0') strcpy(serverPort, DEFAULT_SERVER_PORT);

    Serial.println(String("[EC] Device name: ") + deviceName);

    Serial.println("[EC] Initializing lcd");
    lcd.init();
    lcd.backlight();

    lcdInitMsg();

    Serial.println("[EC] Initializing DHT");
    dht.setup(DHT_PIN, DHTesp::DHT22);

    Serial.println("[EC] Initializing fan servo");
    fan.attach(2);

    Serial.print("[EC] Connecting to WiFi [");
    WiFi.begin(WIFI_SSID, WIFI_PASS, 6);
    while (WiFi.status() != WL_CONNECTED) {
      delay(100);
      Serial.print(".");
    }
    Serial.println("] Done");

    setupWebsocketConnection();

    Serial.println("[EC] Initializing timer");
    timer = timerBegin(0, 80, true);
    timerAttachInterrupt(timer, &onTimer, true);
    timerAlarmWrite(timer, 1000000 * dataUpdateTimeout, true);
    timerAlarmEnable(timer);
  }

  void initEEPROM() {
    Serial.println("[EC] Initializing EEPROM");
    EEPROM.begin(EEPROM_SIZE);

    byte isUsed = EEPROM.read(EEPROM_IS_EEPROM_USED_START);

    if (isUsed == 1) {
      Serial.println("[EC] Loading data from EEPROM");

      for (int i = EEPROM_DEVICE_NAME_START; i < EEPROM_DEVICE_NAME_END; i++) {
        deviceName[i-EEPROM_DEVICE_NAME_START] = EEPROM.read(i);
      }
      deviceName[EEPROM_DEVICE_NAME_SIZE] = '\0';

      for (int i = EEPROM_SERVER_IP_START; i < EEPROM_SERVER_IP_END; i++) {
        serverIp[i-EEPROM_SERVER_IP_START] = EEPROM.read(i);
      }
      serverIp[EEPROM_SERVER_IP_SIZE] = '\0';

      for (int i = EEPROM_SERVER_PORT_START; i < EEPROM_SERVER_PORT_END; i++) {
        serverPort[i-EEPROM_SERVER_PORT_START] = EEPROM.read(i);
      }
      serverPort[EEPROM_SERVER_PORT_SIZE] = '\0';

      fanThresholdTemp = EEPROM.read(EEPROM_FAN_THRESHOLD_START);
      dataUpdateTimeout = EEPROM.read(EEPROM_UPDATE_TIMEOUT_START);
    } else {
      Serial.println("[EC] No data in EEPROM");
      strcpy(deviceName, DEFAULT_DEVICE_NAME);
      strcpy(serverIp, DEFAULT_SERVER_IP);
      strcpy(serverPort, DEFAULT_SERVER_PORT);
      updateEEPROMdata();
    }
  }

  void updateEEPROMdata() {
    Serial.println("[EC] Updating EEPROM");

    EEPROM.write(0, 1);

    for (int i = EEPROM_DEVICE_NAME_START; i < EEPROM_DEVICE_NAME_END; i++) {
      EEPROM.write(i, deviceName[i-EEPROM_DEVICE_NAME_START]);
    }

    for (int i = EEPROM_SERVER_IP_START; i < EEPROM_SERVER_IP_END; i++) {
      EEPROM.write(i, serverIp[i-EEPROM_SERVER_IP_START]);
    }

    for (int i = EEPROM_SERVER_PORT_START; i < EEPROM_SERVER_PORT_END; i++) {
      EEPROM.write(i, serverPort[i-EEPROM_SERVER_PORT_START]);
    }

    EEPROM.write(EEPROM_FAN_THRESHOLD_START, fanThresholdTemp);
    EEPROM.write(EEPROM_UPDATE_TIMEOUT_START, dataUpdateTimeout);
    EEPROM.commit();
  }

  void lcdInitMsg() {
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("EnvControl v1.0");
    lcd.setCursor(0, 1);
    lcd.print("Initializing");
  }

  void printConfig() {
    Serial.println(String("[cfg] deviceName=") + deviceName);
    Serial.println(String("[cfg] serverIp=") + serverIp);
    Serial.println(String("[cfg] serverPort=") + serverPort);
    Serial.println(String("[cfg] fanThresholdTemp=") + fanThresholdTemp);
    Serial.println(String("[cfg] dataUpdateTimeout=") + dataUpdateTimeout);
  }

  int calculateFanTimeoutFromSpeed() {
    return 20 - (20 * (fanSpeed / 100)) + 5;
  }

  void updateData() {
    TempAndHumidity data = dht.getTempAndHumidity();

    lcd.clear();

    lcd.setCursor(0, 0);
    String line1 = "T: ";
    line1 += data.temperature;
    lcd.print(line1);
    
    lcd.setCursor(0, 1);
    String line2 = "H: ";
    line2 += data.humidity;
    lcd.print(line2);

    if (data.temperature >= fanThresholdTemp) {
      fanEnabled = true;
      fanAutoEnabled = true;
    } else if (data.temperature < fanThresholdTemp && fanAutoEnabled) {
      fanAutoEnabled = false;
      fanEnabled = false;
    }

    StaticJsonDocument<200> json;
    json["command"] = "update_data";
    json["sender"] = deviceName;

    JsonObject jsonData = json.createNestedObject("data");
    jsonData["temperature"] = data.temperature;
    jsonData["humidity"] = data.humidity;

    char buffer[256] = {0};
    serializeJson(json, buffer, 256);

    webSocket.send(buffer);
  }

  void setupWebsocketConnection() {
    char server[64] = {0};
    sprintf(server, "ws://%s:%s/api", serverIp, serverPort);

    Serial.println(String("[EC] Connecting to ") + server);

    webSocket.onMessage(onMessageCallback);
    webSocket.onEvent(onEventsCallback);
    webSocket.connect(server);

    StaticJsonDocument<200> json;
    json["command"] = "register";
    json["sender"] = deviceName;

    JsonObject data = json.createNestedObject("data");
    data["type"] = "controller";

    char buffer[256] = {0};
    serializeJson(json, buffer, 256);

    webSocket.send(buffer);
    webSocket.ping();
  }

} state;

void IRAM_ATTR onTimer() {
  Serial.println("[T] Timer");
  dataHasToUpdate = true;
}

void handleCommandUpdateName(StaticJsonDocument<200>& json) {
  String name = json["data"]["name"].as<String>();
  if (name.length() > EEPROM_DEVICE_NAME_SIZE) {
    String msg = "[WS] Error: name length (";
    msg += name.length();
    msg += ") is more than ";
    msg += String(EEPROM_DEVICE_NAME_SIZE);
    Serial.println(msg);
    return;
  }
  StaticJsonDocument<200> command;
  command["command"] = "reregister";
  command["sender"] = state.deviceName;

  JsonObject data = command.createNestedObject("data");
  data["name"] = name;
  data["type"] = "controller";

  char buffer[256] = {0};
  serializeJson(command, buffer, 256);

  state.webSocket.send(buffer);
  strcpy(state.deviceName, name.c_str());
  state.updateEEPROMdata();
}

void handleCommandUpdateServer(StaticJsonDocument<200>& json) {
  String ip = json["data"]["ip"].as<String>();
  String port = json["data"]["ip"].as<String>();
  if (ip.length() < EEPROM_SERVER_IP_SIZE) {
    String msg = "[WS] Error ip length is more than ";
    msg += String(EEPROM_SERVER_IP_SIZE);
    Serial.println(msg);
    return;
  }
  if (ip.length() < EEPROM_SERVER_PORT_SIZE) {
    String msg = "[WS] Error port length is more than ";
    msg += String(EEPROM_SERVER_PORT_SIZE);
    Serial.println(msg);
    return;
  }
  strcpy(state.serverIp, ip.c_str());
  strcpy(state.serverPort, port.c_str());
  state.updateEEPROMdata();
}

void onMessageCallback(WebsocketsMessage message) {
  StaticJsonDocument<200> json;
  if (deserializeJson(json, message.data()) == DeserializationError::Ok) {
    if (json.containsKey("command")) {
      String command = json["command"].as<String>();
      Serial.print("[WS] Recieved command: ");
      Serial.println(command);
      if (command == "fan_start") {
        state.fanEnabled = true;
        state.fanSpeed = json["data"]["speed"].as<int>();
      } else if (command == "fan_stop") {
        state.fanEnabled = false;
      } else if (command == "fan_rule") {
        state.fanSpeed = json["data"]["speed"].as<int>();
        state.fanThresholdTemp = json["data"]["threshold"].as<int>();
      } else if (command == "update_name") {
        handleCommandUpdateName(json);
      } else if (command == "update_server") {
        handleCommandUpdateServer(json);
      } else {
        Serial.println("Unknown command");
      }
    } else if (json.containsKey("status")) {
      String status = json["status"].as<String>();
      Serial.print("[WS] Recieved response: ");
      if (status == "error") {
        String message = json["message"].as<String>();
        Serial.println(status + ":" + message);
      } else {
        Serial.println(status);
      }
    }
  } else {
    Serial.print("[WS] Error deserializing message");
  }
}

void onEventsCallback(WebsocketsEvent event, String data) {
  if (event == WebsocketsEvent::ConnectionOpened) {
    Serial.println("[WS] Connnection Opened");
  } else if (event == WebsocketsEvent::ConnectionClosed) {
    Serial.println("[WS] Connnection Closed");
    state.setupWebsocketConnection();
  } else if (event == WebsocketsEvent::GotPing) {
    Serial.println("[WS] Got a Ping");
  } else if (event == WebsocketsEvent::GotPong) {
    Serial.println("[WS] Got a Pong");
  }
}

void httpGet(String url, bool printPayload) {
  HTTPClient http;

  http.begin(url.c_str());
  int httpResponseCode = http.GET();

  Serial.println("[HTTP] GET " + url + " " + httpResponseCode);

  if (httpResponseCode > 0 && printPayload) {
    String payload = http.getString();
    Serial.println(payload);
  }
  http.end();
}

void setup() {
  state.init();
}

void loop() {
  state.webSocket.poll();

  if (dataHasToUpdate) {
    state.updateData();
    dataHasToUpdate = false;
  }

  if (Serial.available()) {
    String cmd = Serial.readString();
    cmd.trim();
    if (cmd.startsWith("config")) {
      state.printConfig();
    } else if (cmd.startsWith("http_get_r")) {
      httpGet(cmd.substring(11), true);
    } else if (cmd.startsWith("http_get")) {
      httpGet(cmd.substring(9), false);
    }
  }

  if (state.fanEnabled) {
    for (int pos = 0; pos <= 180; pos++) {
      state.fan.write(pos);
      delay(state.calculateFanTimeoutFromSpeed());
    }

    for (int pos = 180; pos >= 0; pos--) {
      state.fan.write(pos);
      delay(state.calculateFanTimeoutFromSpeed());
    }
  }

  delay(100);
}
