package com.example.gridimage;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

public class RecyclingBitmapDrawable extends BitmapDrawable {

	private int displayCount;
	private int cacheCount;
	private boolean hasBeenDisplay;
	
	public RecyclingBitmapDrawable (Resources resources) {
		super(resources);
	}
	
	public RecyclingBitmapDrawable (Resources resources, Bitmap bitmap) {
		super(resources, bitmap);
	}
	
	public void display (boolean isDisplay) {
		synchronized (this) {
			if (isDisplay) {
				displayCount++;
				hasBeenDisplay = true;
			}
			else {
				displayCount--;
			}
		}
		checkState ();
	}
	
	public void cache (boolean inCache) {
		synchronized (this) {
			if (inCache) {
				cacheCount++;
			}
			else {
				cacheCount--;
			}			
		}
		checkState ();
	}
	
	public synchronized void checkState () {
		if (displayCount<0&&cacheCount<0&&!hasBeenDisplay&&isValidate()) {
			getBitmap().recycle();
		}
	}
	
	public boolean isValidate () {
		Bitmap bitmap = getBitmap();
		return bitmap!=null&&!bitmap.isRecycled();
	}
}
