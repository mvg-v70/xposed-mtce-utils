package ru.mvgv70.xposed_mtce_utils;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.RemoteViews;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import ru.mvgv70.utils.IniFile;
import ru.mvgv70.utils.Utils;

public class Widget implements IXposedHookLoadPackage {
	
  private static final String TAG = "xposed-mtce-utils-widget";
  private static final String PACKAGE_NAME = "com.microntek.hctwidget";
  private static String EXTERNAL_SD = "/storage/external_sd/";
  private static final String INIFILE_NAME = "mtce-utils/settings.ini";
  private static ClassLoader loader;
  // настройки
  private static IniFile props = new IniFile();
  private static final String WIDGET_SECTION = "widget.settings";
  private static boolean radioEnabled = false;
  private static boolean runActivePlayer = false;
  private static String dateFormat = "";
  private static boolean runNaviOnTimeWidget = false;
  // разметка
  private static int radio_layout_id = 0;
  private static int radio_name_id = 0;
  private static int music_layout_id = 0;
  private static int music_icon_id = 0;
  private static int time_layout_id = 0;
  private static int time_week_id = 0;
  private static int time_data_id = 0;

  @Override
  public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable 
  {
    // RadioWidget.onReceive(Context, Intent)
    XC_MethodHook onRadioReceive = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Context context = (Context)param.args[0];
        Intent intent = (Intent)param.args[1];
        String type = intent.getStringExtra("type");
        if ((type != null) && (type.equals("freq.name")))
        {
          String name = intent.getStringExtra("name");
          Log.d(TAG,"onRadioReceive: name="+name);
          if (radioEnabled && (radio_name_id > 0))
          {
            RemoteViews widgetView = new RemoteViews(context.getPackageName(), radio_layout_id);
            widgetView.setTextViewText(radio_name_id, name);
            // update widget
            AppWidgetManager wm = AppWidgetManager.getInstance(context);
            ComponentName cn = new ComponentName(context, param.thisObject.getClass());
            int[] ids = wm.getAppWidgetIds(cn);
            wm.partiallyUpdateAppWidget(ids, widgetView);
            Log.d(TAG,"update radio widget ok");
          }
        }
      }
    };
    
    // RadioWidget.updateRadio()
    XC_MethodHook updateRadio = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"updateRadio");
      }
    };
    
    // MusicWidget.onReceive(Context, Intent)
    XC_MethodHook onMusicReceive = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Context context = (Context)param.args[0];
        Intent intent = (Intent)param.args[1];
        if (intent.getAction().equals("com.microntek.music.report")) 
        {
          String type = intent.getStringExtra("type");
          String className = intent.getStringExtra("class");
          Log.d(TAG,"onMusicReceive: "+intent.getAction()+" type="+type+", class="+className);
          if ((type != null) && (type.equals("music.albumart")))
          {
            Bitmap bitmap = (Bitmap)intent.getParcelableExtra("value");
            RemoteViews widgetView = new RemoteViews(context.getPackageName(), music_layout_id);
            if (bitmap != null)
              bitmap = (Bitmap)Utils.callStaticMethod("com.microntek.Util", loader, "getCroppedBitmap", bitmap);
            widgetView.setImageViewBitmap(music_icon_id, bitmap);
            // update widget
            AppWidgetManager wm = AppWidgetManager.getInstance(context);
            ComponentName cn = new ComponentName(context, param.thisObject.getClass());
            int[] ids = wm.getAppWidgetIds(cn);
            wm.partiallyUpdateAppWidget(ids, widgetView);
            Log.d(TAG,"update music widget ok");
          }
        }
      }
    };
    
    // MusicWidget.updateMusic(Context, AppWidgetManager, int)
    XC_MethodHook updateMusic = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"updateMusic");
        Context context = (Context)param.args[0];
        AppWidgetManager wm = (AppWidgetManager)param.args[1];
        int widget_id = (int)param.args[2];	
        // заменим событие на нажатие на иконку
        if (runActivePlayer && music_layout_id > 0 && music_icon_id > 0) 
        {
          RemoteViews widgetView = new RemoteViews(context.getPackageName(), music_layout_id);
          // update widget
          widgetView.setOnClickPendingIntent(music_icon_id, PendingIntent.getBroadcast(context, 0, new Intent("ru.mvgv70.mtceutils.runactiveplayer"), 0));
          wm.partiallyUpdateAppWidget(widget_id, widgetView);
          Log.d(TAG,"update music widget ok");
        }
      }
    };
        
    // RadioWidget.onEnabled(Context)
    XC_MethodHook onEnabledRadio = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"onEnabledRadio");
        Context context = (Context)param.args[0];
        // путь к файлу из build.prop
        EXTERNAL_SD = Utils.getModuleSdCard();
        // чтение настроек
        readRadioSettings(context);
      }
    };
    
    // MusicWidget.onEnabled(Context)
    XC_MethodHook onEnabledMusic = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"onEnabledMusic");
        Context context = (Context)param.args[0];
        // путь к файлу из build.prop
        EXTERNAL_SD = Utils.getModuleSdCard();
        // чтение настроек
        readMusicSettings(context);
      }
    };
    
    // TimeWidget.onEnabled(Context)
    XC_MethodHook onEnabledTime = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"onEnabledTime");
        Context context = (Context)param.args[0];
        // путь к файлу из build.prop
        EXTERNAL_SD = Utils.getModuleSdCard();
        // чтение настроек
        readTimeSettings(context);
      }
    };
	
    // TimeWidget.updateTime(Context, AppWidgetManager, int)
    XC_MethodHook updateTime = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"updateTime");
        Context context = (Context)param.args[0];
        AppWidgetManager wm = (AppWidgetManager)param.args[1];
        int widget_id = (int)param.args[2];	
        // заменим событие на нажатие на иконку
        if (runNaviOnTimeWidget && time_layout_id > 0) 
        {
          RemoteViews widgetView = new RemoteViews(context.getPackageName(), time_layout_id);
          // update widget
          if (time_week_id > 0)
            widgetView.setOnClickPendingIntent(time_week_id, PendingIntent.getBroadcast(context, 0, new Intent("ru.mvgv70.mtceutils.runnavi"), 0));
          if (time_data_id > 0)
            widgetView.setOnClickPendingIntent(time_data_id, PendingIntent.getBroadcast(context, 0, new Intent("ru.mvgv70.mtceutils.runnavi"), 0));
          wm.partiallyUpdateAppWidget(widget_id, widgetView);
          Log.d(TAG,"update time widget ok");
        }
      }
    };
    
    // SimpleDateFormat(String)
    XC_MethodHook SimpleDateFormatCreate = new XC_MethodHook() {
      
      @Override
      protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        // меняем формат
        if (!dateFormat.isEmpty()) param.args[0] = dateFormat;
      }
      
    };

    // begin hooks
    if (!lpparam.packageName.equals(PACKAGE_NAME)) return;
    Log.d(TAG,PACKAGE_NAME);
    loader = lpparam.classLoader;
    Utils.setTag(TAG);
    Utils.readXposedMap();
    Utils.findAndHookMethodCatch("com.microntek.hctwidget.RadioWidget", lpparam.classLoader, "onEnabled", Context.class, onEnabledRadio);
    Utils.findAndHookMethodCatch("com.microntek.hctwidget.RadioWidget", lpparam.classLoader, "onReceive", Context.class, Intent.class, onRadioReceive);
    Utils.findAndHookMethodCatch("com.microntek.hctwidget.RadioWidget", lpparam.classLoader, "updateRadio", Context.class, AppWidgetManager.class, int.class, updateRadio);
    Utils.findAndHookMethodCatch("com.microntek.hctwidget.MusicWidget", lpparam.classLoader, "onEnabled", Context.class, onEnabledMusic);
    Utils.findAndHookMethodCatch("com.microntek.hctwidget.MusicWidget", lpparam.classLoader, "onReceive", Context.class, Intent.class, onMusicReceive);
    Utils.findAndHookMethodCatch("com.microntek.hctwidget.MusicWidget", lpparam.classLoader, "updateMusic", Context.class, AppWidgetManager.class, int.class, updateMusic);
    Utils.findAndHookMethodCatch("com.microntek.hctwidget.TimeWidget", lpparam.classLoader, "onEnabled", Context.class, onEnabledTime);
	Utils.findAndHookMethodCatch("com.microntek.hctwidget.TimeWidget", lpparam.classLoader, "updateTime", Context.class, AppWidgetManager.class, int.class, updateTime);
    Utils.findAndHookConstructorCatch("java.text.SimpleDateFormat", lpparam.classLoader, String.class, SimpleDateFormatCreate);
    Log.d(TAG,PACKAGE_NAME+" hook OK");
  }
  
  // чтение настроек радио
  private void readRadioSettings(Context context)
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
    radioEnabled = props.getBoolValue(WIDGET_SECTION, "radio", false);
    Log.d(TAG,"radio="+radioEnabled);
    String radio_name = props.getValue(WIDGET_SECTION, "radio.name");
    Log.d(TAG,"radio.name="+radio_name);
    // id радио по имени
    Resources res = context.getResources();
    radio_layout_id = res.getIdentifier("radio", "layout", context.getPackageName());
    Log.d(TAG,"radio_layout_id="+radio_layout_id);
    radio_name_id = res.getIdentifier(radio_name, "id", context.getPackageName());
    Log.d(TAG,"radio_name_id="+radio_name_id);
  }
  
  // чтение настроек музыки
  private void readMusicSettings(Context context)
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
    runActivePlayer = props.getBoolValue(WIDGET_SECTION, "runactiveplayer", false);
    Log.d(TAG,"runactiveplayer="+runActivePlayer);
    // id музыки по имени
    Resources res = context.getResources();
    music_layout_id = res.getIdentifier("music", "layout", context.getPackageName());
    Log.d(TAG,"music_layout_id="+music_layout_id);
    music_icon_id = res.getIdentifier("musicalumb", "id", context.getPackageName());
    Log.d(TAG,"music_icon_id="+music_icon_id);
  }
  
   // чтение настроек времени
  private void readTimeSettings(Context context)
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
    dateFormat = props.getValue(WIDGET_SECTION, "date.format");
    Log.d(TAG,"date.format="+dateFormat);
	runNaviOnTimeWidget = props.getBoolValue(WIDGET_SECTION, "date.navi", false);
    Log.d(TAG,"date.navi="+runNaviOnTimeWidget);
	// id 
    Resources res = context.getResources();
    time_layout_id = res.getIdentifier("time", "layout", context.getPackageName());
    Log.d(TAG,"time_layout_id="+time_layout_id);
	time_week_id = res.getIdentifier("week", "id", context.getPackageName());
    Log.d(TAG,"time_week_id="+time_week_id);
	time_data_id = res.getIdentifier("yearmonthdata", "id", context.getPackageName());
    Log.d(TAG,"time_data_id="+time_data_id);
  }

}
