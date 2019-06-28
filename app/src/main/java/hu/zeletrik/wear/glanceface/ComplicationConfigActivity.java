package hu.zeletrik.wear.glanceface;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.util.concurrent.Executors;

/**
 * The watch-side config activity for {@link GlanceFace}, which allows for setting
 * the bottom complications of watch face.
 */
public class ComplicationConfigActivity extends Activity implements View.OnClickListener {

    static final int COMPLICATION_CONFIG_REQUEST_CODE = 1001;
    private static final String TAG = "ConfigActivity";
    private int mComplicationId;

    private int mSelectedComplicationId;

    private ComponentName mWatchFaceComponentName;

    private ProviderInfoRetriever mProviderInfoRetriever;

    private ImageView mComplicationBackground;

    private ImageButton mComplication;

    private Drawable mDefaultAddComplicationDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_config);

        mDefaultAddComplicationDrawable = getDrawable(R.drawable.add_complication);
        mSelectedComplicationId = -1;
        mComplicationId = GlanceFace.getComplicationId();

        mWatchFaceComponentName =
                new ComponentName(getApplicationContext(), GlanceFace.class);
        mComplicationBackground = findViewById(R.id.complication_background);
        mComplication = findViewById(R.id.complication);
        mComplication.setOnClickListener(this);
        mComplication.setImageDrawable(mDefaultAddComplicationDrawable);
        mComplicationBackground.setVisibility(View.INVISIBLE);
        mProviderInfoRetriever =
                new ProviderInfoRetriever(getApplicationContext(), Executors.newCachedThreadPool());
        mProviderInfoRetriever.init();

        retrieveInitialComplicationsData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProviderInfoRetriever.release();
    }

    public void retrieveInitialComplicationsData() {

        mProviderInfoRetriever.retrieveProviderInfo(
                new ProviderInfoRetriever.OnProviderInfoReceivedCallback() {
                    @Override
                    public void onProviderInfoReceived(
                            int watchFaceComplicationId,
                            @Nullable ComplicationProviderInfo complicationProviderInfo) {
                        Log.d(TAG, "onProviderInfoReceived: " + complicationProviderInfo);
                        updateComplicationViews(watchFaceComplicationId, complicationProviderInfo);
                    }
                },
                mWatchFaceComponentName,
                mComplicationId);
    }

    @Override
    public void onClick(View view) {
        if (view.equals(mComplication)) {
            Log.d(TAG, " Complication click()");
            launchComplicationHelperActivity();

        }
    }

    private void launchComplicationHelperActivity() {
        mSelectedComplicationId = mComplicationId;

        if (mSelectedComplicationId >= 0) {
            int[] supportedTypes =
                    GlanceFace.getSupportedComplicationTypes();

            startActivityForResult(
                    ComplicationHelperActivity.createProviderChooserHelperIntent(
                            getApplicationContext(),
                            mWatchFaceComponentName,
                            mSelectedComplicationId,
                            supportedTypes),
                    ComplicationConfigActivity.COMPLICATION_CONFIG_REQUEST_CODE);
        } else {
            Log.d(TAG, "Complication not supported by watch face.");
        }
    }

    public void updateComplicationViews(
            int watchFaceComplicationId, ComplicationProviderInfo complicationProviderInfo) {
        Log.d(TAG, "updateComplicationViews(): id: " + watchFaceComplicationId);
        Log.d(TAG, "\tinfo: " + complicationProviderInfo);

        if (complicationProviderInfo != null) {
            mComplication.setImageIcon(complicationProviderInfo.providerIcon);
            mComplicationBackground.setVisibility(View.VISIBLE);

        } else {
            mComplication.setImageDrawable(mDefaultAddComplicationDrawable);
            mComplicationBackground.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == COMPLICATION_CONFIG_REQUEST_CODE && resultCode == RESULT_OK) {
            ComplicationProviderInfo complicationProviderInfo =
                    data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO);
            Log.d(TAG, "Provider: " + complicationProviderInfo);
            if (mSelectedComplicationId >= 0) {
                updateComplicationViews(mSelectedComplicationId, complicationProviderInfo);
            }
        }
    }
}
