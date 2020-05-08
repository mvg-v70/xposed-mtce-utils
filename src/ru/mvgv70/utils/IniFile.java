package ru.mvgv70.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;

//
//     version 1.2.6
//

public class IniFile 
{
	
  HashMap<String,ArrayList<String>> ini_file = new HashMap<String,ArrayList<String>>();
	
  // ������ �� �����
  public void loadFromFile(String fileName) throws IOException
  {
    BufferedReader br = new BufferedReader(new FileReader(fileName));
    try
    {
      readFromBufferedReader(br);
    }
    finally
    {
      br.close();
    }
  }
  
  // ������ �� assets
  public void loadFromAssets(Context context, String fileName) throws IOException
  {
    InputStream is = context.getAssets().open(fileName);
    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
    try
    {
      readFromBufferedReader(br);
    }
    finally
    {
      br.close();
      is.close();
    }
  }
  
  // ������ �� BufferedReader
  private void readFromBufferedReader(BufferedReader br) throws IOException
  {
    final String BOM = "\uFEFF"; 
    int line_no = 1;
    String line;
    String section = "";
    ini_file.clear();
    // ������ ������
    ini_file.put("", new ArrayList<String>());
    while ((line = br.readLine()) != null)
    {
      // �������� BOM �� ������ ������
      if (line_no == 1) 
      {
        if (line.startsWith(BOM))
          line = line.replace(BOM,"");
      }
      // ��������� �� �������
      if (line.trim().isEmpty())
      {
        // ������ ������
      }
      else if (line.startsWith("#"))
      {
        // �����������
      }
      else if (line.startsWith(";"))
      {
        // �����������
      }
      else if (line.startsWith("["))
      {
        // ������
        section = line.substring(1,line.lastIndexOf("]")).trim();
        if (ini_file.get(section) == null)
          ini_file.put(section, new ArrayList<String>());
      }
      else
      {
        // ��������
        int equalIndex = line.indexOf("=");
        if (equalIndex > 0)
        {
          String key = line.substring(0,equalIndex).trim();
          String value = line.substring(equalIndex+1);
          ini_file.get(section).add(key+"="+value);
      }
      else
        ini_file.get(section).add(line);
      }
      line_no++;
    }
  }
  
  public void clear()
  {
    ini_file.clear();
  }
  
  public int getSectionCount()
  {
    return ini_file.size();
  }
  
  public Iterator<String> enumSections()
  {
    return ini_file.keySet().iterator();
  }
  
  public Iterator<String> enumLines(String section)
  {
    ArrayList<String> asection = ini_file.get(section);
    if (asection != null)
      return ini_file.get(section).iterator();
    else
      return null;
  }
  
  public class KeyIterator implements Iterator<String>
  {
    private Iterator<String> iterator;
    private ArrayList<String> asection;
	  
    KeyIterator(String section)
    {
      asection = ini_file.get(section);
      if (asection == null)
        // ���� ������ �� ������� ������ ������ ������
        asection = new ArrayList<String>();
      iterator = asection.iterator();
    }
    
    public boolean isNullIterator()
    {
      if (iterator == null) return true; else return false;
    }

    @Override
    public boolean hasNext() 
    {
      return iterator.hasNext();
    }

    @Override
    public String next() 
    {
      String line = iterator.next();
      int equalIndex = line.indexOf("=");
      if (equalIndex > 0)
        line = line.substring(0,equalIndex).trim();
      return line;
    }

    @Override
    public void remove() 
    {
      iterator.remove();
    }
    
    public int size()
    {
      return asection.size();
    }
	  
  };
  
  public KeyIterator enumKeys(String section)
  {
    KeyIterator iterator = new KeyIterator(section);
    if (iterator.isNullIterator())
      return null;
    else
      return iterator;
  }
  
  public List<String> getLines(String section)
  {
    return ini_file.get(section);
  }
  
  public int linesCount(String section)
  {
    ArrayList<String> asection = ini_file.get(section);
    if (asection != null)
      return asection.size();
    else
      return 0;
  }
  
  public String getStringKey(String line)
  {
    String key;
    int equalIndex = line.indexOf("=");
    if (equalIndex > 0)
      key = line.substring(0,equalIndex).trim();
    else
      key = line;
    return key;
  }
  
  public String getStringValue(String line)
  {
    String value = "";
    int equalIndex = line.indexOf("=");
    if (equalIndex > 0)
      value = line.substring(equalIndex+1).trim();
    return value;
  }
  
  // ����� ���������� ��������
  public String getValue(String section, String key)
  {
    return getValue(section, key, "");
  }
  
  // ����� ���������� ��������
  public String getValue(String section, String key, String defValue)
  {
    String line;
    ArrayList<String> lines = ini_file.get(section);
    if (lines != null)
    {
      for(int i = 0; i < lines.size(); i++)
      {
        line = lines.get(i);
        if (line.startsWith(key+"="))
        {
          int equalIndex = line.indexOf("=");
          String value = line.substring(equalIndex+1);
          return value; 
        }
      }
    }
    return defValue;
  }
  
  // ����� �������������� ��������
  public int getIntValue(String section, String key, int defValue)
  {
    int result = defValue;
    String value = getValue(section, key).trim();
    if (!value.isEmpty())
    {
      try
      {
        result = Integer.decode(value);
      }
      catch (Exception E)
      {
        result = defValue;
      }
    }
    return result;
  }
  
  // ����� �������� long
  public long getLongValue(String section, String key, long defValue)
  {
    long result = defValue;
    String value = getValue(section, key).trim();
    if (!value.isEmpty())
    {
      try
      {
        result = Long.decode(value);
      }
      catch (Exception E)
      {
        result = defValue;
      }
    }
    return result;
  }
  
  // ����� �������� boolean
  public boolean getBoolValue(String section, String key, boolean defValue)
  {
    boolean result = defValue;
    String value = getValue(section, key).trim();
    if (!value.isEmpty())
    {
      if ((value.equals("1")) || (value.equalsIgnoreCase("true")))
        result = true;
      else if ((value.equals("0")) || (value.equalsIgnoreCase("false")))
        result = false;
    }
    return result;
  }
  
  // ����� �������� float
  public float getFloatValue(String section, String key, float defValue)
  {
    float result = defValue;
    String value = getValue(section, key).trim();
    if (!value.isEmpty())
    {
      try
      {
        result = Float.valueOf(value);
      }
      catch (Exception E)
      {
        result = defValue;
      }
    }
    return result;
  }
  
  // ����� �������� ����
  public int getColorValue(String section, String key, int defValue)
  {
    int result = defValue;
    String color = getValue(section, key);
    try
    {
      if (!TextUtils.isEmpty(color)) result = Color.parseColor(color);
    }
    catch (Exception e) {}
    //
    return result;
  }
  
  // ��������� ��������
  public void setValue(String section, String key, String value)
  {
    String set_line = key+"="+value;
    ArrayList<String> lines = ini_file.get(section);
    if (lines == null)
    {
      // ������ ���
      ini_file.put(section, new ArrayList<String>());	  
      ini_file.get(section).add(set_line);
    }
    else
    {
      int index = -1;
      String line;
      // ������ ����
      for(int i = 0; i < lines.size(); i++)
      {
        line = lines.get(i);
        if (line.startsWith(key+"="))
        {
          // ����� ����
          index = i;
          break;
        }
      }
      // ���� ���������� ?
      if (index >= 0)
        lines.set(index,set_line);
      else
    	lines.add(set_line);
    }
  }
  
  // ���������� ������
  public void addLine(String section, String line)
  {
    ArrayList<String> lines = ini_file.get(section);
    if (lines == null)
      // ������ ���
      ini_file.put(section, new ArrayList<String>());	  
    ini_file.get(section).add(line);
  }
  
  // ���������� � ����, �������� ����������� � ������ ������
  public void saveToFile(String fileName) throws IOException
  {
    BufferedWriter bw;
    bw = new BufferedWriter(new FileWriter(fileName));
    try 
    {
      Iterator<String> sections = enumSections();
      while (sections.hasNext()) 
      {
        String line = sections.next();
        bw.write("["+line+"]");
        bw.newLine();
        Iterator<String> lines = enumLines(line);
        while (lines.hasNext()) 
        {
          bw.write(lines.next());
          bw.newLine();
        }
      }
    }
    finally
    {
      bw.close();
    }
  }
  
  // ����� ����������� ����� � Log
  public void LogProps(String TAG)
  {
    Iterator<String> sections = enumSections();
    while (sections.hasNext()) 
    {
      String line = sections.next();
      Log.d(TAG,"["+line+"]");
      Iterator<String> lines = enumLines(line);
      while (lines.hasNext()) 
        Log.d(TAG,lines.next());
    }
  }
  
}
