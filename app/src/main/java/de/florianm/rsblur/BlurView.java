package de.florianm.rsblur;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class BlurView extends View {

    public static final String TAG = BlurView.class.getSimpleName();
    public static final float MAX_ELEVATION_DP = 25.0f;


    private float borderRadius = 0.0f;
    private float shadowBlurRadius = 0.0f;

    private Paint shadowPaint = new Paint(ANTI_ALIAS_FLAG);
    private Paint figurePaint = new Paint(ANTI_ALIAS_FLAG);
    private Path figurePath = new Path();
    private Bitmap shadowBitmap;

    private boolean dirty;


    public BlurView(Context context) {
        super(context);
        init(null);
    }

    public BlurView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public BlurView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (null == attrs) {
            return;
        }

        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.BlurView);
        borderRadius = typedArray.getDimensionPixelSize(R.styleable.BlurView_bvBorderRadius, 0);

        float elevation = 0.0f;
        if(typedArray.hasValue(R.styleable.BlurView_bvElevation)){
            elevation = typedArray.getDimensionPixelSize(R.styleable.BlurView_bvElevation, 0);
        } else {
            elevation = typedArray.getDimensionPixelSize(R.styleable.BlurView_android_elevation, 0);
        }

        if(0 < elevation){
            setShadowBlurRadius(elevation);
        }

        typedArray.recycle();

        Log.d(TAG, "borderRadius=" + borderRadius);
        Log.d(TAG, "shadowBlurRadius=" + shadowBlurRadius);

        shadowPaint.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN));
        shadowPaint.setAlpha(51);

        initElements();

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private void initElements() {

        if (null == figurePaint) {
            figurePaint = new Paint();
        }

        figurePaint.setAlpha(0);
        figurePaint.setAntiAlias(true);
        figurePaint.setColor(Color.WHITE);
        figurePaint.setStyle(Paint.Style.FILL);

        dirty = true;
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dirty) {
            doLayout();
        }

        if (0 < shadowBlurRadius) {
            canvas.drawBitmap(shadowBitmap, 0, shadowBlurRadius / 2.0f, null);
        }

        canvas.drawPath(figurePath, figurePaint);
    }

    private void doLayout() {
        Log.d(TAG, "doLayout()");

        float left = getPaddingLeft() + shadowBlurRadius;
        float top = getPaddingTop() + shadowBlurRadius;
        float right = getWidth() - getPaddingRight() - shadowBlurRadius;
        float bottom = getHeight() - getPaddingBottom() - shadowBlurRadius;
        figurePath.reset();
        figurePath.addRoundRect(left, top, right, bottom,
                borderRadius, borderRadius,
                Path.Direction.CW);

        generateShadow();

        dirty = false;
    }

    private void generateShadow() {
        if (0 == shadowBlurRadius) return;

        if (null == shadowBitmap) {
            shadowBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ALPHA_8);
        } else {
            shadowBitmap.eraseColor(Color.TRANSPARENT);
        }

        Canvas canvas = new Canvas(shadowBitmap);
        canvas.drawPath(figurePath, shadowPaint);

        RenderScript rs = RenderScript.create(getContext());
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8(rs));
        Allocation input = Allocation.createFromBitmap(rs, shadowBitmap);
        Allocation output = Allocation.createTyped(rs, input.getType());
        blurScript.setRadius(shadowBlurRadius);
        blurScript.setInput(input);
        blurScript.forEach(output);
        output.copyTo(shadowBitmap);
        input.destroy();
        output.destroy();
    }

    public float getBorderRadius() {
        return borderRadius;
    }

    public void setBorderRadius(float borderRadius) {
        this.borderRadius = borderRadius;
        initElements();
    }

    public void setElevation(float elevation) {
        setShadowBlurRadius(elevation);
        initElements();
    }

    private void setShadowBlurRadius(float elevation) {

        float maxElevationPx = ViewUtils.dpToPx(MAX_ELEVATION_DP, getContext());
        shadowBlurRadius = Math.min(MAX_ELEVATION_DP * (elevation / maxElevationPx), MAX_ELEVATION_DP);
    }
}
