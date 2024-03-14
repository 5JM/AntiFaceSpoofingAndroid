package aero.cubox.icheckerpassive;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class CustomPreview extends FrameLayout {
    //    protected final Paint rectanglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap bitmap = null;
    private int alpha=180;
    private int scaleFactor = 1;
    public CustomPreview(Context context){
        super(context);
        setWillNotDraw(false);
    }

    public CustomPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
//        R.styleable.CustomPreview,

        setWillNotDraw(false);
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension((int) (width * scaleFactor), (int) (height * scaleFactor));
    }
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        bitmap = null;
    }
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (bitmap == null) {
            createWindowFrame();
        }
        canvas.drawBitmap(bitmap, 0f, 0f, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setWillNotDraw(false);
    }

    protected void createWindowFrame(){
        bitmap = Bitmap.createBitmap(
                getWidth(),
                getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas onCanvas = new Canvas(bitmap);
        //배경
        RectF outerRect = new RectF(20f, 20f, (float)getWidth()-20, (float)getHeight()-20);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(Color.WHITE);
        paint.setAlpha(alpha);

        onCanvas.drawRoundRect(outerRect,70f, 70f, paint);

        paint.setColor(Color.TRANSPARENT);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));


        onCanvas.drawRoundRect(new RectF(
                40f, 40f, (float)getWidth()-40, (float)getHeight()-40

        ),70f, 70f,paint);
    }
}

