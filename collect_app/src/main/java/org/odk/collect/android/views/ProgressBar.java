package org.odk.collect.android.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import org.odk.collect.android.R;

public class ProgressBar extends FrameLayout {

    private static final int DURATION_MILLIS = 1000;

    View divider;
    FrameLayout mainLayout;

    View progressBar;

    public ProgressBar(Context context) {
        this(context, null);
    }

    public ProgressBar(@NonNull Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressBar(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (!isInEditMode()) {
            init();
        }
    }

    private void init() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.progress_bar_view, this, true);
        progressBar = view.findViewById(R.id.progress_view);
        divider = view.findViewById(R.id.divider);
        mainLayout = view.findViewById(R.id.main_layout);
    }

    public void setProgressPercent(int progress, boolean animate) {
        mainLayout.post(() -> {
            int progressWidth = (progress / 100) * divider.getMeasuredWidth();

            if (animate) {
                ValueAnimator anim = ValueAnimator.ofInt(progressBar.getMeasuredWidth(), progressWidth);
                anim.addUpdateListener(valueAnimator -> {
                    int val = (Integer) valueAnimator.getAnimatedValue();
                    ViewGroup.LayoutParams layoutParams = progressBar.getLayoutParams();
                    layoutParams.width = val;
                    progressBar.setLayoutParams(layoutParams);
                });
                anim.setDuration(DURATION_MILLIS);
                anim.start();
            } else {
                ViewGroup.LayoutParams layoutParams = progressBar.getLayoutParams();
                layoutParams.width = progressWidth;
                progressBar.setLayoutParams(layoutParams);
            }
        });
    }
}
