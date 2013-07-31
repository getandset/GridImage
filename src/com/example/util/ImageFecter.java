package com.example.util;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

public class ImageFecter extends ImageResize{
    private static final String Tag = "ImageFecter";
    
    private static final String DISK_CACHE_DIR = "http";
    private static final int DISK_CACHE_SIZE = 10*1024*1024;
    private static final int IO_BUFF_SIZE = 2*1024*1024;
    private static final int DISK_CACHE_INDEX = 0;
    private DiskLruCache mDiskLruCache;
    private Object mDiskLock = new Object(); 
    private File httpDiskCache;
    private boolean initDiskStarting = true;
    
    
    public ImageFecter (Context context, int width, int height) {
	super(context, width, height);
	init (context);
    }
    
    public ImageFecter (Context context, int size) {
	super(context, size);
	init (context);
    }
    
    private void init (Context context) {
	checkConnect (context);
	httpDiskCache = ImageCache.getDiskDir(context, DISK_CACHE_DIR);
    }
    
    @Override
    protected void initDiskInternal() {
        super.initDiskInternal();
        synchronized (mDiskLock) {
            if (null!=httpDiskCache&&!httpDiskCache.exists()) {
        	httpDiskCache.mkdir();
            }
            if (null!=httpDiskCache) {
        	try {
        	    mDiskLruCache = DiskLruCache.open(httpDiskCache, 1, 1, DISK_CACHE_SIZE);
        	}catch (IOException ex) {
        	    mDiskLruCache = null;
        	    Log.e(Tag, "IOException in init httpDisk", ex);
        	}
            }
            mDiskLock.notifyAll();
            initDiskStarting = false;
        }
        
    }
    
    @Override
    protected void clearInternal() {
	super.clearInternal();
	synchronized (mDiskLock) {
	    if (mDiskLruCache!=null&&!mDiskLruCache.isClosed()) {
		try {
		    mDiskLruCache.delete();
		}catch (IOException ex) {
		    Log.e(Tag, "Exception in httpDisk cache init", ex);
		}
		mDiskLruCache = null;
		initDiskStarting = true;
		initDiskInternal();
	    }
	}
    }
    
    @Override
    protected void flushInternal() {
        super.flushInternal();
        synchronized (mDiskLock) {
            if (null!=mDiskLruCache&&!mDiskLruCache.isClosed()) {
        	try {
        	    mDiskLruCache.flush();
        	}catch (IOException ex) {
        	    Log.e(Tag, Tag, ex);
        	}
            }
        }
    }
    
    
    @Override
    protected void closeInternal() {
        super.closeInternal();
        synchronized (mDiskLock) {
            if (mDiskLruCache!=null&&!mDiskLruCache.isClosed()) {
        	try {
        	    mDiskLruCache.close();
        	}catch (IOException ex) {
        	    Log.e(Tag, "IOException in http disk cache close", ex);
        	}
            }
        }
    }
    
    
    private boolean checkConnect (Context context) {
	ConnectivityManager connManager = (ConnectivityManager)context.getSystemService
		(Context.CONNECTIVITY_SERVICE);
	NetworkInfo netInfo = connManager.getNetworkInfo(NetworkInfo.CONTENTS_FILE_DESCRIPTOR);
	if (null==netInfo||!netInfo.isConnectedOrConnecting()) {
	    Toast.makeText(context, "network can't connect", Toast.LENGTH_LONG).show();
	    return false;
	}
	return true;
    }
    
    @Override
    public Bitmap proccessImage(Object data) {
	String dataString = String.valueOf(data);
       return proccessImage(dataString);
    }
    
    private Bitmap proccessImage (String dataString) {
	String hashKey = ImageCache.getHashKeyForDisk(dataString);
	synchronized (mDiskLock) {
	    
	}
    }

}
