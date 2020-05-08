package ru.mvgv70.xposed_mtce_utils;

import ru.mvgv70.utils.IniFile;
import ru.mvgv70.utils.Utils;

import java.io.File;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class BtMusic implements IXposedHookLoadPackage 
{
  private static final String TAG = "xposed-mtce-utils-btmusic";
  private static final String PACKAGE_NAME = "com.microntek.btmusic";
  private static String EXTERNAL_SD = "/storage/external_sd/";
  private static final String INIFILE_NAME = "mtce-utils/settings.ini";
  // настройки
  private static final String SETTINGS_SECTION = "btmusic.settings";
  private static IniFile props = new IniFile();
  private static boolean toastEnable = false;
  private static int toastSize = 0;
  private static boolean storeTags = true;
  // теги 
  private static String title = "";
  private static String album = "";
  // переменные
  private static Context context = null;
    
  @Override
  public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable 
  {
	    
    // MainActivity.onCreate(Bundle)
    XC_MethodHook onCreate = new XC_MethodHook() {
        
      @SuppressLint("ShowToast")
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"onCreate");
        context = (Context)param.thisObject;
        EXTERNAL_SD = Utils.getModuleSdCard();
        Log.d(TAG,EXTERNAL_SD+" "+Environment.getExternalStorageState(new File(EXTERNAL_SD)));
        readSettings();
        createReceivers();
      }
    };
    
    // MainActivity.onDestroy()
    XC_MethodHook onDestroy = new XC_MethodHook() {
        
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"onDestroy");
        context.unregisterReceiver(btMusicReceiver);
      }
    };
    
    // start hooks
    if (!lpparam.packageName.equals(PACKAGE_NAME)) return;
    Log.d(TAG,PACKAGE_NAME);
    Utils.setTag(TAG);
    Utils.readXposedMap();
    Utils.findAndHookMethodCatch("com.microntek.btmusic.MainActivity", lpparam.classLoader, "onCreate", Bundle.class, onCreate);
    Utils.findAndHookMethodCatch("com.microntek.btmusic.MainActivity", lpparam.classLoader, "onDestroy", onDestroy);
    Log.d(TAG,PACKAGE_NAME+" hook OK");
  }
	
  // чтение настроек
  private void readSettings()
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
    // toast
    toastEnable = props.getBoolValue(SETTINGS_SECTION, "toast", false);
    Log.d(TAG,"toast="+toastEnable);
    toastSize = props.getIntValue(SETTINGS_SECTION, "toast.size", 0);
    Log.d(TAG,"toast.size="+toastSize);
    // tags
    storeTags = props.getBoolValue(SETTINGS_SECTION, "storetags", true);
    Log.d(TAG,"storetags="+storeTags);
  }
  
  // регистрация receiver
  private void createReceivers()
  {
    // bt music
    IntentFilter ti = new IntentFilter();
    ti.addAction("hct.btmusic.info");
    ti.addAction("com.microntek.btmusic.report");
    context.registerReceiver(btMusicReceiver, ti);
    Log.d(TAG,"bluetooth music receiver created");
  }
  
  // show toast
  private void showToast()
  {
    Log.d(TAG,"showToast");
    Intent intent = new Intent("com.microntek.music.toast");
    intent.putExtra("toast.size", toastSize);
    intent.putExtra("toast.format", "%album% %title%");
    intent.putExtra("class", PACKAGE_NAME);
    intent.putExtra(MediaStore.Audio.AudioColumns.TITLE, title);
    intent.putExtra(MediaStore.Audio.AudioColumns.ALBUM, album);
    context.sendBroadcast(intent);
  }
  
  // send tags
  private void sendTags()
  {
    Log.d(TAG,"sendTags");
    // title
    Intent ti = new Intent("com.microntek.btmusic.report");
    ti.putExtra("type", "music.title");
    ti.putExtra("value", title);
    ti.putExtra("class", PACKAGE_NAME);
    context.sendBroadcast(ti);
    // album
    Intent ai = new Intent("com.microntek.btmusic.report");
    ai.putExtra("type", "music.albunm");
    ai.putExtra("value", album);
    ti.putExtra("class", PACKAGE_NAME);
    context.sendBroadcast(ai);
  }
  
  // com.microntek.btmusic.report
  private BroadcastReceiver btMusicReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      String action = intent.getAction();
      if (intent.hasExtra("class")) return;
      Log.d(TAG,"bluetooth music receiver: "+action);
      if (action.equals("hct.btmusic.info") && storeTags)
      {
        // послать теги в скринсейвер
        sendTags();
      }
      else if (action.equals("com.microntek.btmusic.report"))
      {
        // сохраним теги, сначала получаем title, потом album
        String type = intent.getStringExtra("type");
        if (type.equals("music.title"))
        {
          album = "";
          title = intent.getStringExtra("value");
          Log.d(TAG,"title="+title);
        }
        else if (type.equals("music.albunm"))
        {
          album = intent.getStringExtra("value");
          Log.d(TAG,"album="+album);
          // show toast
          if (toastEnable) showToast();
        }
      }
    }
  };
  
}
