package ru.mvgv70.xposed_mtce_utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ru.mvgv70.utils.IniFile;
import ru.mvgv70.utils.Utils;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.microntek.CarManager;
import android.microntek.HCTApi;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Keys implements IXposedHookLoadPackage {
	
  private static final String TAG = "xposed-mtce-utils-keys";
  private static final String MANAGER_CLASS = "android.microntek.service";
  private static String packageName = null;
  private String EXTERNAL_SD = "/storage/external_sd/";
  private static final String INIFILE_NAME = "mtce-utils/settings.ini";
  private boolean receiver_created = false;
  // секции
  private IniFile props = null;
  private static final String KEYS_SECTION = "#keys";
  // обработчики нажатий
  private Context mContext = null;
  private ActivityManager acm = null;
  private CarManager cm = null;
  private ArrayList<Handler> keyHandlerList = new ArrayList<Handler>();
  
  @Override
  public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable 
  {
    // CarManager.attach(Handler)
    XC_MethodHook attach = new XC_MethodHook() {
      
      @Override
      protected void beforeHookedMethod(MethodHookParam param) throws Throwable 
      {
        Handler handler = (Handler)param.args[0];
        String type = (String)param.args[1];
        String handlerClass = handler.getClass().getPackage().getName();
        Log.d(TAG,packageName+" attach ["+type+"] "+handlerClass);
        if (type.contains("KeyDown") && handlerClass.equals(packageName))
        {
          Log.d(TAG,"hook attach");
          // создаем новый обработчик
          KeyHandler keyHandler = new KeyHandler(handler, packageName, Keys.this);
          keyHandlerList.add(keyHandler);
          // замен€ем параметр
          param.args[0] = keyHandler;
        }
      }

    };
    
    // CarManager.detach(Handler)
    XC_MethodHook detach = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable 
      {
        Log.d(TAG,packageName+" detach");
        // очищаем массив handler'ов
        for (int i=0; i < keyHandlerList.size(); i++) keyHandlerList.set(i, null);
        keyHandlerList.clear();
      }
    };
    
    // Application.onCreate()
    XC_MethodHook onCreateApp = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable 
      {
        Log.d(TAG,packageName+" onCreateApp");
        cm = new CarManager();
        mContext = (Context)param.thisObject;
        acm = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
        // путь к файлу из build.prop
        EXTERNAL_SD = Utils.getModuleSdCard();
        Log.d(TAG,"receiver_created="+receiver_created);
        if (Environment.getExternalStorageState(new File(EXTERNAL_SD)).equals(Environment.MEDIA_MOUNTED))
          readSettings();
        if (!receiver_created) createMediaReceiver();
      }
    };    

    // begin hooks
    if (!lpparam.packageName.contains(".microntek.")) return;
    Log.d(TAG, lpparam.packageName);
    Utils.setTag(TAG);
    Utils.readXposedMap();
    if (packageName == null) packageName = lpparam.packageName;
    Utils.findAndHookMethodCatch("android.microntek.CarManager", lpparam.classLoader, "attach", Handler.class, String.class, attach);
    Utils.findAndHookMethodCatch("android.microntek.CarManager", lpparam.classLoader, "detach", detach);
    Utils.findAndHookMethodCatch("android.app.Application", lpparam.classLoader, "onCreate", onCreateApp);
    Log.d(TAG,packageName+" hook OK");
  }
  
  // чтение настроек
  private void readSettings()
  {
    props = new IniFile();
    try
    {
      Log.d(TAG,packageName+": read settings from "+EXTERNAL_SD+INIFILE_NAME);
      props.clear();
      props.loadFromFile(EXTERNAL_SD+INIFILE_NAME);
    }
    catch (Exception e)
    {
      Log.e(TAG,e.getMessage());
    }
  }
  
  // поиск строкового параметра кнопки
  private String getStringKey(String pkgName, String key)
  {
    String result = props.getValue(pkgName+KEYS_SECTION, key);
    return result;
  }
  
  // поиск целочисленного параметра кнопки
  private int getIntKey(String pkgName, String key, int defValue)
  {
    int result = props.getIntValue(pkgName+KEYS_SECTION, key, defValue);
    return result;
  }
  
  // обработчик нажати€ клавиши
  // event_xx
  public int replaceKey(String pkgName, int keyCode)
  {
    keyCode = getIntKey(pkgName, "event_"+keyCode, keyCode);
    return keyCode;
  }
  
  // обработчик нажати€ клавиши
  // app_xx / keycode_xx
  public boolean processKey(String pkgName, int keyCode)
  {
    boolean result = false;
    // app
    String app = getStringKey(pkgName, "app_"+keyCode);
    if (!TextUtils.isEmpty(app))
      Log.d(TAG,pkgName+" app_"+keyCode+"="+app);
    // intent
    String intent = getStringKey(pkgName, "intent_"+keyCode);
    if (!TextUtils.isEmpty(intent))
      Log.d(TAG,pkgName+" intent_"+keyCode+"="+intent);
    // activity
    String activity = getStringKey(pkgName, "activity_"+keyCode);
    if (!TextUtils.isEmpty(activity))
      Log.d(TAG,pkgName+" activity_"+keyCode+"="+activity);
    // android code
    int code_android = getIntKey(pkgName, "keycode_"+keyCode, 0);
    if (code_android > 0)
      Log.d(TAG,pkgName+" keycode_"+keyCode+"="+code_android);
    // mtce code
    int code_mtce = getIntKey(pkgName, "keymtc_"+keyCode, 0);
    if (code_mtce > 0)
      Log.d(TAG,pkgName+" keymtc_"+keyCode+"="+code_mtce);
    if (!app.isEmpty())
    {
      runApp(app);
      result = true;
    }
    else if (code_android > 0)
    {
      emulateKey(code_android);
      result = true;
    }
    else if (code_mtce > 0)
    {
      mtceKey(code_mtce);
      result = true;
    }
    else if (!activity.isEmpty())
    {
      Log.d(TAG,"run activity");
      runActivity(activity);
      result = true;
    }
    else if (!intent.isEmpty())
    {
      sendIntent(intent);
      result = true;
    }
    return result;
  }
  
  // запуск приложени€
  private void runApp(String appName)
  {
    // активные приложени€
    @SuppressWarnings("deprecation")
    List<ActivityManager.RunningTaskInfo> taskList = acm.getRunningTasks(2);
    if (appName.equals(taskList.get(0).topActivity.getPackageName()) || appName.isEmpty())
      // запускаем предыдущее приложение в списке
      appName = taskList.get(1).topActivity.getPackageName();
    Log.d(TAG,"run app="+appName);
    Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(appName);
    if (intent != null)
    {
      intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      mContext.startActivity(intent);
    }
    else
      Log.w(TAG,"no activity found for "+appName);
  }
  
  // запуск activity
  private void runActivity(String activity)
  {
    int i = activity.indexOf("/");
    if (i > 0)
    {
      String packageName = activity.substring(0,i);
      String className = activity.substring(i+1);
      Log.d(TAG,"start activity "+packageName+"/"+className);
      //
      Intent appIntent = new Intent();
      ComponentName cn = new ComponentName(packageName, className);
      try 
      {
        mContext.getPackageManager().getActivityInfo(cn, PackageManager.GET_META_DATA);
        appIntent.setComponent(cn);
        appIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(appIntent);
      } 
      catch (Exception e) 
      {
        Log.w(TAG,"activity "+activity+" not found");
      }
    }
    else
      Log.w(TAG,"wrong format for activity: "+activity);
  }

  // отправка намерени€
  private void sendIntent(String intentName)
  {
    Log.d(TAG,"intent "+intentName);
    mContext.sendBroadcast(new Intent(intentName));
  }
  
  // эмул€ци€ нажати€ андроид-кнопки
  private void emulateKey(int code)
  {
    Log.d(TAG,"emulate android key="+code);
    HCTApi.SystemKey(code, 0);
  }
  
  // эмул€ци€ нажати€ mtce-кнопки
  private void mtceKey(int code)
  {
    Log.d(TAG,"emulate mtc key="+code);
    cm.setParameters("ctl_key="+code);
  }
  
  // обработчик нажатий
  @SuppressLint("HandlerLeak")
  private class KeyHandler extends Handler
  {
    private Handler origHandler;
    private String packageName;
    private Keys owner;
    
    KeyHandler(Handler handler, String pkgName, Keys caller)
    {
      super();
      origHandler = handler;
      packageName = pkgName;
      owner = caller;
    }
    
    public void handleMessage(Message msg)
    {
      String event = (String)msg.obj;
      Bundle data = msg.getData();
      if (event.equals("KeyDown") && (props != null))
      {
        int keyCode = data.getInt("value");
        if (Keys.packageName.equals(MANAGER_CLASS))
          Log.d(TAG,"Handler: key="+keyCode);
        // замена кода
        int newCode = owner.replaceKey(packageName, keyCode);
        if (newCode != keyCode)
        {
          Log.d(TAG,"change event to "+newCode);
          // мен€ем код клавиши в штатном обработчике
          data.putInt("value", newCode);
        }
        else
          // если кнопка обработана не запускаем штатный обработчик
          if (owner.processKey(packageName, keyCode)) return;
      }
      origHandler.dispatchMessage(msg);
    }
  };
  
  // включить обработчик подключени€ носителей
  private void createMediaReceiver()
  {
    IntentFilter ui = new IntentFilter();
    ui.addAction(Intent.ACTION_MEDIA_MOUNTED);
    ui.addDataScheme("file");
    mContext.registerReceiver(mediaReceiver, ui);
    Log.d(TAG,"media mount receiver created");
    receiver_created = true;
  }

  // обработчик MEDIA_MOUNT
  private BroadcastReceiver mediaReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      String action = intent.getAction(); 
      String drivePath = intent.getData().getPath();
      Log.d(TAG,"media receiver: "+drivePath+" "+action);
      // если подключаетс€ external_sd
      if (action.equals(Intent.ACTION_MEDIA_MOUNTED) && Utils.isExternalCard(drivePath, EXTERNAL_SD))
      {
        // читаем настройки
        readSettings();
      }
    }
  };
  
}
