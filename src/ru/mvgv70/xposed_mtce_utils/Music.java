package ru.mvgv70.xposed_mtce_utils;

import java.io.File;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import ru.mvgv70.utils.IniFile;
import ru.mvgv70.utils.Utils;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Music implements IXposedHookLoadPackage  {

  private static final String TAG = "xposed-mtce-utils-music";
  private static final String PACKAGE_NAME = "com.microntek.music";
  private static String EXTERNAL_SD = "/storage/external_sd/";
  private static final String INIFILE_NAME = "mtce-utils/settings.ini";
  private static IniFile props = new IniFile();
  // настройки
  private static final String SETTINGS_SECTION = "music.settings";
  private static boolean playAfterCall = false;
  private static boolean alternateCover = false;
  private static boolean toastEnable = false;
  private static int toastSize = 0;
  private static String toastFormat = "%title%";
  private static boolean savePostion = false;
  private static boolean musicAlumb = false;
  // bluetooth constants
  private static final String BLUETOOTH_STATE = "connect_state";
  private static final int BLUETOOTH_CALL_END = 1;
  private static final int BLUETOOTH_CALL_OUT = 2;
  private static final int BLUETOOTH_CALL_IN = 3;
  // music constants
  private static final int MUSIC_NONE = 0;
  private static final int MUSIC_PLAY = 1;
  private static final int MUSIC_PAUSE = 2;
  //
  private static Context context = null;
  private static int musicState = MUSIC_NONE;
  private static int prevMusicState = 0;
  private static long _id = 0;
  private static String title = "";
  private static String album = "";
  private static String artist = "";
  private static String fileName = "";
  private static long track_no = -1;
  private static String shortFileName = "";
  private static String folder = "";
  
  @Override
  public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable 
  {
    // MusicActivity.onCreate(Bundle)
    @SuppressLint("ShowToast")
    XC_MethodHook onCreateActivity = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable 
      {
        Log.d(TAG,"onCreate");
        context = (Context)param.thisObject;
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
    
    // MusicActivity.onDestroy(Bundle)
    XC_MethodHook onDestroyActivity = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable 
      {
        Log.d(TAG,"onDestroy");
        context.unregisterReceiver(bluetoothReceiver);
        context.unregisterReceiver(requestInfoReceiver);
        context.unregisterReceiver(mediaReceiver);
        context = null;
      }
    };
    
    // Util.getMp3Art(Context, long, long)
    XC_MethodHook getMp3Art = new XC_MethodHook() {
      
      @Override
      protected void beforeHookedMethod(MethodHookParam param) throws Throwable 
      {
        Log.d(TAG,"getMp3Art");
        if (alternateCover)
        {
          Context context = (Context)param.args[0];
          long _id = (long)param.args[1];
          long album_id = (long)param.args[2];
          Bitmap bitmap = getCover(context, _id, album_id);
          if (bitmap != null) param.setResult(bitmap);
        }
      }
    };
            
    // begin hooks
    if (!lpparam.packageName.equals(PACKAGE_NAME)) return;
    Log.d(TAG,PACKAGE_NAME);
    Utils.setTag(TAG);
    Utils.readXposedMap();
    Utils.findAndHookMethodCatch("com.microntek.music.MusicActivity", lpparam.classLoader, "onCreate", Bundle.class, onCreateActivity);
    Utils.findAndHookMethodCatch("com.microntek.music.MusicActivity", lpparam.classLoader, "onDestroy", onDestroyActivity);
    Utils.findAndHookMethodCatch("com.music.Util", lpparam.classLoader, "getMp3Art", Context.class, long.class, long.class, getMp3Art);
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
    alternateCover = props.getBoolValue(SETTINGS_SECTION, "cover.alt", false);
    Log.d(TAG,"cover.alt="+alternateCover);
    playAfterCall = props.getBoolValue(SETTINGS_SECTION, "play_after_call", false);
    Log.d(TAG,"play_after_call="+playAfterCall);
    savePostion = props.getBoolValue(SETTINGS_SECTION, "save_position", false);
    Log.d(TAG,"save_position="+savePostion);
    // music.alumb
    musicAlumb = props.getBoolValue(SETTINGS_SECTION, "music.alumb", false);
    Log.d(TAG,"music.alumb="+musicAlumb);
    // toast
    toastEnable = props.getBoolValue(SETTINGS_SECTION, "toast", false);
    Log.d(TAG,"toast="+toastEnable);
    toastSize = props.getIntValue(SETTINGS_SECTION, "toast.size", 0);
    Log.d(TAG,"toast.size="+toastSize);
    toastFormat = props.getValue(SETTINGS_SECTION, "toast.format", "%title%");
    Log.d(TAG,"toast.format="+toastFormat);
  }
  
  // создание receivers
  private void createReceivers()
  {
    // bluetooth
    IntentFilter bi = new IntentFilter();
    bi.addAction("com.microntek.bt.report");
    context.registerReceiver(bluetoothReceiver, bi);
    Log.d(TAG,"create bluetooth receiver");
    // music state & tags
    IntentFilter ti = new IntentFilter();
    ti.addAction("com.microntek.music.report");
    context.registerReceiver(musicReceiver, ti);
    Log.d(TAG,"create music receiver");
    // info
    IntentFilter ii = new IntentFilter();
    ii.addAction("hct.music.info");
    context.registerReceiver(requestInfoReceiver, ii);
    Log.d(TAG,"create info receiver");
  }
  
  // обработчик bluetooth
  private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      int state = intent.getIntExtra(BLUETOOTH_STATE, -1);
      Log.d(TAG,"bluetoothState="+state+", musicState="+musicState);
      if ((state == BLUETOOTH_CALL_IN) || (state == BLUETOOTH_CALL_OUT)) 
      {
        prevMusicState = musicState;
        // вход€щий или исход€щий звонок
        if (musicState == MUSIC_PLAY)
        {
          // поставим на паузу, если плеер включен
          Log.d(TAG,"pause player");
          playerPlayPause();
        }
      }
      else if (playAfterCall && (state == BLUETOOTH_CALL_END) && (prevMusicState == MUSIC_PLAY) && (musicState == MUSIC_PAUSE)) 
      {
        // включим плеер, если до звонка он играл
        Log.d(TAG,"play player");
        playerPlayPause();
      }
    }
  };

  //  com.microntek.playmusic
  private void playerPlayPause()
  {
    Intent intent = new Intent("hct.music.playpause");
    context.sendBroadcast(intent);
  }
  
  // com.microntek.music.report
  private BroadcastReceiver musicReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      String type = intent.getStringExtra("type");
      if (type == null) return;
      // из модулей xposed-mtce-poweramp/pcradio
      if (intent.hasExtra("class")) return;
      Log.d(TAG,"music receiver: "+type);
      if (type.equals("music.alumb"))
      {
        long value[] = intent.getLongArrayExtra("value");
    	// [0]=album_id, [1]=_id
        _id = value[1];
        readFileInfo(context, value[1]);
      }
      else if (type.equals("music.state"))
      {
        musicState = intent.getIntExtra("value", MUSIC_NONE);
        Log.d(TAG,"musicState="+musicState);
      }
      else if (type.equals("music.idx") && musicAlumb)
      {
        // эмул€ци€ music.alumb
        int id = intent.getIntExtra("value", -1);
        Log.d(TAG,"send com.microntek.music.report type=music.alumb id="+id);
        Intent pintent = new Intent("com.microntek.music.report");
        pintent.putExtra("type", "music.alumb");
        pintent.putExtra("value", new long[] {-1, id});
        // pintent.putExtra("class", PACKAGE_NAME);
        context.sendBroadcast(pintent);
      }
    }
  };
  
  // hct.music.info
  private BroadcastReceiver requestInfoReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      // читаем и посылаем информацию о треке по требованию
      Log.d(TAG,"info request receiver");
      readFileInfo(context, _id);
    }
  };
  
  // чтение параметров mp3-файла
  private static void readFileInfo(Context context, long _id)
  {
    track_no = 0;
    album = "";
    title = "";
    artist = "";
    folder = "";
    fileName = "";
    shortFileName = "";
    String[] ids = { Long.toString(_id) };
    Log.d(TAG,"_id="+_id);
    try
    {
      Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.AudioColumns.TITLE, MediaStore.Audio.AudioColumns.ARTIST, MediaStore.Audio.AudioColumns.ALBUM, MediaStore.Audio.AudioColumns.DISPLAY_NAME, MediaStore.Audio.AudioColumns._ID, MediaStore.Audio.AudioColumns.ALBUM_ID, MediaStore.Audio.AudioColumns.DATA, MediaStore.Audio.AudioColumns.TRACK }, MediaStore.Audio.AudioColumns._ID+"=?", ids, "");
      if (cursor.moveToFirst())
      {
    	// MediaStore.Audio.AudioColumns.ALBUM
        album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM));
        Log.d(TAG,"album="+album);
        // MediaStore.Audio.AudioColumns.TITLE
        title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE));
        Log.d(TAG,"title="+title);
        // MediaStore.Audio.AudioColumns.ARTIST
        artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST));
        Log.d(TAG,"artist="+artist);
        // MediaStore.Audio.AudioColumns.ALBUM_ID
        long album_id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID));
        Log.d(TAG,"album_id="+album_id);
        // MediaStore.Audio.AudioColumns.DATA
        fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA));
        Log.d(TAG,"_data="+fileName);
        // MediaStore.Audio.AudioColumns.TRACK
        track_no = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TRACK));
        Log.d(TAG,"track_no="+track_no);
        // разберем им€ файла
        String file = "";
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
        Log.d(TAG,"filename="+shortFileName);
        Log.d(TAG,"folder="+folder);
        // отсылаем информацию по тегам
        Intent intent = new Intent("com.microntek.music.report");
        intent.putExtra("type", "music.tags");
        intent.putExtra(MediaStore.Audio.AudioColumns.ALBUM, album);
        intent.putExtra(MediaStore.Audio.AudioColumns.TITLE, title);
        intent.putExtra(MediaStore.Audio.AudioColumns.ARTIST, artist);
        intent.putExtra(MediaStore.Audio.AudioColumns.DATA, fileName);
        intent.putExtra(MediaStore.Audio.AudioColumns._ID, _id);
        intent.putExtra(MediaStore.Audio.AudioColumns.ALBUM_ID, album_id);
        intent.putExtra(MediaStore.Audio.AudioColumns.TRACK, track_no);
        context.sendBroadcast(intent);
        // сохран€ем текущую позицию
        if (savePostion) Utils.callMethod(context, "SaveLocalData");
        // показываем уведомление
        if (toastEnable) showToast();
      }
      else 
        Log.w(TAG,"no data found");
      if (!cursor.isClosed()) cursor.close();
    } 
    catch (Exception e)
    {
      Log.e(TAG,"error: "+e.getMessage());
    }
  }
  
  //  показать уведомление о смене трека
  private static void showToast()
  {
    Log.d(TAG,"showToast");
    Intent intent = new Intent("com.microntek.music.toast");
    intent.putExtra("toast.size", toastSize);
    intent.putExtra("toast.format", toastFormat);
    intent.putExtra("class", PACKAGE_NAME);
    intent.putExtra(MediaStore.Audio.AudioColumns.TITLE, title);
    intent.putExtra(MediaStore.Audio.AudioColumns.ALBUM, album);
    intent.putExtra(MediaStore.Audio.AudioColumns.ARTIST, artist);
    intent.putExtra(MediaStore.Audio.AudioColumns.DATA, fileName);
    intent.putExtra(MediaStore.Audio.AudioColumns.TRACK, track_no);
    context.sendBroadcast(intent);
  }
  
  // картинка
  public static Bitmap getCover(Context context, long _id, long album_id)
  {
    try
    {
      Bitmap bitmap = null;
      // поиск по композиции
      Uri uri = Uri.parse("content://media/external/audio/media/" + _id + "/albumart");
      ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(uri, "r");
      if (fd != null)
      {
        bitmap = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor());
        if (bitmap != null)
        {
          Log.d(TAG,"bitmap find by _id");
          return bitmap;
        }
      }
      // поиск по альбому AlbumArt.jpg
      uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), album_id);
      // error: Attempt to invoke virtual method 'char[] java.lang.String.toCharArray()' on a null object reference
      bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
      if (bitmap != null)
      {
        Log.d(TAG,"bitmap find by album_id");
        return bitmap;
      }
    } 
    catch (Exception e)
    {
      Log.e(TAG,"error: "+e.getMessage());
    }
    return null;
  }
  
  // включить обработчик подключени€ носителей
  private void createMediaReceiver()
  {
    IntentFilter ui = new IntentFilter();
    ui.addAction(Intent.ACTION_MEDIA_MOUNTED);
    ui.addDataScheme("file");
    context.registerReceiver(mediaReceiver, ui);
    Log.d(TAG,"media mount receiver created");
  }
  
  // обработчик MEDIA_MOUNT
  private BroadcastReceiver mediaReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent)
    {
      String action = intent.getAction();
      // путь к подключаемой карте без завершающего слеша
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
