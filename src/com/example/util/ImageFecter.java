package com.example.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
	FileInputStream fileInputStream = null;
	FileDescriptor fileDescriptor = null;
	synchronized (mDiskLock) {
	    if (initDiskStarting) {
		try {
		    mDiskLock.wait();
		}catch (InterruptedException ex) {}
	    }
	    if (null!=mDiskLruCache&&!mDiskLruCache.isClosed()) {
		try {
		    DiskLruCache.Snapshot snapshot = mDiskLruCache.get(hashKey);
		    if (snapshot==null) {
			DiskLruCache.Editor editor = mDiskLruCache.edit(hashKey);
			if (null!=editor) {
			    if (loadImageFromURL(dataString, editor.newOutputStream(DISK_CACHE_INDEX))){
				editor.commit();				
			    }
			    else {
				editor.abort();
			    }
			}
			snapshot = mDiskLruCache.get(hashKey);
			if (snapshot!=null) {
			    fileInputStream = (FileInputStream)snapshot.getInputStream(DISK_CACHE_INDEX);
			    fileDescriptor = fileInputStream.getFD();
			}
		    }
		}catch (IOException ex){
		    
		    Log.e(Tag, "IOException in proccessImage", ex);
		}finally {
		    if (fileDescriptor==null&&fileInputStream!=null) {
			try{
			    fileInputStream.close();
			}catch (IOException ex){}
		    }
		}
	    }
	}
	Bitmap bitmap = null;
	if (fileDescriptor!=null) {
	    bitmap = decodeSanpleBitmapFromFileDecriptor(fileDescriptor, getWidth(), getHeight(), getImageCache());
	}
	if (fileInputStream!=null) {
	    try {
		fileInputStream.close();
	    }catch (IOException ex) {
		ex.printStackTrace();
	    }
	}
	return bitmap;
    }
    
    private boolean loadImageFromURL (String urlString, OutputStream outputStream) {
	HttpURLConnection conn = null;
	BufferedOutputStream bufferedOutputStream = null;
	BufferedInputStream bufferedInputStream = null;
	try {
	    URL url = new URL(urlString);
	    conn = (HttpURLConnection)url.openConnection();
	    bufferedInputStream = new BufferedInputStream(conn.getInputStream(), IO_BUFF_SIZE);
	    bufferedOutputStream = new BufferedOutputStream(outputStream, IO_BUFF_SIZE);
	    
	    int b;
	    while ((b=bufferedInputStream.read())!=-1) {
		bufferedOutputStream.write(b);
	    }
	    return true;
	}catch (IOException ex) {
	    Log.e(Tag, "in loadImageFromURL", ex);
	}finally {
	    try {
		if (conn!=null) {
		    conn.disconnect();
		}
		if (bufferedOutputStream!=null) {
		    bufferedOutputStream.close();
		}
		if (bufferedInputStream!=null) {
		    bufferedInputStream.close();
		}
	    }catch (Exception ex) {
		Log.e(Tag, "in loadImageFromURL", ex);
	    }
	}
	return false;
    }

}
