package com.assessment.flow.workflowengine.spinner;

import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;

public abstract class SpinnerInteractionListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener {

    boolean userSelect = false;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        userSelect = true;
        return false;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        if (userSelect) {
            // Your selection handling code here
            onItemSelected(pos, view);
            userSelect = false;
        }
    }


    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    public abstract void onItemSelected(int position, View view);
}