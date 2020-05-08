package ru.mvgv70.xposed_mtce_utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.location.LocationListener;
import ru.mvgv70.brightness.BrightnessTimer;
import ru.mvgv70.utils.IniFile;
import ru.mvgv70.utils.Utils;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.microntek.CarManager;
import android.microntek.HCTApi;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Microntek implements IXposedHookLoadPackage {
	
  private static final String TAG = "xposed-mtce-utils-manager";
  private static final String PACKAGE_NAME = "android.microntek.service";
  private static final String SCREENCLOCK_PACKAGE = "com.microntek.screenclock";
  private static final String MTCE_MUSIC_PACKAGE = "com.microntek.music";
  private static String EXTERNAL_SD = "/storage/external_sd/";
  private static final String INIFILE_NAME = "mtce-utils/settings.ini";
  private static final int KEY_HANGUP = 317;
  private static final int KEY_VOLUME_PLUS = 273;
  private static final int KEY_VOLUME_MINUS = 281;
  // bluetooth
  private static final String BLUETOOTH_STATE = "connect_state";
  private static final int BLUETOOTH_CALL_END = 1;
  private static final int BLUETOOTH_CALL_OUT = 2;
  private static final int BLUETOOTH_CALL_IN = 3;
  private static boolean isBTState = false;
  // секции
  private static final String SETTINGS_SECTION = "mtce.settings";
  private static final String WHITELIST_SECTION = "mtce.whitelist";
  private static final String WEATHER_SECTION = "weather.settings";
  private static final String MODE_SECTION = "mtce.mode";
  private static final String DEBUG_SECTION = "debug";
  private static final String SPEED_VOLUME = "mtce.speed.volume";
  private static final String SCREENSAVER_EXCEPTIONS = "screenclock.exceptions";
  private static final String SERVICES_SECTION = "services.start";
  private static final String APPS_SECTION = "apps.start";
  private static final String TOAST_SECTION = "mtce.toast";
  private static final String BRIGHTNESS_SECTION = "brightness";
  private static Service microntekServer = null;
  private static Context context = null;
  private static boolean loaded = false;
  private static int wakeupCount = 0;
  private static String activeClassName = "";
  private static String activePlayerClassName = "";
  private static String topPackage = "";
  // настройки
  private static IniFile props = new IniFile();
  // список исключений для таск-киллера
  private static List<String> white_list = null;
  // список исключений скринсейвера
  private static List<String> ss_exceptions = new ArrayList<String>();
  // mode
  private static ActivityManager acm; 
  private static boolean modeSwitch = false;
  private static List<String> mode_app_list = new ArrayList<String>();
  private static String mode_app = "";
  private static int mode_index = -1;
  private static String navi_package = "";
  private static boolean gps_isfront = false;
  private static boolean naviAutoStart = false;
  // настройки
  private static boolean hangup_as_back = false;
  private static int safeVolume = 0;
  private static int maxVolume = 35;
  private static boolean sync_gps_time = false;
  private static boolean wifi_on = false;
  private static boolean posDetected = true;
  private static boolean screenClock = false;
  private static String temperature = "";
  private static int battery_voltage = 0;
  private static boolean weatherEnabled = false;
  private static String weatherIntent = "";
  private static String weatherTempExtra = "";
  private static boolean directSound = false;
  private static boolean clearLastApp = false;
  private static boolean correctVolume = false;
  private static int currentVolume = 0;
  private static boolean listVolumes = false;
  private static boolean customVolumeBar = true;
  // автоматическая яркость
  private static boolean autoBrightness = false;
  private static int dayBrightnessLevel = -1;
  private static int nightBrightnessLevel = -1;
  private static BrightnessTimer timer = null;
  // уровень горомкости от скорости
  private static boolean speedVolume = true;
  private static int volumeDelta = 1;
  private static double last_speed = 0;
  private static ArrayList<Integer> speedValues = new ArrayList<Integer>();
  private static LocationManager lcm = null;
  private static CarManager cm = null;
  // music toast
  private static Toast toast = null;
  private static TextView toastText = null;
  private static int toastSize = 0;
  private static int toastColor = 0;
  // линейка громкости
  private static BitmapDrawable seekbar_bg = null; 
  private static BitmapDrawable seekbar_fg = null;
  private static BitmapDrawable seekbar_handle = null;
  // apps start event
  private static final int START_WAKEUP = 1;
  private static final int START_REBOOT = 2;
  private static int start_event = START_REBOOT;
  
  @Override
  public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable 
  {
    // MicrontekServer.onCreate()
    XC_MethodHook onCreate = new XC_MethodHook() {
    	
      @Override
      @SuppressLint("ShowToast")
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        microntekServer = (Service)param.thisObject;
        Log.d(TAG,"onCreate");
        acm = (ActivityManager)microntekServer.getSystemService(Context.ACTIVITY_SERVICE);
        lcm = (LocationManager)microntekServer.getSystemService(Context.LOCATION_SERVICE);
        cm = new CarManager();
        // показать версию модуля
        try 
        {
          context = microntekServer.createPackageContext(getClass().getPackage().getName(), Context.CONTEXT_IGNORE_SECURITY);
          String version = context.getString(R.string.app_version_name);
          Log.i(TAG,"version="+version);
        } catch (Exception e) {}
        // версия Андроид
        Log.i(TAG,"android "+Build.VERSION.RELEASE);
        // показать версию mcu
        Log.i(TAG,cm.getParameters("sta_mcu_version="));
        // toast
        prepareToast();
        // путь к файлу из build.prop или mvgv70.xposed.map
        EXTERNAL_SD = Utils.getModuleSdCard();
        Log.d(TAG,EXTERNAL_SD+" "+Environment.getExternalStorageState(new File(EXTERNAL_SD)));
        // чтение настроечного файла
      	if (Environment.getExternalStorageState(new File(EXTERNAL_SD)).equals(Environment.MEDIA_MOUNTED))
      	{
      	  // чтение настроек
          readSettings();
      	}
      	createReceivers();
        createKeyHandler();
        createMediaReceiver();
      }
    };
	    
    // ClearProcess.getisdontclose(String)
    XC_MethodHook getisdontclose = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        String className = (String)param.args[0];
        if (((boolean)param.getResult() == false) && ((white_list != null)))
        {
          // если microntek собирается закрыть программу или сервис
          for (String pkg_name : white_list)
          if (className.startsWith(pkg_name))
          {
            Log.d(TAG,className+" not closed");
            param.setResult(true);
            break;
          }
        }
      }
    };
    
    // VolumeDialog.onCreate
    XC_MethodHook volumeDialogCreate = new XC_MethodHook() {
        
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"volumeDialog create");
        // set gravity to bollom
        Dialog dialog = (Dialog)param.thisObject;
        Window window = dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(0xE0101010));
        WindowManager.LayoutParams lpw = window.getAttributes();
        lpw.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        window.setAttributes(lpw); 
        // increase width and height of layout for volume slider
        int dialog_id = microntekServer.getResources().getIdentifier("volume_slider", "id", microntekServer.getPackageName());
        Log.d(TAG,"dialog_id="+dialog_id);
        LinearLayout toggleSlider = (LinearLayout)dialog.findViewById(dialog_id);
        LinearLayout.LayoutParams lps = (LinearLayout.LayoutParams)toggleSlider.getLayoutParams();
        lps.width = maxVolume*23+90;
        lps.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        toggleSlider.setLayoutParams(lps);
      }
    };
    
    // VolumeController.onCreate
    XC_MethodHook volumeControllerCreate = new XC_MethodHook() {
        
      @Override
      protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"volumeController create");
        // установка текущей громкости
        param.args[2] = getVolume();
      }
    };
    
    // ToggleSlider.onAttachedToWindow
    XC_MethodHook onAttachedToWindow = new XC_MethodHook() {
        
      @Override
      protected void beforeHookedMethod(MethodHookParam param) throws Throwable 
      {
        Log.d(TAG,"onAttachedToWindow");
        if (customVolumeBar == false) return;
        LinearLayout layout = (LinearLayout)param.thisObject;
        // slider
        int slider_id = microntekServer.getResources().getIdentifier("slider", "id", microntekServer.getPackageName());
        Log.d(TAG,"slider_id="+slider_id);
        SeekBar seekbar = (SeekBar)layout.findViewById(slider_id);
        if (seekbar == null) Log.w(TAG,"seekbar == null");
        // set drawable for SeekBar
        if ((seekbar_fg != null) && (seekbar_bg != null) && (seekbar != null))
        {
          Log.d(TAG,"change seekbar drawables");
          ClipDrawable seekbar_pr = new ClipDrawable(seekbar_fg, Gravity.START, ClipDrawable.HORIZONTAL);
          Drawable[] layers = new Drawable[] { seekbar_bg, seekbar_pr };
          LayerDrawable progress = new LayerDrawable(layers);
          progress.setId(0, android.R.id.background);
          progress.setId(1, android.R.id.progress);
          seekbar.setProgressDrawable(progress);
          if (seekbar_handle != null) seekbar.setThumb(seekbar_handle);
          // increase width of SeekBar
          LayoutParams lp = seekbar.getLayoutParams();
          lp.width = maxVolume*23;
          seekbar.setLayoutParams(lp);
        }
      }
    };
    
    // MicrontekServer.ModeSwitch()
    XC_MethodHook ModeSwitch = new XC_MethodHook() {
      
      @Override
      protected void beforeHookedMethod(MethodHookParam param) throws Throwable 
      {
        Log.d(TAG,"ModeSwitch");
        if (modeSwitch)
        {
          modeSwitch();
          // не вызываем штатный обработчик
          param.setResult(null);
        }
      }
    };
    
    // MicrontekServer.startScreenSaver()
    XC_MethodHook startScreenSaver = new XC_MethodHook() {
      
      @Override
      protected void beforeHookedMethod(MethodHookParam param) throws Throwable 
      {
        Log.d(TAG,"start ScreenSaver");
        if (!screenClock) return;
        try 
        {
          @SuppressWarnings("deprecation")
          List<ActivityManager.RunningTaskInfo> taskList = acm.getRunningTasks(1);
          String topActivity = taskList.get(0).baseActivity.getPackageName();
          Log.d(TAG,"topActivity="+topActivity);
          if ((ss_exceptions != null) && ss_exceptions.contains(topActivity))
          {
            Log.d(TAG,topActivity+" in screensaver exceptions list");
            // сброс счетчика скринсейвера
            Utils.callMethod(microntekServer, "clearScreenSaverTimer");
          }
          else
          {
            Intent clockIntent = new Intent("android.intent.action.MAIN");
            clockIntent.setComponent(new ComponentName("com.microntek.screenclock","com.microntek.screenclock.MainActivity"));
            clockIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_ACTIVITY_SINGLE_TOP); 
            microntekServer.startActivity(clockIntent);
          }
          // не вызываем штатный обработчик
          param.setResult(null);
        } catch (Exception e) 
        {
          Log.d(TAG,e.getMessage());
        }
      }
    };
    
    // begin hooks
    if (!lpparam.packageName.equals(PACKAGE_NAME)) return;
    Log.d(TAG,PACKAGE_NAME);
    Utils.setTag(TAG);
    Utils.readXposedMap();
    Utils.findAndHookMethodCatch("android.microntek.service.MicrontekServer", lpparam.classLoader, "onCreate", onCreate);
    Utils.findAndHookMethodCatch("android.microntek.ClearProcess", lpparam.classLoader, "getisdontclose", String.class, getisdontclose);
    Utils.findAndHookMethodCatch("android.microntek.ClearProcess", lpparam.classLoader, "getisdontclose2", String.class, getisdontclose);
    Utils.findAndHookMethodCatch("com.microntek.app.VolumeDialog", lpparam.classLoader, "onCreate", Bundle.class, volumeDialogCreate);
    Utils.findAndHookConstructorCatchNoWarn("android.microntek.common.VolumeController", lpparam.classLoader, Context.class, "android.microntek.common.ToggleSlider", int.class, int.class, volumeControllerCreate);
    Utils.findAndHookMethodCatch("android.microntek.service.MicrontekServiceBase", lpparam.classLoader, "modeSwitch", ModeSwitch);
    Utils.findAndHookMethodCatch("android.microntek.service.MicrontekServiceBase", lpparam.classLoader, "startScreenSaver", startScreenSaver);
    Utils.findAndHookMethodCatch("android.microntek.common.ToggleSlider", lpparam.classLoader, "onAttachedToWindow", onAttachedToWindow);
    Log.d(TAG,PACKAGE_NAME+" hook OK");
  }
  
  // чтение настроек
  private void readSettings()
  {
    try
    {
      Log.d(TAG,"read settings from "+EXTERNAL_SD+INIFILE_NAME);
      props.clear();
      props.loadFromFile(EXTERNAL_SD+INIFILE_NAME);
      // модуль загружен
      loaded = true;
    }
    catch (Exception e)
    {
      Log.e(TAG,e.getMessage());
      listAllVolumes();
      return;
    }
    // настройки
    // белый список
    white_list = props.getLines(WHITELIST_SECTION);
    if (white_list != null)
      Log.d(TAG,WHITELIST_SECTION+": count="+white_list.size());
    // mode
    modeSwitch = props.getBoolValue(SETTINGS_SECTION, "mode", false);
    Log.d(TAG,"modeSwitch="+modeSwitch);
    // mode list
    mode_app_list = props.getLines(MODE_SECTION);
    if (mode_app_list != null)
      Log.d(TAG,MODE_SECTION+": count="+mode_app_list.size());
    else if (modeSwitch)
    {
      Log.w(TAG,MODE_SECTION+" is empty");
      modeSwitch = false;
    }
    // service list
    Log.d(TAG,SERVICES_SECTION+": count="+props.linesCount(SERVICES_SECTION));
    // apps list
    Log.d(TAG,APPS_SECTION+": count="+props.linesCount(APPS_SECTION));
    // hangup_as_back
    hangup_as_back = props.getBoolValue(SETTINGS_SECTION, "hangup_as_back", false);
    Log.d(TAG,"hangup_as_back="+hangup_as_back);
    // safe_volume
    safeVolume = props.getIntValue(SETTINGS_SECTION, "volume.safe", 0);
    Log.d(TAG,"volume.safe="+safeVolume);
    // sync_gps_time
    sync_gps_time = props.getBoolValue(SETTINGS_SECTION, "sync_gps_time", false);
    Log.d(TAG,"sync_gps_time="+sync_gps_time);
    // wifi_on
    wifi_on = props.getBoolValue(SETTINGS_SECTION, "wifi.on", false);
    Log.d(TAG,"wifi.on="+wifi_on);
    // screenclock
    screenClock = props.getBoolValue(SETTINGS_SECTION, "screenclock", false);
    Log.d(TAG,"screenclock="+screenClock);
    // исключения скринсейвера
    ss_exceptions = props.getLines(SCREENSAVER_EXCEPTIONS);
    if (ss_exceptions != null)
      Log.d(TAG,SCREENSAVER_EXCEPTIONS+": count="+ss_exceptions.size());
    // max volume
    try
    {
      maxVolume = Integer.parseInt(cm.getParameters("cfg_maxvolume="));
    }
    catch (Exception e) {}
    Log.d(TAG,"max.volume="+maxVolume);
    // speed.volume
    speedVolume = props.getBoolValue(SPEED_VOLUME, "enable", true);
    Log.d(TAG,"speed.volume="+speedVolume);
    // volume.delta
    volumeDelta = props.getIntValue(SPEED_VOLUME, "volume.delta", 1);
    Log.d(TAG,"volume.delta="+volumeDelta);
    // weather
    weatherEnabled = props.getBoolValue(SETTINGS_SECTION, "weather.enabled", false);
    Log.d(TAG,"weather.enabled="+weatherEnabled);
    weatherIntent = props.getValue(WEATHER_SECTION, "intent");
    Log.d(TAG,"intent="+weatherIntent);
    weatherTempExtra = props.getValue(WEATHER_SECTION, "extra");
    Log.d(TAG,"extra="+weatherTempExtra);
    if (TextUtils.isEmpty(weatherIntent) || TextUtils.isEmpty(weatherTempExtra)) weatherEnabled = false;
    // direct sound
    directSound = props.getBoolValue(SETTINGS_SECTION, "directsound", false);
    Log.d(TAG,"directSound="+directSound);
    // toast
    toastSize = props.getIntValue(TOAST_SECTION, "size", 0);
    Log.d(TAG,"toast.size="+toastSize);
    toastColor = props.getColorValue(TOAST_SECTION, "color", 0);
    Log.d(TAG,"toast.color="+Integer.toHexString(toastColor));
    // customVolumeBar
    customVolumeBar = props.getBoolValue(SETTINGS_SECTION, "volume_bar", true);
    Log.d(TAG,"volume_bar="+customVolumeBar);
    // navi_package
    navi_package = Settings.System.getString(microntekServer.getContentResolver(), "gpspkname");
    Log.d(TAG,"navi_package="+navi_package);
    // navi_auto_start
    naviAutoStart = props.getBoolValue(SETTINGS_SECTION, "navi_auto_start", false);
    Log.d(TAG,"navi_auto_start="+naviAutoStart);
    // auto_brightness
    autoBrightness = props.getBoolValue(SETTINGS_SECTION, "auto_brightness", false);
    Log.d(TAG,"auto_brightness="+autoBrightness);
    dayBrightnessLevel = props.getIntValue(BRIGHTNESS_SECTION, "day", -1);
    Log.d(TAG,"day="+dayBrightnessLevel);
    nightBrightnessLevel = props.getIntValue(BRIGHTNESS_SECTION, "night", -1);
    Log.d(TAG,"night="+nightBrightnessLevel);
    if (autoBrightness)
    {
      timer = new BrightnessTimer(microntekServer, dayBrightnessLevel, nightBrightnessLevel);
      Log.d(TAG,"create auto brightness timers from preferences");
      timer.setTimers(null);
    }
    // картинки для линейки громкости
    prepareVolumeDrawable();
    // speed values
    readSpeedValues();
    // start services & apps
    startServiceThread(START_REBOOT);
    // debug 
    processDebugSettings();
  }
  
  // debug settings
  private void processDebugSettings()
  {
    // list volumes
    listVolumes = props.getBoolValue(DEBUG_SECTION, "list_volumes", false);
    Log.d(TAG,"list_volumes="+listVolumes);
    if (listVolumes) listAllVolumes();
    // correct volume
    correctVolume = props.getBoolValue(DEBUG_SECTION, "correct_volume", false);
    Log.d(TAG,"correct_volume="+correctVolume);
    if (correctVolume) setSavedVolume();
    currentVolume = getVolume();
    Log.d(TAG,"currentVolume="+currentVolume);
    // last app
    clearLastApp = props.getBoolValue(DEBUG_SECTION, "clear_last_app", false);
    Log.d(TAG,"clear_last_app="+clearLastApp);
    if (clearLastApp)
    {
      String lastApps = "null,null,null";
      Log.d(TAG,"microntek.lastpackname="+Settings.System.getString(microntekServer.getContentResolver(), "microntek.lastpackname"));
      Settings.System.putString(microntekServer.getContentResolver(),"microntek.lastpackname",lastApps);
    }
  }
  
  private void setSavedVolume()
  {
    currentVolume = Settings.System.getInt(context.getContentResolver(),"av_mvg_volume=",0);
    Log.d(TAG,"screen on: saved volume="+currentVolume);
    if ((currentVolume > 0) && ((currentVolume < safeVolume) || (safeVolume == 0)))
    {
      setVolume(currentVolume);
      currentVolume = getVolume();
      Log.d(TAG,"currentVolume="+currentVolume);
    }
  };
  
  // чтение некоторых настроек после просыпания
  private void readSettingsWakeUp()
  {
    try
    {
      Log.d(TAG,"*read settings from "+EXTERNAL_SD+INIFILE_NAME);
      props.clear();
      props.loadFromFile(EXTERNAL_SD+INIFILE_NAME);
      // safe_volume
      safeVolume = props.getIntValue(SETTINGS_SECTION, "volume.safe", 0);
      Log.d(TAG,"volume.safe="+safeVolume);
      // sync_gps_time
      sync_gps_time = props.getBoolValue(SETTINGS_SECTION, "sync_gps_time", false);
      Log.d(TAG,"sync_gps_time="+sync_gps_time);
      // speed.volume
      speedVolume = props.getBoolValue(SPEED_VOLUME, "enable", false);
      Log.d(TAG,"speed.volume="+speedVolume);
      // volume.delta
      volumeDelta = props.getIntValue(SPEED_VOLUME, "volume.delta", 1);
      Log.d(TAG,"volume.delta="+volumeDelta);
      // speed values
      readSpeedValues();
      // apps list
      Log.d(TAG,APPS_SECTION+": count="+props.linesCount(APPS_SECTION));
      // синхронизация времени и заход/восход
      createTimeReceiver();
      // navi_package
      navi_package = Settings.System.getString(microntekServer.getContentResolver(), "gpspkname");
      Log.d(TAG,"navi_package="+navi_package);
      // auto brightness
      if (autoBrightness)
      {
        Log.d(TAG,"create auto brightness timers from preferences");
        timer.setTimers(null);
      }
      // start services & apps
      startServiceThread(START_WAKEUP);
      // debug
      processDebugSettings();
    }
    catch (Exception e)
    {
      Log.e(TAG,e.getMessage());
      listAllVolumes();
    }
  }
  
  // подготовка drawable для линейки громкости
  private void prepareVolumeDrawable()
  {
    Log.d(TAG,"prepareVolumeDrawable");
    TextPaint paint = new TextPaint();
    paint.setColor(Color.WHITE);
    // bg
    Bitmap bitmap_bg = Bitmap.createBitmap(8*2*maxVolume, 48, Bitmap.Config.ARGB_8888);
    Canvas canvas_bg = new Canvas(bitmap_bg);
    canvas_bg.drawColor(Color.TRANSPARENT); 
    for (int i=0; i<maxVolume; i++)
    {
      canvas_bg.drawRect(4+16*i, 20, 12+16*i, 28, paint);
    }
    seekbar_bg = new BitmapDrawable(microntekServer.getResources(), bitmap_bg);
    // fg
    Bitmap bitmap_fg = Bitmap.createBitmap(8*2*maxVolume, 48, Bitmap.Config.ARGB_8888);
    Canvas canvas_fg = new Canvas(bitmap_fg);
    canvas_fg.drawColor(Color.TRANSPARENT); 
    for (int i=0; i<maxVolume; i++)
    {
      canvas_fg.drawRect(4+16*i, 8, 12+16*i, 40, paint);
    }
    seekbar_fg = new BitmapDrawable(microntekServer.getResources(), bitmap_fg);
    // drawable handle
    Bitmap bitmap_h = Bitmap.createBitmap(1, 48, Bitmap.Config.ARGB_8888);
    Canvas canvas_h = new Canvas(bitmap_h);
    canvas_h.drawColor(Color.TRANSPARENT);
    seekbar_handle = new BitmapDrawable(microntekServer.getResources(), bitmap_h);
  }
  
  private void startServiceThread(int event)
  {
    start_event = event;
    // запуск сервисов и приложений в отдельном потоке
    Thread start_services = new Thread("start_service")
    {
      public void run()
      {
        if (start_event == START_REBOOT)
        {
          startServices();
          turnWiFiOn();
        }
        startApps(start_event);
      }
    };
    Log.d(TAG,"start services & apps thread");
    start_services.run();
  }
  
  // включение wi-fi
  private void turnWiFiOn()
  {
    if (wifi_on)
    {
      // включение wi-fi
      try
      {
        WifiManager Wifi = (WifiManager)microntekServer.getSystemService(Context.WIFI_SERVICE);
        Log.d(TAG,"wifi state="+Wifi.getWifiState());
        Wifi.setWifiEnabled(true);
        Log.d(TAG,"wifi set on");
      }
      catch (Exception e)
      {
        Log.e(TAG,e.getMessage());
      }
    }
  }

  // ускоренный запуск сервисов
  private void startServices()
  {
    // services
    int pos;
    String line;
    Log.d(TAG,"startServices");
    Iterator<String> lines = props.enumLines(SERVICES_SECTION);
    if (lines == null) return;
    while (lines.hasNext()) 
    {
      line = lines.next();
      pos = line.indexOf("/");
      if (pos >= 0)
      {
        // разбираем формат имя пакета/имя сервиса
        String packageName = line.substring(0,pos);
        String className = line.substring(pos+1,line.length());
        Log.d(TAG,"start service: "+packageName+"/"+className);
        ComponentName cn = new ComponentName(packageName,className);
        Intent intent = new Intent();
        intent.setComponent(cn);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        try
        {
          // пытаемся стартовать сервис
          ComponentName cr = microntekServer.startService(intent);
          if (cr == null) Log.w(TAG,"service "+line+" not found");
        }
        catch (Exception e)
        {  	      
          Log.e(TAG,e.getMessage());
        }
      }
      else
        Log.w(TAG,"incorrect service declaration: "+line);
    }
  }
  
  // запуск программ
  private void startApps(int what)
  {
    String app;
    String event;
    Log.d(TAG,"startApps("+what+")");
    Iterator<String> lines = props.enumKeys(APPS_SECTION);
    if (lines == null) return;
    while (lines.hasNext()) 
    {
      app = lines.next();
      event = props.getValue(APPS_SECTION, app);
      Log.d(TAG,app+"="+event);
      if (TextUtils.isEmpty(event)) continue;
      if (event.equals("all") || (event.equals("reboot") && (what == START_REBOOT)) || (event.equals("wakeup") && (what == START_WAKEUP)))
      {
        Log.d(TAG,"start app: "+app);
        Intent intent = microntekServer.getPackageManager().getLaunchIntentForPackage(app);
        if (intent != null)
        {
          intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY | Intent.FLAG_ACTIVITY_SINGLE_TOP);
          microntekServer.startActivity(intent);
        }
        else
          Log.w(TAG,"no activity found for "+app);
      }
    }
  }

  // default navigation start
  private static void startNavi()
  {
    if (!TextUtils.isEmpty(navi_package)) 
    {
      Log.d(TAG,"run navi program: "+navi_package);
      Intent intent = microntekServer.getPackageManager().getLaunchIntentForPackage(navi_package);
      if (intent != null)
      {
        intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        microntekServer.startActivity(intent);
      }
    } 
  }
  
  // разбираем строку со скоростями	
  private void readSpeedValues() 
  {
    int speed;
    String speed_cfg = props.getValue(SPEED_VOLUME, "speed.volume", "40,80,100,120");
    Log.d(TAG,"speed.volume="+speed_cfg);
    // рассчитываем
    List<String> speed_vals = Arrays.asList(speed_cfg.split("\\s*,\\s*"));
    speedValues.clear();
    for (String speed_step : speed_vals) 
    {
      try
      {
        speed = Integer.parseInt(speed_step);
        // отбрасываем некорректные значения
        if ((speed > 0) && (speed < 300)) 
          speedValues.add(speed);
      } catch (Exception e) {}
    }
    Log.d(TAG,SPEED_VOLUME+": count="+speedValues.size());
  }
  
  // переключение приложений
  private void modeSwitch()
  {
    if (mode_app_list.size() <= 0) return;
    // определить активную программу и ее индекс
    try 
    {
      @SuppressWarnings("deprecation")
      List<ActivityManager.RunningTaskInfo> taskList = acm.getRunningTasks(10);
      for (ActivityManager.RunningTaskInfo task : taskList)
      {
        if (mode_app_list.contains(task.baseActivity.getPackageName())) 
        {
          mode_app = task.baseActivity.getPackageName();
          mode_index = mode_app_list.indexOf(mode_app);
          Log.d(TAG,"mode_app="+mode_app+", mode_index="+mode_index);
          break;
        }
      }
     } catch (Exception e) {}
     // следующий индекс
     int index;
     if ((mode_index+1) < mode_app_list.size())
       index = mode_index+1;
     else
       index = 0;
     Log.d(TAG,"next="+index);
     // запускаемая программа
     String run_app = mode_app_list.get(index);
     Log.d(TAG,"runApp="+run_app);
     // стартуем следующую
     Intent intent = microntekServer.getPackageManager().getLaunchIntentForPackage(run_app);
     // DVD может не запустится, если нет диска
     if (intent != null)
     {
       intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY | Intent.FLAG_ACTIVITY_SINGLE_TOP);
       // штатные приложения в фоновом режиме при работающей навигации
       if (gps_isfront && run_app.startsWith("com.microntek."))
       {
         Log.d(TAG,"add extra start=1");
         intent.putExtra("start", 1);
       }
       microntekServer.startActivity(intent);
     }
     else
       Log.e(TAG, "can not run "+run_app);
     // 
     mode_app = run_app;
     mode_index = index;
  }
  
  // уведомление об изменении громкости
  private void changeVolumeNotify(int level)
  {
    Intent intent = new Intent("com.microntek.VOLUME_CHANGED");
    intent.putExtra("volume",level);
    context.sendBroadcast(intent);
  } 
  
  public void setVolume(int level) 
  {
    Settings.System.putInt(context.getContentResolver(),"av_volume=",level);
    int mtcLevel = mtcGetRealVolume(level);
    cm.setParameters("av_volume="+mtcLevel);
    Utils.setIntField(microntekServer,"mCurVolume",level);
    changeVolumeNotify(level);
  } 
  
  private int mtcGetRealVolume(int paramInt) 
  {
    float f1 = 100.0F * paramInt / maxVolume;
    float f2;
    if (f1 < 20.0F)
      f2 = f1 * 3.0F / 2.0F;
    else if (f1 < 50.0F)
      f2 = f1 + 10.0F;
    else
      f2 = 20.0F + f1 * 4.0F / 5.0F;
    return (int)f2;
  } 
  
  private int getVolume()
  {
    return Settings.System.getInt(microntekServer.getContentResolver(), "av_volume=", 10);
  }
  
  private boolean getMute()
  {
    return cm.getParameters("av_mute=").equals("true");
  }
  
  // если текущая громкость больше безопасной установим безопасную громкость
  private void setSafeVolume()
  {
    int volume = getVolume();
    Log.d(TAG,"current volume="+volume+", safe volume="+safeVolume);
    if ((safeVolume > 0) && (volume > safeVolume)) 
    {
      Log.d(TAG,"set safe volume "+safeVolume);
      setVolume(safeVolume);
      currentVolume = safeVolume;
      Log.d(TAG,"currentVolume="+currentVolume);
    }
  }
  
  // включить обработчик подключения носителей
  private void createMediaReceiver()
  {
    IntentFilter ui = new IntentFilter();
    ui.addAction(Intent.ACTION_MEDIA_MOUNTED);
    ui.addDataScheme("file");
    microntekServer.registerReceiver(mediaReceiver, ui);
    Log.d(TAG,"media mount receiver created");
  }
  
  private void listAllVolumes()
  {
    try
    {
      Log.d(TAG,"listAllVolumes");
      StorageManager sm = (StorageManager)microntekServer.getSystemService(StorageManager.class);
      @SuppressWarnings("unchecked")
      List<VolumeInfo> volumes = (List<VolumeInfo>)Utils.callMethod(sm, "getVolumes");
      if (volumes != null)
      {
        Log.d(TAG,"-");
        Iterator<VolumeInfo> volumes_i = volumes.iterator();
        while (volumes_i.hasNext())
        {
          VolumeInfo volume = (VolumeInfo)volumes_i.next();
          if ((volume.getType() == VolumeInfo.TYPE_PUBLIC) || (volume.getType() == VolumeInfo.TYPE_EMULATED))
          {
            Log.d(TAG,"path="+volume.getPath());
            if (volume.getDisk() != null)
              Log.d(TAG,"deviceName="+HCTApi.getDeviceName(volume.getDisk().sysPath));
            Log.d(TAG,"-");
          }
        } 
      }
    }
    catch (Exception e)
    {
      Log.e(TAG,e.getMessage());
    }
  }
  
  // обработчик MEDIA_MOUNT
  private BroadcastReceiver mediaReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      String action = intent.getAction(); 
      String drivePath = intent.getData().getPath();
      Log.d(TAG,"media receiver: "+drivePath+" "+action);
      // если подключается external_sd
      if (action.equals(Intent.ACTION_MEDIA_MOUNTED) && Utils.isExternalCard(drivePath, EXTERNAL_SD))
      {
        // читаем настройки
        if (loaded)
          // подключение карты после сна
          readSettingsWakeUp();
        else
          // чтение настроек
          readSettings();
      }
    }
  };
  
  // создание обработчика нажатий
  private void createKeyHandler()
  {
    cm.attach(new KeyHandler(), "KeyDown,CarEvent");
    Log.d(TAG,"KeyHandler created");
  }
  
  // обработчик событий
  @SuppressLint("HandlerLeak")
  private class KeyHandler extends Handler
  {
    public void handleMessage(Message msg)
    {
      String event = (String)msg.obj;
      if (event.equals("KeyDown"))
      {
        int keyCode = msg.getData().getInt("value");
        handleKeyDown(keyCode);
      }
      else if (event.equals("CarEvent"))
      {
        String type = msg.getData().getString("type");
        if (type.equals("battery"))
        {
          battery_voltage = msg.getData().getInt("value");
          Log.d(TAG,"battery_voltage="+battery_voltage);
        }
      }
    }
    
    private void handleKeyDown(int keyCode)
    {
      if (keyCode == KEY_HANGUP)
      {
        if (!isBTState && hangup_as_back)
        {
          Log.d(TAG,"press back");
          HCTApi.SystemKey(KeyEvent.KEYCODE_BACK, 0);
        }
      }
      else if ((keyCode == KEY_VOLUME_PLUS) || keyCode == KEY_VOLUME_MINUS)
      {
        if (directSound)
        {
          // эмуляция нажатия для прямого управления звуком
          Intent intent = new Intent("com.microntek.irkeyDown");
          intent.putExtra("keyCode", keyCode);
          microntekServer.sendBroadcast(intent);
        }
        if (keyCode == KEY_VOLUME_PLUS) 
        {
          currentVolume++;
          if (currentVolume > maxVolume) currentVolume = maxVolume;
        }
        else if (keyCode == KEY_VOLUME_MINUS)
        {
          currentVolume--;
          if (currentVolume < 0) currentVolume = 0;
        }
        Log.d(TAG,"currentVolume="+currentVolume);
      }
    }      
  };
  
  private void createReceivers()
  {
    // bluetooth
    IntentFilter bi = new IntentFilter();
    bi.addAction("com.microntek.bt.report");
    microntekServer.registerReceiver(bluetoothReceiver, bi);
    Log.d(TAG,"bluetooth receiver created");
    // screen_on
    IntentFilter oi = new IntentFilter();
    oi.addAction("android.intent.action.SCREEN_ON");
    microntekServer.registerReceiver(screenOnReceiver, oi);
    Log.d(TAG,"screen on receiver created");
    // canbus
    IntentFilter ci = new IntentFilter();
    ci.addAction("com.canbus.temperature");
    microntekServer.registerReceiver(canbusReceiver, ci);
    Log.d(TAG,"canbus receiver created");
    // screenclock
    IntentFilter li = new IntentFilter();
    li.addAction("hct.canbus.info");
    microntekServer.registerReceiver(screenClockReceiver, li);
    Log.d(TAG,"screenclock request receiver created");
    // запуск штатных приложений
    IntentFilter mi = new IntentFilter();
    mi.addAction("com.microntek.bootcheck");
    microntekServer.registerReceiver(microntekReceiver, mi);
    Log.d(TAG,"bootcheck receiver created");
    // mcte utils
    IntentFilter ui = new IntentFilter();
    ui.addAction("ru.mvgv70.mtceutils.runactivemusic");
    ui.addAction("ru.mvgv70.mtceutils.runactiveplayer");
    ui.addAction("ru.mvgv70.mtceutils.mode");
    ui.addAction("ru.mvgv70.mtceutils.runnavi");
    microntekServer.registerReceiver(mtceutilsReceiver, ui);
    Log.d(TAG,"mtce utils receiver created");
    // weather
    if (weatherEnabled)
    {
      IntentFilter wi = new IntentFilter();
      wi.addAction(weatherIntent);
      microntekServer.registerReceiver(weatherReceiver, wi);
      Log.d(TAG,"weather receiver created");
    }
    // top package
    IntentFilter ti = new IntentFilter();
    ti.addAction("com.microntek.STATUS_BAR_CHANGED");
    microntekServer.registerReceiver(statusBarReceiver, ti);
    Log.d(TAG,"status bar change receiver created");
    // music toast
    IntentFilter ts = new IntentFilter();
    ts.addAction("com.microntek.music.toast");
    microntekServer.registerReceiver(musicToastReceiver, ts);
    Log.d(TAG,"music toast receiver created");
    // music receiver
    IntentFilter mp = new IntentFilter();
    mp.addAction("com.microntek.music.report");
    microntekServer.registerReceiver(musicReceiver, mp);
    Log.d(TAG,"music treceiver created");
    // speed
    if (speedVolume)
    {
      // определение скорости
      try
      {
        lcm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listenerSpeed);
        Log.d(TAG,"location updates receiver created");
      }
      catch (SecurityException e)
      {
        Log.e(TAG, e.getMessage());
      }
    }
    // sync_gps_time и auto_brightness
    createTimeReceiver();
  }
  
  // создание listener для синхронизации времени 
  private void createTimeReceiver()
  {
    try
    {
      posDetected = false;
      lcm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listenerTime, null);
      Log.d(TAG,"location listener created");
    }
    catch (Exception e) 
    { 
      Log.e(TAG, e.getMessage());
    }
  }
  
  // обработчик bluetooth
  private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      int state = intent.getIntExtra(BLUETOOTH_STATE, -1);
      if (intent.hasExtra(BLUETOOTH_STATE))
        Log.d(TAG,"bluetooth.state="+state);
      if (state == BLUETOOTH_CALL_IN) 
        isBTState = true;
      else if (state == BLUETOOTH_CALL_OUT)
        isBTState = true;
      else if (state == BLUETOOTH_CALL_END)
        isBTState = false;
    }
  };
  
  // обработчик screen on
  private BroadcastReceiver screenOnReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      Log.d(TAG,"wakeup: screen on");
      wakeupCount++;
      Log.d(TAG,"wakeupCount="+wakeupCount);
      if (wakeupCount > 1)
      {
        if (Environment.getExternalStorageState(new File(EXTERNAL_SD)).equals(Environment.MEDIA_MOUNTED))
          readSettingsWakeUp();
        else
          Log.w(TAG,EXTERNAL_SD+" "+Environment.getExternalStorageState(new File(EXTERNAL_SD)));
      }
      if (naviAutoStart) startNavi();
    }
  };
  
  // hct.canbus.info пошлем информацию в скринсейвер
  private BroadcastReceiver screenClockReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      // температура
      Intent tempIntent = new Intent("com.canbus.temperature");
      tempIntent.putExtra("temperature", temperature);
      microntekServer.sendBroadcast(tempIntent);
      // напряжение на аккумуляторе
      Intent battIntent = new Intent("com.canbus.battery");
      battIntent.putExtra("battery", battery_voltage);
      microntekServer.sendBroadcast(battIntent);
      // 
      Log.d(TAG,"send info to screensaver");
    }
  };
  
  // обработчик canbus.temperature
  private BroadcastReceiver canbusReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      String type = intent.getAction();
      Log.d(TAG,type);
      if ((type != null) && type.equals("com.canbus.temperature"))
      {
        String temp = intent.getStringExtra("temperature");
        if (!TextUtils.isEmpty(temp)) temperature = temp;
        Log.d(TAG,"temperature="+temperature);
      }
    }
  };
  
  // обработчик сторонней погоды
  private BroadcastReceiver weatherReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      int temp = intent.getIntExtra(weatherTempExtra,0xFF);
      if (temp != 0xFF) temperature = temp+"°C";
      Log.d(TAG,"temperature="+temperature);
    }
  };
  
  // com.microntek.bootcheck
  private BroadcastReceiver microntekReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      String className = intent.getStringExtra("class");
      if (className == null) return;
      if (!className.startsWith("phonecall")) activeClassName = className;
      Log.d(TAG,"com.microntek.bootcheck, class="+activeClassName);
      // выключение, сохранение громкости
      if (className.equals("poweroff"))
      {
        setSafeVolume();
        Settings.System.putInt(context.getContentResolver(),"av_mvg_volume=",currentVolume);
      }
    }
  };
  
  // статус бар, смена приложений
  private static BroadcastReceiver statusBarReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      // сохраним приложение верзнего уровня
      topPackage = intent.getStringExtra("pkname");
      Log.d(TAG,"topPackage="+topPackage);
	  // определим признак gps_isfront
      gps_isfront = topPackage.equals(navi_package);
      Log.d(TAG,"gps_isfront="+gps_isfront);
    }
  };
  
  // com.microntek.music.toast
  private static BroadcastReceiver musicToastReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      Log.d(TAG,"music toast");
      String className = intent.getStringExtra("class");
      Log.d(TAG,"className="+className+", topPackage="+topPackage+", isBTState="+isBTState);
      if (!topPackage.equals(SCREENCLOCK_PACKAGE) && !topPackage.equals(className) && !isBTState)
      {
    	try
    	{
          showToast(intent);
    	}
    	catch(Exception e)
    	{
    	  Log.e(TAG,e.getMessage());
    	}
      }
    }
  };
  
  // com.microntek.music.report
  private static BroadcastReceiver musicReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      String type = intent.getStringExtra("type");
      Log.d(TAG,"music report (type="+type+")");
      if ((type != null) && (type.equals("music.state")))
      {
        int state = intent.getIntExtra("value", 0);
        String className = intent.getStringExtra("class");
        Log.d(TAG,"className="+className+", state="+state);
        if (TextUtils.isEmpty(className)) className = MTCE_MUSIC_PACKAGE;
        if (state != 0) activePlayerClassName = className;
      }
    }
  };
  
  @SuppressLint({"ShowToast", "InflateParams"})
  private static void prepareToast()
  {
    toast = Toast.makeText(microntekServer,"",Toast.LENGTH_SHORT);
    LayoutInflater inflater = LayoutInflater.from(context);
    View view = inflater.inflate(R.layout.toast_layout, null, false);
    toast.setView(view);
    toastText = (TextView)view.findViewById(R.id.toast_text);
    toast.setDuration(Toast.LENGTH_SHORT);
    // ViewGroup group = (ViewGroup)toast.getView();
    // if (group == null) Log.w(TAG,"toast.group == null");
    // TextView toastText = (TextView)group.getChildAt(0);
  }
  
  // show toast
  @SuppressLint("InflateParams")
  private static void showToast(Intent intent)
  {
    Log.d(TAG,"showToast");
    String fileName = intent.getStringExtra(MediaStore.Audio.AudioColumns.DATA);
    Log.d(TAG,"_data="+fileName);
    // разберем имя файла
    String file = "";
    String folder = "";
    String shortFileName = "";
    if (fileName != null)
    {
      String dirs[] = fileName.split("\\s*/\\s*");
      if (dirs.length > 0)
      {
        file = dirs[dirs.length-1];
        // уберем расширение
        int lastPointPos = file.lastIndexOf('.');
        if (lastPointPos > 0)
          file = file.substring(0, lastPointPos);
        shortFileName = file;
      }
      if (dirs.length > 1) folder = dirs[dirs.length-2];
    }
    Log.d(TAG,"filename="+shortFileName);
    Log.d(TAG,"folder="+folder);
    // toast
    String text;
    if (intent.hasExtra("toast.text"))
    {
      // текст задан явно
      text = intent.getStringExtra("toast.text");
    }
    else
    {
      // форматируем теги
      String toastFormat = intent.getStringExtra("toast.format");
      // формат по-умолчанию
      if (TextUtils.isEmpty(toastFormat)) toastFormat = "%title%";
      // цикл по экстра-параметрам
      text = toastFormat;
      Bundle bundle = intent.getExtras();
      if (bundle == null) return;
      Set<String> keys = bundle.keySet();
      Iterator<String> it = keys.iterator();
      while (it.hasNext()) 
      {
        String key = it.next();
        // Log.d(TAG,key + "=" + bundle.get(key));
        text = text.replaceAll("%"+key+"%", bundle.get(key).toString());
      }
      // file names
      text = text.replaceAll("%filename%", shortFileName);
      text = text.replaceAll("%fullfilename%", fileName);
    }
    Log.d(TAG,"toast text="+text);
    // toast size
    int size = intent.getIntExtra("toast.size", 0);
    if (size == 0) size = toastSize;
    if (size > 0) toastText.setTextSize(size);
    // toast color
    int color = intent.getIntExtra("toast.color", 0);
    if (color == 0) color = toastColor;
    if (color != 0) toastText.setTextColor(color);
    // toast text
    toastText.setText(text);
    toast.show();
  }
  
  // ru.mvgv70.mtceutils.*
  private BroadcastReceiver mtceutilsReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      String action = intent.getAction();
      Log.d(TAG,action);
      if (action.equals("ru.mvgv70.mtceutils.runactivemusic") && (isBTState == false))
      {
        String className = activeClassName;
        if (TextUtils.isEmpty(className)) className = MTCE_MUSIC_PACKAGE; 
        // переключаемся на активное штатное приложение
        Log.d(TAG,"run active music: "+className);
        runApp(className);
      }
      else if (action.equals("ru.mvgv70.mtceutils.runactiveplayer") && (isBTState == false))
      {
        String className = activePlayerClassName;
        Log.d(TAG,"activePlayerClassName="+activePlayerClassName);
        if (TextUtils.isEmpty(className)) className = MTCE_MUSIC_PACKAGE; 
        // переключаемся на активный плеер
        Log.d(TAG,"run active player: "+className);
        runApp(className);
      }
      else if (action.equals("ru.mvgv70.mtceutils.mode") && (isBTState == false))
      {
        // переключение режимов, карусель
        modeSwitch();
      }
      else if (action.equals("ru.mvgv70.mtceutils.runnavi") && (isBTState == false))
      {
        // старт навигации
        startNavi();
      }
    }
  };
  
  // запуск приложения
  private void runApp(String appName)
  {
    Log.d(TAG,"run app="+appName);
    Intent intent = microntekServer.getPackageManager().getLaunchIntentForPackage(appName);
    if (intent != null)
    {
      intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      microntekServer.startActivity(intent);
    }
  }
  
  // изменение скорости
  private LocationListener listenerSpeed = new LocationListener() 
  {
    @Override
    public void onLocationChanged(Location location)
    {
      // нет скорости в объекте
      if (!location.hasSpeed()) return;
      // текущая скорость
      double speed = location.getSpeed();
      // перевод m/s в km/h
      speed *= 3.6;
      //
      double prev_speed = last_speed;
      last_speed = speed;
      // включен режим mute
      if (getMute()) return;
      // текущая громкость
      int volume = getVolume();
      // нулевая громкость
      if (volume == 0) return;
      // не в режиме разговора
      if (isBTState) return;
      // если скорость не изменилась
      if ((int)speed == (int)prev_speed) return;
      // кол-во инкрементов по скорости
      int changeIncr = 0;
      // новая громкость
      int volumeNew = 0;
      // на сколько единиц нужно увеличить громкость по сравнению с current_app_volume
      for (Integer speed_step : speedValues)
      {
        if (prev_speed >= speed_step) 
          changeIncr -= volumeDelta;
        if (speed >= speed_step) 
          changeIncr += volumeDelta;
      }
      // новый уровень громкости
      volumeNew = volume + changeIncr;
      if (volumeNew <= 0) volumeNew = 1;
      // коррекция громкости от скорости
      if (volumeNew != volume)
      {
        Log.d(TAG,"speed="+(int)speed+", prev_speed="+(int)prev_speed+", changeIncr="+changeIncr+", volume="+volume+", volumeNew="+volumeNew);
        // меняем громкость
        setVolume(volumeNew);
      }
    }
  
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }
    
    @Override
    public void onProviderEnabled(String provider) { }
	
    @Override
    public void onProviderDisabled(String provider) { }

  };
  
  // одноразовое опредление времени и координат
  private static LocationListener listenerTime = new LocationListener() 
  {
    public void onLocationChanged(Location location)
    {
      try
      {
        posDetected = true;
        Log.d(TAG,"gps location detected");
        // установка времени
        if (sync_gps_time)
        {
          SystemClock.setCurrentTimeMillis(location.getTime());
          Log.d(TAG,"system time changed");
          microntekServer.sendBroadcast(new Intent("android.intent.action.TIME_SET"));
        }
        // автоматическая яркость
        if (autoBrightness)
        {
          Log.d(TAG,"create auto brightness timers from gps location");
          timer.setTimers(location);
        }
      }
      catch (Exception e)
      {
        Log.e(TAG,e.getMessage());
      }
    }
      
    public void onProviderDisabled(String provider) {}
      
    public void onProviderEnabled(String provider) {}
    
    public void onStatusChanged(String provider, int status, Bundle extras) { } 
  };

}
