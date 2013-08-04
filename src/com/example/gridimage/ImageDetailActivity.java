package com.example.gridimage;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;

import com.example.provider.Images;
import com.example.util.ImageFecter;

public class ImageDetailActivity extends FragmentActivity {
    private static final String TAG = "ImageDetailFragment";
    
    private ViewPager mViewPager;
    private ImageAdapter mImageAdater;
    private ImageFecter mImageFecter;
    
    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.view_pager);
        mViewPager = (ViewPager)findViewById(R.id.pager);
        mImageAdater = new ImageAdapter(getSupportFragmentManager(), Images.imageUrls.length);
        mViewPager.setAdapter(mImageAdater);
        
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        int longest = (screenHeight>screenWidth? screenHeight:screenWidth)/2;
        
    }
    
    public static class ImageAdapter extends FragmentStatePagerAdapter {
	
	private int size;

	public ImageAdapter(FragmentManager fm, int size) {
	    super(fm);
	    this.size =size;
	}
	

	@Override
	public Fragment getItem(int arg0) {
	    // TODO Auto-generated method stub
	    return null;
	}

	@Override
	public int getCount() {
	   return size;
	}
	
	
    }

}
