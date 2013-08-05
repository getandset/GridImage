package com.example.gridimage;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

import com.example.provider.Images;
import com.example.util.BuildDebug;
import com.example.util.ImageCache;
import com.example.util.ImageFecter;

public class GridImageFragment extends Fragment implements OnItemClickListener {

    private static final String TAG = "GridImageFragment";
    private static final String DISK_CACHE_DIR = "thuml";
    private static final float MEMORY_CACHE_PERCENTEGE = 0.3f;
    private int thumlnailSize;
    private int thumlNailSpacing;
    private ImageGridAdapter mAdapter;
    private ImageFecter imageFecter;
    private ImageCache.CacheParams cacheParams;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	if (BuildDebug.DEBUG) {
	    Log.d(TAG, "GridFragment create");
	}
	
	super.onCreate(savedInstanceState);
	thumlnailSize = getResources().getDimensionPixelSize(
		R.dimen.image_thumbnail_size);
	thumlNailSpacing = getResources().getDimensionPixelOffset(
		R.dimen.image_thumbnail_spacing);
	mAdapter = new ImageGridAdapter(getActivity());
	cacheParams = new ImageCache.CacheParams(getActivity(), DISK_CACHE_DIR);
	cacheParams.setMemorySize(MEMORY_CACHE_PERCENTEGE);
	imageFecter = new ImageFecter(getActivity(), thumlnailSize);
	imageFecter.setBitmap(R.drawable.empty_photo);
	imageFecter.addImageCache(getFragmentManager(), cacheParams);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
	    Bundle savedInstanceState) {
	if (BuildDebug.DEBUG) {
	    Log.d(TAG, "GridFragment onCreate View befor inflate");
	}
	final View view = inflater.inflate(R.layout.grid_fragment, container, false);
	if (BuildDebug.DEBUG) {
	    Log.d(TAG, "after infalte");
	}
	final GridView gridView = (GridView) view
		.findViewById(R.id.gridImageView);
	gridView.setOnItemClickListener(this);
	gridView.setAdapter(mAdapter);
	gridView.setOnScrollListener(new OnScrollListener() {

	    @Override
	    public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollState==AbsListView.OnScrollListener.SCROLL_STATE_FLING){
		    imageFecter.setPause(true);
		}else {
		    imageFecter.setPause(false);
		}

	    }

	    @Override
	    public void onScroll(AbsListView view, int firstVisibleItem,
		    int visibleItemCount, int totalItemCount) {}
	});

	gridView.getViewTreeObserver().addOnGlobalLayoutListener(
		new OnGlobalLayoutListener() {
		    @Override
		    public void onGlobalLayout() {
			if (BuildDebug.DEBUG) {
			    Log.d(TAG, "onGlobalLayout invoked");
			}
			if (mAdapter.getColunmNums() == 0) {
			    final int colunmNum = (int) Math.floor(gridView .getWidth()
				    / (thumlnailSize + thumlNailSpacing));
			    if (colunmNum > 0) {
				if (BuildDebug.DEBUG) {
				    Log.d(TAG, "set adapter colunms and height");
				}
				final int width = (gridView.getWidth()/colunmNum)-thumlNailSpacing;
				mAdapter.setColunmNums(colunmNum);
				mAdapter.setColunmHeight(width);
			    }
			}
		    }
		});

	return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        imageFecter.setForceExitEarly(false);
        mAdapter.notifyDataSetChanged();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        imageFecter.setFadeIn(true);
        imageFecter.setPause(true);
        imageFecter.flush();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        imageFecter.close();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	inflater.inflate(R.menu.main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case R.id.clear_cache:
	    imageFecter.clear();
	    Toast.makeText(getActivity(), "has clear image cache", Toast.LENGTH_LONG).show();
	}
	return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
	    long id) {
	Intent intent = new Intent(getActivity(), ImageDetailActivity.class);
	intent.putExtra(ImageDetailActivity.EXTRA_DATA, (int) id);
        startActivity(intent);
    }

    public class ImageGridAdapter extends BaseAdapter {

	private int colunmNums;
	private int height;
	private GridView.LayoutParams imageViewParams;
	private final Context context;
	private int actionBarHeight;

	public ImageGridAdapter(Context context) {
	    super();
	    this.context = context;
	    imageViewParams = new GridView.LayoutParams(
		    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
	    TypedValue value = new TypedValue();
	    if (context.getTheme().resolveAttribute(
		    android.R.attr.actionBarSize, value, true)) {
		actionBarHeight = TypedValue.complexToDimensionPixelOffset(
			value.data, context.getResources().getDisplayMetrics());
		if (BuildDebug.DEBUG) {
		    Log.d(TAG, "actioBarHeight: "+String.valueOf(actionBarHeight));
		}
	    }

	}

	/**
	 * return the num of Item in the gridView,which is the length of
	 * {Images#imageThumbUrls} + colunmNums
	 * 
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
	    return Images.imageThumbUrls.length + colunmNums;
	}

	@Override
	public Object getItem(int position) {
	    if (position < colunmNums) {
		return null;
	    }
	    return Images.imageThumbUrls[position - colunmNums];
	}

	@Override
	public long getItemId(int position) {
	    return position < colunmNums ? 0 : position - colunmNums;
	}

	@Override
	public int getViewTypeCount() {
	    return 2;
	}

	@Override
	public int getItemViewType(int position) {
	    return position < colunmNums ? 1 : 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	    if (position < colunmNums) {
		if (BuildDebug.DEBUG) {
		    Log.d(TAG, "colunms: "+colunmNums);
		}
		if (convertView == null) {
		    convertView = new View(context);
		}
		convertView.setLayoutParams(new AbsListView.LayoutParams(
			android.widget.AbsListView.LayoutParams.MATCH_PARENT,
			actionBarHeight));
		return convertView;
	    }

	    ImageView imageView;
	    if (convertView == null) {
		imageView = new RecyclingImageView(context);
		imageView.setScaleType(ScaleType.CENTER_CROP);
		imageView.setLayoutParams(imageViewParams);
	    }
	    else {
		imageView = (RecyclingImageView) convertView;		
	    }
	    if (imageView.getLayoutParams().height!=height) {
		imageView.setLayoutParams(imageViewParams);
	    }
            imageFecter.loadImage(Images.imageThumbUrls[position-colunmNums], imageView);
	    return imageView;
	}
	
	@Override
	public boolean hasStableIds() {
	    return true;
	}

	public void setColunmNums(int colunmNum) {
	    this.colunmNums = colunmNum;
	}

	public int getColunmNums() {
	    return colunmNums;
	}

	public void setColunmHeight(int height) {
	    if (this.height==height) {
		return;
	    }
	    this.height = height;
	    imageViewParams = new GridView.LayoutParams (LayoutParams.MATCH_PARENT,height);
	    imageFecter.setImageSize(height);
	    notifyDataSetChanged();
	}
	

	public int getColunmHeight() {
	    return height;
	}

    }

}
