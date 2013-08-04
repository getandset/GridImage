package com.example.gridimage;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

public class MainActivity extends FragmentActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_main);
	if (getSupportFragmentManager().findFragmentByTag(TAG) == null) {
	    GridImageFragment fragment = new GridImageFragment();
	    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
	    transaction.add(android.R.id.content, fragment, TAG);
	    transaction.commit();
	}

    }

}
