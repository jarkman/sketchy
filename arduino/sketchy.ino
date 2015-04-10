//#include <NewSoftSerial.h>


#include <Servo.h> 
 
#define  DO_LOGGING

float baseZ = 220; // neutral plane - we get about +-70mm in x and y on the 220 plane
float baseSensorRange = 33; // height from paper (as measured by sensor) when we are at neutral plane

float sensorRange = 0.0;
int numPoints = 0;
boolean pathWorking = true;

boolean motionWorking = false;

float max_servo_speed = 0.0;
float sum_servo_speed = 0.0;
float num_servo_speed = 1.0;

void moveTo( float x0, float y0, float z0, boolean s = true );
boolean addPoint( int x, int y, int z, int overhead = 2 );


void setup() 
{ 
  
  
  #ifdef DO_LOGGING
  Serial.begin (9600); // for debugging

  Serial.print ("setup\n");
  #endif

  setupControls();
  setupRangeSensor();
  setupPosition();
  setupMotion();
  setupBluetooth();
  
  //buildPath();

  startReadingBluetooth();
}




float z = 0;
boolean wasOff = false;

#define STATE_CALIBRATE 0
#define STATE_RUN 1
#define STATE_HOME 2
#define STATE_SERVO_OFF 3
#define STATE_IDLE 4
#define STATE_READING_BLUETOOTH 5

int state = STATE_RUN;

void loop() 
{ 
  
 
  if( readHouseSwitch() && state == STATE_READING_BLUETOOTH )
 {
    clearPath();
    buildDemoPath();
    startCalibrate();
 }
 
  if( readHomeSwitch() && state != STATE_HOME)
  {
    state = STATE_HOME;
    #ifdef DO_LOGGING
    Serial.println ("STATE_HOME");
    #endif
    turnOnServos();
    stopPath();

  }
  
  if( readServoOffSwitch() && state != STATE_SERVO_OFF)
  {
    state = STATE_SERVO_OFF;
    #ifdef DO_LOGGING
    Serial.println ("STATE_SERVO_OFF");
    #endif
    turnOffServos();
    stopPath();
    wasOff = true;
  }

  
  if( (state ==  STATE_HOME || state == STATE_SERVO_OFF) && 
        ! readHomeSwitch() && ! readServoOffSwitch())
  {
    // calibrate when eiher mode is switched off, just to test calibrate and prove we are alive
    clearPath();
    startCalibrate();
  }
  

  
  if( state == STATE_HOME )
  {
    if( ! motionWorking )
      moveTo( 0.0,0.0,-5.0 );
  }
  
  if( state == STATE_RUN )
  {
   boolean done = ! loopPath();
   
   if( done )
   {
     clearPath();
     startReadingBluetooth();
   }
  }
   
   
  if( state == STATE_READING_BLUETOOTH )
  {
   loopBluetooth();
  }
  
  
   if( state != STATE_IDLE && state != STATE_READING_BLUETOOTH)
   {
     
     if( state == STATE_CALIBRATE)
     {
       loopRangeSensor(); 
       loopCalibrate();
     }
     
     loopMotion();
   }
   
}

void startReadingBluetooth()
{
  state = STATE_READING_BLUETOOTH;
  #ifdef DO_LOGGING
  Serial.println ("STATE_READING_BLUETOOTH");
  #endif
  
  turnOffServos();
  turnOnBluetooth();
}






void startRun()
{
  #ifdef DO_LOGGING
   Serial.println ("STATE_RUN");
  #endif
     
    state = STATE_RUN;

    startPath();
}
void loopSensorTest()
{
  float error = sensorRange - 10.0;
  z = z + error/4.0;
  if( z > 20.0 )
    z = 20.0;
  if( z < -20.0 )
    z = -20.0;
  goTo( 0.0, 0.0, z );
}

void buildDemoPath()
{

  
  addPoint( 0,0,-10);
  
  //dog();
  house();
 
  addPoint( -70, -70 ,-15);
}

void dog()
{
  rectangle( -30, 0, 30, 20 );
  rectangle( 30, 20, 45, 40 );
  addPoint( -28, 0, -10 );
  addPoint( -28, 0, 0 );
  addPoint( -40, -20, 0 );
  addPoint( -28, 0, 0 );
  addPoint( -28, -20, 0 );
  addPoint( 28, 0, 0 );
  addPoint( 28, -20, 0 );
  addPoint( 28, -20, -10 );
  addPoint( 40, 35, -10 );
  addPoint( 40, 35, 0 );
  addPoint( -70, -70, -10 );
}
void house()
{
  square( 50 );
  
  // roof
  addPoint( -50, 50, 0);
  addPoint( 0, 70, 0);
  addPoint( 50, 50, 0);
  addPoint( 50, 50, -10);
  
  //chimney
  rectangle( 20, 50, 30, 80 );

// door
  rectangle( -15, -50, 15, 0 ); 

  // window
  
  rectangle( -40, -20, -15, 0 );
  rectangle( 40, -20, 15, 0 );

  rectangle( -40, 20, -15, 40 );
  rectangle( 40, 20, 15, 40 );
  
  rectangle( 8, -23, 12, -27 );

}

void squares()
{
   square(10);
   square(20);
  square(30);
  square(40);
  square(50);
 square( 60 );
 square( 70 );
}

void rectangle( int x1, int y1, int x2, int y2 )
{
 addPoint( x1, y1,-10);
  addPoint( x1, y1, 0 );

  addPoint( x1, y2, 0 );
  addPoint( x2, y2, 0 );
  addPoint( x2, y1, 0 );

  addPoint( x1, y1, 0 );
  addPoint( x1, y1,-10);
  
 } 
 
void square( int x )
{
 addPoint( x,x,-10);
  addPoint( x, x, 0 );

  addPoint( x, -x, 0 );
  addPoint( -x, -x, 0 );
  addPoint( -x, x, 0 );

  addPoint( x, x, 0 );
  addPoint( x,x,-10);
  
 } 


#define HOME_PIN 11
#define SERVO_OFF_PIN 12
#define HOUSE_PIN 7

void setupControls()
{
  pinMode( HOME_PIN, INPUT );
  pinMode( SERVO_OFF_PIN, INPUT );
  pinMode( HOUSE_PIN, INPUT );
  
  // pullups:
  digitalWrite( HOME_PIN, HIGH );
  digitalWrite( SERVO_OFF_PIN, HIGH );
  digitalWrite( HOUSE_PIN, HIGH );
}

int readHomeSwitch()
{
  return LOW == digitalRead( HOME_PIN );
}

int readHouseSwitch()
{
  //return 0;
  
  return LOW == digitalRead( HOUSE_PIN );
}



int readServoOffSwitch()
{
  return LOW == digitalRead( SERVO_OFF_PIN );
}
