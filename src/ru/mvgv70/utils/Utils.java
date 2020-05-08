package ru.mvgv70.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.microntek.HCTApi;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.util.Log;

//
// version 1.4.8
//

public class Utils 
{
  private final static String INI_FILE_SD_BUILDPROP = "persist.sys.mvgv70.card";
  private final static String INI_FILE_SDNAME_BUILDPROP = "persist.sys.mvgv70.cardname";
  private final static String EXTERNAL_SD = "/storage/external_sd/";
  private final static String XPOSED_MAP_PATH = "/system/etc/mvgv70.xposed.map";
  private final static String CLASS_PARAM = ".class";
  private final static String LOG_FILE_NAME = "xposed-mtce.log";
  private static IniFile xposedMap = new IniFile();
  private static String TAG = "mvgv70-xposed";
	
  // TAG
  public static void setTag(String newTag)
  {
    TAG = newTag;
  }
  
  // ��������� �������� �� build.prop
  public static String getSystemProperty(String key) 
  {
    String value = null;
    try 
    {
      value = (String)Class.forName("android.os.SystemProperties").getMethod("get", String.class).invoke(null, key);
    } 
    catch (Exception e) 
    {
      Log.e(TAG,e.getMessage());
    }
    return value;
  }
  
  // sd-����� ��� ������ ����� ��������
  public static String getModuleSdCard()
  {
    String result = EXTERNAL_SD;
    // ������� � build.prop � ��������� �����������
    String value = getSystemProperty(INI_FILE_SD_BUILDPROP);
    // ������� � mvgv70.xposed.map
    if (TextUtils.isEmpty(value)) value = getStringSettings("main", INI_FILE_SD_BUILDPROP, null);
    Log.i(TAG,INI_FILE_SD_BUILDPROP+"="+value);
    // ���� �������� ������
    if (!TextUtils.isEmpty(value)) result = value;
    if (!result.endsWith("/")) result = result.concat("/");
    Log.i(TAG,"EXTERNAL_SD="+result);
    return result;
  }
  
  // ��� sd-����� ��� ������ ����� ��������
  public static String getModuleSdCardName()
  {
    String result = "";
    // ������� � build.prop � ��������� �����������
    String value = getSystemProperty(INI_FILE_SDNAME_BUILDPROP);
    // ������� � mvgv70.xposed.map
    if (TextUtils.isEmpty(value)) value = getStringSettings("main", INI_FILE_SDNAME_BUILDPROP, null);
    Log.i(TAG,INI_FILE_SDNAME_BUILDPROP+"="+value);
    // ���� �������� ������
    if (!TextUtils.isEmpty(value)) result = value;
    Log.i(TAG,"EXTERNAL_SD_NAME="+result);
    return result;
  }
  
  // sd-����� ��� ������ ����� �������� � ������ ������ ��������� cardname
  public static String getModuleSdCardEx(Context context)
  {
    String result = "";
    String cardName = getModuleSdCardName();
    if (cardName != null)
    {
      result = getVolumePath(context, cardName);
      Log.i(TAG,"EXTERNAL_SD="+result);
    }
    else
      result = getModuleSdCard();
    return result;
  }
  
  // �������� ������������ �����
  public static boolean isExternalCard(String drivePath, String External_Sd)
  {
    return External_Sd.startsWith(drivePath);
  }
  
  // ��������� ���� � ����� �� �����
  private static String getVolumePath(Context context, String deviceName)
  {
    StorageManager sm = (StorageManager)context.getSystemService(StorageManager.class);
    @SuppressWarnings("unchecked")
    List<VolumeInfo> volumes = (List<VolumeInfo>)callMethod(sm, "getVolumes");
    if (volumes != null)
    {
      Iterator<VolumeInfo> volumes_i = volumes.iterator();
      while (volumes_i.hasNext())
      {
        VolumeInfo volume = (VolumeInfo)volumes_i.next();
        if (volume.getDisk() != null)
        {
          if (deviceName.equalsIgnoreCase(HCTApi.getDeviceName(volume.getDisk().sysPath)))
          {
            String result = volume.path;
            if (!result.endsWith("/")) result = result.concat("/");
            return result;
          }
        }
      } 
    }
    return null;
  }
  
  // ������ ����� ����� � ������� xposed
  public static void readXposedMap()
  {
    xposedMap.clear();
    try 
    {
      Log.d(TAG,"read xposed map from "+XPOSED_MAP_PATH);
      xposedMap.loadFromFile(XPOSED_MAP_PATH);
    } 
    catch (Exception e) 
    {
      Log.w(TAG,e.getMessage());
    }
  }
  
  // ���������� ����� ����� ����� � �������
  public static void LogXposedMap()
  {
    Log.d(TAG,"");
    Log.d(TAG,XPOSED_MAP_PATH);
    xposedMap.LogProps(TAG);
    Log.d(TAG,"");
  }
  
  // ��������� ���������������� ����� ������
  public static String getXposedMapClass(String TAG, String className)
  {
    String value = xposedMap.getValue(className,CLASS_PARAM,className);
    return value;
  }
  
  // ��������� ���������������� ����� ������� ��� ����
  public static String getXposedMapValue(String TAG, String section, String key)
  {
    // ���� ��� ������ ������ null
    String value = xposedMap.getValue(section, key, null);
    if (value == null)
      return key;
    else
      return value;
  }
  
  // �������� ������ ������
  public static XC_MethodHook.Unhook findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) 
  {
    String nameOfMethod = getXposedMapValue(TAG, className, methodName);
    if (!nameOfMethod.isEmpty())
    {
      String nameOfClass = getXposedMapClass(TAG, className);
      Log.d(TAG,"findAndHook "+nameOfClass+"."+nameOfMethod);
      return XposedHelpers.findAndHookMethod(nameOfClass, classLoader, nameOfMethod, parameterTypesAndCallback);
    }
    else
    {
      Log.w(TAG,className+"."+methodName+" not hooked");
      return null;
    }
  }
  
  // �������� ������ ������
  public static XC_MethodHook.Unhook findAndHookMethodCatch(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) 
  {
    try
    {
      return findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
    }
    catch (Error e)
    {
      Log.e(TAG,e.getMessage());
      XposedBridge.log(e.getMessage());
      return null;
    }
  }
  
  // �������� ������ ���� ������� � �������� ������
  public static Set<XC_MethodHook.Unhook> findAndHookAllMethods(String className, ClassLoader classLoader, String methodName, XC_MethodHook callback) 
  {
    String nameOfMethod = getXposedMapValue(TAG, className, methodName);
    if (!nameOfMethod.isEmpty())
    {
      return XposedBridge.hookAllMethods(findClass(className, classLoader), methodName, callback);
    }
    else
    {
      Log.w(TAG,className+"."+methodName+" not hooked");
      return null;
    }
  }

  // �������� ������ ���� ������� � �������� ������
  public static Set<XC_MethodHook.Unhook> findAndHookAllMethodsCatch(String className, ClassLoader classLoader, String methodName, XC_MethodHook callback) 
  {
    try
    {
      return findAndHookAllMethods(className, classLoader, methodName, callback);
    }
    catch (Error e)
    {
      Log.e(TAG,e.getMessage());
      XposedBridge.log(e.getMessage());
      return null;
    }
  }
  
  // �������� ������ ������������
  public static XC_MethodHook.Unhook findAndHookConstructor(String className, ClassLoader classLoader, Object... parameterTypesAndCallback)
  {
    String nameOfClass = getXposedMapClass(TAG, className);
    Log.d(TAG,"findAndHok "+nameOfClass+" constructor");
    return XposedHelpers.findAndHookConstructor(nameOfClass, classLoader, parameterTypesAndCallback);
  }
  
  // �������� ������ ������������
  public static XC_MethodHook.Unhook findAndHookConstructorCatch(String className, ClassLoader classLoader, Object... parameterTypesAndCallback)
  {
    try
    {
      return findAndHookConstructor(className, classLoader, parameterTypesAndCallback);
    }
    catch (Error e)
    {
      Log.e(TAG,e.getMessage());
      XposedBridge.log(e.getMessage());
      return null;
    }
  }
  
  // findAndHookConstructorCatch ��� ������ ������ � ��� xposed
  public static XC_MethodHook.Unhook findAndHookConstructorCatchNoWarn(String className, ClassLoader classLoader, Object... parameterTypesAndCallback)
  {
    try
    {
      return findAndHookConstructor(className, classLoader, parameterTypesAndCallback);
    }
    catch (Error e)
    {
      Log.e(TAG,e.getMessage());
      return null;
    }
  }
  
  // �������� ���� �������������
  public static Set<XC_MethodHook.Unhook> findAndHookAllConstructors(String className, ClassLoader classLoader, XC_MethodHook callback) 
  {
    return XposedBridge.hookAllConstructors(findClass(className, classLoader), callback);
  }

  // �������� ���� �������������
  public static Set<XC_MethodHook.Unhook> findAndHookAllConstructorsCatch(String className, ClassLoader classLoader, XC_MethodHook callback) 
  {
    try
    {
      return findAndHookAllConstructors(className, classLoader, callback);
    }
    catch (Error e)
    {
      Log.e(TAG,e.getMessage());
      XposedBridge.log(e.getMessage());
      return null;
    }
  }
  
  // ����� ������
  public static Class<?> findClass(String className, ClassLoader classLoader)
  {
    String nameOfClass = getXposedMapClass(TAG, className);
    Log.d(TAG,"find class "+nameOfClass);
    return XposedHelpers.findClass(nameOfClass, classLoader);
  }
  
  // ����� ������
  public static Class<?> findClassCatch(String className, ClassLoader classLoader)
  {
    try
    {
      return findClass(className, classLoader);
    }
    catch (Error e)
    {
      Log.e(TAG,e.getMessage());
      XposedBridge.log(e.getMessage());
      return null;
    }
  }
  
  // ����� ������������������ ����� ������
  /*
    [com.microntek.classname]
    .class=com.microntek.obf
  */
  // ��� com.microntek.obf ���������� com.microntek.classname
  public static String getClassName(String className)
  {
    String key;
    String value;
    // ������ ������
    Iterator<String> sections = xposedMap.enumSections();
    while (sections.hasNext()) 
    {
      String section = sections.next();
      Iterator<String> names = xposedMap.enumKeys(section);
      while (names.hasNext()) 
      {
        key = names.next();
        if (key.equals(CLASS_PARAM))
        {
          value = xposedMap.getValue(section, key);
          if (value.equals(className))
          {
            Log.d(TAG,"map classname "+className+" to "+section);
            return section;
          }
        }
      }

    }
    // ��� ������ �� �������������
    return className;
  }
  
  // ����� ������
  public static Object callMethod(Object obj, String methodName, Object... args) 
  {
    // ����������������� ��� ������
    String className = getClassName(obj.getClass().getName());
    String nameOfMethod = getXposedMapValue(TAG, className, methodName);
    if (nameOfMethod.isEmpty()) return null;
    try
    {
      return XposedHelpers.callMethod(obj, nameOfMethod, args);
    }
    catch (Error e)
    {
      Log.e(TAG,obj.getClass().getName()+":"+nameOfMethod+" -> "+e.getMessage());
      XposedBridge.log(obj.getClass().getName()+":"+nameOfMethod+" -> "+e.getMessage());
      return null;
    }
  }
  
  // ����� ������������ ������
  public static Object callStaticMethod(String className, ClassLoader classLoader, String methodName, Object... args) 
  { 
    // ��� ������ 
    String nameOfClass = getXposedMapClass(TAG, className);
    String nameOfMethod = getXposedMapValue(TAG, className, methodName);
    if (nameOfMethod.isEmpty()) return null;
    try
    {
      return XposedHelpers.callStaticMethod(XposedHelpers.findClass(nameOfClass, classLoader), methodName, args);
    }
    catch (Exception x)
    {
      Log.e(TAG,nameOfClass+":"+nameOfMethod+" -> "+x.getMessage());
      XposedBridge.log(nameOfClass+":"+nameOfMethod+" -> "+x.getMessage());
      return null;
    }
    catch (Error e)
    {
      Log.e(TAG,nameOfClass+":"+nameOfMethod+" -> "+e.getMessage());
      XposedBridge.log(nameOfClass+":"+nameOfMethod+" -> "+e.getMessage());
      return null;
    }
  }
  
  // ��������� ���������� ����
  public static Object getObjectField(Object obj, String fieldName)
  {
    String className = getClassName(obj.getClass().getName());
    String nameOfField = getXposedMapValue(TAG, className, fieldName);
    if (nameOfField.isEmpty()) return null;
    try
    {
      return XposedHelpers.getObjectField(obj, nameOfField);
    }
    catch (Error e)
    {
      Log.e(TAG,obj.getClass().getName()+":"+nameOfField+" -> "+e.getMessage());
      XposedBridge.log(obj.getClass().getName()+":"+nameOfField+" -> "+e.getMessage());
      return null;
    }
  }
  
  // ��������� ���������� ����
  public static void setObjectField(Object obj, String fieldName, Object value)
  {
    String className = getClassName(obj.getClass().getName());
    String nameOfField = getXposedMapValue(TAG, className, fieldName);
    if (nameOfField.isEmpty()) return;
    try
    {
      XposedHelpers.setObjectField(obj, nameOfField, value);
    }
    catch (Error e)
    {
      Log.e(TAG,obj.getClass().getName()+":"+nameOfField+" -> "+e.getMessage());
      XposedBridge.log(obj.getClass().getName()+":"+nameOfField+" -> "+e.getMessage());
    }
  }
  
  // ��������� �������������� ����
  public static int getIntField(Object obj, String fieldName)
  {
    String className = getClassName(obj.getClass().getName());
    String nameOfField = getXposedMapValue(TAG, className, fieldName);
    if (nameOfField.isEmpty()) return 0;
    try
    {
      return XposedHelpers.getIntField(obj, nameOfField);
    }
    catch (Error e)
    {
      Log.e(TAG,obj.getClass().getName()+":"+nameOfField+" -> "+e.getMessage());
      XposedBridge.log(obj.getClass().getName()+":"+nameOfField+" -> "+e.getMessage());
      return 0;
    }
  }
  
  // ��������� �������������� ����
  public static void setIntField(Object obj, String fieldName, int value)
  {
    String className = getClassName(obj.getClass().getName());
    String nameOfField = getXposedMapValue(TAG, className, fieldName);
    if (nameOfField.isEmpty()) return;
    try
    {
      XposedHelpers.setIntField(obj, nameOfField, value);
    }
    catch (Error e)
    {
      Log.e(TAG,obj.getClass().getName()+":"+nameOfField+" -> "+e.getMessage());
      XposedBridge.log(obj.getClass().getName()+":"+nameOfField+" -> "+e.getMessage());
    }
  }
  
  //��������� boolean ����
  public static Boolean getBooleanField(Object obj, String fieldName)
  {
    String className = getClassName(obj.getClass().getName());
    String nameOfField = getXposedMapValue(TAG, className, fieldName);
    if (nameOfField.isEmpty()) return false;
    try
    {
      return XposedHelpers.getBooleanField(obj, nameOfField);
    }
    catch (Error e)
    {
      Log.e(TAG,obj.getClass().getName()+":"+nameOfField+" -> "+e.getMessage());
      XposedBridge.log(obj.getClass().getName()+":"+nameOfField+" -> "+e.getMessage());
      return false;
    }
  }
  
  // ��������� ���� ���� boolean
  public static void setBooleanField(Object obj, String fieldName, boolean value)
  {
    String className = getClassName(obj.getClass().getName());
    String nameOfField = getXposedMapValue(TAG, className, fieldName);
    if (nameOfField.isEmpty()) return;
    try
    {
      XposedHelpers.setBooleanField(obj, nameOfField, value);
    }
    catch (Error e)
    {
      Log.e(TAG,obj.getClass().getName()+":"+nameOfField+" -> "+e.getMessage());
      XposedBridge.log(obj.getClass().getName()+":"+nameOfField+" -> "+e.getMessage());
    }
  }
  
  // ��������� ������������ ���� 
  public static void setStaticObjectField(Class<?> cls, String fieldName, String value)
  {
    String className = getClassName(cls.getName());
    String nameOfField = getXposedMapValue(TAG, className, fieldName);
    if (nameOfField.isEmpty()) return;
    try
    {
      XposedHelpers.setStaticObjectField(cls, nameOfField, value);
    }
    catch (Error e)
    {
      Log.e(TAG,cls.getName()+":"+nameOfField+" -> "+e.getMessage());
      XposedBridge.log(cls.getName()+":"+nameOfField+" -> "+e.getMessage());
    }
  }
  
  // ��������� ������������ ����
  public static int getStaticIntField(Class<?> cls, String fieldName)
  {
    String className = getClassName(cls.getName());
    String nameOfField = getXposedMapValue(TAG, className, fieldName);
    if (nameOfField.isEmpty()) return 0;
    try
    {
      return XposedHelpers.getStaticIntField(cls, nameOfField);
    }
    catch (Error e)
    {
      Log.e(TAG,cls.getName()+":"+nameOfField+" -> "+e.getMessage());
      XposedBridge.log(cls.getName()+":"+nameOfField+" -> "+e.getMessage());
      return 0;
    }
  }
  
  // ��������� boolean
  public static boolean getBooleanSettings(String xposedName, String key, boolean defValue)
  {
    return xposedMap.getBoolValue(xposedName+"#settings", key, defValue);
  }
  
  // ��������� boolean
  public static boolean getBooleanSettings(Object obj, String key, boolean defValue)
  {
    String xposedName = obj.getClass().getPackage().getName();
    return xposedMap.getBoolValue(xposedName+"#settings", key, defValue);
  }
  
  // ��������� int
  public static int getIntSettings(String xposedName, String key, int defValue)
  {
    return xposedMap.getIntValue(xposedName+"#settings", key, defValue);
  }
  
  // ��������� String
  public static String getStringSettings(String xposedName, String key, String defValue)
  {
    return xposedMap.getValue(xposedName+"#settings", key, defValue);
  }
  
  // ��������� String
  public static String getStringSettings(Object obj, String key, String defValue)
  {
    String xposedName = obj.getClass().getPackage().getName();
    return xposedMap.getValue(xposedName+"#settings", key, defValue);
  }
  
  // ��������� bitmap � ����
  public static void saveBitmapToFile(Bitmap bitmap, String fileName)
  {
    try
    {
      File file = new File(fileName);
      FileOutputStream fOut = new FileOutputStream(file);
      //
      bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut);
      fOut.flush();
      fOut.close();
    }
    catch (Exception e)
    {
      Log.d(TAG,e.getMessage());
    }
  }
  
  // �������������� ����� �� ������
  public static int parseColor(String colorStr, int defValue)
  {
    int color = defValue;
    try
    {
      if (!TextUtils.isEmpty(colorStr)) color = Color.parseColor(colorStr);
    }
    catch (Exception e)
    {
      Log.e(TAG,"invalid color: "+colorStr);
    }
    return color;
  }
  
  // ����������� � ����
  public static void logToFile(Context context, String text)
  {
    try
    {
      File file = new File(context.getFilesDir(), LOG_FILE_NAME);
      BufferedWriter wr = new BufferedWriter(new FileWriter(file, file.exists()));
      try
      {
        wr.write(text);
        wr.write((char)10);
      }
      finally
      {
        wr.close();
      } 
    }
    catch (Exception e)
    { 
      Log.d(TAG, e.getMessage());
    }
  }
  
}
