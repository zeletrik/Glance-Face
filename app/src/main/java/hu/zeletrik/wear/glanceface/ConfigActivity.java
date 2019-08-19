package hu.zeletrik.wear.glanceface;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;

import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import java.util.ArrayList;

/**
 * The watch-side config activity for {@link GlanceFace}, which allows for setting
 * the bottom complications of watch face.
 */
public class ConfigActivity extends WearableActivity {

    private static final String TAG = "ConfigActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);

        WearableRecyclerView recyclerView = findViewById(R.id.main_menu_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setEdgeItemsCenteringEnabled(true);
        recyclerView.setLayoutManager(new WearableLinearLayoutManager(this));

        ArrayList<MenuItem> menuItems = new ArrayList<>();
        menuItems.add(new MenuItem(R.drawable.ic_complication, "Element settings"));
        menuItems.add(new MenuItem(R.drawable.ic_time, "Event settings"));

        recyclerView.setAdapter(new MainMenuAdapter(this, menuItems, menuPosition -> {
            switch (menuPosition){
                case 0:  setComplication(); break;
                case 1:  action(); break;
                default : action();
            }
        }));

    }

    private void action() {
        Log.d(TAG, "ACTION CLICKED");
    }

    private void setComplication() {
        Intent myIntent = new Intent(getBaseContext(), ComplicationConfigActivity.class);
        startActivity(myIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
