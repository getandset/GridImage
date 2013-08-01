package com.example.gridimage;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
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
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridLayout.LayoutParams;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.example.provider.Images;
import com.example.util.BuildDebug;
import com.example.util.ImageCache;
import com.example.util.ImageFecter;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class GridImageFragment extends Fragment implements OnItemClickListener {

    private static final String TAG = "GridImageFragment";
    private static final String DISK_CACHE_DIR = "thuml";
    private static final float MEMORY_CACHE_PERCENTEGE = 0.5f;
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
			if (mAdapter.getCount() == 0) {
			    final int colunmNum = (int) Math.floor(gridView
				    .getWidth()
				    / (thumlnailSize + thumlNailSpacing));
			    if (colunmNum > 0) {
				final int width = (gridView.getWidth() - thumlNailSpacing)
					/ colunmNum;
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
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        imageFecter.close();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	// TODO Auto-generated method stub
	super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	// TODO Auto-generated method stub
	return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
	    long id) {
	// TODO Auto-generated method stub

    }

    public class ImageGridAdapter extends BaseAdapter {

	private int colunmNums;
	private int height;
	private GridView.LayoutParams imageViewParams;
	private Context context;
	private int actionBarHeight;

	public ImageGridAdapter(Context context) {
	    super();
	    this.context = context;
	    imageViewParams = new GridView.LayoutParams(
		    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	    TypedValue value = new TypedValue();
	    if (context.getTheme().resolveAttribute(
		    android.R.attr.actionBarSize, value, true)) {
		actionBarHeight = TypedValue.complexToDimensionPixelOffset(
			value.data, context.getResources().getDisplayMetrics());
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
	    return position < colunmNums ? -1 : position - colunmNums;
	}

	@Override
	public int getViewTypeCount() {
	    return 2;
	}

	@Override
	public int getItemViewType(int position) {
	    return position < colunmNums ? 0 : 1;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	    if (position < colunmNums) {
		if (convertView == null) {
		    convertView = new ImageView(context);
		}
		convertView.setLayoutParams(new AbsListView.LayoutParams(
			android.widget.AbsListView.LayoutParams.MATCH_PARENT,
			actionBarHeight));
		return convertView;
	    }

	    ImageView imageView;
	    if (convertView == null) {
		imageView = new RecyclingImageView(context);
		imageView.setLayoutParams(imageViewParams);
		imageView.setScaleType(ScaleType.CENTER);
	    }
	    else {
		imageView = (RecyclingImageView) convertView;		
	    }
            imageFecter.loadImage(Images.imageThumbUrls[position-colunmNums], imageView);
	    return imageView;
	}

	public void setColunmNums(int colunmNum) {
	    this.colunmNums = colunmNum;
	}

	public int getColunmNums() {
	    return colunmNums;
	}

	public void setColunmHeight(int height) {
	    this.height = height;
	}

	public int getColunmHeight() {
	    return height;
	}

    }

}
