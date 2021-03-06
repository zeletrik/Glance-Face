package hu.zeletrik.wear.glanceface;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

public class PermissionRequestActivity extends Activity {
    private static int PERMISSIONS_CODE = 0;
    String[] mPermissions;
    int mRequestCode;

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == mRequestCode) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];
                Log.d("PermissionRequestActivity", "" + permission + " " + (grantResult == PackageManager.PERMISSION_GRANTED ? "granted" : "revoked"));
            }
        }
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPermissions = new String[1];
        mPermissions[0] = this.getIntent().getStringExtra("KEY_PERMISSIONS");
        mRequestCode = this.getIntent().getIntExtra("KEY_REQUEST_CODE", PERMISSIONS_CODE);

        ActivityCompat.requestPermissions(this, mPermissions, mRequestCode);
    }
}