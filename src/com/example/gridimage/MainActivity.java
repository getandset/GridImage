package com.example.gridimage;

import android.os.Bundle;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.view.Menu;

public class MainActivity extends Activity {
	
	private static final String TAG = "MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getFragmentManager().findFragmentByTag(TAG)==null) {
			GridImageFragment fragment = new GridImageFragment();
			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			transaction.add(fragment, TAG);
			transaction.commit();
		}
		
	}

}
