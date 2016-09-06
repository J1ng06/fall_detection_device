// =========================================================
// ===           4IO6 - CAPSTONE PROJECT                 ===       
// ===     SUDDEN FALL DEVICE FOR THE ELDERLY            ===
// ===  CODE WRITTEN BY: JINGNAN CHEN & SEBASTIEN WOO    ===
// =========================================================


// =========================================================
// ===                  DECLARATIONS                     ===
// =========================================================

#include "Wire.h"
#include "I2Cdev.h"
#include "MPU6050_6Axis_MotionApps20_10Hz.h"
#include <SoftwareSerial.h>

MPU6050 mpu;

#define OUTPUT_READABLE_YAWPITCHROLL
#define OUTPUT_READABLE_REALACCEL
#define LED_PIN 13 // (Arduino is 13, Teensy is 11, Teensy++ is 6)
#define ALARM_LED_PIN 4 // For testing, LED alarm
#define BUTTON 12 // push button for disabling alarm
#define PIEZO 6 //Piezoelectric Speaker output
#define CONNECT_FLAG 13 // Connection indicator

bool blinkState = false; // program running LED indicator

// MPU control/status vars
bool dmpReady = false;  // set true if DMP init was successful
uint8_t mpuIntStatus;   // holds actual interrupt status byte from MPU
uint8_t devStatus;      // return status after each device operation (0 = success, !0 = error)
uint16_t packetSize;    // expected DMP packet size (default is 42 bytes)
uint16_t fifoCount;     // count of all bytes currently in FIFO
uint8_t fifoBuffer[64]; // FIFO storage buffer

// orientation/motion vars
Quaternion q;           // [w, x, y, z]         quaternion container
VectorInt16 aa;         // [x, y, z]            accel sensor measurements
VectorInt16 aaReal;     // [x, y, z]            gravity-free accel sensor measurements
//VectorInt16 aaWorld;    // [x, y, z]            world-frame accel sensor measurements
VectorFloat gravity;    // [x, y, z]            gravity vector
float euler[3];         // [psi, theta, phi]    Euler angle container
float ypr[3];           // [yaw, pitch, roll]   yaw/pitch/roll container and gravity vector
int16_t ax, ay, az;
int16_t gx, gy, gz;

//================================================================
//===                      Variables                           ===
//================================================================

float pitch, roll, ref_pitch, ref_roll; //Gyro
float x_accel, y_accel, z_accel, accel_vector; //Accelerometer
float sinVal, toneVal; //Piezo Speaker
float sum;
int calibration_count = 0;
boolean flag_1, calibrate, stablize, trigger = false;
String Message;
String Message_Arduino;
char ACK = 'A';
char ALARM ='f';
char CANCEL = 'c';
char bluetooth_echo;
int PB_buf;

//THRESHOLDS
float ACCEL_THRESHOLD = 2.5;
int PITCH_THRESHOLD = 50;
int ROLL_THRESHOLD=50;

// ================================================================
// ===               INTERRUPT DETECTION ROUTINE                ===
// ================================================================

volatile bool mpuInterrupt = false;     // indicates whether MPU interrupt pin has gone high
void dmpDataReady() {
  mpuInterrupt = true;
}

SoftwareSerial mySerial(10,11); // RX & TX

// ================================================================
// ===                      INITIAL SETUP                       ===
// ================================================================

void setup() {
  // join I2C bus (I2Cdev library doesn't do this automatically)

  Wire.begin();
  Serial.begin(9600);
  mySerial.begin(9600);
  while (!Serial); // wait for Leonardo enumeration, others continue immediately

  Serial.println(F("Initializing I2C devices..."));
  mpu.initialize();

  // verify connection
  Serial.println(F("Testing device connections..."));
  Serial.println(mpu.testConnection() ? F("MPU6050 connection successful") : F("MPU6050 connection failed"));

  // wait for ready
  //pinMode(BUTTON, INPUT);
  Serial.println(F("\nSend any character to begin DMP programming and demo: "));

  // load and configure the DMP
  Serial.println(F("Initializing DMP..."));
  devStatus = mpu.dmpInitialize();
  mpu.setFullScaleAccelRange(0x01);    //Set Accel Range to +/-4g

  if (devStatus == 0) {
    // turn on the DMP, now that it's ready
    Serial.println(F("Enabling DMP..."));
    mpu.setDMPEnabled(true);

    // enable Arduino interrupt detection
    Serial.println(F("Enabling interrupt detection (Arduino external interrupt 0)..."));
    attachInterrupt(0, dmpDataReady, RISING);
    mpuIntStatus = mpu.getIntStatus();

    // set our DMP Ready flag so the main loop() function knows it's okay to use it
    Serial.println(F("DMP ready! Waiting for first interrupt..."));
    dmpReady = true;

    // get expected DMP packet size for later comparison
    packetSize = mpu.dmpGetFIFOPacketSize();
  } 
  else {
    // ERROR!
    // 1 = initial memory load failed
    // 2 = DMP configuration updates failed
    // (if it's going to break, usually the code will be 1)
    Serial.print(F("DMP Initialization failed (code "));
    Serial.print(devStatus);
    Serial.println(F(")"));
  }

  // configure Button, Speaker and the flag for connection
  pinMode(BUTTON, INPUT);
  digitalWrite(BUTTON,HIGH);
  pinMode(PIEZO, OUTPUT);
  pinMode(CONNECT_FLAG,INPUT);
}

// ================================================================
// ===                    MAIN PROGRAM LOOP                     ===
// ================================================================

void loop() {

  if (!dmpReady) return;

  if (stablize) { //wait for the sensors to stablize

      //=============================================================
    //===                    STANDALONE MODE                    ===
    //=============================================================

    while(digitalRead(CONNECT_FLAG)==0){
      while (!mpuInterrupt && fifoCount < packetSize) {
      }
      mpuInterrupt = false;
      mpuIntStatus = mpu.getIntStatus();
      fifoCount = mpu.getFIFOCount();

      if ((mpuIntStatus & 0x10) || fifoCount == 1024) {
        mpu.resetFIFO();
      } 
      else if (mpuIntStatus & 0x02) {

        // wait for correct available data length, should be a VERY short wait
        while (fifoCount < packetSize) fifoCount = mpu.getFIFOCount();

        // read a packet from FIFO
        mpu.getFIFOBytes(fifoBuffer, packetSize);

        // track FIFO count here in case there is > 1 packet available
        // (this lets us immediately read more without waiting for an interrupt)
        fifoCount -= packetSize;
        mpu.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);  
        //Data transmition via the bluetooth module (HC-05)

        // display Euler angles in degrees
        mpu.dmpGetQuaternion(&q, fifoBuffer);
        mpu.dmpGetGravity(&gravity, &q);
        mpu.dmpGetYawPitchRoll(ypr, &q, &gravity);

        //create a message containing information from the gyro and sensor
        pitch = ypr[1] * 180/M_PI; //get the pitch and parse it to the message
        Message =String(int(pitch)) + "." + String(getDecimal(pitch))+","; //Take the whole number, then the decimal portion
        roll = ypr[2] * 180/M_PI; //get the roll and parse it to the message
        Message +=String(int(roll)) + "." + String(getDecimal(roll))+","; 
        //divide by 8092
        x_accel = (float(ax)/8192); 
        Message +=String(int(ax))+","; //parse the x-axis acceleration
        y_accel = (float(ay)/8192);
        Message +=String(int(ay))+","; //parse the y-axis acceleration
        z_accel = (float(az)/8192);
        Message +=String(int(az))+"/"; //parse the z-axis acceleration with a terminator "/" for end of the 5 raw inputs
        accel_vector = sqrt(x_accel*x_accel + y_accel*y_accel + z_accel*z_accel); // calculating the acceleration vector
        //parse the acceleration vector to the message and include a recognition symbol "S/" indicating in Standalone Mode
        Message +=String(int(accel_vector))+ "."+String(getDecimal(accel_vector))+"S/"; 
        Serial.println(Message); 

        if (accel_vector > ACCEL_THRESHOLD){         
          if (abs(pitch) > PITCH_THRESHOLD || abs(roll) > ROLL_THRESHOLD){
          //if (pitch > float(PITCH_THRESHOLD) || roll > float(ROLL_THRESHOLD) || pitch > float(-PITCH_THRESHOLD)|| roll > float(-ROLL_THRESHOLD)){
            //Serial.print(pitch); Serial.print(", ");Serial.println(roll);
            PB_buf = digitalRead(BUTTON);
            trigger = true;
            while (trigger){
              setAlarm();
            }
          }  
        }
      }
    }

    //================================================================
    //===                    MASTER-SLAVE MODE                     ===
    //================================================================

    while(digitalRead(CONNECT_FLAG)==1){
      if(!mySerial.available()){
        while (!mpuInterrupt && fifoCount < packetSize) {
        }
        mpuInterrupt = false;
        mpuIntStatus = mpu.getIntStatus();
        fifoCount = mpu.getFIFOCount();

        if ((mpuIntStatus & 0x10) || fifoCount == 1024) {
          mpu.resetFIFO();
        } 
        else if (mpuIntStatus & 0x02) {

          // wait for correct available data length, should be a VERY short wait
          while (fifoCount < packetSize) fifoCount = mpu.getFIFOCount();

          // read a packet from FIFO
          mpu.getFIFOBytes(fifoBuffer, packetSize);

          // track FIFO count here in case there is > 1 packet available
          // (this lets us immediately read more without waiting for an interrupt)
          fifoCount -= packetSize;

          mpu.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);  

          // display Euler angles in degrees
          mpu.dmpGetQuaternion(&q, fifoBuffer);
          mpu.dmpGetGravity(&gravity, &q);
          mpu.dmpGetYawPitchRoll(ypr, &q, &gravity);
          //Message = String("888,");
          //create a message containing information from the gyro and sensor
          sum = 0;
          pitch = ypr[1] * 180/M_PI; //get the pitch and parse it to the message
          Message =String(int(pitch))+ "."+String(getDecimal(pitch))+","; // get the whole number and the decimal portion
          roll = ypr[2] * 180/M_PI; //get the roll and parse it to the message
          Message +=String(int(roll))+ "."+String(getDecimal(roll))+","; 
          //divede by 8092
          x_accel = (float(ax)/8192);
          Message +=String(int(ax))+","; //parse the x-axis acceleration
          y_accel = (float(ay)/8192);
          Message +=String(int(ay))+","; //parse the y-axis acceleration
          z_accel = (float(az)/8192);
          Message +=String(int(az))+"/"; //parse the z-axis acceleration
          //sum = x_accel + y_accel + z_accel;
          sum = pitch + roll + float(ax) + float(ay) + float(az);
          sum = abs(int(sum));
          //Message = String(int(sum))+Message; 
          Message = String(int(sum))+","+Message; 
          Serial.println(Message);
          accel_vector = sqrt(x_accel*x_accel + y_accel*y_accel + z_accel*z_accel); //calculate the acceleration vector
          mySerial.print(Message); //pass the message via bluetooth
          //Message +=String(int(accel_vector))+ "."+String(getDecimal(accel_vector))+"A/"; 
          //Serial.println(Message);
        }
      }
      //Check to see if a sudden acceleration was detected
      if (mySerial.available()){        
        if (mySerial.read()==ALARM){
          PB_buf = digitalRead(BUTTON);
          trigger = true;
          while (trigger){
             setAlarm();
          }
          //Clear the buffer to insure alarm does not get triggered again
          while(mySerial.available()){
            Serial.write(mySerial.read());
          }
        }  
      }
    }
  }
  else { // waiting for the mpu to stablize (500)
    stablization();
  }


  // blink LED to indicate activity
  blinkState = !blinkState;
  digitalWrite(LED_PIN, blinkState);
}

//=============================================================
//===                      FUNCTIONS                        ===
//=============================================================

void calc(){

  mpu.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);  
  //Data transmition via the bluetooth module (HC-05)

  // display Euler angles in degrees
  mpu.dmpGetQuaternion(&q, fifoBuffer);
  mpu.dmpGetGravity(&gravity, &q);
  mpu.dmpGetYawPitchRoll(ypr, &q, &gravity);
  pitch = ypr[1] * 180/M_PI;
  Message =String(int(pitch))+ "."+String(getDecimal(pitch))+",";
  roll = ypr[2] * 180/M_PI;
  Message +=String(int(roll))+ "."+String(getDecimal(roll))+","; 
  //divede by 8092
  x_accel = (float(ax)/8092);
  Message +=String(int(ax))+","; 
  //Serial.print(x_accel);
  y_accel = (float(ay)/8092);
  Message +=String(int(ay))+","; 
  z_accel = (float(az)/8092);
  Message +=String(int(az))+"/";

  accel_vector = sqrt(x_accel*x_accel + y_accel*y_accel + z_accel*z_accel);

  // blink LED to indicate activity
  blinkState = !blinkState;
  digitalWrite(LED_PIN, blinkState);
}

long getDecimal(float val){
  int intPart = int(val);
  long decPart = 1000*(val-intPart); //I am multiplying by 1000 assuming that the foat values will have a maximum of 3 decimal places
  //Change to match the number of decimal places you need
  if(decPart>0)return(decPart);           //return the decimal part of float number if it is available 
  else if(decPart<0)return((-1)*decPart); //if negative, multiply by -1
  else if(decPart=0)return(00);           //return 0 if decimal part of float number is not available
}

void setAlarm(){
  for (int x=0; x<180; x++) {
    sinVal = (sin(x*(3.1412/180))); // convert degrees to radians then obtain sin value
    toneVal = 3500+(int(sinVal*1000)); // generate a frequency from the sin value
    tone(PIEZO, toneVal);   
    if(digitalRead(BUTTON)==!PB_buf){
      trigger=false;
      noTone(PIEZO);
      break;
    }else if(mySerial.available()){        
        if (mySerial.read()==CANCEL){
          trigger=false;
          noTone(PIEZO);
          //Clear the buffer to insure alarm does not get triggered again
          while(mySerial.available()){
            Serial.write(mySerial.read());
          }
          break;
        }  
    } 
  }     
}

void stablization(){
    if (calibration_count == 50) {
      stablize = true;
      Serial.println("stablized");
    }
    else {
      calibration_count++;
    }
}
