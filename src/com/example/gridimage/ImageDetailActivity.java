package com.example.gridimage;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import com.example.provider.Images;
import com.example.util.ImageCache;
import com.example.util.ImageCache.CacheParams;
import com.example.util.ImageFecter;
import com.example.util.Utils;

public class ImageDetailActivity extends FragmentActivity implements
	OnClickListener {
    private static final String TAG = "ImageDetailFragment";

    private static final String IMAGE_CACHE_DIR = "img";
    private static final float MEMORY_CACHE_PERCENTAGE = 0.25f;
    public static final String EXTRA_DATA = "image_extra_data";

    private ViewPager mViewPager;
    private ImageAdapter mImageAdater;
    private ImageFecter mImageFecter;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle arg0) {
	super.onCreate(arg0);
	setContentView(R.layout.view_pager);

	// set up {ImageFecter}
	DisplayMetrics metrics = new DisplayMetrics();
	getWindowManager().getDefaultDisplay().getMetrics(metrics);
	int screenWidth = metrics.widthPixels;
	int screenHeight = metrics.heightPixels;
	int longest = (screenHeight > screenWidth ? screenHeight : screenWidth) / 2;
	mImageFecter = new ImageFecter(this, longest);
	ImageCache.CacheParams cacheParams = new CacheParams(this,
		IMAGE_CACHE_DIR);
	cacheParams.setMemorySize(MEMORY_CACHE_PERCENTAGE);
	mImageFecter.addImageCache(getSupportFragmentManager(), cacheParams);
	mImageFecter.setFadeIn(false);
	// set up {ImageAdapter} and configure it
	mViewPager = (ViewPager) findViewById(R.id.pager);
	mImageAdater = new ImageAdapter(getSupportFragmentManager(),
		Images.imageUrls.length);
	mViewPager.setPageMargin(getResources().getDimensionPixelSize(
		R.dimen.pager_margin));
	mViewPager.setOffscreenPageLimit(2);
	mViewPager.setAdapter(mImageAdater);

	// set full screen mode
	getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN);

	if (Utils.hasHoneyComb()) {
	    final ActionBar actionBar = getActionBar();
	    actionBar.setDisplayShowTitleEnabled(false);
	    actionBar.setDisplayHomeAsUpEnabled(true);
	    mViewPager
		    .setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {

			@Override
			public void onSystemUiVisibilityChange(int visibility) {
			    if ((visibility & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
				actionBar.hide();
			    } else {
				actionBar.show();
			    }
			    mViewPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
			    actionBar.hide();
			}

		    });

	}
	final int currentItem = getIntent().getIntExtra(EXTRA_DATA, -1);
	if (currentItem != -1) {
	    mViewPager.setCurrentItem(currentItem);
	}

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	getMenuInflater().inflate(R.menu.main, menu);
	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case android.R.id.home:
	    NavUtils.navigateUpFromSameTask(getParent());
	    return true;
	case R.id.clear_cache:
	    mImageFecter.clear();
	    Toast.makeText(this, "cache has clear", Toast.LENGTH_LONG).show();
	    return true;
	}
	return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
	super.onResume();
	mImageFecter.setForceExitEarly(false);
    }

    @Override
    protected void onPause() {
	super.onPause();
	mImageFecter.setForceExitEarly(true);
	mImageFecter.flush();
    }
    

    @Override
    protected void onDestroy() {
	super.onDestroy();
	mImageFecter.close();
    }

    // get {ImageFecter} use in {ImageDetailFragment}
    public ImageFecter getImageFecter() {
	return mImageFecter;
    }

    public static class ImageAdapter extends FragmentStatePagerAdapter {

	private int size;

	public ImageAdapter(FragmentManager fm, int size) {
	    super(fm);
	    this.size = size;
	}

	@Override
	public Fragment getItem(int position) {
	    return ImageDetailFragment.getImageDetailFragment(position);
	}

	@Override
	public int getCount() {
	    return size;
	}

    }
    
    @Override
    public void onClick(View v) {
        final int vis = v.getSystemUiVisibility();
        if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE)!=0) {
            mViewPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }else {
            mViewPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
        
    }

}
