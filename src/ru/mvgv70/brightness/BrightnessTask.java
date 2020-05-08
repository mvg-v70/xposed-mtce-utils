package ru.mvgv70.brightness;

import java.util.TimerTask;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BrightnessTask extends TimerTask {
	
  private int level = -1;
  private Context context = null;
  private String event = null;
  private static final String TAG = "xposed-mtce-utils-brightness";
  
  BrightnessTask(String text)
  {
    event = text;
  }
	
  public void setParams(int value, Context ctx)
  {
    level = value;
    context = ctx;
  }

  @Override
  public void run() 
  {
    setBrightness(level);
  }
  
  public void setBrightness(int value)
  {
    // проверим граничные значения
    if (value <= 0) value = 10;
    if (value > 100) value = 100;
    //
    Log.d(TAG,"set "+event+" brightness "+value);
    Intent intent = new Intent("com.microntek.BLIGHT_SET");
    intent.putExtra("level", value);
    context.sendBroadcast(intent);
  }
  
}