//#include <Wire.h>
#include <avr/sleep.h>
#include <Ticker.h>
//#include "sleep.h"

/**
   Board Notes

   v1.3
   Removed EEPROM

   v1.2
   Enable Internal PullUp
   Check Interrupts: Reed Falling, Wake Low
*/

#define SLEEP_ENABLED

#define STATE A3
#define KEY A2
#define WHEEL_SENSOR 3
#define BT_ENABLE 7
#define RED_LED  A0

#define SLEEP 180000000L
//#define SLEEP 15000000L
#define DATA_BUFFER 1500 //about 1000' of buffered data

unsigned long previousTriggerTime = 0;
unsigned int seqChecksum = 0;
volatile unsigned long triggeredTime = 0;
volatile int triggered = false;
volatile int error = false;

void tick();

Ticker ledTicker(tick, 250);

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

  //http://www.gammon.com.au/power
  //disable ADC for power savings
  ADCSRA = 0;

  enableBluetooth();

  attachInterrupt(1, sensorRead_isr, FALLING);

  ledTicker.start();
}

void enableBluetooth() {
  digitalWrite(BT_ENABLE, LOW);
  digitalWrite(KEY, LOW);
  delay(1000);
  digitalWrite(BT_ENABLE, HIGH);
}

void tick() {
  int state = digitalRead(RED_LED);
  digitalWrite(RED_LED, !state);
}

void loop() {
  ledTicker.update();

  int btConnected = digitalRead(STATE);
  if (!btConnected && ledTicker.state() == STOPPED) {
    ledTicker.start();
  } else if (btConnected) {
    ledTicker.stop();
    digitalWrite(RED_LED, HIGH);
  }

  //check for new data
  if (triggered) {
    //calculate current split time
    unsigned long split = triggeredTime - previousTriggerTime;
    seqChecksum++;

    //save previous trigger time to calcuate split next trigger
    previousTriggerTime = triggeredTime;

    //TODO Improve error handling
    if (error) {
      seqChecksum++;
      error = false;
    }
    
    if (btConnected) {

      //sends split and seqCheck
      //to ensure message wasn't lost
      Serial.print("BMX");
      Serial.write(split >> 24);
      Serial.write(split >> 16);
      Serial.write(split >> 8);
      Serial.write(split);
      Serial.write(seqChecksum >> 8);
      Serial.write(seqChecksum);

    }
    triggered = false;
  }

#ifdef SLEEP_ENABLED
  //goto sleep after long delay
  long lastRead = micros() - triggeredTime;
  if ( lastRead > SLEEP) {
    ledTicker.stop();

    digitalWrite(BT_ENABLE, LOW);
    digitalWrite(RED_LED, LOW);

    //remove current interrupt, sensorRead_isr()
    detachInterrupt(1);

    //disable ADC
    ADCSRA = 0;

    set_sleep_mode(SLEEP_MODE_PWR_DOWN);
    noInterrupts();
    sleep_enable();

    //setup new interrupt, wake_isr()
    attachInterrupt(1, wake_isr, HIGH);

    // turn off brown-out enable in software
    MCUCR = bit (BODS) | bit (BODSE);
    MCUCR = bit (BODS); 
  
    interrupts();
    sleep_cpu();
  }
#endif

}

/*
   Triggered by magnet to record time
*/
void sensorRead_isr() {
  if (triggered) {
    error = true;
    return;
  }

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
  detachInterrupt(1);

  digitalWrite(BT_ENABLE, HIGH);
  digitalWrite(RED_LED, HIGH);

  attachInterrupt(1, sensorRead_isr, FALLING);

  triggeredTime = micros();
}
#endif




