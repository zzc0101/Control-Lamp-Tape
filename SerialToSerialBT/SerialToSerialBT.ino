#include "BluetoothSerial.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

#include <FastLED.h>
#define NUM 60
#define TYPE WS2812
#define PIN 32
#define COLOR GRB
uint8_t bright = 20;
uint8_t beginHue=0;
CRGB leds[NUM];
CRGB currentColor(0, 0, 255);
CHSV mycolor(0, 255, 120);
BluetoothSerial SerialBT;

bool deviceConnected = false;
char data;
String data_sum = "";
char number_index = 'A';
// 蓝牙串口回调函数
void callback(esp_spp_cb_event_t event, esp_spp_cb_param_t *param)
{
  if (event == ESP_SPP_SRV_OPEN_EVT)
  {
    Serial.println("设备连接");
    deviceConnected = true;
    SerialBT.println(String(number_index)+";"+String(currentColor.r)+";"+String(currentColor.g)+";"+String(currentColor.b));
  }

  if (event == ESP_SPP_CLOSE_EVT)
  {
    Serial.println("设备断开");
    deviceConnected = false;
  }  
}

void setup() {
  Serial.begin(9600);
  
  FastLED.addLeds<TYPE, PIN, COLOR>(leds, NUM);
  FastLED.setBrightness(bright); // 亮度
  
  SerialBT.register_callback(callback);
  SerialBT.begin("ZZCLight"); //Bluetooth device name
  Serial.println("设备启动");
}

// Serial.read()此函数用于存储开发板发送的数据
// SerialBT.read()此函数用于存储开发板接收的数据
void loop() {
// 蓝牙接收数据
  if (deviceConnected) {
    while (SerialBT.available() > 0) {
      data = SerialBT.read();
      data_sum += data;
    }
//      Serial.println("由Serial打印:"+data_sum);
      if(data_sum.charAt(0) >= 'A' && data_sum.charAt(0) <= 'F') {
      number_index = data;
      } else if(data_sum.charAt(0) == 'G') {
//      Serial.println("由Serial打印1:"+data_sum);
      data_sum.trim();
      int kk = data_sum.indexOf(';');
      data_sum = data_sum.substring(kk+1);
//      Serial.println("由Serial打印2:"+data_sum);
      int start_with = data_sum.indexOf(';');
      int end_with = data_sum.lastIndexOf(";");
      
      currentColor.r = data_sum.substring(0,start_with).toInt();
//      Serial.println("由Serial打印3:"+String(currentColor.r));
      
      currentColor.g = data_sum.substring(start_with+1,end_with).toInt();
//      Serial.println("由Serial打印4:"+String(currentColor.g));
      
      currentColor.b = data_sum.substring(end_with+1).toInt();
//      Serial.println("由Serial打印5:"+String(currentColor.b));
      }
    data_sum = "";
  }
  switch (number_index) {
    case 'A': blingbling(); break;
    case 'B': rainbow(); break;
    case 'C': runningWater(); break;
    case 'D': backAndForth(); break;
    case 'E': three(); break;
    case 'F': single(); break;
    default: rainbow();
  }
}

// blingbling
void blingbling() {
  fill_gradient(leds, 0,CHSV(0,255,120),NUM-1,CHSV(177,255,120), LONGEST_HUES);
  FastLED.setBrightness(120);
  addShine(10);
  FastLED.show();
}

void addShine( uint8_t chance) 
{
  if( random8() < chance) {   
    leds[ random8(NUM) ] = currentColor;
  }
}

// 彩虹灯
void rainbow() {
  beginHue++;
  FastLED.setBrightness(120);
  fill_rainbow(leds, NUM, beginHue, 9);
  FastLED.show();
  delay(25);
}

// 流水灯
void runningWater() {
   int i;
   FastLED.setBrightness(120);
   for(i=0;i<NUM;i++)
   { 
      fill_solid(leds+i,1,currentColor  );
      FastLED.show();
      delay(10); 
   }
   delay(600);
   FastLED.clear();
   FastLED.show();
   delay(600);
}

// 跑马灯
void backAndForth() {
  int i;
  FastLED.setBrightness(120);
  for (i = 0; i < NUM; i++)
  {
    leds[i] = mycolor;
    FastLED.show();
    delay(20);
    mycolor.h++;
  }
    for (i = NUM-1; i >=0; i--)
  {
    leds[i] = mycolor;
    FastLED.show();
    delay(20);
    mycolor.h++;
  }
}

// 三点灯
void three() {
  FastLED.clear();
  FastLED.show();
  int i;
  FastLED.setBrightness(120);
  for(i=0; i<NUM-2;i++  )
  {
    fill_solid(leds+i,3,currentColor  );
    FastLED.show();
    delay(10);
    fill_solid(leds+i,3,CRGB::Black  );
    FastLED.show();
    delay(10);
  }
  delay(200);
  for(i=NUM-3; i>=0;i--  )
  {
    fill_solid(leds+i,3,currentColor  );
    FastLED.show();
    delay(10);
    fill_solid(leds+i,3,CRGB::Black  );
    FastLED.show();
    delay(10);
  }
  delay(200);
}

// 单色亮
void single() {
  fill_solid(leds, NUM, currentColor);
  for (int fadeValue = 0 ; fadeValue <= 120; fadeValue += 5) {
    // sets the value (range from 0 to 255):
    FastLED.setBrightness(fadeValue);
    FastLED.show();
    // wait for 30 milliseconds to see the dimming effect
    delay(30);
  }

  // fade out from max to min in increments of 5 points:
  for (int fadeValue = 120 ; fadeValue >= 0; fadeValue -= 5) {
    // sets the value (range from 0 to 255):
    FastLED.setBrightness(fadeValue);
    FastLED.show();
    // wait for 30 milliseconds to see the dimming effect
    delay(30);
  }
}
