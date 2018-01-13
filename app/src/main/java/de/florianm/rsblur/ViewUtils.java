package de.florianm.rsblur;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

/**
 * Created by florian on 2018-01-13.
 */

public class ViewUtils {
    public static int dpToPx(float dp, Context context) {
        return dpToPx(dp, context.getResources());
    }

    public static int dpToPx(float dp, Resources resources) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
        return (int) px;
    }
}
