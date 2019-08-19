package hu.zeletrik.wear.glanceface;

import android.content.ComponentName;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.util.Log;

import java.util.concurrent.Executors;

/**
 * The watch-side config activity for {@link GlanceFace}, which allows for setting
 * the bottom complications of watch face.
 */
public class ComplicationConfigActivity extends WearableActivity {

    static final int COMPLICATION_CONFIG_REQUEST_CODE = 1001;
    private static final String TAG = "ComplicationConfigActivity";
    private int mComplicationId;

    private int mSelectedComplicationId;

    private ComponentName mWatchFaceComponentName;

    private ProviderInfoRetriever mProviderInfoRetriever;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSelectedComplicationId = -1;
        mComplicationId = 0;
        mWatchFaceComponentName = new ComponentName(getApplicationContext(), GlanceFace.class);
        mProviderInfoRetriever = new ProviderInfoRetriever(getApplicationContext(), Executors.newCachedThreadPool());
        mProviderInfoRetriever.init();

        launchComplicationHelperActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProviderInfoRetriever.release();
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

        finish();
    }
}
