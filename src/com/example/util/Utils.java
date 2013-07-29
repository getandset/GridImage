package com.example.util;

import android.os.Build;

public class Utils {
	
	private Utils (){};
	
	
	public static boolean hasFroyo () {
		return Build.VERSION.SDK_INT>=Build.VERSION_CODES.FROYO;
	}
	
	public static boolean hasGinerbread () {
		return Build.VERSION.SDK_INT>=Build.VERSION_CODES.GINGERBREAD;
	}
	
	public static boolean hasHoneyComb () {
		return Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB;
	}
	
	public static boolean hasJellyBean () {
		return Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN;
	}

}
