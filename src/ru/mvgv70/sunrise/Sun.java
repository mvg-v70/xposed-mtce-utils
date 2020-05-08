package ru.mvgv70.sunrise;

import java.util.Calendar;

//
//  упрощенный расчет восхода и заката солнца
//  взято с сайта ab-log.ru/smart-house/linux/sunset 
//  изначально алгоритм на PHP
//

public class Sun 
{
  private double latitude;
  private double longitude;
  private double timezone;
  //
  private int yday;
  private int mon;
  private int mday;
  private int year;
  //
  private double A;
  private double B;
  private double C;
  private double D;
  private double E;
  private double F;
  private double G;
  private double R;
	    
  public Sun (double h, double i, long $timezone)
  {
    latitude = h;
    longitude = i;
    timezone = $timezone;
    //
    Calendar calendar = Calendar.getInstance();
    yday = calendar.get(Calendar.DAY_OF_YEAR);
    mon = calendar.get(Calendar.MONTH);
    mday = calendar.get(Calendar.DAY_OF_MONTH);
    year = calendar.get(Calendar.YEAR);
    //
    if (timezone == 13) timezone = -11;
    //
    A = 1.5708;
    B = 3.14159;
    C = 4.71239;
    D = 6.28319;
    E = 0.0174533 * latitude;
    F = 0.0174533 * longitude;
    G = 0.261799  * timezone;
    // For sunrise or sunset, use
    R = -.0145439;
  }
	    
  public Calendar sunrise()
  {
    double J =  A;
    double K = yday + ((J - F) / D);
    // Solar Mean Anomoly
    double L = (K * .017202) - .0574039;              
    // Solar True Longitude
    double M = L + .0334405 * Math.sin(L);                
    M += 4.93289 + (3.49066E-04) * Math.sin(2 * L);
    if (D == 0) return null;
    while (M < 0)
      M = (M + D);
    while (M >= D)
      M = (M - D);
    if ((M / A) - (int)(M / A) == 0)
      M += 4.84814E-06;
    // Solar Right Ascension
    double P = Math.sin(M) / Math.cos(M);                   
    P = Math.atan2(.91746 * P, 1);
    // Quadrant Adjustment
    if (M > C)
      P += D;
    else
    {
      if (M > A)
        P += B;
    }
    // Solar Declination
    double Q = .39782 * Math.sin(M);   
    // This is how the original author wrote it!
    Q = Q / Math.sqrt(-Q * Q + 1);     
    Q = Math.atan2(Q, 1);
    double S = R - (Math.sin(Q) * Math.sin(E));
    S = S / (Math.cos(Q) * Math.cos(E));
    if (Math.abs(S) > 1)
    {
      // Null phenomenon
    }      
    S = S / Math.sqrt(-S * S + 1);
    S = A - Math.atan2(S, 1);
    S = D - S ;
    // Local apparent time
    double T = S + P - 0.0172028 * K - 1.73364; 
    // Universal timer
    double U = T - F;                     
    //  Wall clock time
    double V = U + G;                            
    // Quadrant Determination
    if (D == 0)
    {
      // Trying to normalize with zero offset
      return null;
    }
    while (V < 0)
      V = (V + D);
    while (V >= D)
      V = (V - D);
    V = V * 3.81972;
    int hour = (int)V;
    int min  = (int)(((V - hour) * 60) + 0.5);
    // 
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, hour);
    calendar.set(Calendar.MINUTE, min);
    calendar.set(Calendar.MONTH, mon);
    calendar.set(Calendar.DAY_OF_MONTH, mday);
    calendar.set(Calendar.YEAR, year);
    return calendar; // -1800
  }
  
  public Calendar sunset()
  {
    double J =  C;
    double K = yday + ((J - F) / D);
    // Solar Mean Anomoly
    double L = (K * .017202) - .0574039;              
    // Solar True Longitude
    double M = L + .0334405 * Math.sin(L);                
    M += 4.93289 + (3.49066E-04) * Math.sin(2 * L);
    if (D == 0)
    {
      // Trying to normalize with zero offset
     return null;
    }
    while (M < 0)
      M = (M + D);
    while (M >= D)
      M = (M - D);
    if ((M / A) - (int)(M / A) == 0)
      M += 4.84814E-06;
    // Solar Right Ascension
    double P = Math.sin(M) / Math.cos(M);                   
    P = Math.atan2(.91746 * P, 1);
    // Quadrant Adjustment
    if (M > C)
      P += D;
    else
    {
      if (M > A)
        P += B;
    }
    // Solar Declination
    double Q = .39782 * Math.sin(M);    
    // This is how the original author wrote it!
    Q = Q / Math.sqrt(-Q * Q + 1);     
    Q = Math.atan2(Q, 1);
    double S = R - (Math.sin(Q) * Math.sin(E));
    S = S / (Math.cos(Q) * Math.cos(E));
    if (Math.abs(S) > 1)
    {
      // # Null phenomenon
    }      
    S = S / Math.sqrt(-S * S + 1);
    S = A - Math.atan2(S, 1);
    // $S = $this->D - $S ;
    // # Local apparent time
    double T = S + P - 0.0172028 * K - 1.73364;
    // Universal timer
    double U = T - F;                     
    // Wall clock time
    double V = U + G;                            
    // Quadrant Determination
    if (D == 0)
    {
      // Trying to normalize with zero offset
      return null; 
    }
    while (V < 0)
      V = (V + D);
    while (V >= D)
      V = (V - D);
    V = V * 3.81972;
    int hour = (int)V;
    int min  = (int)(((V - hour) * 60) + 0.5);
    // 
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, hour);
    calendar.set(Calendar.MINUTE, min);
    calendar.set(Calendar.MONTH, mon);
    calendar.set(Calendar.DAY_OF_MONTH, mday);
    calendar.set(Calendar.YEAR, year);
    return calendar; // +1800
  }

}