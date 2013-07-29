package com.example.util;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.example.gridimage.RecyclingBitmapDrawable;
import com.example.util.ImageCache.CacheParams;

import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

public abstract class ImageWorker {
    private static final String TAG = "ImageWorker";

    private ImageCache mImageCache;
    private ImageCache.CacheParams mCacheParams;
    private Bitmap mLoadingBitmap;
    private boolean forceExitEarly = false;
    private boolean mPause = false;
    private boolean fadeIn = false;
    private Object mPauseWorkerLock = new Object();
    protected Resources resources;

    public ImageWorker(Context context) {
	this.resources = context.getResources();
    }

    public void loadImage(Object data, ImageView imageView) {
	if (null == data) {
	    return;
	}
	String key = String.valueOf(data);
	BitmapDrawable bitmapDrawable = null;
	if (mImageCache != null) {
	    bitmapDrawable = mImageCache.getBitmapFromMem(key);
	}
	if (bitmapDrawable != null) {
	    imageView.setImageDrawable(bitmapDrawable);
	}
	if (null == bitmapDrawable) {
	    if (cancelPotentialTask(data, imageView)) {
		final AsyncBitmapTask bitmapTask = new AsyncBitmapTask(
			imageView);
		final AsyncDrawable asyncDrawable = new AsyncDrawable(
			resources, mLoadingBitmap, bitmapTask);
		imageView.setImageDrawable(asyncDrawable);
		bitmapTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
			data);
	    }
	}

    }
    
    /**
     * download image by Internet which is implements in subclass 
     * @param data
     * @return
     */
    public abstract Bitmap proccessImage (Object data);
    
    public void addImageCache (FragmentManager fragmentManager, CacheParams cacheParams) {
	mCacheParams = cacheParams;
	mImageCache = ImageCache.getInstance(fragmentManager, cacheParams);
    }
    
    public void setFadeIn (boolean fadeIn) {
	this.fadeIn = fadeIn;
    }
    
    public void setForceExitEarly (boolean exitEarly) {
	this.forceExitEarly = exitEarly;
	setPause (false);
    }
    
    public void setPause (boolean pause) {
	synchronized (mPauseWorkerLock) {
	    this.mPause = pause;
	    if (pause==false) {
		mPauseWorkerLock.notifyAll();
	    }
	}
    }
    
    

    private static boolean cancelPotentialTask(Object data, ImageView imageView) {
	final AsyncBitmapTask bitmapTask = getBitmapTask(imageView);
	if (bitmapTask.data != data) {
	    bitmapTask.cancel(true);
	}
	// else has the same data in bitmapTask
	else {
	    return false;
	}
	return true;
    }

    private static AsyncBitmapTask getBitmapTask(ImageView imageView) {
	if (null != imageView) {
	    final Drawable drawable = imageView.getDrawable();
	    if (drawable instanceof AsyncDrawable) {
		return ((AsyncDrawable) drawable).getBitmapTask();
	    }
	}
	return null;
    }
    
    public void setDrawable (ImageView imageView, BitmapDrawable drawable) {
	
    }

    private class AsyncBitmapTask extends AsyncTask<Object, Void, BitmapDrawable> {
	public Object data;
	private final WeakReference<ImageView> imageViewReference;

	public AsyncBitmapTask(ImageView imageView) {
	    imageViewReference = new WeakReference<ImageView>(imageView);
	}

	@Override
	protected BitmapDrawable doInBackground(Object... params) {
	    data = params[0];
	    String key = String.valueOf(data);
	    Bitmap bitmap = null;
	    BitmapDrawable drawable = null;
	    //judge if force pause if true
	    synchronized (mPauseWorkerLock) {
		if (mPause&&!isCancelled()) {
		    try {
			mPauseWorkerLock.wait();
		    }catch (Exception ex) {}
		}
	    }
	    //search from disk first
	    if (null!=mImageCache&&!isCancelled()&&!forceExitEarly&&getAttachImageView()!=null) {
	    	bitmap = mImageCache.getBitmapFromDisk(key);
	    }
	    if (bitmap==null&&!isCancelled()&&!forceExitEarly&&getAttachImageView()!=null) {
		//processImage implements in subClass link{ImageFecter#processImage}
		bitmap  = proccessImage(params[0]);
	    }
	    if (null!=bitmap) {
		if (Utils.hasHoneyComb()) {
		    drawable = new BitmapDrawable(resources, bitmap);
		}
		else {
		    drawable = new RecyclingBitmapDrawable(resources, bitmap);
		}
		if (mImageCache!=null) {
		    mImageCache.addBitmapToCache(key, drawable);
		}
	    }
	    
	    return drawable;
	}

	@Override
	protected void onPostExecute(BitmapDrawable bitmapDrawable) {
	    if (imageViewReference!=null&&bitmapDrawable!=null) {
		final ImageView imageView = getAttachImageView();
		if (imageView!=null) {
		    setDrawable (imageView, bitmapDrawable);
		}
	    }
	}
	
	private ImageView getAttachImageView () {
	    final ImageView imageView = imageViewReference.get();
	    final AsyncBitmapTask bitmapTask = getBitmapTask(imageView);
	    if (this==bitmapTask) {
		return imageView;
	    }
	    return null;
	}
    }

    /**
     * 
     * this AsyncDrawable holds a bitmapWorkerTask to associate with a imageView
     * 
     */
    public static class AsyncDrawable extends BitmapDrawable {
	private final WeakReference<AsyncBitmapTask> bitmapTaskReference;

	public AsyncDrawable(Resources res, Bitmap bitmap,
		AsyncBitmapTask bitmapTask) {
	    super(res, bitmap);
	    bitmapTaskReference = new WeakReference<AsyncBitmapTask>(bitmapTask);
	}

	public AsyncBitmapTask getBitmapTask() {
	    return bitmapTaskReference.get();
	}
    }

}
