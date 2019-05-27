package hu.zeletrik.wear.glanceface;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.CalendarContract;
import android.support.wearable.provider.WearableCalendarContract;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;

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
    // stop showing current meeting after this amount of time since it started.
    static final int EVENT_START_CUTOFF = 60000 * 20;
    static final int MSG_LOAD_MEETINGS = 0;


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
        private AsyncTask<Void, Void, Boolean> mLoadMeetingsTask;
        /**
         * Handler to load the meetings once a minute in interactive mode.
         */
        final Handler mLoadMeetingsHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_LOAD_MEETINGS:
                        cancelLoadMeetingTask();
                        mLoadMeetingsTask = new LoadEventsTask();
                        mLoadMeetingsTask.execute();
                        break;
                }
            }
        };
        private List<CalendarEvent> events = new ArrayList<>();
        private boolean mIsReceiverRegistered;

        private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_PROVIDER_CHANGED.equals(intent.getAction())
                        && WearableCalendarContract.CONTENT_URI.equals(intent.getData())) {
                    cancelLoadMeetingTask();
                    mLoadMeetingsHandler.sendEmptyMessage(MSG_LOAD_MEETINGS);
                }
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            mTypeface = Typeface.createFromAsset(getBaseContext().getAssets(), "fonts/ProductSans-Thin.ttf");


            time = currentTimeMillis();

            setWatchFaceStyle(new WatchFaceStyle.Builder(GlanceFace.this)
                    .setAcceptsTapEvents(true)
                    .build());

            dateFormat = new SimpleDateFormat("EE, MMM dd", Locale.getDefault());
            timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            mTimePaint = new TextPaint();
            mTimePaint.setColor(Color.WHITE);
            mTimePaint.setAntiAlias(true);
            mTimePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics()));
            mTimePaint.setTypeface(mTypeface);

            mDatePaint = new TextPaint();
            mDatePaint.setColor(Color.WHITE);
            mDatePaint.setAntiAlias(true);
            mDatePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics()));
            mDatePaint.setTypeface(mTypeface);

            mLoadMeetingsHandler.sendEmptyMessage(MSG_LOAD_MEETINGS);

        }

        @Override
        public void onDestroy() {
            mLoadMeetingsHandler.removeMessages(MSG_LOAD_MEETINGS);
            cancelLoadMeetingTask();
            super.onDestroy();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            time = currentTimeMillis();
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
                    List<CalendarEvent> filtered = getFilteredEvents(true);
                    String msg = filtered.stream().map(CalendarEvent::getTitle).findFirst().orElse("");
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT)
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

            canvas.drawText(timeText, timeX, timeY, mTimePaint);
            canvas.drawText(dateText, dateX, dateY, mDatePaint);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            Log.d("", "Visibility changed");
            super.onVisibilityChanged(visible);

            if (visible) {
                IntentFilter filter = new IntentFilter(Intent.ACTION_PROVIDER_CHANGED);
                filter.addDataScheme("content");
                filter.addDataAuthority(WearableCalendarContract.AUTHORITY, null);
                registerReceiver(mBroadcastReceiver, filter);
                mIsReceiverRegistered = true;

                mLoadMeetingsHandler.sendEmptyMessage(MSG_LOAD_MEETINGS);
            } else {
                if (mIsReceiverRegistered) {
                    unregisterReceiver(mBroadcastReceiver);
                    mIsReceiverRegistered = false;
                }
                mLoadMeetingsHandler.removeMessages(MSG_LOAD_MEETINGS);
            }
        }


        private List<CalendarEvent> getFilteredEvents(int numEvents, Boolean startCutoff) {
            Date now = new Date();
            List<CalendarEvent> filtered = new ArrayList<>();
            int index = 0;
            for (CalendarEvent event : events) {
                if (startCutoff) {
                    Date cutoff = new Date(now.getTime() - (EVENT_START_CUTOFF));
                    if (event.getStartDate().compareTo(cutoff) < 0)
                        continue;
                }
                if (event.getEndDate().compareTo(now) < 0) continue;
                filtered.add(event);

                index++;
                if (index >= numEvents)
                    break;
            }
            return filtered;
        }

        private List<CalendarEvent> getFilteredEvents(Boolean startCutoff) {
            return getFilteredEvents(events.size(), startCutoff);
        }

        private void cancelLoadMeetingTask() {
            if (mLoadMeetingsTask != null) {
                mLoadMeetingsTask.cancel(true);
            }
        }

        private void onMeetingsLoaded(Boolean changed) {
            if (changed) {
                Log.d("", "Meetings loaded and changed");
                invalidate();
            }
        }

        /**
         * Asynchronous task to load the meetings from the content provider and report the number of
         * meetings back.
         */
        private class LoadEventsTask extends AsyncTask<Void, Void, Boolean> {
            private PowerManager.WakeLock mWakeLock;

            @Override
            protected Boolean doInBackground(Void... voids) {
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                mWakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK, "GlanceFace:WakeLock");
                mWakeLock.acquire(10 * 60 * 500L);

                long begin = System.currentTimeMillis();
                Uri.Builder builder =
                        WearableCalendarContract.Instances.CONTENT_URI.buildUpon();
                ContentUris.appendId(builder, begin);
                ContentUris.appendId(builder, begin + DateUtils.DAY_IN_MILLIS);
                final Cursor cursor = getContentResolver().query(builder.build(),
                        null, null, null, null);

                Calendar calBegin = Calendar.getInstance();
                Calendar calEnd = Calendar.getInstance();
                events.clear();

                assert cursor != null;
                while (cursor.moveToNext()) {
                    long beginVal = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.BEGIN));
                    long endVal = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.END));
                    String title = cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.TITLE));
                    boolean isAllDay = !cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)).equals("0");
                    Optional<String> attendingValue = Optional.ofNullable(cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.SELF_ATTENDEE_STATUS)));
                    boolean isAttending = !attendingValue.orElse("99").equals("2");
                    String location = cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION));

                    if (isAllDay) continue;
                    if (!isAttending) continue;
                    calBegin.setTimeInMillis(beginVal);
                    calEnd.setTimeInMillis(endVal);
                    CalendarEvent newEvent = CalendarEvent.builder().title(title)
                            .startDate(calBegin.getTime())
                            .endDate(calEnd.getTime())
                            .location(location)
                            .build();

                    events.add(newEvent);
                }
                cursor.close();
                Collections.sort(events, new CalendarEvent.EventComparator());

                return true;
            }

            @Override
            protected void onPostExecute(Boolean changed) {
                releaseWakeLock();
                onMeetingsLoaded(changed);
            }

            @Override
            protected void onCancelled() {
                releaseWakeLock();
            }

            private void releaseWakeLock() {
                if (mWakeLock != null) {
                    mWakeLock.release();
                    mWakeLock = null;
                }
            }
        }
    }
}