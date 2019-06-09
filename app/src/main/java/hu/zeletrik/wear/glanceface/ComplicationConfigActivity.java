package hu.zeletrik.wear.glanceface;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.concurrent.Executors;

/**
 * The watch-side config activity for {@link GlanceFace}, which allows for setting
 * the left and right complications of watch face.
 */
public class ComplicationConfigActivity extends Activity implements View.OnClickListener {

    static final int COMPLICATION_CONFIG_REQUEST_CODE = 1001;
    private static final String TAG = "ConfigActivity";
    private int mComplicationId;

    // Selected complication id by user.
    private int mSelectedComplicationId;

    // ComponentName used to identify a specific service that renders the watch face.
    private ComponentName mWatchFaceComponentName;

    // Required to retrieve complication data from watch face for preview.
    private ProviderInfoRetriever mProviderInfoRetriever;

    private ImageView mComplicationBackground;

    private ImageButton mComplication;

    private Drawable mDefaultAddComplicationDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_config);

        mDefaultAddComplicationDrawable = getDrawable(R.drawable.add_complication);

        // TODO: Step 3, initialize 1
        mSelectedComplicationId = -1;

        mComplicationId =
                GlanceFace.getComplicationId();

        mWatchFaceComponentName =
                new ComponentName(getApplicationContext(), GlanceFace.class);

        // Sets up left complication preview.
        mComplicationBackground = findViewById(R.id.complication_background);
        mComplication = findViewById(R.id.complication);
        mComplication.setOnClickListener(this);

        // Sets default as "Add Complication" icon.
        mComplication.setImageDrawable(mDefaultAddComplicationDrawable);
        mComplicationBackground.setVisibility(View.INVISIBLE);

        // Initialization of code to retrieve active complication data for the watch face.
        mProviderInfoRetriever =
                new ProviderInfoRetriever(getApplicationContext(), Executors.newCachedThreadPool());
        mProviderInfoRetriever.init();

        retrieveInitialComplicationsData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // TODO: Step 3, release
        // Required to release retriever for active complication data.
        mProviderInfoRetriever.release();
    }

    // TODO: Step 3, retrieve complication data
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

    // Verifies the watch face supports the complication location, then launches the helper
    // class, so user can choose their complication data provider.
    // TODO: Step 3, launch data selector
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

        // TODO: Step 3, update views
        if (requestCode == COMPLICATION_CONFIG_REQUEST_CODE && resultCode == RESULT_OK) {

            // Retrieves information for selected Complication provider.
            ComplicationProviderInfo complicationProviderInfo =
                    data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO);
            Log.d(TAG, "Provider: " + complicationProviderInfo);

            if (mSelectedComplicationId >= 0) {
                updateComplicationViews(mSelectedComplicationId, complicationProviderInfo);
            }
        }
    }
}
