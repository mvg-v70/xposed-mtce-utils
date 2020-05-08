package ru.mvgv70.xposed_mtce_utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import android.view.View.OnLongClickListener;
import android.widget.TextView;
import ru.mvgv70.utils.IniFile;
import ru.mvgv70.utils.Utils;

public class Bluetooth implements IXposedHookLoadPackage {
	
  private static final String TAG = "xposed-mtce-utils-bluetooth";
  private static final String PACKAGE_NAME = "com.microntek.bluetooth";
  private static String EXTERNAL_SD = "/storage/external_sd/";
  private static final String INIFILE_NAME = "mtce-utils/settings.ini";
  private static final String QUICKDIAL_SECTION = "bluetooth.quickdial";
  private static final String SETTINGS_SECTION = "bluetooth.settings";
  private static IniFile props = new IniFile();
  //
  private static boolean sortAddressBook = true;
  private static Activity btActivity;
  private static Object dialFragment;
  private static Handler handler; 

  @Override
  public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable 
  {
    // MainActivity.onCreate(Bundle)
    XC_MethodHook onCreate = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"onCreate");
        btActivity = (Activity)param.thisObject;
        // путь к файлу из build.prop
        EXTERNAL_SD = Utils.getModuleSdCard();
        Log.d(TAG,EXTERNAL_SD+" "+Environment.getExternalStorageState(new File(EXTERNAL_SD)));
        handler = new Handler();
        // чтение настроек
        readSettings();
        // ACTION_CALL
        Log.d(TAG,"action="+btActivity.getIntent().getAction());
        if (btActivity.getIntent().getAction().equals(Intent.ACTION_CALL))
          actionCall(btActivity.getIntent());
      }
    };
    
    // MainActivity.onDestroy()
    XC_MethodHook onDestroy = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"onDestroy");
        handler = null;
      }
    };
    
    // MainActivity.updatePhoneBookFirstChar()
    XC_MethodHook updatePhoneBookFirstChar = new XC_MethodHook() {
        
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"updatePhoneBookFirstChar");
        if (sortAddressBook)
        {
          sortPhoneBook();
          param.setResult(null);
        }
      }
    };
    
    // DialFragment.init()
    XC_MethodHook init = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"init");
        dialFragment = param.thisObject;
        Resources res = btActivity.getResources();
        int button_id;
        String number;
        View button;
        // на какие кнопки нужно повесить long-click
        for (int i=1; i<=9; i++)
        {
          number = getQuickDial(i);
          button_id = res.getIdentifier("dial_num"+i, "id", btActivity.getPackageName());
          button = btActivity.findViewById(button_id);
          if ((button != null) && (!TextUtils.isEmpty(number)))
          {
            Log.d(TAG,"set long click for button("+i+")");
            button.setLongClickable(true);
            button.setOnLongClickListener(buttonLongClick);
            button.setTag(number);
          }
        }
      }
    };
    
    // begin hooks
    if (!lpparam.packageName.equals(PACKAGE_NAME)) return;
    Log.d(TAG,PACKAGE_NAME);
    Utils.setTag(TAG);
    Utils.readXposedMap();
    Utils.findAndHookMethodCatch("com.microntek.bluetooth.ui.DialFragment", lpparam.classLoader, "init", init);
    Utils.findAndHookMethodCatch("com.microntek.bluetooth.MainActivity", lpparam.classLoader, "onCreate", Bundle.class, onCreate);
    Utils.findAndHookMethodCatch("com.microntek.bluetooth.MainActivity", lpparam.classLoader, "onDestroy", onDestroy);
    Utils.findAndHookMethodCatch("com.microntek.bluetooth.MainActivity", lpparam.classLoader, "updatePhoneBookFirstChar", updatePhoneBookFirstChar);
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
    sortAddressBook = props.getBoolValue(SETTINGS_SECTION, "sort", true);
    Log.d(TAG,"sort="+sortAddressBook);
  }

  // номер быстрого набора
  private String getQuickDial(int index)
  {
    return props.getValue(QUICKDIAL_SECTION, Integer.toString(index));
  }
  
  // набор номера
  private void dialNumber(StringBuffer number)
  {
    Log.d(TAG,"dial "+number.toString());
    TextView numberText = (TextView)Utils.getObjectField(dialFragment, "numberText");
    if (numberText != null) numberText.setText(number);
    Utils.setObjectField(dialFragment, "dialoutNumbers", number);
    Utils.callMethod(dialFragment, "dialAnswer");
  }
  
  @SuppressWarnings("unchecked")
  private void sortPhoneBook() 
  {
    Log.d(TAG,"assortPhoneBook");
    List<String> phoneBookList = (List<String>)Utils.getObjectField(btActivity, "phoneBookList");
    Log.d(TAG,"phoneBookList.size="+phoneBookList.size());
    if (phoneBookList.size() == 0) return;
    // отсортированный список
    List<String> phoneBookListSorted = new ArrayList<String>();
    // русские буквы
    for (Character ch = 'А'; ch <= 'Я'; ch++)
    {
      for (String line : phoneBookList)
        if (Character.toUpperCase(line.charAt(0)) == ch)
          phoneBookListSorted.add(line);
    }
    // английские буквы
    for (Character ch = 'A'; ch <= 'Z'; ch++)
    {
      for (String line : phoneBookList)
        if (Character.toUpperCase(line.charAt(0)) == ch)
          phoneBookListSorted.add(line);
    }
    // символы и цифры
    for (Character ch = '!'; ch <= '9'; ch++)
    {
      for (String line : phoneBookList)
        if (Character.toUpperCase(line.charAt(0)) == ch)
          phoneBookListSorted.add(line);
    if (phoneBookListSorted.size() < phoneBookList.size())
    {
      // добавим записи, которые не были добавлены
      for (String line : phoneBookList)
        if (!phoneBookListSorted.contains(line))
          phoneBookListSorted.add(line);
      }
    }
    // устанавливаем отсортированный список
    Utils.setObjectField(btActivity, "phoneBookList", phoneBookListSorted);
    Log.d(TAG,"sorted");
  }
  
  private OnLongClickListener buttonLongClick = new OnLongClickListener()
  {
    public boolean onLongClick(View v)
    {
      String number = (String)v.getTag();
      Log.d(TAG,"long click "+number);
      // проверка пустого номера
      if (number.isEmpty()) return false;
      StringBuffer dialoutNumbers = (StringBuffer)Utils.getObjectField(dialFragment, "dialoutNumbers");
      // если ничего не введено
      if (dialoutNumbers.length() > 0) return false;
      // добавляем номер телефона в набор
      dialoutNumbers.append(number);
      dialNumber(dialoutNumbers);
      return true;
    }
  };
  
  // звонок через Runnable
  private class CallRunnable implements Runnable
  {
    private StringBuffer number = null; 
	
    public void setNumber(String text) 
    {
      number = new StringBuffer(text);
    }
	
    public void run() 
    {
      Log.d(TAG,"dial "+number);
      dialNumber(number);
    }
  };
  
  private CallRunnable callNumberRunnable = new CallRunnable();
  
  private void actionCall(Intent intent)
  {
    if (intent.getAction().equals(Intent.ACTION_CALL))
    {
      // телефонный номер
      String tel = intent.getData().toString().substring(4);
      // проверка на пустую строку
      if (!TextUtils.isEmpty(tel))
      {
        // выбросить все символы кроме цифр
        tel = tel.replaceAll("[^0123456789]","");
        Log.d(TAG,"ACTION_CALL - tel:"+tel);
        // позвонить по номеру
        callNumberRunnable.setNumber(tel);
        handler.post(callNumberRunnable);
      }
    }
  }

	  
};