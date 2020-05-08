package ru.mvgv70.brightness;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;

import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;
import ru.mvgv70.sunrise.Sun;

public class BrightnessTimer {

  private static Context context = null;
  private int dayBrightNess = 0;
  private int nightBrightness = 0;
  private static Timer timerSunrise = null;
  private static Timer timerSunset = null;
  private static BrightnessTask taskSunrise = null;
  private static BrightnessTask taskSunset = null;
  private static final String TAG = "xposed-mtce-utils-brightness";
  
  // установка контекста
  public BrightnessTimer(Context ctx, int dayValue, int nightValue)
  {
    context = ctx;
    dayBrightNess = dayValue;
    nightBrightness = nightValue;
  }
  
  // создание таймеров
  private static void createTimers()
  {
    Log.d(TAG,"createTimers");
    timerSunrise = new Timer();
    timerSunset = new Timer();
    taskSunrise = new BrightnessTask("sunrise");
    taskSunset = new BrightnessTask("sunset");
  }
  
  // остановка таймеров
  private static void cancelTimers()
  {
    Log.d(TAG,"cancelTimers");
    // sunrise
    if (taskSunrise != null)
    {
      taskSunrise.cancel();
      taskSunrise = null;
    }
    if (timerSunrise != null)
    {
      timerSunrise.cancel();
      timerSunrise = null;
    }
    // sunset
    if (taskSunset != null)
    {
      taskSunset.cancel();
      taskSunset = null;
    }
    if (timerSunset != null)
    {
      timerSunset.cancel();
      timerSunset = null;
    }
  }
	
  // установка таймеров изменени€ €ркости
  public void setTimers(Location location)
  {
    SimpleDateFormat fmt = new SimpleDateFormat("dd.MM HH:mm", Locale.getDefault());
    Calendar sunrise = null;
	Calendar sunset = null;
	Calendar current = Calendar.getInstance();
    Log.d(TAG,"setTimers");
    // сброс и создание нового таймера
    cancelTimers();
    createTimers();
    //
    if (location != null)
    {
      // определим врем€ рассвета и заката
      Sun sun = new Sun(location.getLatitude(), location.getLongitude(), TimeZone.getDefault().getRawOffset()/(60*60*1000));
      sunrise = sun.sunrise();
      sunset = sun.sunset();
      // сохраним врем€ рассвета и заката
      setSunSettings(sunrise.get(Calendar.HOUR_OF_DAY), sunrise.get(Calendar.MINUTE), sunset.get(Calendar.HOUR_OF_DAY), sunset.get(Calendar.MINUTE));
    }
    else
    {
      Log.d(TAG,"read sun events from preferences");
      sunrise = getPrefEvent("sunrise");
      sunset = getPrefEvent("sunset");
    }
    // установим таймеры восхода и захода
    if (sunrise != null)
    {
      if (sunrise.getTimeInMillis() < current.getTimeInMillis())
        sunrise.add(Calendar.DAY_OF_YEAR, 1);
      Log.d(TAG,"sunrise="+fmt.format(sunrise.getTime()));
      taskSunrise.setParams(dayBrightNess, context);
      timerSunrise.schedule(taskSunrise, sunrise.getTime(), AlarmManager.INTERVAL_DAY);
    }
    else
      Log.w(TAG,"sunrise is null");
    if (sunset != null)
    {
      if (sunset.getTimeInMillis() < current.getTimeInMillis())
        sunset.add(Calendar.DAY_OF_YEAR, 1);
      Log.d(TAG,"sunset="+fmt.format(sunset.getTime()));
      taskSunset.setParams(nightBrightness, context);
      timerSunset.schedule(taskSunset, sunset.getTime(), AlarmManager.INTERVAL_DAY);
    }
    else
      Log.w(TAG,"sunset is null");
    if (sunrise != null && sunset != null)
    {
      // изменим текущую €ркость
      if (sunrise.getTimeInMillis() > sunset.getTimeInMillis())
        taskSunrise.run();
      else
        taskSunset.run();
    }
  }
	    
  // сохранение времени заката и рассвета в preference
  private void setSunSettings(int sunriseHour, int sunriseMin, int sunsetHour, int sunsetMin)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    Editor editor = prefs.edit();
    editor.putInt("sunrise.hour",sunriseHour);
    editor.putInt("sunrise.min",sunriseMin);
    editor.putInt("sunset.hour",sunsetHour);
    editor.putInt("sunset.min",sunsetMin);
    editor.commit(); 
  }
  
  private Calendar getPrefEvent(String event)
  {
	Calendar calendar = null;
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    // текущий день
    Calendar current = Calendar.getInstance();
    int mon = current.get(Calendar.MONTH);
    int day = current.get(Calendar.DAY_OF_MONTH);
    int year = current.get(Calendar.YEAR);
    // врем€ событи€ из настроек
    int hour = prefs.getInt(event+".hour", -1);
    int min = prefs.getInt(event+".min", -1);
    if (hour >= 0 && min >= 0)
    {
      // формируем результат
      calendar = Calendar.getInstance();
      calendar.set(Calendar.HOUR_OF_DAY, hour);
      calendar.set(Calendar.MINUTE, min);
      calendar.set(Calendar.MONTH, mon);
      calendar.set(Calendar.DAY_OF_MONTH, day);
      calendar.set(Calendar.YEAR, year);
    }
    return calendar;
  }
}
