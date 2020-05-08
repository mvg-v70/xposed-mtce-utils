package ru.mvgv70.xposed_mtce_utils;

import ru.mvgv70.utils.IniFile;
import ru.mvgv70.utils.Utils;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ScreenClock implements IXposedHookLoadPackage 
{
  private static final String TAG = "xposed-mtce-utils-screenclock";
  private static final String PACKAGE_NAME = "com.microntek.screenclock";
  private static String EXTERNAL_SD = "/storage/external_sd/";
  private final static String ASSETS_FILE_NAME = "settings.ini";
  private final static String INIFILE_NAME = "mtce-utils/screenclock.ini";
  private final static String SETTINGS_SECTION = "settings";
  private final static String SCREENCLOCK_SECTION = "screenclock";
  private static IniFile props = new IniFile();
  private static LocationManager locationManager = null;
  private static Activity screenClockActivity;
  private static int musicState = 0; 
  private static boolean radioState = false;
  // names
  private static String title_name = "";
  private static String album_name = "";
  private static String artist_name = "";
  private static String freq_name = "";
  private static String station_name = "";
  private static String speed_name = "";
  private static String speed_units_name = "";
  private static String speed_units_label = "";
  private static String title_add = "";
  private static String artist_add = "";
  private static String freq_add = "";
  private static String station_add = "";
  private static String date_name = "";
  private static String date_format = "";
  private static String cover_name = "";
  private static String temperature_name = "";
  private static String battery_name = "";
  // parameters
  private static String layout_name = "";
  private static float cover_alpha = 0;
  private static String background_name = "";
  private static int backgroundColor = 0;
  private static int textColor = 0;
  private static int speedColor = 0;
  private static int speedBackColor = 0;
  private static int speedUnitsColor = 0;
  private static int clockColor = 0;
  private static int subTitleColor = 0;
  private static boolean bluetoothClose = false;
  private static float speed_divider = 1;
  private static boolean parseMusic = true;
  private static boolean parseMTCMusic = true;
  private static boolean parseBtMusic = true;
  private static boolean parseRadio = true;
  private static boolean showBattery = false;
  // view
  private static TextView titleView = null;
  private static TextView albumView = null;
  private static TextView artistView = null;
  private static TextView freqView = null;
  private static TextView stationView = null;
  private static TextView speedView = null;
  private static TextView speedUnitsView = null;
  private static TextClock dateView = null;
  private static ImageView coverView = null;
  private static View backgroundView = null;
  private static View layoutView = null;
  private static TextView temperatureView = null;
  private static TextView batteryView = null;
  // настройки
  private static int DEFAULT_COLOR = 0xffe3d3b6;
  private static boolean touchMode = false;
  private static boolean showCover = false;

  @Override
  public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable 
  {
    
    // MainActivity.onCreate(Bundle)
    XC_MethodHook onCreate = new XC_MethodHook() {
    
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"onCreate");
        screenClockActivity = (Activity)param.thisObject;
        // путь к файлу из build.prop
        EXTERNAL_SD = Utils.getModuleSdCard();
        // чтение настроек
        readSettings();
        // старт скринсейвера
        Intent intent = new Intent("com.microntek.screenclock.active");
        intent.putExtra("value", true);
        screenClockActivity.sendBroadcast(intent);
        // создание broadcast receiver
        createReceivers();
      }
    };
    
    // MainActivity.onDestroy()
    XC_MethodHook onDestroy = new XC_MethodHook() {
        
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"onDestroy");
        if (parseMusic)
          screenClockActivity.unregisterReceiver(musicReceiver);
        if (parseBtMusic)
          screenClockActivity.unregisterReceiver(btMusicReceiver);
        if (parseRadio) 
          screenClockActivity.unregisterReceiver(radioReceiver);
        if (bluetoothClose)
          screenClockActivity.unregisterReceiver(bluetoothReceiver);
        screenClockActivity.unregisterReceiver(canbusReceiver);
        // определение скорости
        if ((speedView != null) && (locationManager != null))
          locationManager.removeUpdates(locationListener);
        // top package
        screenClockActivity.unregisterReceiver(statusBarReceiver);
        // стоп скринсейвера
        Intent intent = new Intent("com.microntek.screenclock.active");
        intent.putExtra("value", false);
        screenClockActivity.sendBroadcast(intent);
        //
        screenClockActivity = null;
      }
    };
    
    // start hooks
    if (!lpparam.packageName.equals(PACKAGE_NAME)) return;
    Log.d(TAG,PACKAGE_NAME);
    Utils.setTag(TAG);
    Utils.readXposedMap();
    Utils.findAndHookMethodCatch("com.microntek.screenclock.MainActivity", lpparam.classLoader, "onCreate", Bundle.class, onCreate);
    Utils.findAndHookMethodCatch("com.microntek.screenclock.MainActivity", lpparam.classLoader, "onDestroy", onDestroy);
    Log.d(TAG,PACKAGE_NAME+" hook OK");
  }
  
  // чтение настроек из mtc-music.ini
  private void readSettings()
  {
    try
    {
      // читаем настроечный файл из assests
      try
      {
        Log.d(TAG,"read settings from assets/"+ASSETS_FILE_NAME);
        props.loadFromAssets(screenClockActivity, ASSETS_FILE_NAME);
      }
      catch (Exception e)
      {
        // если нет assets
        Log.e(TAG,"error read assets/"+ASSETS_FILE_NAME);
      }
      // имена элементов
      title_name = props.getValue(SCREENCLOCK_SECTION, "music_title", "");
      Log.d(TAG,"music_title="+title_name);
      album_name = props.getValue(SCREENCLOCK_SECTION, "music_album", "");
      Log.d(TAG,"music_album="+album_name);
      artist_name = props.getValue(SCREENCLOCK_SECTION, "music_artist", "");
      Log.d(TAG,"music_artist="+artist_name);
      cover_name = props.getValue(SCREENCLOCK_SECTION, "music_cover", "");
      Log.d(TAG,"music_cover="+cover_name);
      artist_add = props.getValue(SCREENCLOCK_SECTION, "artist_add", "");
      Log.d(TAG,"artist_add="+artist_add);
      freq_name = props.getValue(SCREENCLOCK_SECTION, "radio_freq", "");
      Log.d(TAG,"radio_freq="+freq_name);
      station_name = props.getValue(SCREENCLOCK_SECTION, "radio_station", "");
      Log.d(TAG,"radio_station="+station_name);
      speed_name = props.getValue(SCREENCLOCK_SECTION, "speed", "");
      Log.d(TAG,"speed="+speed_name);
      speed_units_name = props.getValue(SCREENCLOCK_SECTION, "speed_units", "");
      Log.d(TAG,"speed_units="+speed_units_name);
      speed_units_label = props.getValue(SCREENCLOCK_SECTION, "speed_units_label", "км/ч");
      Log.d(TAG,"speed_units_label="+speed_units_label);
      title_add = props.getValue(SCREENCLOCK_SECTION, "title_add", "");
      Log.d(TAG,"title_add="+title_add);
      freq_add = props.getValue(SCREENCLOCK_SECTION, "freq_add", "");
      Log.d(TAG,"freq_add="+freq_add);
      station_add = props.getValue(SCREENCLOCK_SECTION, "station_add", "");
      Log.d(TAG,"station_add="+station_add);
      date_name = props.getValue(SCREENCLOCK_SECTION, "date_name", "yeardate");
      Log.d(TAG,"date_name="+date_name);
      background_name = props.getValue(SCREENCLOCK_SECTION, "background_name", "");
      Log.d(TAG,"background_name="+background_name);
      layout_name = props.getValue(SCREENCLOCK_SECTION, "layout_name", "");
      Log.d(TAG,"layout_name="+layout_name);
      temperature_name = props.getValue(SCREENCLOCK_SECTION, "temperature_name", "");
      Log.d(TAG,"temperature_name="+temperature_name);
      battery_name = props.getValue(SCREENCLOCK_SECTION, "battery_name", "");
      Log.d(TAG,"battery_name="+battery_name);
      // формат даты
      date_format = props.getValue(SCREENCLOCK_SECTION, "date_format", "");
      Log.d(TAG,"date_format="+date_format);
      // скорость в милях
      speed_divider = props.getFloatValue(SCREENCLOCK_SECTION, "speed.divider", 1);
      Log.d(TAG,"speed.divider="+speed_divider);
    }
    catch (Exception e)
    {
      Log.e(TAG,e.getMessage());
    }
    // цвета по-умолчанию
    textColor = DEFAULT_COLOR;
    speedColor = DEFAULT_COLOR;
    speedBackColor = DEFAULT_COLOR;
    speedUnitsColor = 0;
    clockColor = DEFAULT_COLOR;
    subTitleColor = DEFAULT_COLOR;
    backgroundColor = 0xc0000000;
    // читаем пользовательские настройки
    IniFile user_props = new IniFile();
    try
    {
      Log.d(TAG,"read user settings from "+EXTERNAL_SD+INIFILE_NAME);
      user_props.loadFromFile(EXTERNAL_SD+INIFILE_NAME);
    }
    catch (Exception e)
    {
      Log.w(TAG,e.getMessage());
    }
    if (user_props.getSectionCount() > 0)
    {
      // touch_mode
      touchMode = user_props.getBoolValue(SCREENCLOCK_SECTION, "touch_mode", false);
      Log.d(TAG,"touch_mode="+touchMode);
      // show_cover=true/false
      showCover = user_props.getBoolValue(SCREENCLOCK_SECTION, "show_cover", false);
      Log.d(TAG,"show_cover="+showCover);
      // цвет надписей
      String color = user_props.getValue(SCREENCLOCK_SECTION, "color", "").trim();
      Log.d(TAG,"color="+color);
      try
      {
        if (!color.isEmpty()) textColor = Color.parseColor(color);
      }
      catch (Exception e)
      {
        Log.e(TAG,"invalid color: "+color);
      }
      // цвет надписей скорости
      color = user_props.getValue(SCREENCLOCK_SECTION, "speed.color", "").trim();
      Log.d(TAG,"speed.color="+color);
      try
      {
        if (!color.isEmpty()) speedColor = Color.parseColor(color);
      }
      catch (Exception e)
      {
        Log.e(TAG,"invalid color: "+color);
      }
      // цвет надписи км/час
      color = user_props.getValue(SCREENCLOCK_SECTION, "speed.units.color", "").trim();
      Log.d(TAG,"speed.units.color="+color);
      try
      {
        if (!color.isEmpty()) speedUnitsColor = Color.parseColor(color);
      }
      catch (Exception e)
      {
        Log.e(TAG,"invalid color: "+color);
      }
      // цвет подложки скорости
      color = user_props.getValue(SCREENCLOCK_SECTION, "speed_back.color", "").trim();
      Log.d(TAG,"speed_back.color="+color);
      try
      {
        if (!color.isEmpty()) speedBackColor = Color.parseColor(color);
      }
      catch (Exception e)
      {
        Log.e(TAG,"invalid color: "+color);
      }
      // цвет цифровых часов
      color = user_props.getValue(SCREENCLOCK_SECTION, "clock.color", "").trim();
      Log.d(TAG,"clock.color="+color);
      try
      {
        if (!color.isEmpty()) clockColor = Color.parseColor(color);
      }
      catch (Exception e)
      {
        Log.e(TAG,"invalid color: "+color);
      }
      // цвет подписей
      color = user_props.getValue(SCREENCLOCK_SECTION, "sub_title.color", "").trim();
      Log.d(TAG,"sub_title.color="+color);
      try
      {
        if (!color.isEmpty()) subTitleColor = Color.parseColor(color);
      }
      catch (Exception e)
      {
        Log.e(TAG,"invalid color: "+color);
      }
      // цвет фона
      color = user_props.getValue(SCREENCLOCK_SECTION, "background_color", "").trim();
      Log.d(TAG,"background.color="+color);
      try
      {
        if (!color.isEmpty()) backgroundColor = Color.parseColor(color);
      }
      catch (Exception e)
      {
        Log.e(TAG,"invalid color: "+color);
      }
      // cover_alpha
      cover_alpha = user_props.getFloatValue(SCREENCLOCK_SECTION, "cover_alpha", cover_alpha);
      Log.d(TAG,"cover_alpha="+cover_alpha);
      // формат даты
      date_format = user_props.getValue(SCREENCLOCK_SECTION, "date_format", date_format);
      Log.d(TAG,"date_format="+date_format);
      // bluetooth
      bluetoothClose = user_props.getBoolValue(SCREENCLOCK_SECTION, "bluetooth.close", false);
      Log.d(TAG,"bluetooth.close="+bluetoothClose);
      // скорость в милях
      speed_divider = user_props.getFloatValue(SCREENCLOCK_SECTION, "speed.divider", speed_divider);
      if (speed_divider <= 0) speed_divider = 1;
      Log.d(TAG,"speed.divider="+speed_divider);
      // additionals
      freq_add = user_props.getValue(SCREENCLOCK_SECTION, "freq_add", freq_add);
      Log.d(TAG,"freq_add="+freq_add);
      // единицы скорости
      speed_units_label = user_props.getValue(SCREENCLOCK_SECTION, "speed_units_label", speed_units_label);
      Log.d(TAG,"speed_units_label="+speed_units_label);
      // обработка тегов и радио
      parseRadio = user_props.getBoolValue(SETTINGS_SECTION, "radio", true);
      Log.d(TAG,"radio="+parseRadio);
      parseMusic = user_props.getBoolValue(SETTINGS_SECTION, "music", true);
      Log.d(TAG,"music="+parseMusic);
	  parseMTCMusic = user_props.getBoolValue(SETTINGS_SECTION, "mtc.music", true);
      Log.d(TAG,"mtc.music="+parseMTCMusic);
      parseBtMusic = user_props.getBoolValue(SETTINGS_SECTION, "btmusic", true);
      Log.d(TAG,"btmusic="+parseBtMusic);
      // напряжение на аккумуляторе
      showBattery = user_props.getBoolValue(SETTINGS_SECTION, "battery", false);
      Log.d(TAG,"battery="+showBattery);
    }
    // id
    int title_id = 0;
    int album_id = 0;
    int artist_id = 0;
    int cover_id = 0;
    int freq_id = 0;
    int station_id = 0;
    int speed_id = 0;
    int speed_units_id = 0;
    int date_id = 0;
    int background_id = 0;
    int layout_id = 0;
    int temperature_id = 0;
    int battery_id = 0;
    Resources res = screenClockActivity.getResources();
    if (!title_name.isEmpty())
      title_id = res.getIdentifier(title_name, "id", screenClockActivity.getPackageName());
    if (!album_name.isEmpty())
      album_id = res.getIdentifier(album_name, "id", screenClockActivity.getPackageName());
    if (!artist_name.isEmpty())
      artist_id = res.getIdentifier(artist_name, "id", screenClockActivity.getPackageName());
    if (!cover_name.isEmpty())
      cover_id = res.getIdentifier(cover_name, "id", screenClockActivity.getPackageName());
    if (!freq_name.isEmpty())
      freq_id = res.getIdentifier(freq_name, "id", screenClockActivity.getPackageName());
    if (!station_name.isEmpty())
      station_id = res.getIdentifier(station_name, "id", screenClockActivity.getPackageName());
    if (!speed_name.isEmpty())
      speed_id = res.getIdentifier(speed_name, "id", screenClockActivity.getPackageName());
    if (!speed_units_name.isEmpty())
      speed_units_id = res.getIdentifier(speed_units_name, "id", screenClockActivity.getPackageName());
    if (!date_name.isEmpty())
      date_id = res.getIdentifier(date_name, "id", screenClockActivity.getPackageName());
    if (!background_name.isEmpty())
      background_id = res.getIdentifier(background_name, "id", screenClockActivity.getPackageName());
    if (!layout_name.isEmpty())
      layout_id = res.getIdentifier(layout_name, "id", screenClockActivity.getPackageName());
    if (!temperature_name.isEmpty())
      temperature_id = res.getIdentifier(temperature_name, "id", screenClockActivity.getPackageName());
    if (!battery_name.isEmpty())
      battery_id = res.getIdentifier(battery_name, "id", screenClockActivity.getPackageName());
    Log.d(TAG,"title_id="+title_id);
    Log.d(TAG,"album_id="+album_id);
    Log.d(TAG,"artist_id="+artist_id);
    Log.d(TAG,"cover_id="+cover_id);
    Log.d(TAG,"freq_id="+freq_id);
    Log.d(TAG,"station_id="+station_id);
    Log.d(TAG,"speed_id="+speed_id);
    Log.d(TAG,"speed_units_id="+speed_units_id);
    Log.d(TAG,"date_id="+date_id);
    Log.d(TAG,"background_id="+background_id);
    Log.d(TAG,"layout_id="+layout_id);
    Log.d(TAG,"temperature_id="+temperature_id);
    Log.d(TAG,"battery_id="+battery_id);
    // views
    titleView = null;
    albumView = null;
    artistView = null;
    coverView = null;
    freqView = null;
    stationView = null;
    speedView = null;
    dateView = null;
    backgroundView = null;
    layoutView = null;
    temperatureView = null;
    batteryView = null;
    if (title_id > 0)
    {
      titleView = (TextView)screenClockActivity.findViewById(title_id);
      if (titleView != null)
      {
        titleView.setTextColor(textColor);
        titleView.setText("");
      }
      else
        Log.w(TAG,"titleView == null");
    }
    if (album_id > 0)
    {
      albumView = (TextView)screenClockActivity.findViewById(album_id);
      if (albumView != null)
      {
        albumView.setTextColor(textColor);
        albumView.setText("");
      }
      else
        Log.w(TAG,"albumView == null");
    }
    if (artist_id > 0)
    {
      artistView = (TextView)screenClockActivity.findViewById(artist_id);
      if (artistView != null)
      {
        artistView.setTextColor(textColor);
        artistView.setText("");
      }
      else 
        Log.w(TAG,"artistView == null");
    }
    if (cover_id > 0)
    {
      coverView = (ImageView)screenClockActivity.findViewById(cover_id);
      if (coverView != null)
        coverView.setImageBitmap(null);
      else
        Log.w(TAG,"coverView == null");
    }
    if (freq_id > 0)
    {
      freqView = (TextView)screenClockActivity.findViewById(freq_id);
      if (freqView != null)
      {
        freqView.setTextColor(textColor);
        freqView.setText("");
      }
      else
        Log.w(TAG,"freqView == null");
    }
    if (station_id > 0)
    {
      stationView = (TextView)screenClockActivity.findViewById(station_id);
      if (stationView != null)
      {
        stationView.setTextColor(textColor);
        stationView.setText("");
      }
      else
        Log.w(TAG,"stationView == null");
    }
    if (speed_id > 0)
    {
      speedView = (TextView)screenClockActivity.findViewById(speed_id);
      if (speedView == null) Log.w(TAG,"speedView == null");
      if (speedView != null)
      {
        // установка цвета
        speedView.setTextColor(speedColor);
        speedView.setText("");
      }
    }
    if (speed_units_id > 0)
    {
      speedUnitsView = (TextView)screenClockActivity.findViewById(speed_units_id);
      if (speedUnitsView == null) Log.w(TAG,"speedUnitsView == null");
      if (speedUnitsView != null)
      {
        // установка цвета
        if (speedUnitsColor != 0) speedUnitsView.setTextColor(speedUnitsColor);
        if (!TextUtils.isEmpty(speed_units_label)) speedUnitsView.setText(speed_units_label);
      }
    }
    if (date_id > 0)
    {
      dateView = (TextClock)screenClockActivity.findViewById(date_id);
      if (dateView != null)
      {
        // установим цвет если задан
    	if (textColor != 0) dateView.setTextColor(textColor);
        // формат даты если задан
        if (!date_format.isEmpty())
        {
          dateView.setFormat24Hour(date_format);
          dateView.setFormat12Hour(date_format);
        }
      }
    }
    if (background_id > 0)
    {
      backgroundView = (View)screenClockActivity.findViewById(background_id);
      if (backgroundView == null) Log.w(TAG,"backgroundView == null");
    }
	// background
	if (backgroundView != null)
	{
      if (touchMode)
      {
        backgroundView.setOnClickListener(screenClick);
        Log.d(TAG,"background click listener created");
      }
	  // установка цвета фона
	  Log.d(TAG,"setBackgroundColor("+Integer.toHexString(backgroundColor)+")");
      backgroundView.setBackgroundColor(backgroundColor); 
	}
    if (layout_id > 0)
    {
      layoutView = (View)screenClockActivity.findViewById(layout_id);
      if (layoutView == null) Log.w(TAG,"layoutView == null");
    }
	// temperature
	if (temperature_id > 0)
    {
      temperatureView = (TextView)screenClockActivity.findViewById(temperature_id);
      if (temperatureView != null)
      {
        temperatureView.setTextColor(textColor);
        temperatureView.setText("");
      }
      else
        Log.w(TAG,"temperatureView == null");
    }
	// battery
	if (battery_id > 0)
    {
      batteryView = (TextView)screenClockActivity.findViewById(battery_id);
      if (batteryView != null)
      {
        batteryView.setTextColor(textColor);
        batteryView.setText("");
      }
      else
        Log.w(TAG,"batteryView == null");
    }
    // установка цветов по тегам
    if (layoutView instanceof ViewGroup)
    {
      Log.d(TAG,"setColorByTag: speed = "+Integer.toHexString(speedColor));
      setColorByTag((ViewGroup)layoutView, "speed", speedColor);
      Log.d(TAG,"setColorByTag: speed_back = "+Integer.toHexString(speedBackColor));
      setColorByTag((ViewGroup)layoutView, "speed_back", speedBackColor);
      Log.d(TAG,"setColorByTag: clock = "+Integer.toHexString(clockColor));
      setColorByTag((ViewGroup)layoutView, "clock", clockColor);
      Log.d(TAG,"setColorByTag: sub_title = "+Integer.toHexString(subTitleColor));
      setColorByTag((ViewGroup)layoutView, "sub_title", subTitleColor);
      Log.d(TAG,"setColorByTag OK");
    }
    else
      Log.w(TAG,"layoutView is not a ViewGroup");
  }
  
  // создание broadcast receiver
  private void createReceivers()
  {
    // music
    if (parseMusic)
    {
      IntentFilter ti = new IntentFilter();
      ti.addAction("com.microntek.music.report");
      screenClockActivity.registerReceiver(musicReceiver, ti);
      Log.d(TAG,"music receiver created");
    }
    if (parseBtMusic)
    {
      // bt music
      IntentFilter bmi = new IntentFilter();
      bmi.addAction("com.microntek.btmusic.report");
      screenClockActivity.registerReceiver(btMusicReceiver, bmi);
      Log.d(TAG,"bluetooth music receiver created");
    }
    // radio
    if (parseRadio)
    {
      IntentFilter ri = new IntentFilter();
      ri.addAction("com.microntek.radio.report");
      screenClockActivity.registerReceiver(radioReceiver, ri);
      Log.d(TAG,"radio receiver created");
    }
    if (touchMode)
    {
      // выключим receiver закрытия
      BroadcastReceiver MTCBootReceiver = (BroadcastReceiver)Utils.getObjectField(screenClockActivity, "MTCBootReceiver");
      if (MTCBootReceiver != null)
      {
        screenClockActivity.unregisterReceiver(MTCBootReceiver);
        screenClockActivity.registerReceiver(MTCBootReceiver, new IntentFilter());
        Log.d(TAG,"com.microntek.endclock receiver disabled");
      }
    }
    // bluetooth
    if (bluetoothClose)
    {
      IntentFilter bi = new IntentFilter();
      bi.addAction("com.microntek.bt.report");
      screenClockActivity.registerReceiver(bluetoothReceiver, bi);
      Log.d(TAG,"bluetooth receiver created");
    }
    // canbus: temperature & battery
    IntentFilter ci = new IntentFilter();
    ci.addAction("com.canbus.temperature");
    ci.addAction("com.canbus.battery");
    screenClockActivity.registerReceiver(canbusReceiver, ci);
    Log.d(TAG,"canbus receiver created");
    // top package
    IntentFilter bi = new IntentFilter();
    bi.addAction("com.microntek.STATUS_BAR_CHANGED");
    screenClockActivity.registerReceiver(statusBarReceiver, bi);
    // ресиверы созданы, посшлем сообщение плееру и радио о чтении тегов
    screenClockActivity.sendBroadcast(new Intent("hct.radio.request.data"));
    screenClockActivity.sendBroadcast(new Intent("hct.music.info"));
    screenClockActivity.sendBroadcast(new Intent("hct.btmusic.info"));
    screenClockActivity.sendBroadcast(new Intent("hct.canbus.info"));
    Log.d(TAG,"requests update sent");
    // очистим поля
    if (titleView != null) titleView.setText("");
    if (artistView != null) artistView.setText("");
    if (albumView != null) albumView.setText("");
    if (coverView != null) coverView.setImageBitmap(null);
    if (freqView != null) freqView.setText("");
    if (stationView != null) stationView.setText("");
    Log.d(TAG,"tag fields cleared");
    // скорость
    if (speedView != null)
    {
      try
      {
        locationManager = (LocationManager)screenClockActivity.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null)
        {
          // определение скорости
          locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
          Log.d(TAG,"speed listener created");
        }
      }
      catch (Exception e)
      {
        // нет прав
        Log.e(TAG,"LocationManager: "+e.getMessage());
      }
    }
  }
  
  // Музыка: com.microntek.music.report
  private BroadcastReceiver musicReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      String type = intent.getStringExtra("type");
      if (type == null) return;
	  // если не обрабатываем события от штатного плеера
      Log.d(TAG,"hasExtra="+intent.hasExtra("class"));
      Log.d(TAG,"parseMTCMusic="+parseMTCMusic);
	  if (!intent.hasExtra("class") && !parseMTCMusic) return;
      Log.d(TAG,"music receiver: "+type);
      if (type.equals("music.state"))
      {
        musicState = intent.getIntExtra("value", 0);
    	Log.d(TAG,"music.state="+musicState);
        if (musicState == 0)
        {
          // музыка выключена: очистим поля музыка
          if (titleView != null) titleView.setText("");
          if (artistView != null) artistView.setText("");
          if (albumView != null) albumView.setText("");
          if (coverView != null) coverView.setImageBitmap(null);
        }
      }
      else if (type.equals("music.title"))
      {
        // String title = intent.getStringExtra("value");
      }
      else if (type.equals("music.tags"))
      {
        String title = intent.getStringExtra(MediaStore.Audio.AudioColumns.TITLE);
        String artist = intent.getStringExtra(MediaStore.Audio.AudioColumns.ARTIST);
        String album = intent.getStringExtra(MediaStore.Audio.AudioColumns.ALBUM);
        String filename = intent.getStringExtra(MediaStore.Audio.AudioColumns.DATA);
        // show tags
        Log.d(TAG,"title="+title);
        Log.d(TAG,"artist="+artist);
        Log.d(TAG,"album="+album);
        Log.d(TAG,"filename="+filename);
        // установим теги
        if (titleView != null)
        {
          if (!TextUtils.isEmpty(title)) title = title + title_add;
          titleView.setText(title);
        }
        if (artistView != null)
        {
          if (!TextUtils.isEmpty(artist)) artist = artist + artist_add;
          artistView.setText(artist);
        }
        if (albumView != null) albumView.setText(album);
        
      }
      else if (type.equals("music.alumb"))
      {
        long value[] = intent.getLongArrayExtra("value");
        long album_id = value[0];
        long _id = value[1];
        if (_id > 0) setCover(_id, album_id);
      }
    }
  };
  
  // BtMusic: com.microntek.btmusic.report
  private BroadcastReceiver btMusicReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      String type = intent.getStringExtra("type");
      if (type == null) return;
      Log.d(TAG,"bluetooth music receiver: "+type);
      if (type.equals("music.state"))
      {
        int btMusicState = intent.getIntExtra("value", 0);
    	Log.d(TAG,"btmusic.state="+btMusicState);
        if (btMusicState == 0)
        {
          // музыка выключена: очистим поля музыка
          if (titleView != null) titleView.setText("");
          if (artistView != null) artistView.setText("");
          if (albumView != null) albumView.setText("");
        }
      }
      else if (type.equals("music.title"))
      {
        String title = intent.getStringExtra("value");
        Log.d(TAG,"title="+title);
        if (titleView != null)
        {
          if (!TextUtils.isEmpty(title)) title = title + title_add;
          titleView.setText(title);
        }
      }
      else if (type.equals("music.albunm"))
      {
        String album = intent.getStringExtra("value");
        Log.d(TAG,"album="+album);
        if (albumView != null) 
          albumView.setText(album);
        else if (artistView != null) 
          artistView.setText(album);
      }
    }
  };
  
  // чтение и показ картинки из тегов
  private static void setCover(long _id, long album_id)
  {
    // cover
    if ((coverView != null) && showCover)
    {
      Log.d(TAG,"setCover("+_id+")");
      // TODO: ошибка прав 
      Bitmap cover = Music.getCover(screenClockActivity, _id, album_id);
      coverView.setImageBitmap(cover);
      coverView.setAlpha(cover_alpha);
    }
  }
  
  // установка цвета по тегам
  private void setColorByTag(ViewGroup group, String tag, int color)
  {
    int count = group.getChildCount();
    for (int i=0; i < count; i++)
    {
      if (group.getChildAt(i) instanceof TextView)
      {
        TextView text = (TextView)group.getChildAt(i);
        String viewTag = (String)text.getTag();
        if (viewTag != null)
          if (viewTag.equals(tag))
          {
            // тег совпадает - установим цвет текста
            text.setTextColor(color);
            Log.d(TAG,"TextView="+text.getId()+", tag="+viewTag+", setTextColor OK");
          }
      }
      else if (group.getChildAt(i) instanceof ViewGroup)
      {
    	// установка цвета у дочерних элементов
        setColorByTag((ViewGroup)group.getChildAt(i), tag, color);
      }
    }
  }
  
  // Радио: com.microntek.radio.report
  private BroadcastReceiver radioReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      String type = intent.getStringExtra("type");
      if (type == null) return;
      Log.d(TAG,"radio receiver: "+type);
      if (type.equals("content"))
      {
        // freq & station
        int freq = intent.getIntExtra("freq", 0) & 0xFFFFFFF;
        String sFreq = getFreqString(freq);
        Log.d(TAG,"freq="+sFreq);
        // установим теги
        if ((freqView != null) && (freq > 0))
        {
          freqView.setText(sFreq+freq_add);
        }
      }
      else if (type.equals("freq.name"))
      {
        // station
        String station = intent.getStringExtra("name");
        Log.d(TAG,"station="+station);
        // установим теги
        if (stationView != null)
        {
          if (!TextUtils.isEmpty(station)) station = station + station_add;
          stationView.setText(station);
        }
      }
      else if (type.equals("power"))
      {
        // on/off
        radioState = intent.getBooleanExtra("state", false);
        Log.d(TAG,"radio.state="+radioState);
        if (radioState == false)
        {
          // радио выключено: очистим поля радио
          if (freqView != null) freqView.setText("");
          if (stationView != null) stationView.setText("");
        }
      }
    }
  };
  
  private static String getFreqString(int freq)
  {
    Log.d(TAG,"getFreqString: freq="+freq);
    if (freq > 10000000)
    {
      // FM
      int i = freq % 1000000 / 10000;
      String frac;
      if (i == 0) 
        frac = "00";
      else if (i < 10) 
        frac = "0" + i;
      else
        frac = "" + i;
      return freq / 1000000 + "." + frac;
    }
    else
      // AM
      return freq / 1000 + "";
  }
  
  @SuppressWarnings("unused")
  private static String getHzString(int freq)
  {
    if (freq > 10000000) 
      return "MHz";
    else
      return "KHz";
  }
  
  // bluetooth receiver
  private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      int connect_state = intent.getIntExtra("connect_state",0);
      if ((connect_state == 2) || (connect_state == 3))
      {
        // CALL_OUT or CALL_IN
        screenClockActivity.finish();
        Log.d(TAG,"finish by bluetooth");
      }
    }
  };
  
  // обработчик com.canbus.temperature & com.canbus.battery
  private BroadcastReceiver canbusReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      String action = intent.getAction();
      Log.d(TAG,"canbus: "+action);
      if (action.equals("com.canbus.temperature"))
      {
        String temperature = intent.getStringExtra("temperature");
        Log.d(TAG,"temperature="+temperature);
        if (temperatureView != null) temperatureView.setText(temperature.trim());
      }
      else if (action.equals("com.canbus.battery") && showBattery)
      {
        float battery_voltage = intent.getIntExtra("battery",0);
        Log.d(TAG,"battery_voltage="+battery_voltage);
        if ((batteryView != null) && (battery_voltage > 0)) batteryView.setText(Float.toString(battery_voltage/10));
      }
    }
  };
  
  // статус бар
  private static BroadcastReceiver statusBarReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      String topPackage = intent.getStringExtra("pkname");
      // закрываем скрисейвер при старте другого приложения
      screenClockActivity.finish();
      Log.d(TAG,"finish by "+topPackage);
    }
  };
  
  // изменение скорости
  private LocationListener locationListener = new LocationListener() 
  {
    public void onLocationChanged(Location location)
    {
      if (!location.hasSpeed()) return;
      int speed = (int)(location.getSpeed()*3.6/speed_divider);
      speedView.setText(speed+"");
    }
      
    public void onProviderDisabled(String provider) {}
      
    public void onProviderEnabled(String provider) {}
    
    public void onStatusChanged(String provider, int status, Bundle extras) {}
  };
  
  // нажатие на экран
  public View.OnClickListener screenClick = new View.OnClickListener() 
  {
    public void onClick(View v) 
    {
      screenClockActivity.finish();
      Log.d(TAG,"finish by touch");
    }
  };
    
}
