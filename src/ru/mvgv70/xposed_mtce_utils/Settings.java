package ru.mvgv70.xposed_mtce_utils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import android.os.Bundle;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import ru.mvgv70.utils.IniFile;
import ru.mvgv70.utils.Utils;

public class Settings implements IXposedHookLoadPackage 
{
  private static final String TAG = "xposed-mtce-utils-settings";
  private static final String PACKAGE_NAME = "com.android.settings";
  private static String EXTERNAL_SD = "/storage/external_sd/";
  private static final String INIFILE_NAME = "mtce-utils/settings.ini";
  private final static String SETTINGS_SECTION = "mtce.settings";
  //
  private static IniFile props = new IniFile();
  private static String obdDevicesName = null;
  private static List<String> obdDevicesList;
  
  @Override
  public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable 
  {
    
    // SettingsActivity.onCreate(Bundle)
    XC_MethodHook onCreate = new XC_MethodHook() {
        
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"onCreate");
        // путь к файлу из build.prop
        EXTERNAL_SD = Utils.getModuleSdCard();
        // чтение настроек
        readSettings();
      }
    };
    
    // BluetoothSettings.isOBDDevice(String)
    XC_MethodHook isOBDDevice = new XC_MethodHook() {
        
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        String deviceName = (String)param.args[0];
        if (obdDevice(deviceName)) param.setResult(true);
      }
    };
    
    // start hooks
    if (!lpparam.packageName.equals(PACKAGE_NAME)) return;
    Log.d(TAG,PACKAGE_NAME);
    Utils.setTag(TAG);
    Utils.readXposedMap();
    Utils.findAndHookMethodCatch("com.android.settings.SettingsActivity", lpparam.classLoader, "onCreate", Bundle.class, onCreate);
    Utils.findAndHookMethodCatch("com.android.settings.hct.BluetoothSettings", lpparam.classLoader, "isOBDDevice", String.class, isOBDDevice);
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
    // настройки
    obdDevicesName = props.getValue(SETTINGS_SECTION, "obd_device").toUpperCase(Locale.US);
    Log.d(TAG,"obd_device="+obdDevicesName);
    if (!obdDevicesName.isEmpty())
    {
      obdDevicesList = Arrays.asList(obdDevicesName.split("\\s*,\\s*"));
      Log.d(TAG,"obd_device.count="+obdDevicesList.size());
    }
  }
  
  private boolean obdDevice(String deviceName)
  {
    if (obdDevicesList == null) return false;
    if (deviceName == null) return false;
    for (String name : obdDevicesList)
      if (deviceName.toUpperCase(Locale.US).contains(name)) return true;
    return false;
  }

}
