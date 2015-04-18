int calibrateIndex = 0;
float calX = 40.0;
long nextCalTime = 0;
long calDelay = 1000;
long initialCalZ = -10.0;
long calZ;  // height we calibrate at, to keep the pen off the paper
float nominalHeight = 45.0; // measure zero height offset from here - pen tip is 50mm from effector

float calValue00 = 0.0; // sensor height in middle

float calValuePP = 0.0; // sensor ranges at four corners - add this value to the Z we have in mind to get a real target z
float calValuePN = 0.0;
float calValueNP = 0.0;
float calValueNN = 0.0;

void startCalibrate()
{
  #ifdef DO_LOGGING
    Serial.println ("STATE_CALIBRATE");
    #endif
     
    state = STATE_CALIBRATE;
    turnOffBluetooth();
    turnOnServos();
    
    
    //reset the calibrate state machine
    calibrateIndex = -3;
    nextCalTime = 0;
    calZ = initialCalZ;
    // set our cal values to zero so they do not influence our motion during calibration
    calValue00 = 0.0;
    calValuePP = 0.0; 
    calValuePN = 0.0;
    calValueNP = 0.0;
    calValueNN = 0.0;
    
    moveTo( 0,0, -15 );

}


void loopCalibrate()
{
  if( motionWorking || (nextCalTime != 0 && millis() < nextCalTime) )
    return;
  
    #ifdef DO_LOGGING
    Serial.print ("calibrate ");
    Serial.print (calibrateIndex);
    
    Serial.print ("\n");
     #endif  

  switch( calibrateIndex )
  {
      case -3:
      // go to middle, quite high off the paper, to get an initial offset
      moveTo( 0,0, calZ * 2 );
      calibrateIndex++;
      break;
    case -2:
      // when we get there, wait
      nextCalTime = millis() + calDelay;
      calibrateIndex++;
      break;
    case -1:
      // after wait, measure height
      calValue00 = sensorRange + calZ*2 - nominalHeight;
      #ifdef DO_LOGGING
      Serial.print ("c ");
      Serial.print (calZ*2);    

      Serial.print (" range ");
      Serial.print (sensorRange);    

      Serial.print (" cal 00 ");
      Serial.print (calValue00);    
      Serial.print ("\n");
     #endif  
     
     calZ += calValue00; // keep subsequent call motions off the paper too
     
     // and set this offset, which will influence all subsequent motion
       /*
     calValuePP = calValue00; 
     calValuePN = calValue00;
     calValueNP = calValue00;
     calValueNN = calValue00;
      */
     calibrateIndex++;

     //calibrateIndex = -3;
      //n++ ;  

      
      break;
      
      
      
    case 0:
      // go to 1st corner
      moveTo( calX, calX, calZ );
      calibrateIndex++;
      break;
    case 1:
      // when we get there, wait
      nextCalTime = millis() + calDelay;
      calibrateIndex++;
      break;
    case 2:
      // after wait, measure height
      calValuePP += sensorRange + calZ  - nominalHeight;
      #ifdef DO_LOGGING
      Serial.print ("range ");
      Serial.print (sensorRange);    

      Serial.print (" cal 0 ");
      Serial.print (calValuePP);    
      Serial.print ("\n");
     #endif  
       // and move again
      moveTo( calX, -calX, calZ );
      calibrateIndex++;
      break;
   case 3:
     // when we get there, wait
      nextCalTime = millis() + calDelay;
      calibrateIndex++;
      break;
    case 4:
      // after wait, measure height
      calValuePN += sensorRange + calZ - nominalHeight;
      #ifdef DO_LOGGING
      Serial.print ("cal 1 ");
      Serial.print (calValuePN);    
      Serial.print ("\n");
     #endif  
       // and move again
      moveTo( -calX, -calX, calZ );
      calibrateIndex++;
      break;
    case 5:
      // when we get there, wait
      nextCalTime = millis() + calDelay;
      calibrateIndex++;
      break;
    case 6:
      // after wait, measure height
      calValueNN += sensorRange + calZ - nominalHeight;
      #ifdef DO_LOGGING
      Serial.print ("cal 2 ");
      Serial.print (calValueNN);    
      Serial.print ("\n");
     #endif  
       // and move again
      moveTo( -calX, calX, calZ );
      calibrateIndex++;
      break;   
      
     case 7:
      // when we get there, wait
      nextCalTime = millis() + calDelay;
      calibrateIndex++;
      break;
    case 8:
      // after wait, measure height
      calValueNP += sensorRange + calZ - nominalHeight;
      #ifdef DO_LOGGING
      Serial.print ("cal 3 ");
      Serial.print (calValueNP);    
      Serial.print ("\n");
     #endif  
      // and move again
      moveTo( 0,0,-20 ); // so we don't make a dot when we droop
      calibrateIndex++;
      break;   
    
    case 9:
      // when we get there, wait  
     if( numPoints > 0 )
        startRun();
     else
        startReadingBluetooth();
      break;
  }
}

float calOffset( float x, float y ) // amount to add to Z
{

  float xP = (x + calX) / (2*calX); // fraction of positive-X contributions we want
  float xN = (calX - x) / (2*calX); // fraction of negative-X contributions we want
  float yP = (y + calX) / (2*calX); // fraction of positive-Y contributions we want
  float yN = (calX - y) / (2*calX); // fraction of negative-Y contributions we want
  

  //combine the four corner Z according to our ratio of position:
  float z = calValuePP * xP * yP  + 
            calValueNP * xN * yP  + 
            calValuePN * xP * yN  + 
            calValueNN * xN * yN ;
  return z;
}
