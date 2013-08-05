package com.example.gridimage;

import com.example.provider.Images;
import com.example.util.ImageFecter;
import com.example.util.ImageWorker;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ImageDetailFragment extends Fragment{
    private static final String TAG = "ImageDetailFragment";
    
    private static final String IMAGE_DATA_EXREA = "image_id";
    private ImageView mImageView;
    private int mImageId;
    
    public static Fragment getImageDetailFragment (int id) {
	Bundle arg = new Bundle();
	arg.putInt(IMAGE_DATA_EXREA, id);
	ImageDetailFragment f = new ImageDetailFragment();
	f.setArguments(arg);
	return f;
    }
    
    public ImageDetailFragment () {}
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int resId = getArguments().getInt(IMAGE_DATA_EXREA, -1);
        if (resId!=-1) {
            mImageId = getArguments().getInt(IMAGE_DATA_EXREA, -1);
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
       View v = inflater.inflate(R.layout.image_detail_fregment, container, false);
       mImageView = (RecyclingImageView)v.findViewById(R.id.dedailImageView);
       return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String url = Images.imageUrls[mImageId];
        if (getActivity() instanceof ImageDetailActivity) {
            ImageFecter imageFecter = ((ImageDetailActivity)getActivity()).getImageFecter();
            imageFecter.loadImage(url, mImageView);
        }
    }
    
    @Override
    public void onDestroy() {
	super.onDestroy();
        ImageWorker.cancelWorker(mImageView);
        mImageView.setImageDrawable(null);
    }

}
