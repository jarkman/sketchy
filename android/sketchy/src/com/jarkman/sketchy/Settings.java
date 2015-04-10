package com.jarkman.sketchy;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
 
        addPreferencesFromResource(R.layout.preferences);

  
    }

	@Override
	  protected void onResume() {
	      super.onResume();

	  	      // Set up a listener whenever a key changes
	      getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	      
	       updateLabels();
	       
	  }
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{

		if( key.equals("restoreDefaults") && sharedPreferences.getBoolean("restoreDefaults", false))
		{
			setPrefs( sharedPreferences, getBaseContext(), true );
			this.finish();
		}
		
	       updateLabels();

	}

	public static void setInitialPrefs( Context context, boolean force )
	{
		//Set initial values for preferences on first run
	    SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(context);
	    
	   setPrefs( prefs, context, force );
	   
	}
	
	public static int mBaseImageWidth = 1024; // nominal width for the autoscaling parameters
	
	private static String imageSizeDefault = "256"; //"128";
	private static String lowThresholdDefault = "2.5";
	private static String highThresholdDefault = "7.5";
	
	private static String gaussianKernelRadiusDefault = "8"; //"16";  // in pixels on a 1024-pix image
	private static String gaussianKernelWidthDefault = "64"; //"128";
	
	private static String shortLineLimitDefault = "50"; //"40";
	private static String lineBendLimitDefault = "20"; // changed from "8"; 20/5/11  
	
	private static void setPrefs( SharedPreferences prefs, Context context, boolean force )
	{
	    // general
	   	String imageSize = prefs.getString("imageSize", imageSizeDefault);
	  
	   	// canny
	   	String lowThreshold = prefs.getString("lowThreshold", lowThresholdDefault);
	  	String highThreshold = prefs.getString("highThreshold", highThresholdDefault);
	  	String gaussianKernelRadius = prefs.getString("gaussianKernelRadius", gaussianKernelRadiusDefault);
	 	String gaussianKernelWidth = prefs.getString("gaussianKernelWidth", gaussianKernelWidthDefault);
	 	boolean contrastNormalized = prefs.getBoolean("contrastNormalized", false);
	 	
	 	// vectorwalker
	 	String shortLineLimit = prefs.getString("shortLineLimit", shortLineLimitDefault);
	 	String lineBendLimit = prefs.getString("lineBendLimit", lineBendLimitDefault);

	 	 if( force )
	 	 {
	 		imageSize = imageSizeDefault;
	 		  
		   	// canny
		   	lowThreshold = lowThresholdDefault;
		  	highThreshold = highThresholdDefault;
		  	gaussianKernelRadius = gaussianKernelRadiusDefault;
		 	gaussianKernelWidth = gaussianKernelWidthDefault;
		 	contrastNormalized = false;
		 	
		 	// vectorwalker
		 	shortLineLimit = shortLineLimitDefault;
		 	lineBendLimit = lineBendLimitDefault;
	 	 }
	 	 
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("imageSize", imageSize);
		
		editor.putBoolean("restoreDefaults", false);

		editor.putString("lowThreshold", lowThreshold);
		editor.putString("highThreshold", highThreshold);
		editor.putString("gaussianKernelRadius", gaussianKernelRadius);
		editor.putString("gaussianKernelWidth", gaussianKernelWidth);
		editor.putBoolean("contrastNormalized", contrastNormalized);
		editor.putString("shortLineLimit", shortLineLimit);
		editor.putString("lineBendLimit", lineBendLimit);
	
		editor.commit();
	}
	
	
	private void updateLabels()
	{
		SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
		
	   	String imageSize = prefs.getString("imageSize", imageSizeDefault);
		  
	   	// canny
	   	String lowThreshold = prefs.getString("lowThreshold", lowThresholdDefault);
	  	String highThreshold = prefs.getString("highThreshold", highThresholdDefault);
	  	String gaussianKernelRadius = prefs.getString("gaussianKernelRadius", gaussianKernelRadiusDefault);
	 	String gaussianKernelWidth = prefs.getString("gaussianKernelWidth", gaussianKernelWidthDefault);
	 	
	 	// vectorwalker
	 	String shortLineLimit = prefs.getString("shortLineLimit", shortLineLimitDefault);
	 	String lineBendLimit = prefs.getString("lineBendLimit", lineBendLimitDefault);

	 	
	 	
	    getPreferenceScreen().findPreference("imageSize").setSummary(imageSize + " px (" + imageSizeDefault + ")");
	    getPreferenceScreen().findPreference("lowThreshold").setSummary(lowThreshold + " (" + lowThresholdDefault + ")");
	    getPreferenceScreen().findPreference("highThreshold").setSummary(highThreshold + " (" + highThresholdDefault + ")");
	    getPreferenceScreen().findPreference("gaussianKernelRadius").setSummary(gaussianKernelRadius + " px (" + gaussianKernelRadiusDefault + ")");
	    getPreferenceScreen().findPreference("gaussianKernelWidth").setSummary(gaussianKernelWidth + " px (" + gaussianKernelWidthDefault + ")");
	    getPreferenceScreen().findPreference("shortLineLimit").setSummary(shortLineLimit + " px (" + shortLineLimitDefault + ")");
	    getPreferenceScreen().findPreference("lineBendLimit").setSummary(lineBendLimit + " px (" + lineBendLimitDefault + ")");
	   
	}
	
	
	public static String getDeviceMacAddress( Context context )
	{
	    SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(context);

		
	   	return prefs.getString("deviceMacAddress", null);
	   	
	}
	
	public static void setDeviceMacAddress( Context context, String address )
	{
	    SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(context);

	    SharedPreferences.Editor editor = prefs.edit();
		editor.putString("deviceMacAddress", address);
		editor.commit();

	   	
	}
}