package ru.mvgv70.xposed_mtce_utils;

import ru.mvgv70.utils.Utils;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Canbus  implements IXposedHookLoadPackage 
{
  private static final String TAG = "xposed-mtce-utils-canbus";
  private static final String PACKAGE_NAME = "android.microntek.canbus";
	  
  @Override
  public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable 
  {
	    
    XC_MethodHook callO = new XC_MethodHook() {
        
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,param.thisObject.getClass().getName()+".o");
        byte param1 = (byte)param.args[0];
        int param2 = (int)param.args[1];
        byte param3 = (byte)param.args[2];
        Log.d(TAG,"("+Integer.toHexString(param1)+", "+Integer.toString(param2)+", "+Integer.toHexString(param3)+")");
      }
    };
    
    XC_MethodHook pu = new XC_MethodHook() {
        
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        String param1 = (String)param.args[0];
        Log.d(TAG,"CanBusServer.pu: "+param1);
      }
    };
    
    // start hooks
    if (!lpparam.packageName.equals(PACKAGE_NAME)) return;
    Log.d(TAG,PACKAGE_NAME);
    Utils.setTag(TAG);
    Utils.readXposedMap();
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.a", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.ac", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.ae", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.af", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.ag", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.ai", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.aj", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.al", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.am", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.an", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.ao", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.ar", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.ay", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.ba", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.bc", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.bf", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.bg", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.bh", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.bi", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.bk", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.bn", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.bp", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.br", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.bs", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.bt", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.bw", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.bx", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.ca", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.cc", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.cd", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.ch", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.cl", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.cm", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.cs", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.cu", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.cw", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.cz", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.d", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.dc", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.dd", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.dg", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.dj", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.e", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.f", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.i", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.j", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.k", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.r", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.u", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.v", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.a.x", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    //
    Utils.findAndHookMethodCatch("android.microntek.canbus.c.a", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    Utils.findAndHookMethodCatch("android.microntek.canbus.c.a", lpparam.classLoader, "o", byte.class, int.class, byte.class, callO);
    //
    Utils.findAndHookMethodCatch("android.microntek.canbus.CanBusServer", lpparam.classLoader, "pu", String.class, pu);
    Log.d(TAG,PACKAGE_NAME+" hook OK");
  }
	  

}
