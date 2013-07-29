package com.example.util;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.security.MessageDigest;
import java.util.HashSet;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.webkit.WebView.FindListener;

import com.example.gridimage.RecyclingBitmapDrawable;

public class ImageCache {
	
	private static final String TAG = "ImageCache";
	
	private static final int DEFAULT_MEN_CACHE = 4*1024; //default memory cache size in kiloByte
	private static final int DEFAULT_DISK_CACHE = 10*1024*1024; //default disk cache size
	
	//define easy toggle to change state 
	private static final boolean DEFAULT_MEN_CACHE_ENABLE = true;
	private static final boolean DEFAULT_DISK_CACHE_ENABLE = true;
	private boolean diskInitStarting = true;
	private static final boolean DEFAULT_DISK_INIT_CREAT = false;
	
	//compress file while write to disk
	private static final int DISK_INDEX = 0;
	private static final CompressFormat DEFAULT_COMPRESS = CompressFormat.JPEG;
	private static final int PICTURE_QUALITY = 70;
	
	private final Object mDiskCacheLock = new Object();
	
	private LruCache<String, BitmapDrawable> memCache;
	private DiskLruCache diskCache;
	private CacheParams cacheParams;
	
	private HashSet<SoftReference<Bitmap>> reusableBitmap;
	
	/**
	 * this constructor can't be instance directly,use getInstance to the object of this 
	 */
	private ImageCache (CacheParams cacheParams) {
		init (cacheParams);
	}
	
	public static ImageCache getInstance (FragmentManager fm, CacheParams cacheParams) {
		RetainFragment retainFragment = findOrCreateFramgment(fm);
		ImageCache imageCache = null;
		if (retainFragment.getObject()==null) {
			imageCache = new ImageCache(cacheParams);
			retainFragment.setObject(imageCache);
		}
		else {
			imageCache = (ImageCache)retainFragment.getObject();
		}
		return imageCache;
	}
	
	private static RetainFragment findOrCreateFramgment (FragmentManager fragmentManager) {
		RetainFragment fragment = null;
		if (fragmentManager.findFragmentByTag(TAG)==null) {
			fragment = new RetainFragment();
			FragmentTransaction ft = fragmentManager.beginTransaction();
			ft.add(fragment, TAG);
		}
		else {
			fragment = (RetainFragment)fragmentManager.findFragmentByTag(TAG);
		}
		return fragment;
	}
	
	private void init (CacheParams cacheParams) {
		this.cacheParams = cacheParams;
		if (cacheParams.memCacheEnable) {
			memCache = new LruCache<String, BitmapDrawable>(cacheParams.memCache){
				
				@Override
				protected void entryRemoved(boolean evicted, String key,
						BitmapDrawable oldValue, BitmapDrawable newValue) {
					if (oldValue instanceof RecyclingBitmapDrawable) {
						((RecyclingBitmapDrawable)oldValue).cache(false);
					}
					else {
						if (Utils.hasHoneyComb()) {
							reusableBitmap = new HashSet<SoftReference<Bitmap>>();
							reusableBitmap.add(new SoftReference<Bitmap>(oldValue.getBitmap()));
						}
					}
				}
				
				@Override
				protected int sizeOf(String key, BitmapDrawable value) {
					int bitmapSize = value.getBitmap().getByteCount();
					return bitmapSize==0? 1:bitmapSize;
				}
			};
		}
		if (cacheParams.diskInitOnCreate) {
			initDisk();
		}
	}
	
	private void initDisk () {
		synchronized (mDiskCacheLock) {
			if (diskCache==null||diskCache.isClosed()) {
				File diskCacheDir = cacheParams.diskCacheDir;
				if (cacheParams.diskCacheEnable&&diskCacheDir!=null) {
					if (!diskCacheDir.exists()) {
						diskCacheDir.mkdir();
					}
					if (getUsableSpace(diskCacheDir)>cacheParams.diskCache) {
						try {
							diskCache = DiskLruCache.open(diskCacheDir, 1,1, cacheParams.diskCache);
							
						}catch (IOException ex) {
							cacheParams = null;
							Log.e(TAG, "IOException in initDisk", ex);
						}
					}
				}
			}
			diskInitStarting = false;
			mDiskCacheLock.notifyAll();
		}
	}
	
	/**
	 * add bitmap to memory cache and disk cache
	 * @param key identifier for a bitmap to store in cache
	 * @param value bitmap to be stored in cache
	 */
	public void addBitmapToCache (String key, BitmapDrawable value) {
		if (key==null||value==null) {
			return;
		}
		if (memCache!=null) {
			if (value instanceof RecyclingBitmapDrawable) {
				((RecyclingBitmapDrawable)value).cache(true);
			}
			memCache.put(key, value);
		}
		
		//add bitmap to disk cache
		synchronized (mDiskCacheLock) {
			OutputStream out = null;
			String hashKey = getHashKeyForDisk(key);
			if (null!=diskCache&&!diskCache.isClosed()) {
				try {
					DiskLruCache.Snapshot snapshot = diskCache.get(hashKey);
					if (null==snapshot) {
						DiskLruCache.Editor editor = diskCache.edit(hashKey);
						if (null!=editor) {
							out = editor.newOutputStream(DISK_INDEX);
							value.getBitmap().compress(DEFAULT_COMPRESS, PICTURE_QUALITY, out);
							editor.commit();
						}
					}
					else {
						snapshot.getInputStream(DISK_INDEX);
					}
				}catch (IOException ex) {
					Log.e(TAG, "IOException in addDiskCache", ex);
				}
			}
		}
	}
	
	/**
	 * get the bitmap from memory
	 * @param key
	 * @return bitmap
	 */
	public BitmapDrawable getBitmapFromMem (String key) {
		BitmapDrawable bitmapDrawable = null;
		if (memCache!=null) {
			bitmapDrawable = memCache.get(key);
		}
		return bitmapDrawable;
	}
	
	/**
	 * get the bitmap from disk by an identifier
	 * @param data
	 * @return
	 */
	public Bitmap getBitmapFromDisk (String data) {
		String key = getHashKeyForDisk(data);
		Bitmap bitmap = null;
		
		synchronized (mDiskCacheLock) {
			if (diskInitStarting) {
				try {
					//if diskInitStarting is true,thread wait
					mDiskCacheLock.wait();
				}catch (Exception ex) {}
			}
			if (null!=diskCache&&!diskCache.isClosed()) {
				try {
					DiskLruCache.Snapshot snapshot = diskCache.get(key);
					if (null!=snapshot) {
						InputStream inputStream = snapshot.getInputStream(DISK_INDEX);
						if (null!=inputStream) {
							FileDescriptor fileDescriptor = ((FileInputStream)inputStream).getFD();
							
						}
					}
				}catch(IOException ex) {
					Log.e(TAG, "IOException in getBitmapFromDisk", ex);
				}
			}
		}
		return bitmap;
		
	}
	
	/**
	 * clear both memory and disk cache
	 */
	public void clearCache () {
		if (null!=memCache) {
			memCache.evictAll();
		}
		synchronized (mDiskCacheLock) {
			diskInitStarting = true;
			if (null!=diskCache&&!diskCache.isClosed()) {
				try {
					diskCache.delete();
				}catch (IOException ex) {
					Log.e(TAG, "IOException in clear disk cache", ex);
				}
			}
		}
		diskCache = null;
		initDisk();
	}
	
	/**
	 * flush cache in disk
	 */
	public void flush () {
		synchronized (mDiskCacheLock) {
			if (null!=diskCache) {
				try {
					diskCache.flush();
				}catch (IOException ex) {
					Log.e(TAG, "IOExption in disk flush", ex);
				}
			}
			
		}
	}
	
	/**
	 * close disk cache
	 */
	public void close () {
		synchronized (mDiskCacheLock) {
			if (null!=diskCache&&!diskCache.isClosed()) {
				try {
					diskCache.close();
				}catch (IOException ex) {
					Log.e(TAG, "IOException in disk close", ex);
				}
			}
		}
	}
	
	public static class CacheParams {
		
		public int memCache = DEFAULT_MEN_CACHE;
		public int diskCache = DEFAULT_DISK_CACHE;
		public boolean memCacheEnable = DEFAULT_MEN_CACHE_ENABLE;
		public boolean diskCacheEnable = DEFAULT_DISK_CACHE_ENABLE;
		public boolean diskInitOnCreate = DEFAULT_DISK_INIT_CREAT;
		
		public CompressFormat compressFormat = CompressFormat.JPEG;
		public int pictureQuality = PICTURE_QUALITY;
		public int diskCacheIndex = DISK_INDEX;
		public File diskCacheDir;
		
		public CacheParams (Context context, String fileDir) {
			diskCacheDir = getDiskDir(context, fileDir);
		}
		
		public void setMemorySize (float percente) {
			if (percente<0.01||percente>0.8) {
				throw new IllegalArgumentException("memmory error Exception");
			}
			memCache = Math.round(Runtime.getRuntime().maxMemory()/1024);
		}
	}
	
	public static File getDiskDir (Context context, String fileName) {
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())||
				!Environment.isExternalStorageRemovable()) {
			final String path = getExternalCacheDir(context, fileName).getPath();
			return new File (path+File.separator+fileName);
		}
		else {
			final String path = context.getCacheDir().getPath();
			return new File (path+File.separator+fileName);
		}
	}
	
	public static File getExternalCacheDir (Context context, String fileName) {
		if (Utils.hasFroyo()) {
			return context.getCacheDir();
		}
		//if Android sdk before froyo we need to construct cache dir by ourselves
		String cacheDir = "android/data/"+context.getPackageName()+"/cache";
		return new File(Environment.getExternalStorageDirectory()+cacheDir);
	}
	
	public long getUsableSpace (File diskCache) {
		if (Utils.hasGinerbread()) {
			return diskCache.getUsableSpace();
		}
		StatFs statFs = new StatFs(diskCache.getPath());
		return (long)statFs.getBlockSize()*(long)statFs.getFreeBlocks();
	}
	
	/**
	 * 
	 * get hash key for disk cache
	 * @param key
	 * @return a hash key
	 */
	public static String getHashKeyForDisk (String key) {
		String hashKey = null;
		try {
			final MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(key.getBytes());
			hashKey = getHexForHashKey (digest.digest());
		}catch (Exception ex) {
			hashKey = String.valueOf(key.hashCode());
			Log.e(TAG, "Exception in getHashKeyForDisk", ex);
		}
		return hashKey;
	}
	
	/**
	 * covert bytes to hex string
	 * @param bytes
	 * @return 
	 */
	private static String getHexForHashKey (byte[] bytes) {
		StringBuilder builder = new StringBuilder();
		for (int i=0; i<bytes.length; i++) {
			String s = Integer.toHexString(0xFF&bytes[i]);
			if (s.length()==1) {
				builder.append(0);
			}
			builder.append(s);
		}
		return builder.toString();
	}
	
	public static class RetainFragment extends Fragment {
		private Object object;
		
		@Override
		public void onCreate(Bundle savedInstanceState) {			
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
		}
		
		public void setObject (Object object) {
			this.object = object;
		}
		
		public Object getObject () {
			return this.object;
		}
	}
	

}
