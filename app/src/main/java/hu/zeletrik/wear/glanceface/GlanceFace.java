package hu.zeletrik.wear.glanceface;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class GlanceFace extends CanvasWatchFaceService {


    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    private class Engine extends CanvasWatchFaceService.Engine {

        private long time;
        private SimpleDateFormat dateFormat;
        private SimpleDateFormat timeFormat;
        private TextPaint mTimePaint;
        private TextPaint mDatePaint;
        private Typeface mTypeface;
        private int backgroundColor = Color.BLACK;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            mTypeface = Typeface.createFromAsset(getBaseContext().getAssets(), "fonts/ProductSans-Thin.ttf");


            time = System.currentTimeMillis();

            setWatchFaceStyle(new WatchFaceStyle.Builder(GlanceFace.this)
                    .setAcceptsTapEvents(true)
                    .build());

            // We setup the time and date formatter
            dateFormat = new SimpleDateFormat("EE, MMM dd", Locale.getDefault());
            timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            mTimePaint = new TextPaint();
            mTimePaint.setColor(Color.WHITE);
            mTimePaint.setAntiAlias(true);
            mTimePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics()));
            mTimePaint.setTypeface(mTypeface);


            // The date paint
            mDatePaint = new TextPaint();
            mDatePaint.setColor(Color.WHITE);
            mDatePaint.setAntiAlias(true);
            mDatePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics()));
            mDatePaint.setTypeface(mTypeface);

        }


        @Override
        public void onTimeTick() {
            super.onTimeTick();
            time = System.currentTimeMillis();
            invalidate();
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
                    Toast.makeText(getApplicationContext(), R.string.message, Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            drawWatchFace(canvas, bounds);
        }

        private void drawWatchFace(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            // We draw the watch face here

            Rect timeBounds = new Rect();
            String timeText = timeFormat.format(time);
            int timeX;
            int timeY;

            String dateText = dateFormat.format(time);
            Rect dateBounds = new Rect();
            int dateX;
            int dateY;

            mTimePaint.getTextBounds(timeText, 0, timeText.length(), timeBounds);
            timeX = Math.abs(bounds.centerX() - timeBounds.centerX());
            timeY = Math.round((Math.abs(bounds.centerY())) - (bounds.height() * 0.02f));

            mDatePaint.getTextBounds(dateText, 0, dateText.length(), dateBounds);
            dateX = Math.abs(bounds.centerX() - dateBounds.centerX());
            dateY = Math.round((bounds.centerY() + dateBounds.height()) + (bounds.height() * 0.02f));

            canvas.drawColor(backgroundColor);


            // We draw the date and the time
            canvas.drawText(timeText, timeX, timeY, mTimePaint);
            canvas.drawText(dateText, dateX, dateY, mDatePaint);
        }

    }
}
