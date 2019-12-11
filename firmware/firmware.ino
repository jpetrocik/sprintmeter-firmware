//#include <Wire.h>
#include <avr/sleep.h>
#include "sleep.h"

/**
   Board Notes

   v1.3
   Removed EEPROM

   v1.2
   Enable Internal PullUp
   Check Interrupts: Reed Falling, Wake Low
*/

//#define SLEEP_ENABLED

#define BOUNCE_DELAY 5

#define STATE A3
#define KEY A2
#define WHEEL_SENSOR 3
#define BT_ENABLE 7
#define RED_LED  A0

#define SLEEP 1200000
#define DATA_BUFFER 1500 //about 1000' of buffered data

unsigned long previousTriggerTime = 0;
volatile unsigned long triggeredTime = 0;
volatile int triggered = false;

void setup() {
  Serial.begin(115200);
  pinMode(RED_LED, OUTPUT);
  pinMode(KEY, OUTPUT);
  pinMode(BT_ENABLE, OUTPUT);
  pinMode(STATE, INPUT_PULLUP);
  pinMode(WHEEL_SENSOR, INPUT);

  //Turn on i2c and set to 400Hz
  //Wire.begin();
  //TWBR = 12;

  //disable ADC for power savings
  ADCSRA = 0;

  enableBluetooth();

  attachInterrupt(1, sensorRead_isr, FALLING);

  digitalWrite(RED_LED, HIGH);
}

void enableBluetooth() {
  digitalWrite(BT_ENABLE, LOW);
  digitalWrite(KEY, LOW);
  delay(1000);
  digitalWrite(BT_ENABLE, HIGH);
}

void loop() {

  int btConnected = digitalRead(STATE);

  //check for new data
  if (triggered) {
    //calculate current split time
    unsigned long split = triggeredTime - previousTriggerTime;

    //save previous trigger time to calcuate split next trigger
    previousTriggerTime = triggeredTime;

    if (btConnected) {

      //sends split and seqCheck
      //to ensure message wasn't lost
      Serial.print("BMX");
      Serial.write(split >> 24);
      Serial.write(split >> 16);
      Serial.write(split >> 8);
      Serial.write(split);
      Serial.write(13);
      Serial.write(10);
      
    }
    triggered = false;
  }

#ifdef SLEEP_ENABLED
    //goto sleep after long delay
    if (micros() - triggeredTime > SLEEP){
      digitalWrite(BT_ENABLE, LOW);
      digitalWrite(RED_LED, LOW);
  
      detachInterrupt(1);
  
      sleep_enable();
      attachInterrupt(1, wake_isr, HIGH);
      set_sleep_mode(SLEEP_MODE_PWR_DOWN);
      cli();
      sei();
      sleep_cpu();
      sleep_disable();
  
      digitalWrite(BT_ENABLE, HIGH);
      digitalWrite(RED_LED, HIGH);
      attachInterrupt(1, sensorRead_isr, FALLING);

    }
#endif
    
}

/*
   Triggered by magnet to record time
*/
void sensorRead_isr() {
  triggeredTime = micros();
  triggered = true;
}

/*
   Triggered by magnet to wake up when in sleep mode
*/
#ifdef SLEEP_ENABLED
void wake_isr()
{
  sleep_disable();
  detachInterrupt(0);
  triggeredTime = micros();
}
#endif




