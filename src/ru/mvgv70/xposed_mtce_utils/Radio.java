package ru.mvgv70.xposed_mtce_utils;

import java.io.File;

import ru.mvgv70.utils.IniFile;
import ru.mvgv70.utils.Utils;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.microntek.CarManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Radio implements IXposedHookLoadPackage {
	
  private static final String TAG = "xposed-mtce-utils-radio";
  private static final String PACKAGE_NAME = "com.microntek.radio";
  private static String EXTERNAL_SD = "/storage/external_sd/";
  private static final String INIFILE_NAME = "mtce-utils/settings.ini";
  private static IniFile props = new IniFile();
  private static CarManager cm = null;
  // секции
  private static final String SETTINGS_SECTION = "radio.settings";
  private static boolean toastEnable = false;
  private static int toastSize = 0;
  private static boolean namesEnable = false;
  private static boolean widgetEnable = false;
  private static Service radioService = null;
  private static Activity radioActivity = null;
  private static Object radioUi = null;
  private static String stationName = "";
  // переменные
  private static boolean rdsDisable = true;
  private static boolean radioState = false;

  @Override
  public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable 
  {
    // RadioService.onCreate()
    XC_MethodHook onCreateService = new XC_MethodHook() {
      
      @Override
      @SuppressLint("ShowToast")
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"onCreateService");
        radioService = (Service)param.thisObject;
        cm = new CarManager();
        // RDS
        rdsDisable = (Utils.getBooleanField(radioService, "rdsUI") == false);
        Log.i(TAG,"rdsDisable="+rdsDisable);
      }
    };
    
    // RadioService.onDestroy()
    XC_MethodHook onDestroyService = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"onDestroyService");
        radioService = null;
        cm = null;
      }
    };
    
    // MainActivity.onCreate()
    XC_MethodHook onCreateActivity = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"onCreateActivity");
        radioActivity = (Activity)param.thisObject;
        // можно перехватывать конструктор
        radioUi = Utils.getObjectField(radioActivity, "mUi");
        // путь к файлу из build.prop
        EXTERNAL_SD = Utils.getModuleSdCard();
        Log.d(TAG,EXTERNAL_SD+" "+Environment.getExternalStorageState(new File(EXTERNAL_SD)));
      	if (Environment.getExternalStorageState(new File(EXTERNAL_SD)).equals(Environment.MEDIA_MOUNTED))
      	{
          // чтение настроек
          readSettings();
      	}
        createReceivers();
        createMediaReceiver();
      }
    };
    
    // MainActivity.onDestroy()
    XC_MethodHook onDestroyActivity = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"onDestroyActivity");
        radioActivity.unregisterReceiver(mediaReceiver);
        radioActivity.unregisterReceiver(radioReceiver);
        radioActivity = null;
      }
    };
    
    // begin hooks
    if (!lpparam.packageName.equals(PACKAGE_NAME)) return;
    Log.d(TAG,PACKAGE_NAME);
    Utils.setTag(TAG);
    Utils.readXposedMap();
    // RadioService
    Utils.findAndHookMethodCatch("com.microntek.radio.RadioService", lpparam.classLoader, "onCreate", onCreateService);
    Utils.findAndHookMethodCatch("com.microntek.radio.RadioService", lpparam.classLoader, "onDestroy", onDestroyService);
    // RadioActivity
    Utils.findAndHookMethodCatch("com.microntek.radio.MainActivity", lpparam.classLoader, "onCreate", Bundle.class, onCreateActivity);
    Utils.findAndHookMethodCatch("com.microntek.radio.MainActivity", lpparam.classLoader, "onDestroy", onDestroyActivity);
    Log.d(TAG,PACKAGE_NAME+" hook OK");
  }
  
  // чтение настроек
  private static void readSettings()
  {
    Log.d(TAG,"read settings from "+EXTERNAL_SD+INIFILE_NAME);
    props.clear();
    try
    {
      props.loadFromFile(EXTERNAL_SD+INIFILE_NAME);
    }
    catch (Exception e)
    {
      Log.e(TAG,e.getMessage());
    }
    // настройки
    toastEnable = props.getBoolValue(SETTINGS_SECTION, "toast", false);
    Log.d(TAG,"toast="+toastEnable);
    toastSize = props.getIntValue(SETTINGS_SECTION, "toast.size", 0);
    Log.d(TAG,"toast.size="+toastSize);
    namesEnable = props.getBoolValue(SETTINGS_SECTION, "names", false);
    Log.d(TAG,"names="+namesEnable);
    widgetEnable = props.getBoolValue(SETTINGS_SECTION, "widget", false);
    Log.d(TAG,"widget="+widgetEnable);
  }
  
  private static void createReceivers()
  {
    // радио
    IntentFilter ri = new IntentFilter();
    ri.addAction("com.microntek.radio.report");
    radioActivity.registerReceiver(radioReceiver, ri);
    Log.d(TAG,"radio state receiver created");
    // play pause
    IntentFilter wi = new IntentFilter();
    wi.addAction("hct.music.playpause");
    radioActivity.registerReceiver(playpauseReceiver, wi);
    Log.d(TAG,"play pause receiver created");
  }
  
  // имя текущей станции
  private static String getCurrentStationName()
  {
    int mBand = Utils.getIntField(radioService, "mBand");
    int mChannel = Utils.getIntField(radioService, "mChannel");
    int mFreq = Utils.getIntField(radioService, "mFreq");
    // int[][] freq = (int[][])Utils.getObjectField(radioService, "freq");
    Log.d(TAG,"mBand="+mBand);
    Log.d(TAG,"mFreq="+mFreq);
    String stationName = getStationName(mChannel, mBand, mFreq);
    Log.d(TAG,"stationName="+stationName);
    return stationName;
  }
  
  // TODO: имя станции по координатам
  private static String getStationName(int mChannel, int mBand, int mFreq)
  {
    String result = "";
    if (mChannel >= 0)
    {
      String[] freqText = (String[])Utils.getObjectField(radioService, "freqText");
      int[][] freq = (int[][])Utils.getObjectField(radioService, "freq");
      if (freqText != null && freq != null)
      {
        int mIndex = mBand*(freq.length-1)+mChannel;
        if ((freqText != null) && (mIndex >= 0) && (mIndex < freqText.length) && (freq[mBand][mChannel] == mFreq))
          result = freqText[mIndex];
      }
    }
    return result;
  }
  
  // посылка информации на виджет и скринсейвер
  protected static void sendToWidget()
  {
    radioState = cm.getStringState("av_channel").equals("fm");
    Log.d(TAG,"sendToWidget: "+radioState);
    if (widgetEnable && radioState)
    {
      // com.microntek.radio.report
      Intent intent = new Intent("com.microntek.radio.report");
      intent.putExtra("type","freq.name");
      intent.putExtra("name", stationName);
      radioService.sendBroadcast(intent);
    }
  }
  
  // показать уведомление о смене станции
  private static void showToast(String text)
  {
    Log.d(TAG,"showToast");
    Intent intent = new Intent("com.microntek.music.toast");
    intent.putExtra("toast.size", toastSize);
    intent.putExtra("class", PACKAGE_NAME);
    intent.putExtra("toast.text", text);
    radioService.sendBroadcast(intent);
  }
  
  // включить обработчик подключения носителей
  private void createMediaReceiver()
  {
    IntentFilter ui = new IntentFilter();
    ui.addAction(Intent.ACTION_MEDIA_MOUNTED);
    ui.addDataScheme("file");
    radioActivity.registerReceiver(mediaReceiver, ui);
    Log.d(TAG,"media mount receiver created");
  }
  
  // обработчик com.microntek.radio.report
  private static BroadcastReceiver radioReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      String type = intent.getStringExtra("type");
      if (type == null) return;
      if (type.equals("state"))
      {
        // не вызывается
        radioState = intent.getBooleanExtra("power", false);
        Log.d(TAG,"type=state, power="+radioState);
      }
      else if (type.equals("content"))
      {
        Log.d(TAG,"type=content");
        sendToWidget();
        String sFreq = "";
        int mFreq = Utils.getIntField(radioService, "mFreq");
        long freq = intent.getLongExtra("freq", 0) & 0x10000000000L;
        Log.d(TAG,"mFreq="+mFreq+" ("+freq+")");
        stationName = getCurrentStationName();
        Log.d(TAG,"stationName="+stationName);
        if (radioUi != null)
        {
          Log.d(TAG,"namesEnable="+namesEnable+", rdsDisable="+rdsDisable);
          // показываем вместо RDS наименование станции
          if (namesEnable && rdsDisable) Utils.callMethod(radioUi, "showRadioPty", stationName);
          // форматируем частоту встроенной функцией
          sFreq = (String)Utils.callMethod(radioUi, "getFreqString", mFreq);
        }
        // показ тоста
        if (toastEnable) showToast(sFreq+" "+stationName);
      }
    }
  };
  
 // обработчик hct.music.playpause
 private static BroadcastReceiver playpauseReceiver = new BroadcastReceiver()
 {
   public void onReceive(Context context, Intent intent)
   {
     radioState = cm.getStringState("av_channel").equals("fm");
     if (radioState)
     {
       // радио работает, включим или выключим mute
       if (cm.getParameters("av_mute=").equals("true"))
         cm.setParameters("av_mute=false");
       else
         cm.setParameters("av_mute=true");
     }
   }
 };
  
  // обработчик MEDIA_MOUNT
  private static BroadcastReceiver mediaReceiver = new BroadcastReceiver()
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
        readSettings();
      }
    }
  };
  
}
