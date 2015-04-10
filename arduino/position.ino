
// position.pde

// Manages xyz->theta? transformation and servo drive for delta robots
// Maths from http://forums.trossenrobotics.com/tutorials/introduction-129/delta-robot-kinematics-3276/

// robot geometry for our rig
 // (look at pics in link above for explanation)
 const float side_from_radius = 3.466;  // we want the sides of the base and effector triangles, 
                                        // but what we can easily measure is the distance from the center to the midpoint of the side
                                        // so multiply that by 2/tan(30)
 const float e = side_from_radius * 25.0;     // end effector
 const float f = side_from_radius * 48;     // base
 const float re = 220.0;  // effector arm length
 const float rf = 37.0;   // base arm length
 
// limits of servo motion for our rig
int minServoPos = 0;
int maxServoPos = 160;


#define STATUS_LED 13 // lights up for bad positions

// from http://code.google.com/p/arduino/source/browse/#svn/trunk/libraries/Servo
#define MIN_PULSE_WIDTH       544.0     // the shortest pulse sent to a servo  
#define MAX_PULSE_WIDTH      2400.0     // the longest pulse sent to a servo 

Servo s0;
Servo s1;
Servo s2;

boolean servosAttached = false;

 
  
 int delta_calcInverse(float x0, float y0, float z0, float &theta1, float &theta2, float &theta3);
 int delta_calcAngleYZ(float x0, float y0, float z0, float &theta);
 boolean transformToServoAngle( float &theta );
void servoWriteFloat( Servo *s, float angle);
void servoWriteCalibrated( Servo *s, float angle);
boolean uSForAngle( float angle, int*result) ;





void setupPosition()
{
   pinMode(STATUS_LED, OUTPUT);
   
  turnOnServos();
   
   /*
   int s =  MIN_PULSE_WIDTH;  // gives us 28 degrees (above horizontal)
   s0.write( s);
   s1.write( s );
   s2.write( s );
   delay(10000);
   */
   /*
   int s =  ( MIN_PULSE_WIDTH + MAX_PULSE_WIDTH ) / 2;  // gives us -66 degrees (below horizontal)
   s0.write( s);
   s1.write( s );
   s2.write( s );
   delay(10000); 
   */
   /*
  // 120 degrees for checking servo cal - should be parallel with body
   servoWriteFloat( &s0, 120 );
   servoWriteFloat( &s1, 120 );
   servoWriteFloat( &s2, 120 ); 
   delay(20000); 
   */
   
   /*
   // 30 degrees for checking servo cal - should be horizontal
   servoWriteFloat( &s0, 30 );
   servoWriteFloat( &s1, 30 );
   servoWriteFloat( &s2, 30 ); 
   delay(20000); 
   */
   
   // 
   servoWriteCalibrated( &s0, 0 );
   servoWriteCalibrated( &s1, 0 );
   servoWriteCalibrated( &s2, 0 ); 
   delay(2000); 
   
   
   goTo( 0,0,-10); // for pen setup
   delay( 2000 );
   //resolutionTest();
   
  /* s = 0;
   s0.write( s);
   s1.write( s );
   s2.write( s );
   delay(1000); 
   */
  
}

void turnOnServos()
{
  if( ! servosAttached )
  {
   s0.attach(8);
   s1.attach(9);
   s2.attach(10);
  }
  
   servosAttached = true;
}
void turnOffServos()
{
  if( servosAttached )
  {
  s0.detach();
  s1.detach();
  s2.detach();
  }
  servosAttached = false;
  
}

int goTo( float x0, float y0, float z0 )
{
   float theta1;
   float theta2;
   float theta3;
   
   static float last_theta1;
   static float last_theta2;
   static float last_theta3;
   
  
    static float tLast = 0;
   
   
     
  if( 0 != delta_calcInverse( x0,  y0,  z0 + baseZ, theta1, theta2, theta3))
  {
    
    #ifdef DO_LOGGING
    Serial.print ("Unreachable pos: ");
    Serial.print (x0);
    Serial.print (", ");
    Serial.print (y0);
    Serial.print (", ");
    Serial.print (z0);
    Serial.print ("\n");
     #endif
     
      digitalWrite(STATUS_LED,true); //Status LED...
      return 0; // no pos
  }
  
  //digitalWrite(STATUS_LED,false);
  
  /*
   #ifdef DO_LOGGING
    Serial.print ("Pos: ");
    Serial.print (x0);
    Serial.print (", ");
    Serial.print (y0);
    Serial.print (", ");
    Serial.print (z0);
    Serial.print ("\n");
     #endif
   */
  
  boolean success = true; 
  int u1;
  int u2;
  int u3;
  
   success &=  uSForAngle( theta1, &u1);
   success &=  uSForAngle( theta2, &u2);
   success &=  uSForAngle( theta3, &u3);
   
 
  if( ! success )
  {
  #ifdef DO_LOGGING
    Serial.print ("Unreachable servo angle\n");
  #endif
   return 0;
  }
  
  digitalWrite(STATUS_LED,! success);
  
  s0.writeMicroseconds( u1 );
  s1.writeMicroseconds( u2 );
  s2.writeMicroseconds( u3 );
  

  float tNow = millis();
 
  logServoSpeed( tLast, tNow, theta1, last_theta1 );
  logServoSpeed( tLast, tNow, theta2, last_theta2 );
  logServoSpeed( tLast, tNow, theta3, last_theta3 );
  last_theta1 = theta1;
  last_theta2 = theta2;
  last_theta3 = theta3;
  tLast = tNow;
  return 1;
}

void logServoSpeed( float t1, float t2, float a1, float a2 )
{
  if( t1 == 0 )
    return;
    
  float spd = fabs( (a1-a2) / (t1-t2)); // degrees per millisec
  if( spd > max_servo_speed )
    max_servo_speed = spd;
    
  sum_servo_speed += spd;
  num_servo_speed += 1.0;
}

  


void servoWriteCalibrated( Servo *s, float angle)
{
  int uSecs;
  if( uSForAngle( angle, &uSecs))
    s->writeMicroseconds(uSecs);
}

// Use writeMicroseconds instead of write() to get more servo resolution, copy conversion formula from Arduino Servo lib source
// This gets us something like ten times the resolution, so we get much smoother motion.
// This version is uncalibrated and is replaced by servoWriteCalibrated above.

void servoWriteFloat( Servo *s, float angle)
{  

  float servoCal = -10.0;
  
  angle -= servoCal;
  
  if(angle < 0) 
    angle = 0;
    
    
  if(angle > 180) 
    angle = 180;
    
   int uS = MIN_PULSE_WIDTH + angle * ( MAX_PULSE_WIDTH - MIN_PULSE_WIDTH ) / 180.0;      

  s->writeMicroseconds(uS);
}

void resolutionTest()
{
  float a;
  for( a = 0; a < 5; a += 0.1 )
  {
    #ifdef DO_LOGGING
    Serial.print (a);
    Serial.print ("\n");
    #endif
    servoWriteFloat( &s0, a );
    delay( 1000 );
  }
}


boolean uSForAngle( float angle, int*result) // expect angle  in geometry frame, 0 with the arm horizontal, -90 at full extend downwards
{  


   int uS = 822 - angle * 9.94;  // magic calibration figures found by measuring our servos
   
  if( uS < MIN_PULSE_WIDTH  )
    return false;
    
  if( uS > MAX_PULSE_WIDTH  )
    return false;

  *result = uS;
  return true;
  //s->writeMicroseconds(uS);
}

boolean transformToServoAngle( float &theta )
{
  // Our servos go from 0 (max retract) to 120 (full extend, arm in line with servo body) to 170 (overextended)
  
  // Theta from the geometry maths has 0 with the arm horizontal, -90 at full extend
  
  theta = -theta;
  theta = theta + 30;
  
  boolean success = true;
  
  if( theta < minServoPos )
  {
      success = false;
     theta = minServoPos;
  }
    
  if( theta > maxServoPos )
    {
      success = false;
     theta = maxServoPos; 
    }
    
    return success;
}



  


 
 // trigonometric constants
 const float sqrt3 = sqrt(3.0);
 const float pi = 3.141592653;    // PI
 const float sin120 = sqrt3/2.0;   
 const float cos120 = -0.5;        
 const float tan60 = sqrt3;
 const float sin30 = 0.5;
 const float tan30 = 1/sqrt3;
 
 // forward kinematics: (theta1, theta2, theta3) -> (x0, y0, z0)
 // returned status: 0=OK, -1=non-existing position
 int delta_calcForward(float theta1, float theta2, float theta3, float &x0, float &y0, float &z0) {
     float t = (f-e)*tan30/2;
     float dtr = pi/(float)180.0;
 
     theta1 *= dtr;
     theta2 *= dtr;
     theta3 *= dtr;
 
     float y1 = -(t + rf*cos(theta1));
     float z1 = -rf*sin(theta1);
 
     float y2 = (t + rf*cos(theta2))*sin30;
     float x2 = y2*tan60;
     float z2 = -rf*sin(theta2);
 
     float y3 = (t + rf*cos(theta3))*sin30;
     float x3 = -y3*tan60;
     float z3 = -rf*sin(theta3);
 
     float dnm = (y2-y1)*x3-(y3-y1)*x2;
 
     float w1 = y1*y1 + z1*z1;
     float w2 = x2*x2 + y2*y2 + z2*z2;
     float w3 = x3*x3 + y3*y3 + z3*z3;
     
     // x = (a1*z + b1)/dnm
     float a1 = (z2-z1)*(y3-y1)-(z3-z1)*(y2-y1);
     float b1 = -((w2-w1)*(y3-y1)-(w3-w1)*(y2-y1))/2.0;
 
     // y = (a2*z + b2)/dnm;
     float a2 = -(z2-z1)*x3+(z3-z1)*x2;
     float b2 = ((w2-w1)*x3 - (w3-w1)*x2)/2.0;
 
     // a*z^2 + b*z + c = 0
     float a = a1*a1 + a2*a2 + dnm*dnm;
     float b = 2*(a1*b1 + a2*(b2-y1*dnm) - z1*dnm*dnm);
     float c = (b2-y1*dnm)*(b2-y1*dnm) + b1*b1 + dnm*dnm*(z1*z1 - re*re);
  
     // discriminant
     float d = b*b - (float)4.0*a*c;
     if (d < 0) return -1; // non-existing point
 
     z0 = -(float)0.5*(b+sqrt(d))/a;
     x0 = (a1*z0 + b1)/dnm;
     y0 = (a2*z0 + b2)/dnm;
     return 0;
 }
 
 // inverse kinematics
 // helper functions, calculates angle theta1 (for YZ-pane)
 int delta_calcAngleYZ(float x0, float y0, float z0, float &theta) {
     float y1 = -0.5 * 0.57735 * f; // f/2 * tg 30
     y0 -= 0.5 * 0.57735    * e;    // shift center to edge
     // z = a + b*y
     float a = (x0*x0 + y0*y0 + z0*z0 +rf*rf - re*re - y1*y1)/(2*z0);
     float b = (y1-y0)/z0;
     // discriminant
     float d = -(a+b*y1)*(a+b*y1)+rf*(b*b*rf+rf); 
     if (d < 0) return -1; // non-existing point
     float yj = (y1 - a*b - sqrt(d))/(b*b + 1); // choosing outer point
     float zj = a + b*yj;
     theta = 180.0*atan(-zj/(y1 - yj))/pi + ((yj>y1)?180.0:0.0);
     return 0;
 }
 
 // inverse kinematics: (x0, y0, z0) -> (theta1, theta2, theta3)
 // returned status: 0=OK, -1=non-existing position
 int delta_calcInverse(float x0, float y0, float z0, float &theta1, float &theta2, float &theta3) {
     theta1 = theta2 = theta3 = 0;
     int status = delta_calcAngleYZ(x0, y0, z0, theta1);
     if (status == 0) status = delta_calcAngleYZ(x0*cos120 + y0*sin120, y0*cos120-x0*sin120, z0, theta2);  // rotate coords to +120 deg
     if (status == 0) status = delta_calcAngleYZ(x0*cos120 - y0*sin120, y0*cos120+x0*sin120, z0, theta3);  // rotate coords to -120 deg
     return status;
 }
