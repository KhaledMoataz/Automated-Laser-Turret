//www.elegoo.com
//2016.12.12

#include "IRremote.h"
#include <Servo.h>
#include <math.h>
#include <EEPROM.h>
#include <LiquidCrystal.h>

LiquidCrystal lcd(3, 4, 5, 6, 7, 8);
  
Servo SX;
Servo SY;

const int receiver = 12; // Signal Pin of IR receiver to Arduino Digital Pin 6

const int servo_x_pin = 9;
const int servo_y_pin = 11;
const int shooter_pin = 13;

int remoteX = 0, remoteY = 0;
int savedX = 0, savedY = 0;
bool remoteShoot = false;
long previousRemote = 0;
const int maxShootDuration = 100;
int shootDuration;

int x_pixel = 0, y_pixel = 0, diameterPixels = 0;
float x_dist = 0, y_dist = 0; //mapping x_pixel in centimeters
double thetaX = 0;
double thetaY = 0;

double near = 325; //368.64639;//324;
double distance;
//const int distance = 75;
float ratio; // = distance / near;//0.20344699428; //cm per pixel
const int margin = 5;

int state = 0;
/*-----( Declare objects )-----*/
IRrecv irrecv(receiver); // create instance of 'irrecv'
decode_results results;  // create instance of 'decode_results'
int mode = -1;           // mode of operation

void setup()
{
  SX.attach(servo_x_pin);
  SY.attach(servo_y_pin);
  pinMode(shooter_pin, OUTPUT);
  irrecv.enableIRIn(); // Start the receiver
  lcd.begin(16, 2);
  Serial.begin(9600);
  savedX = byteToInt(EEPROM.read(0));
  savedY = byteToInt(EEPROM.read(1));
  lcd.clear();
  lcd.setCursor(0,0);
  lcd.print("Load x= ");
  lcd.print(savedX);
  lcd.setCursor(0,1);
  lcd.print("Load y= ");
  lcd.print(savedY);
  delay(2000);
  selectMode();
}

void loop()
{
  checkIR();
  bool valid;
  switch (mode)
  {
  case 1:
    //input new x, y
    valid = false;
    if (state == 0){
      x_pixel = readInteger(0, valid);
      if (!valid)
        return;
    }
    if (state == 1){
      y_pixel = readInteger(1, valid);
      if (!valid)
        return;
    }
    if (state == 2){
      diameterPixels = readInteger(2, valid);
      if (!valid)
        return;
    }
    if (state != 3) return;
    // Calculate distance
    ratio = 25.0f / diameterPixels;
    distance = ratio * near;
//    Serial.write((int)distance);
    // Translate to the center
    x_pixel -= 200;
    y_pixel -= 112;

    // actual distance in real life
    x_dist = x_pixel * ratio + remoteX;
    y_dist = y_pixel * ratio + remoteY;

    // calculating angle
    thetaX = 90 - atan2(x_dist, distance) * 180 / 3.14;
    thetaX = constrain(thetaX, 10, 170);

    thetaY = 90 - atan2(y_dist, distance) * 180 / 3.14;
    thetaY = constrain(thetaY, 10, 170);
    SX.write(thetaX);
    SY.write(thetaY);
    state = 0;
    break;
  case 2:
    remoteX = constrain(remoteX, -80, 80);
    remoteY = constrain(remoteY, -80, 80);
    SX.write(90 + remoteX);
    SY.write(90 + remoteY);
    if(remoteShoot) {
      digitalWrite(shooter_pin, HIGH);
    } else {

      digitalWrite(shooter_pin, LOW);
    }
    break;
  }

//  Serial.println(remoteX);
//  Serial.println(remoteY);
//  Serial.println(remoteShoot);

} /* --end main loop -- */

void selectMode()
{
  lcd.clear();
  lcd.setCursor(0,0);
  lcd.print("Select a Mode");
  lcd.setCursor(0,1);
  lcd.print("1:Mobile   2:IR");        
  remoteX = remoteY = 0;
  remoteShoot = false;
  mode = -1;
  SX.write(90);
  SY.write(90);
  while (1)
  {
    if (irrecv.decode(&results))
    {
      previousRemote = results.value;
      switch (results.value)
      {
      case 0xFF30CF:
        lcd.clear();
        lcd.setCursor(0,0);
        lcd.print("Mode 1 Selected");
        lcd.setCursor(0,1);
        lcd.print("-----Mobile-----");
        mode = 1;
        remoteX = savedX;
        remoteY = savedY;
        while (!Serial){}      // wait for serial port to connect. Needed for native USB port only
        digitalWrite(shooter_pin, HIGH);
        break; // button: 1
      case 0xFF18E7:
        lcd.clear();
        lcd.setCursor(0,0);
        lcd.print("Mode 2 Selected");
        lcd.setCursor(0,1);
        lcd.print("-----Remote-----");
        mode = 2;
        break; // button: 2
      }
      irrecv.resume(); // receive the next value
    }
    if (mode != -1)
    {
      return;
    }
  }
}

int readInteger(byte type, bool &valid)
{
  byte l, r;
  int x;
  if (Serial.available() > 0)
  {
    r = Serial.read();
    if ((r >> 6) != type)
    {
      state = 0;
      Serial.write(126);
      valid = false;
      return 0;
    }
  }
  else{
    valid = true;
    return 0;
  }
  while (1)
  {
    if (Serial.available() > 0)
    {
      l = Serial.read();
      if ((l >> 6) != type)
      {
        state = 0;
        Serial.write(125);
        valid = false;
        return 0;
      }
      break;
    }
  }
  state++;
  l &= 0x3f;
  r &= 0x3f;
  x = l;
  x <<= 6;
  x += r;
  valid = true;
  return x;
}

int byteToInt(byte x) {
  int result = x;
  if(x & 128) {
    result |= 0xFF00;
  }
  return result;
}

void checkIR(){
  shootDuration++;
  if (irrecv.decode(&results)) // have we received an IR signal?
  {
    if (results.value != 0xFFFFFFFF) {
      previousRemote = results.value;
    }
    switch (previousRemote)
    {
    case 0xFFA25D: // button: Power
      selectMode();
      break;
    case 0xFF629D: // button: vol+
      remoteY += 1;
      break;
    case 0xFFA857: // button: vol-
      remoteY -= 1;
      break;
    case 0xFFC23D: // button: >>| Fast Forward
      remoteX -= 1;
      break;
    case 0xFF22DD: // button: |<< Fast Back
      remoteX += 1;
      break;
    case 0xFF02FD: // button: Play/Pause
      remoteShoot = true;
      shootDuration = 0;
      break;
    case 0xFF4AB5: // button: 8
      if (savedX != remoteX) {
        EEPROM.write(0, remoteX);
        savedX = remoteX;
        lcd.clear();
        lcd.setCursor(0,0);
        lcd.print("Save x= ");
        lcd.print(remoteX);
      }
      if (savedY != remoteY) {
        EEPROM.write(1, remoteY);
        savedY = remoteY;
        lcd.setCursor(0,1);
        lcd.print("                ");
        lcd.setCursor(0,1);
        lcd.print("Save y= ");
        lcd.print(remoteY);
      }
    break;
    }
    irrecv.resume(); // receive the next value
  }

  if(shootDuration >= maxShootDuration) {
    remoteShoot = false;
    shootDuration = 0;
  }
}

