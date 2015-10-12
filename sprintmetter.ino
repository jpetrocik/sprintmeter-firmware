
#include <avr/sleep.h>
#include "sleep.h"

/**
 * Board Notes
 *
 * v1.3 
 * Removed EEPROM
 * 
 * v1.2 
 * Enable Internal PullUp
 * Check Interrupts: Reed Falling, Wake Low
 */


#define BOUNCE_DELAY 20

#define REED_SENSOR 3
#define BT_ENABLE 7
#define RED_LED  5
#define SLEEP 1200000
#define DATA_BUFFER 1500 //about 1000' of buffered data

//current read set by interrupt
volatile long triggeredTime;
volatile int triggered = false;

long data[DATA_BUFFER];  
int dataStart = 0;   //set to 1 so first interrupt wont result in out of bounds error
int dataEnd = 0;  //set to 1 so first interrupt wont result in out of bounds error
int dataOverflow=0; //set when previous read hasn't been processed



void setup(){
  Serial.begin(9600);

  //Turn on i2c and set to 400Hz
  Wire.begin();
  TWBR = 12;

  // disable ADC for power savings
  ADCSRA = 0;

  pinMode(REED_SENSOR, INPUT_PULLUP);
  pinMode(BT_ENABLE, OUTPUT);
  pinMode(RED_LED, OUTPUT);

  digitalWrite(BT_ENABLE, HIGH);
  digitalWrite(RED_LED, HIGH);

  attachInterrupt(1, sensorRead_isr, FALLING);
  
  for (int i = 0; i < 32; i++){
    data[i]=0;
  }
}

void loop(){

  //check for new data
  if (triggered){
    int previousSpltcksum = data[dataEnd%DATA_BUFFER];
    int split = 0;

    split = triggeredTime-checksum;
    
    //save split time
    dataEnd++;
    data[dataEnd%DATA_BUFFER] = split;
    
    
    //sends split and checksum
    //to ensure message wasn't lost
    //previous_checksum + split = checksum
    Serial.print("BMX");
    Serial.write(split>>8);
    Serial.write(split);
    Serial.write(checksum>>24);
    Serial.write(checksum>>16);
    Serial.write(checksum>>8);
    Serial.write(checksum);

    triggered=false;
  }
  
  //goto sleep after long delay
  if (millis() - triggeredTime > SLEEP){
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
}

/* 
 * Triggered by magnet to record time
 */
void sensorRead_isr(){
  long now = millis();
  if( now - triggeredTime > BOUNCE_DELAY ){
    triggeredTime=millis();
    triggered=true;
  }
}

/*
 * Triggered by magnet when in sleep mode
 */
void wake_isr()
{
  sleep_disable();
  detachInterrupt(0);
  triggeredTime=millis();
}




