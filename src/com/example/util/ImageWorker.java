package com.example.util;

import java.lang.ref.WeakReference;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.ImageView;

import com.example.gridimage.RecyclingBitmapDrawable;
import com.example.util.ImageCache.CacheParams;

public abstract class ImageWorker {
    private static final String TAG = "ImageWorker";

    private static final int TRANSITION_DURATION = 200;
    private ImageCache mImageCache;
    private ImageCache.CacheParams mCacheParams;
    private Bitmap mLoadingBitmap;
    private boolean forceExitEarly = false;
    private boolean mPause = false;
    private boolean fadeIn = false;
    private Object mPauseWorkerLock = new Object();
    protected static Resources resources;
    
    private static final int MESSAGR_DISK_INIT = 0;
    private static final int MESSAEGE_DISK_CLEAR = 1;
    private static final int MESSAGE_DISK_FLUSH = 2;
    private static final int MESSAGE_DISK_CLOSE = 3;

    public ImageWorker(Context context) {
	resources = context.getResources();
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
     * download image by Internet which is implements in subclass @link{ImageFecter#proccessImage(Object)}
     * @param data
     * @return
     */
    public abstract Bitmap proccessImage (Object data);
    
    public ImageCache getImageCache () {
	return mImageCache;
    }
    
    /**
     * cancel task which associate with ImageView
     * @param imageView
     */
    protected static void cancelWorker (ImageView imageView) {
	final AsyncBitmapTask bitmapTask = getBitmapTask(imageView);
	if (null!=bitmapTask) {
	    bitmapTask.cancel(true);
	}
    }
    
    public void setBitmap (Bitmap bitmap) {
	this.mLoadingBitmap = bitmap;
    }
    
    public void setBitmap (int resId) {
	mLoadingBitmap = BitmapFactory.decodeResource(resources, resId);
    }
    
    public void addImageCache (FragmentManager fragmentManager, CacheParams cacheParams) {
	mCacheParams = cacheParams;
	mImageCache = ImageCache.getInstance(fragmentManager, cacheParams);
	new AsyncDiskOperate().execute(MESSAGR_DISK_INIT);
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
	if (bitmapTask!=null) {
	    if (bitmapTask.data==null||bitmapTask.data.equals(data)) {
		bitmapTask.cancel(true);
	    }
	    // else has the same data in bitmapTask
	    else {
		return false;
	    }    
	}
	return true;
    }

    /**
     * get AsyncBitmapTask associate with current imageView
     * @param imageView
     * @return
     */
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
	if (fadeIn) {
	TransitionDrawable transition = new TransitionDrawable(new Drawable[]{
		new ColorDrawable(R.color.transparent),drawable});
	imageView.setBackground(new BitmapDrawable(mLoadingBitmap));
	imageView.setImageDrawable(transition);
	transition.startTransition(TRANSITION_DURATION);
	}
	else {
	    imageView.setImageDrawable(drawable);
	    if (BuildDebug.DEBUG) {
		Log.d(TAG, "image set drawable");
	    }
	}
    }

    /**
     * deal with get image asynchronously 
     */
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
			if (BuildDebug.DEBUG) {
			    
			}
			mPauseWorkerLock.wait();
		    }catch (Exception ex) {}
		}
	    }
	    //search from disk first
	    if (null!=mImageCache&&!isCancelled()&&!forceExitEarly&&getAttachImageView()!=null) {
	    	bitmap = mImageCache.getBitmapFromDisk(key);
	    	if (BuildDebug.DEBUG) {
	    	    Log.d(TAG, "get bitmap from disk");
	    	    System.out.println("Bitmap:"+bitmap);
	    	}
	    }
	    if (bitmap==null&&!isCancelled()&&!forceExitEarly&&getAttachImageView()!=null) {
		//processImage implements in subClass link{ImageFecter#processImage}
		bitmap  = proccessImage(params[0]);
		if (BuildDebug.DEBUG) {
		    Log.d(TAG, "get bitmap from network");
		}
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
    
    private class AsyncDiskOperate extends AsyncTask<Integer, Void, Void> {
	
	@Override
	protected Void doInBackground(Integer... params) {
	   int param = params[0];
	   switch (param) {
	   case MESSAGR_DISK_INIT:
	       initDiskInternal();
	       break;
	   case MESSAEGE_DISK_CLEAR:
	       clearInternal();
	       break;
	   case MESSAGE_DISK_FLUSH:
	       flushInternal();
	       break;
	   case MESSAGE_DISK_CLOSE:
	       closeInternal();
	       break;
	   }
	   return null;
	}
    }
    
    public void initDisk () {
	new AsyncDiskOperate().execute(MESSAGR_DISK_INIT);
    }
    
    public void clear () {
	new AsyncDiskOperate().execute(MESSAEGE_DISK_CLEAR);
    }
    
    public void flush () {
	new AsyncDiskOperate().execute(MESSAGE_DISK_FLUSH);
    }
    
    public void close () {
	new AsyncDiskOperate().execute(MESSAGE_DISK_CLOSE);
    }
    
    protected void initDiskInternal () {
	if (null!=mImageCache) {
	    mImageCache.initDisk();
	}
    }
    
    protected void clearInternal () {
	if (null!=mImageCache) {
	    mImageCache.clearCache();
	}
    }
    
    protected void flushInternal () {
	if (null!=mImageCache) {
	    mImageCache.flush();
	}
    }
    
    protected void closeInternal () {
	if (null!=mImageCache) {
	    mImageCache.close();
	}
    }

}
