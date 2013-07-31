package com.example.util;

import java.io.FileDescriptor;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

/**
 * subclass of {ImageWorker},destination to deal with size of image
 * 
 *
 */
public class ImageResize extends ImageWorker{
    private static final String TAG = "ImageResize";
    
    private int mHeight;
    private int mWidth;
    
    public ImageResize (Context context, int width, int height) {
	super(context);
	setImageSize (width,height);
    }
    
    public ImageResize (Context context, int size) {
	super(context);
	setImageSize(size);
    }
    
    /**
     * 
     * @param width set target width
     * @param height set target height
     */
    public void setImageSize (int width, int height) {
	this.mWidth = width;
	this.mHeight = height;
    }
    
    /**
     * use only one target size for both width and height
     * @param size
     */
    public void setImageSize (int size) {
	setImageSize(size, size);
    }
    
    public int getWidth () {
	return mWidth;
    }
    
    public int getHeight () {
	return mHeight;
    }
    
    /**
     * this run in background thread 
     */
    @Override
    public Bitmap proccessImage(Object data) {
        int resId = Integer.parseInt(String.valueOf(data));
        return proccessImage (resId);
    }
    
    private Bitmap proccessImage (int resId) {
	return decodeSampleBitmapFromeResource(resId, mWidth, mHeight, getImageCache());
    }
    
    protected Bitmap decodeSampleBitmapFromeResource (int resId, int width, int height, ImageCache imageCache) {
	BitmapFactory.Options options = new BitmapFactory.Options();
	options.inJustDecodeBounds = true;
	options.inSampleSize = 1;
	BitmapFactory.decodeResource(resources, resId, options);
	
	options.inSampleSize = caculateSampleSize(width, width, options);
	options.inJustDecodeBounds = false;
	//if we are running on HoneyComb or newer,try to use inBitmap
	if (Utils.hasHoneyComb()) {
	    addOptionInBitmap (imageCache, options);
	}
	return BitmapFactory.decodeResource(resources, resId, options);
    }
    
    protected Bitmap decodeSampleBitmapFromFile (String fileName, int width, int height, ImageCache imageCache) {
	BitmapFactory.Options options = new BitmapFactory.Options();
	options.inJustDecodeBounds = true;
	options.inSampleSize = 1;
	BitmapFactory.decodeFile(fileName, options);
	options.inSampleSize = caculateSampleSize(width, height, options);
	options.inJustDecodeBounds = false;
	//if we are running on HoneyComb or newer,try to use inBitmap
	if (Utils.hasHoneyComb()) {
	    addOptionInBitmap(imageCache, options);
	}
	return BitmapFactory.decodeFile(fileName, options);
    }
    
    protected Bitmap decodeSanpleBitmapFromFileDecriptor (FileDescriptor fd, int width, int height, ImageCache imageCache) {
	BitmapFactory.Options options = new BitmapFactory.Options();
	options.inSampleSize = 1;
	options.inJustDecodeBounds = true;
	BitmapFactory.decodeFileDescriptor(fd, null, options);
	options.inSampleSize = caculateSampleSize(width, height, options);
	options.inJustDecodeBounds =false;
	//if we are running in HoneyComb or newer,try to use inBitmap
	if (Utils.hasHoneyComb()) {
	    addOptionInBitmap(imageCache, options);
	}
	return BitmapFactory.decodeFileDescriptor(fd, null, options);
    }
    
    private int caculateSampleSize (int reqWidth, int reqHeight, Options options) {
	int sampleSize = 1;
	final int width = options.outWidth;
	final int height = options.outHeight;
	if (width>reqWidth||height>reqHeight) {
	    int widthRatio = (int)Math.ceil((double)width/(double)reqWidth);
	    int heightRatio = (int)Math.ceil((double)height/(double)reqHeight);
	    sampleSize = widthRatio>heightRatio? widthRatio:heightRatio;
	}
	return sampleSize;
    }
    
    //get bitmap from hashSet to reuse
    private void addOptionInBitmap (ImageCache cache, Options options) {
	Bitmap inBitmap = cache.getReusableBitmap(options);
	if (inBitmap!=null) {
	    options.inBitmap = inBitmap;
	}
    }

}
