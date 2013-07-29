package com.example.gridimage;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class RecyclingImageView extends ImageView {
	
	public RecyclingImageView (Context context) {
		super(context);
	}
	
	public RecyclingImageView (Context context, AttributeSet attr) {
		super(context, attr);
	}
	
	@Override
	protected void onDetachedFromWindow() {
		setImageDrawable(null);
		super.onDetachedFromWindow();
	}
	
	@Override
	public void setImageDrawable(Drawable drawable) {
		final Drawable previous = getDrawable();
		super.setImageDrawable(drawable);
		notifyDisplay(drawable, true);
		notifyDisplay(previous, false);
	}
	
	public void notifyDisplay (Drawable drawable, boolean isDisplay) {
		if (drawable instanceof RecyclingBitmapDrawable) {
			((RecyclingBitmapDrawable) drawable).display(isDisplay);
		}
		else if (drawable instanceof LayerDrawable) {
			for (int i=0; i<((LayerDrawable)drawable).getNumberOfLayers();i++) {
				notifyDisplay(((LayerDrawable) drawable).getDrawable(i), isDisplay);
			}
		}
	}

}
