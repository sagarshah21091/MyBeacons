package com.sagar.mybeacons;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.ArmaRssiFilter;

import java.util.Collection;

import at.grabner.circleprogress.CircleProgressView;
import at.grabner.circleprogress.Direction;
import at.grabner.circleprogress.TextMode;

// TURN OFF WIFI
// GIVE LOCATION PERMISSION
// TURN ON BLUETOOTH
public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    public static final String TAG = "BeaconsEverywhere";
    private BeaconManager beaconManager;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    StringBuilder sb = new StringBuilder("");
    String beaconLayout;
    CircleProgressView mCircleView;
    TextView mtxtAddBeacon;
    EditText medtUUID;
    Button mbtnScanDevices;
    public final String UUID_Kbeacon = "7777772e-6b6b-6d63-6e2e-636f6d000001";
    Pref pref;
    interface beaconParser {
        public final String IBEACON = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
        public final String EDDYSTONE_UID = "s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19";
        public final String EDDYSTONE_URL = "s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v";
        public final String EDDYSTONE_TLM = "x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15";
        public final String ALTBEACON = "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pref = new Pref(this);
        mCircleView = (CircleProgressView) findViewById(R.id.circleView);
        mtxtAddBeacon = (TextView) findViewById(R.id.txtAddBeacon);
        medtUUID = (EditText) findViewById(R.id.edtUUID);
        mbtnScanDevices = (Button) findViewById(R.id.btnListDevices);
        beaconManager = BeaconManager.getInstanceForApplication(this);
        bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        beaconLayout = beaconParser.IBEACON;
        beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout(beaconLayout));

//        Customize Default Distance vs Time Value
        BeaconManager.setRssiFilterImplClass(ArmaRssiFilter.class);

//        beaconManager.bind(this);
        sb.append("\n" + beaconLayout + "\n");
        sb.append("\nExample UUID -> " + UUID_Kbeacon + "\n");
        ((TextView) findViewById(R.id.txtDistance)).setText(sb.toString());
        setInitDistValue();
        setBeaconButtonDisplay();
        mtxtAddBeacon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String uuid = medtUUID.getText().toString().trim();
                if(pref.getUUID()==null)
                {
                    if(uuid.length()>0) {
                        pref.setUUID(uuid);
                        setBeaconButtonDisplay();
                        beaconManager.bind(MainActivity.this);
                    }
                }
                else
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Are you sure?");
                    builder.setMessage("You want to delete existing beacon setup?");
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            pref.setUUID(null);
                            setBeaconButtonDisplay();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
                    builder.create().show();
                }
            }
        });

        mbtnScanDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, DeviceScanActivity.class));
                overridePendingTransition(0,0);
            }
        });
    }

    private void setBeaconButtonDisplay() {
        if(pref.getUUID()==null) {
            beaconManager.unbind(MainActivity.this);
            medtUUID.setText("");
            medtUUID.setEnabled(true);
            mtxtAddBeacon.setText(getString(R.string.add_new_device));
            mCircleView.setVisibility(View.GONE);
            findViewById(R.id.txtDistance).setVisibility(View.GONE);
        }
        else
        {
            beaconManager.bind(MainActivity.this);
            medtUUID.setText(pref.getUUID());
            medtUUID.setEnabled(false);
            mtxtAddBeacon.setText(getString(R.string.remove_device));
            mCircleView.setVisibility(View.VISIBLE);
            findViewById(R.id.txtDistance).setVisibility(View.VISIBLE);
        }
    }

    private void setInitDistValue() {
        mCircleView.setSeekModeEnabled(false);
        mCircleView.setMaxValue(10);
        mCircleView.setValue(0);
        mCircleView.setValueAnimated(0);
        mCircleView.spin();
        mCircleView.setDirection(Direction.CCW);
        mCircleView.setUnit("mtr");
        mCircleView.setUnitVisible(false);
        mCircleView.setAutoTextSize(true); // enable auto text size, previous values are overwritten
        mCircleView.setTextMode(TextMode.TEXT); // Shows the current value
        mCircleView.setShowTextWhileSpinning(true); // Show/hide text in spinning mode
        mCircleView.setText(""+getString(R.string.calculating));
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkRequiredPermission();
    }

    private void checkRequiredPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                if (this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons.  Please go to Settings -> Applications -> Permissions and grant location access to this app.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                            startActivity(i);
                        }

                    });
                    builder.show();
                } else {

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                            PERMISSION_REQUEST_FINE_LOCATION);
                }
            } else {
                requestBTEnable();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_ENABLE_BT) {
            Log.d(TAG, "OK");
        } else {
            Log.d(TAG, "OFF");
        }
    }

    void requestBTEnable() {
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        final Region region = new Region("myBeaons",
//                Identifier.parse(UUID_Kbeacon),
                Identifier.parse(pref.getUUID()),
                null, null);

        beaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                try {
                    Log.d(TAG, "didEnterRegion");
                    sb.append("\ndidEnterRegion\n");
                    ((TextView) findViewById(R.id.txtDistance)).setText(sb.toString());
                    beaconManager.startRangingBeaconsInRegion(region);
                    resetProgressView();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didExitRegion(Region region) {
                try {
                    Log.d(TAG, "didExitRegion");
                    sb.append("\ndidExitRegion\n");
                    ((TextView) findViewById(R.id.txtDistance)).setText(sb.toString());
                    beaconManager.stopRangingBeaconsInRegion(region);
                    resetProgressView();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didDetermineStateForRegion(int i, Region region) {
                Log.d(TAG, "Region Uid: " + region.getUniqueId());
            }
        });

        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                for (Beacon oneBeacon : beacons) {
                    Log.d(TAG, "distance: " + oneBeacon.getDistance() + " id:" + oneBeacon.getId1() + "/" + oneBeacon.getId2() + "/" + oneBeacon.getId3());
                    sb.append("\nDist: " + oneBeacon.getDistance() + "\nUUID: " + oneBeacon.getId1() + "\n");
                    setProgressViewValue(Math.round(oneBeacon.getDistance()));
                    ((TextView) findViewById(R.id.txtDistance)).setText(sb.toString());
                }
            }
        });

        try {
            beaconManager.startMonitoringBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void setProgressViewValue(long distance) {
        mCircleView.setTextMode(TextMode.VALUE);
        mCircleView.setValue(distance);
        mCircleView.setUnitVisible(true);
        mCircleView.spin();
    }

    private void resetProgressView() {
        mCircleView.setTextMode(TextMode.TEXT);
        mCircleView.setText(""+getString(R.string.calculating));
        mCircleView.setUnitVisible(false);
        mCircleView.spin();
    }
}
