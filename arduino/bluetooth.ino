
// Can't do this with SofwareSerial, because it blocks on read, so use NewSoftSerial instead
#include <SoftwareSerial.h>
// NewSoftSerial is incompatible with the servo lib, so we need to use one or the other but not both

#define rxPin 2 // wire to pin 14 on RN41
#define txPin 3  // wire to UART_RX, pin 13, on RN41. If running on a 5V Aruino, use 10k/20k divider to avoid frying the RN41!
// set up a new serial port
SoftwareSerial mySerial =  SoftwareSerial(rxPin, txPin);
//NewSoftSerial mySerial(rxPin, txPin);

boolean bluetoothOn = false;


#define BT_STATUS_WAITING 0
#define BT_STATUS_READING 1
#define BT_STATUS_DONE 2

int btStatus = BT_STATUS_WAITING;
int numPointsComing = 0;
int axis = 0;
int axes[3];
long startT = 0;

void setupBluetooth()
{

  // define pin modes for tx, rx, led pins:
  pinMode(rxPin, INPUT);
  pinMode(txPin, OUTPUT);
  // set the data rate for the SoftwareSerial port


}

void echo()
{
   while (mySerial.available()) 
  {
      unsigned char c = (char)mySerial.read();
      Serial.print (c);
  }
}

void turnOnBluetooth()
{
 
    if(  bluetoothOn )
      return;
    
    #ifdef DO_LOGGING
    Serial.print ("turnOnBluetooth");
    
    Serial.print ("\n");
     #endif
     
    startT = 0; 
    mySerial.begin(9600);
    
    bluetoothOn = true;
    btStatus = BT_STATUS_WAITING;
    
    int factoryReset = false;
    
    if( factoryReset )
    {
      /* this was an experiment that didn't help, when I had a dead RN41.*/
      Serial.print ("Resetting in 10\n");
    
     delay(10000);
  
      Serial.print ("Resetting\n");
      mySerial.write("$$$\n");
      echo();
      delay(1000);


      
      Serial.print ("Factory reset\n");
      mySerial.write("SF,1\n");
      echo();
    }
    
}

void turnOffBluetooth()
{
  if( ! bluetoothOn )
    return;
    
  mySerial.end();
  bluetoothOn = false;
  
   #ifdef DO_LOGGING
    Serial.print ("turnOffBluetooth");
    
    Serial.print ("\n");
     #endif
}



void loopBluetooth()
{
  if( ! bluetoothOn )
    return;
  
  if( btStatus == BT_STATUS_READING )
 {
   long elapsed = startT - millis();
   if( elapsed > 5000 )
   {
      #ifdef DO_LOGGING
      Serial.print ("Timeout - numPointsComing: ");    
      Serial.print (numPointsComing);
      Serial.print (" numPoints: ");
      Serial.print (numPoints);
      Serial.print ("\n");
       #endif
     stopReading();
   }
 } 
 
  while (mySerial.available()) 
  {
      unsigned char c = (char)mySerial.read();
      
    
      switch( btStatus )
      {
        case BT_STATUS_WAITING:
           numPointsComing = c; // first byte tells us num points
           clearPath();
           btStatus = BT_STATUS_READING;
           startT = millis();
           
         break;
         
        case BT_STATUS_READING:
          axes[axis++] = c;
          if( axis == 3 )
          {
            int z = axes[2]-127;
            
            if( z < -20 || z > 20 )
              numPointsComing = 1; // abort on bad z
              
            else if( ! addPoint( axes[0]-127, axes[1]-127, z ))
              numPointsComing = 1; // abort
              
            axis = 0;
            numPointsComing --;
            
            if( numPointsComing == 0 )
             {
               stopReading();
             }  
          }
         break;
         
        case BT_STATUS_DONE:
         break;
         
      }
        //mySerial.print(someChar);
  }
  /*
   #ifdef DO_LOGGING
    Serial.print ("numPointsComing ");
    
    Serial.print (numPointsComing);
    Serial.print ("\n");
     #endif
  */
}

void stopReading()
{
   addPoint( 0.0,0.0,-10.0, 1 ); // park pen
  
   #ifdef DO_LOGGING
      Serial.print ("stopReading numPointsComing: ");    
      Serial.print (numPointsComing);
      Serial.print (" numPoints: ");
      Serial.print (numPoints);
      Serial.print ("\n");
       #endif
       
  btStatus = BT_STATUS_DONE;
  startCalibrate();  // which will lead to a run
}
