package org.mariotaku.twidere.view;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageButton;
import android.widget.FrameLayout;

public class ExtendedTouchView extends FrameLayout {
    private final AppCompatImageButton imageButton;
    
    public ExtendedTouchView(Context context) {
        this(context, null);
    }

    public ExtendedTouchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExtendedTouchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        imageButton = new AppCompatImageButton(context, attrs, defStyleAttr);
        addView(imageButton, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
    }
    
    public AppCompatImageButton getImageButton() {
        return imageButton;
    }
}
