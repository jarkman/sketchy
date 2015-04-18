
int analogPin = 0;

extern float sensorRange;

void setupRangeSensor()
{
  sensorRange = -1;
}

void loopRangeSensor()
{
  
  // sensor actually gives a new value once every 38ms, but we will read it more often
  float v = analogRead(analogPin);    // read the input pin - 0->5v gives 0->1023 reading
  
  v = v * 5.0 / 1023.0; // convert to volts
   
   v = (400.0/3.3)/v; // convert to mm
  
  v += 12; //measured offset 
  
  v = v - 40.0; // convert to distance from bottom of effector, not distance from sensor

  if( sensorRange < 0 )
    sensorRange = v; // first time
  else
  {
    float fraction = 0.05; // I see a 12-sample periodicity in the raw data, so smooth to 20 samples
    sensorRange = (v*fraction) + (sensorRange*(1.0-fraction)); // smooth
   }
  //
  
  #ifdef DO_LOGGING
  //Serial.println(sensorRange);             // debug value
  #endif
  
}
