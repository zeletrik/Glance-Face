package hu.zeletrik.wear.glanceface;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.provider.WearableCalendarContract;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;

/**
 * Copyright (c) <GlanceFace> <Patrik Zelena [patrik.zelena@gmail.com]>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Watch Face Service to display digital date/time
 * and event title and time left for this if in a threshold,
 * -20 min and +5 min.
 * The full day events are filtered out.
 * One bottom complication is supported.
 */
public class GlanceFace extends CanvasWatchFaceService {
    private static final int ONE_MINUTE_CORRECTION = 60000;
    private static final String TAG = "GlanceFace";
    private static final String DOTS = "...";
    private static final int EVENT_START_CUTOFF = 60000 * 5;
    private static final int EVENT_START_THRESHOLD = 60000 * 20;
    private static final int MSG_LOAD_MEETINGS = 0;
    private static final int COMPLICATION_ID = 0;
    private static final int MAX_CHAR_FOR_EVENT = 45;
    private static final int[] COMPLICATION_SUPPORTED_TYPES = {
            ComplicationData.TYPE_SMALL_IMAGE,
            ComplicationData.TYPE_RANGED_VALUE,
            ComplicationData.TYPE_ICON,
            ComplicationData.TYPE_SHORT_TEXT
    };
    private static String currentEventTitle = StringUtils.EMPTY;
    private static StaticLayout staticLayout;
    private static boolean vibrateNeeded = false;

    static int[] getSupportedComplicationTypes() {
        return COMPLICATION_SUPPORTED_TYPES;
    }

    @Override
    public Engine onCreateEngine() {
        Intent myIntent = new Intent(getBaseContext(), PermissionRequestActivity.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        myIntent.putExtra("KEY_PERMISSIONS", Manifest.permission.READ_CALENDAR);
        startActivity(myIntent);
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
        private long time;
        private SimpleDateFormat dateFormat;
        private SimpleDateFormat eventTimeFormat;
        private SimpleDateFormat timeFormat;
        private TextPaint mTimePaint;
        private TextPaint mEventTimeInPaint;
        private TextPaint mEventTitlePaint;
        private TextPaint mDatePaint;
        private Typeface mTypefaceThin;
        private Typeface mTypefaceRegular;
        private int backgroundColor = Color.BLACK;
        private Optional<CalendarEvent> calendarEvent;
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
        private SharedPreferences settings;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;
        private ComplicationData mActiveComplicationData;
        private ComplicationDrawable mComplicationDrawable;
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

            settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            settings.registerOnSharedPreferenceChangeListener(this);

            mTypefaceThin = Typeface.createFromAsset(getBaseContext().getAssets(), "fonts/ProductSans-Thin.ttf");
            mTypefaceRegular = Typeface.createFromAsset(getBaseContext().getAssets(), "fonts/ProductSans-Regular.ttf");

            time = currentTimeMillis();

            setWatchFaceStyle(new WatchFaceStyle.Builder(GlanceFace.this)
                    .setAcceptsTapEvents(true)
                    .build());

            dateFormat = new SimpleDateFormat("EE, MMM dd", Locale.getDefault());
            timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            eventTimeFormat = new SimpleDateFormat("mm", Locale.getDefault());

            mTimePaint = new TextPaint();
            mTimePaint.setColor(Color.WHITE);
            mTimePaint.setAntiAlias(true);
            mTimePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 52, getResources().getDisplayMetrics()));
            mTimePaint.setTypeface(mTypefaceRegular);

            mDatePaint = new TextPaint();
            mDatePaint.setColor(Color.WHITE);
            mDatePaint.setAntiAlias(true);
            mDatePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, getResources().getDisplayMetrics()));
            mDatePaint.setTypeface(mTypefaceThin);

            mEventTimeInPaint = new TextPaint();
            mEventTimeInPaint.setColor(Color.WHITE);
            mEventTimeInPaint.setAntiAlias(true);
            mEventTimeInPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics()));
            mEventTimeInPaint.setTypeface(mTypefaceThin);

            mEventTitlePaint = new TextPaint();
            mEventTitlePaint.setColor(Color.WHITE);
            mEventTitlePaint.setAntiAlias(true);
            mEventTitlePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, getResources().getDisplayMetrics()));
            mEventTitlePaint.setTypeface(mTypefaceRegular);

            mLoadMeetingsHandler.sendEmptyMessage(MSG_LOAD_MEETINGS);

            initializeComplications();
        }

        private void initializeComplications() {
            Log.d(TAG, "initializeComplications()");

            mComplicationDrawable = new ComplicationDrawable(GlanceFace.this);
            mComplicationDrawable.setContext(getApplicationContext());

            setActiveComplications(COMPLICATION_ID);
        }

        @Override
        public void onDestroy() {
            settings.unregisterOnSharedPreferenceChangeListener(this);
            mLoadMeetingsHandler.removeMessages(MSG_LOAD_MEETINGS);
            cancelLoadMeetingTask();
            super.onDestroy();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            time = currentTimeMillis();
            calendarEvent = getNextEventInThreshold();
            invalidate();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            int sizeOfComplication = width / 4;
            int midpointOfScreen = width / 2;

            int verticalOffset = midpointOfScreen + (sizeOfComplication / 2);

            Rect compBounds =
                    new Rect(
                            (midpointOfScreen - (sizeOfComplication / 2)),
                            verticalOffset,
                            (midpointOfScreen + (sizeOfComplication / 2)),
                            (verticalOffset + sizeOfComplication));

            mComplicationDrawable.setBounds(compBounds);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            canvas.drawColor(backgroundColor);

            if (calendarEvent.isPresent()) {
                drawEventWatchFace(canvas, bounds);
            } else {
                drawTimeWatchFace(canvas, bounds);
            }

            drawComplications(canvas);
        }


        private void drawComplications(Canvas canvas) {
            mComplicationDrawable.draw(canvas, time);
        }

        private void drawTimeWatchFace(Canvas canvas, Rect bounds) {
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

            canvas.drawText(timeText, timeX, timeY, mTimePaint);
            canvas.drawText(dateText, dateX, dateY, mDatePaint);
        }

        private void drawEventWatchFace(Canvas canvas, Rect bounds) {
            Date now = new Date();
            CalendarEvent event = calendarEvent.get();
            Date eventInTimeBase = new Date((event.getStartDate().getTime()) - now.getTime() + ONE_MINUTE_CORRECTION);

            Rect eventInTimeBounds = new Rect();
            String eventInTime = "in " + eventTimeFormat.format(eventInTimeBase) + " min";
            if (event.getStartDate().before(now)) {
                eventInTime = "Now";
                if (vibrateNeeded) {
                    vibrate(event.getTitle());
                    vibrateNeeded = false;
                }
            } else {
                vibrateNeeded = true;
            }
            int eventInTimeX;
            int eventInTimeY;

            String timeText = timeFormat.format(time);
            String dateText = timeText + " | " + dateFormat.format(time);
            Rect dateBounds = new Rect();
            int dateX;
            int dateY;

            mEventTimeInPaint.getTextBounds(eventInTime, 0, eventInTime.length(), eventInTimeBounds);
            eventInTimeX = Math.abs(bounds.centerX() - eventInTimeBounds.centerX());
            eventInTimeY = Math.round((bounds.centerY()) - (bounds.height() * 0.01f));

            mDatePaint.getTextBounds(dateText, 0, dateText.length(), dateBounds);
            dateX = Math.abs(bounds.centerX() - dateBounds.centerX());
            dateY = Math.round((bounds.centerY() + dateBounds.height()) + (bounds.height() * 0.01f));

            canvas.drawText(eventInTime, eventInTimeX, eventInTimeY, mEventTimeInPaint);
            canvas.drawText(dateText, dateX, dateY, mDatePaint);
            String eventTitle = event.getTitle();

            if (isRecalculateLayoutNeeded(eventTitle)) {
                currentEventTitle = eventTitle;
                staticLayout = createStaticLayout(eventTitle, bounds);
            }

            canvas.save();

            int numberOfTextLines = staticLayout.getLineCount();
            float offset = numberOfTextLines > 1 ? 0.3f : 0.2f;
            float textYCoordinate = Math.round((Math.abs(bounds.centerY())) - (bounds.height() * offset));
            float textXCoordinate = bounds.left + 38;

            canvas.translate(textXCoordinate, textYCoordinate);
            staticLayout.draw(canvas);
            canvas.restore();
        }

        private boolean isRecalculateLayoutNeeded(String eventTitle) {
            return !(currentEventTitle.equals(eventTitle)) || Objects.isNull(staticLayout);
        }

        private StaticLayout createStaticLayout(String eventTitle, Rect bounds) {
            if (eventTitle.length() >= MAX_CHAR_FOR_EVENT) {
                eventTitle = eventTitle.substring(0, MAX_CHAR_FOR_EVENT - 3) + DOTS;

            }

            StaticLayout.Builder slBuilder = StaticLayout.Builder.obtain(eventTitle, bounds.left, eventTitle.length(),
                    mEventTitlePaint, bounds.width() - 75)
                    .setText(eventTitle)
                    .setIncludePad(true)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setEllipsizedWidth(bounds.width())
                    .setMaxLines(2);

            StaticLayout sl = slBuilder.build();

            int charCount = MAX_CHAR_FOR_EVENT;
            while (sl.getLineCount() > 2) {
                charCount = charCount - 10;
                eventTitle = eventTitle.substring(0, charCount) + DOTS;
                slBuilder = StaticLayout.Builder.obtain(eventTitle, bounds.left, eventTitle.length(),
                        mEventTitlePaint, bounds.width() - 75)
                        .setText(eventTitle)
                        .setIncludePad(true)
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .setEllipsizedWidth(bounds.width())
                        .setMaxLines(2);

                sl = slBuilder.build();
            }

            return sl;
        }

        private void vibrate(String title) {
            Log.d(TAG, "Vibrate for event=" + title);
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            long[] vibrationPattern = {0, 500, 50, 300};
            final int indexInPatternToRepeat = -1;
            vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);

        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            Log.d(TAG, "Visibility changed");
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

        private List<CalendarEvent> getFilteredEvents() {
            Date now = new Date();
            List<CalendarEvent> filtered = new ArrayList<>();
            for (CalendarEvent event : events) {
                Date cutoff = new Date(now.getTime() - (EVENT_START_CUTOFF));
                if (event.getStartDate().compareTo(cutoff) < 0) {
                    continue;
                }

                if (event.getEndDate().compareTo(now) < 0) {
                    continue;
                }
                filtered.add(event);
            }
            return filtered;
        }

        private Optional<CalendarEvent> getNextEventInThreshold() {
            Date now = new Date();
            CalendarEvent nextEvent = null;
            List<CalendarEvent> filteredEvents = getFilteredEvents();
            if (filteredEvents.size() > 0) {
                Date cutoff = new Date(now.getTime() + (EVENT_START_THRESHOLD));
                if (filteredEvents.get(0).getStartDate().compareTo(cutoff) < 0) {
                    nextEvent = filteredEvents.get(0);
                }
            }
            return Optional.ofNullable(nextEvent);
        }

        private void cancelLoadMeetingTask() {
            if (mLoadMeetingsTask != null) {
                mLoadMeetingsTask.cancel(true);
            }
        }

        private void onMeetingsLoaded(Boolean changed) {
            if (changed) {
                Log.d(TAG, "Meetings loaded and changed");
                invalidate();
            }
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (Objects.nonNull(mComplicationDrawable)) {
                mComplicationDrawable.setInAmbientMode(inAmbientMode);
            }
            invalidate();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);

            if (Objects.nonNull(mComplicationDrawable)) {
                mComplicationDrawable.setLowBitAmbient(mLowBitAmbient);
                mComplicationDrawable.setBurnInProtection(mBurnInProtection);
            }
        }

        @Override
        public void onComplicationDataUpdate(int complicationId, ComplicationData complicationData) {
            Log.d(TAG, "onComplicationDataUpdate() id: " + complicationId);
            mActiveComplicationData = complicationData;
            mComplicationDrawable.setComplicationData(complicationData);
            invalidate();
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Log.d(TAG, "OnTapCommand()");
            switch (tapType) {
                case TAP_TYPE_TAP:
                    int tappedComplicationId = getTappedComplicationId(x, y);
                    if (tappedComplicationId != -1) {
                        onComplicationTap(tappedComplicationId);
                    }
                    break;
            }
        }

        private int getTappedComplicationId(int x, int y) {

            long currentTimeMillis = System.currentTimeMillis();

            if ((mActiveComplicationData != null)
                    && (mActiveComplicationData.isActive(currentTimeMillis))
                    && (mActiveComplicationData.getType() != ComplicationData.TYPE_NOT_CONFIGURED)
                    && (mActiveComplicationData.getType() != ComplicationData.TYPE_EMPTY)) {

                Rect complicationBoundingRect = mComplicationDrawable.getBounds();

                if (complicationBoundingRect.width() > 0) {
                    if (complicationBoundingRect.contains(x, y)) {
                        return COMPLICATION_ID;
                    }
                } else {
                    Log.e(TAG, "Not a recognized complication id.");
                }
            }

            return -1;
        }

        private void onComplicationTap(int complicationId) {
            Log.d(TAG, "onComplicationTap");

            if (mActiveComplicationData != null) {

                if (mActiveComplicationData.getTapAction() != null) {
                    try {
                        mActiveComplicationData.getTapAction().send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(TAG, "onComplicationTap() tap action error: " + e);
                    }

                } else if (mActiveComplicationData.getType() == ComplicationData.TYPE_NO_PERMISSION) {

                    ComponentName componentName = new ComponentName(
                            getApplicationContext(), GlanceFace.class);

                    Intent permissionRequestIntent =
                            ComplicationHelperActivity.createPermissionRequestHelperIntent(
                                    getApplicationContext(), componentName);

                    startActivity(permissionRequestIntent);
                }

            } else {
                Log.d(TAG, "No PendingIntent for complication " + complicationId + ".");
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

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