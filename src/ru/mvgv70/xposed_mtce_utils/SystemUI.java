package ru.mvgv70.xposed_mtce_utils;

import java.io.File;
import java.util.ArrayList;

import ru.mvgv70.utils.IniFile;
import ru.mvgv70.utils.Utils;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.VolumeInfo;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.Iterator;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class SystemUI implements IXposedHookLoadPackage 
{
  private static final String TAG = "xposed-mtce-utils-systemui";
  private static final String PACKAGE_NAME = "com.android.systemui";
  private static String EXTERNAL_SD = "/storage/external_sd/";
  private static final String INI_FILE_NAME = "mtce-utils/settings.ini";
  private static final String HIDE_STATUS_SECTION = "systemui.icons.status";
  private static final String SETTINGS_SECTION = "systemui.settings";
  private static Context mContext = null;
  private static View mStatusBarView = null;
  private static IniFile props = new IniFile();
  private static boolean noMobileIndicators = false;
  private static boolean noUsbNotification = false;
  private static ArrayList<View> hideStatusList = new ArrayList<View>();
  private static DiskHandler handler;
  private static int mountNotificationId = 0;
  
  @Override
  public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable 
  {
	  
    // constructor PhoneStatusBarView
    XC_MethodHook PhoneStatusBarViewCreate = new XC_MethodHook() {
        
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"PhoneStatusBarView create");
        mStatusBarView = (View)param.thisObject;
        mContext = mStatusBarView.getContext();
        // notification id
        mountNotificationId = Utils.getIntSettings("systemui", "mount_notification_id", 1397773634);
        Log.d(TAG,"mount_notification_id="+mountNotificationId);
        // путь к файлу из build.prop
        EXTERNAL_SD = Utils.getModuleSdCard();
        Log.d(TAG,EXTERNAL_SD+" "+Environment.getExternalStorageState(new File(EXTERNAL_SD)));
        handler = new DiskHandler();
        // чтение настроечного файла
      	if (Environment.getExternalStorageState(new File(EXTERNAL_SD)).equals(Environment.MEDIA_MOUNTED))
        {
      	  // читаем настройки
          readSettings();
          hideStatusBarViews();
        }
        else
          // прочитаем настройки при подключении external_sd
          createMediaReceiver();
      }
    };
    
    // SignalClusterView.setMobileDataIndicators()
    XC_MethodHook setMobileDataIndicators = new XC_MethodHook() {
        
      @Override
      protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"setMobileDataIndicators");
        if (noMobileIndicators) param.setResult(null);
      }
    };
    
    // StorageNotification.onVolumeMounted(VolumeInfo,CharSequence,CharSequence)
    XC_MethodHook onVolumeMounted = new XC_MethodHook() {
        
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"onVolumeMounted");
        VolumeInfo info = (VolumeInfo)param.args[0];
        if (info != null) Log.d(TAG,"id="+info.getId());
        // если установлен признак
        if (noUsbNotification == false) return;
        // закрываем уведомление через 10 секунд
        if (handler != null)
        {
          if (mountNotificationId > 0)
          {
            Message msg = new Message();
            msg.what = 1;
            msg.arg1 = mountNotificationId;
            msg.obj = info.getId();
            handler.sendMessageDelayed(msg, 10*1000);
          }
          else
            Log.w(TAG,"mountNotificationId == 0");
        }
        else
          Log.w(TAG,"handler == null");
      }
    };
    
    // start hooks
    if (!lpparam.packageName.equals(PACKAGE_NAME)) return;
    Log.d(TAG, PACKAGE_NAME);
    Utils.setTag(TAG);
    Utils.readXposedMap();
    Utils.findAndHookConstructorCatch("com.android.systemui.statusbar.phone.PhoneStatusBarView", lpparam.classLoader, Context.class, AttributeSet.class, PhoneStatusBarViewCreate);
    Utils.findAndHookAllMethodsCatch("com.android.systemui.statusbar.SignalClusterView", lpparam.classLoader, "setMobileDataIndicators", setMobileDataIndicators);
    Utils.findAndHookMethodCatch("com.android.systemui.usb.StorageNotification", lpparam.classLoader, "onVolumeMounted", VolumeInfo.class, onVolumeMounted);
    Log.d(TAG, PACKAGE_NAME+" hook OK");
  }
  
  private void readSettings()
  {
    hideStatusList.clear();
    // читаем настроечный файл
    Log.d(TAG,"read settings from "+EXTERNAL_SD+INI_FILE_NAME);
    try
    {
      props.loadFromFile(EXTERNAL_SD+INI_FILE_NAME);
    }
    catch (Exception e)
    {
      Log.e(TAG,e.getMessage());
    }
    // список скрываемых иконок статусбара: id
    readList(hideStatusList, mStatusBarView, HIDE_STATUS_SECTION, "StatusList");
    // уровень сигнала мобильной сети
    noMobileIndicators = props.getBoolValue(SETTINGS_SECTION, "no_mobile_indicators", false);
    Log.d(TAG,"no_mobile_indicators="+noMobileIndicators);
    noUsbNotification = props.getBoolValue(SETTINGS_SECTION, "no_usb_notification", false);
    Log.d(TAG,"no_usb_notification="+noUsbNotification);
  }
  
  // чтение списка иконок статус бара
  private void readList(ArrayList<View> list, View parentView, String section, String listName)
  {
    Log.d(TAG,listName+" read from ["+section+"]");
    list.clear();
    // читаем строки в разделе 
    int id;
    String line;
    Resources res = mContext.getResources(); 
    Iterator<String> names = props.enumKeys(section);
    while (names.hasNext()) 
    {
      line = names.next();
      if (props.getBoolValue(section, line, true) == false)
      {
        id = res.getIdentifier(line, "id", mContext.getPackageName());
        if (id > 0)
        {
          View view = parentView.findViewById(id);
          if (view != null)
          {
            list.add(view);
            Log.d(TAG,"hide "+id);
          }
          else
            Log.w(TAG,"view "+line+" ("+id+") not found");
        }
        else
          Log.w(TAG,"id "+line+" not found");
      }
    }
    Log.d(TAG,listName+".size="+list.size());
  }
  
  // пр€чем кнопки в статус-бара
  private void hideStatusBarViews()
  {
    for (View view : hideStatusList)
    {
      Log.d(TAG,"hide "+view.getId());
      view.setVisibility(View.GONE);
    }
  }
   
  // включить обработчик подключени€ носителей
  private void createMediaReceiver()
  {
    IntentFilter ui = new IntentFilter();
    ui.addAction(Intent.ACTION_MEDIA_MOUNTED);
    ui.addDataScheme("file");
    mContext.registerReceiver(mediaReceiver, ui);
    Log.d(TAG,"media mount receiver created");
  }
    
  // обработчик MEDIA_MOUNT
  private BroadcastReceiver mediaReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      String action = intent.getAction(); 
      String drivePath = intent.getData().getPath();
      Log.d(TAG,"media receiver "+drivePath+" "+action);
      if (action.equals(Intent.ACTION_MEDIA_MOUNTED))
      {
        // если подключаетс€ external_sd
        if (action.equals(Intent.ACTION_MEDIA_MOUNTED) && Utils.isExternalCard(drivePath, EXTERNAL_SD))
        {
          // читаем настройки
          readSettings();
          hideStatusBarViews();
        }
      }
    }
  };

  // закрытие уведомлений с задержкой
  private static class DiskHandler extends Handler
  {
    public void handleMessage(Message msg)
    {
      int id = msg.arg1;
      String tag = (String)msg.obj;
      NotificationManager nm = (NotificationManager)mContext.getSystemService(NotificationManager.class);
      nm.cancel(tag, id);
      Log.d(TAG,"notification "+id+" ("+tag+") cleared");
    }
  }; 
  
}
