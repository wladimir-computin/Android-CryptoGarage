package de.wladimircomputin.cryptogarage;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * Created by spamd on 12.03.2017.
 */

public class SquareLayout extends LinearLayout {


    public SquareLayout(Context context) {
        super(context);
    }

    public SquareLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        // this is the scale between width & height, for square should be == 1
        int scale = 1;

        if (width > (int)(scale * height + 0.5)) {
            width = (int)(scale * height + 0.5);
        } else {
            height = (int)(width / scale + 0.5);
        }

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        );
    }
}
