package com.laurencebush.heightrecognition;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

public class MainActivity extends AppCompatActivity implements SensorEventListener {


    private SensorManager mSensorManager;
    private Sensor mGSensor, mASensor, mMSensor;

    private String height = "";
    private String anglepeak = "";
    private String anglebase = "";
    private String answer = "";

    int ang;

    float[] gData = new float[3];           // Gravity or accelerometer
    float[] mData = new float[3];           // Magnetometer
    float[] orientation = new float[3];
    float[] Rmat = new float[9];
    float[] R2 = new float[9];
    float[] Imat = new float[9];
    boolean haveGrav = false;
    boolean haveAccel = false;
    boolean haveMag = false;


    //camera shingdig
    private TextureView textureView;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupMainView();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    public void setupMainView() {
        setContentView(R.layout.activity_main);
        String height = this.height;
        String peak = this.anglepeak;
        String base = this.anglebase;
        String ans = this.answer;
        ((EditText) findViewById(R.id.text_height_edit)).setText(String.valueOf(height));
        ((TextView) findViewById(R.id.text_angle_top)).setText(String.valueOf(peak));
        ((TextView) findViewById(R.id.text_angle_base)).setText(String.valueOf(base));
        ((TextView) findViewById(R.id.text_answer)).setText(String.valueOf(ans));


        ((Button) findViewById(R.id.button_angle)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                changeToCameraView();
            }
        });

        ((Button) findViewById(R.id.button_calculate)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                calculate();
            }
        });
    }

    public void setupCameraView() {
        setContentView(R.layout.activity_camera);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mGSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mASensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, mGSensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mASensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMSensor, SensorManager.SENSOR_DELAY_GAME);


        //add the camera feed to surface view

        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            //camera didnt work
        }

        CameraPreview preview = new CameraPreview(this, camera);
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.frame_layout);
        frameLayout.addView(preview);


        ((Button) findViewById(R.id.button_return)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                changeToMainView();
            }
        });
        ((Button) findViewById(R.id.button_capture)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                capturePeakAngle();
            }
        });
    }

    public void changeToCameraView() {
        height = (String) ((EditText) findViewById(R.id.text_height_edit)).getText().toString();
        anglepeak = (String) ((TextView) findViewById(R.id.text_angle_top)).getText().toString();
        anglebase = (String) ((TextView) findViewById(R.id.text_angle_base)).getText().toString();
        answer = (String) ((TextView) findViewById(R.id.text_answer)).toString();
        setupCameraView();
    }

    public void changeToMainView() {

        mSensorManager.unregisterListener(this, mGSensor);
        mSensorManager.unregisterListener(this, mASensor);
        mSensorManager.unregisterListener(this, mMSensor);
        setContentView(R.layout.activity_main);
        setupMainView();
    }

    public void capturePeakAngle() {
        anglepeak = "" + ang;
        ((Button) findViewById(R.id.button_capture)).setText("CALCULATE BASE ANGLE");
        ((Button) findViewById(R.id.button_capture)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                captureBaseAngle();
            }
        });
        ((TextView) findViewById(R.id.text_calculate)).setText("Aim Phone at the Bottom of the Object then click 'Calculate Base Angle'");

    }

    public void captureBaseAngle() {
        anglebase = "" + ang;
        ((Button) findViewById(R.id.button_capture)).setText("Done!");
        ((Button) findViewById(R.id.button_capture)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                changeToMainView();
            }
        });
        ((TextView) findViewById(R.id.text_calculate)).setText("Click either button to return.");

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] data;
        switch (event.sensor.getType()) {
            case Sensor.TYPE_GRAVITY:
                gData[0] = event.values[0];
                gData[1] = event.values[1];
                gData[2] = event.values[2];
                haveGrav = true;
                break;
            case Sensor.TYPE_ACCELEROMETER:
                if (haveGrav) break;    // don't need it, we have better
                gData[0] = event.values[0];
                gData[1] = event.values[1];
                gData[2] = event.values[2];
                haveAccel = true;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mData[0] = event.values[0];
                mData[1] = event.values[1];
                mData[2] = event.values[2];
                haveMag = true;
                break;
            default:
                return;
        }

        if ((haveGrav || haveAccel) && haveMag) {
            SensorManager.getRotationMatrix(Rmat, Imat, gData, mData);
            SensorManager.remapCoordinateSystem(Rmat,
                    SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, R2);
            // Orientation isn't as useful as a rotation matrix, but
            // shown it here anyway.
            SensorManager.getOrientation(R2, orientation);
            float incl = SensorManager.getInclination(Imat);
            ang = (int) (orientation[2] * 114.649681529) / 2;  // this number allows the given pitch to return a degree pitch

            ((TextView) findViewById(R.id.text_title_pitch)).setText("Pitch: " + ang);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    public void calculate() {
        if (height != "" && anglebase != "" && anglepeak != "") {
            answer = Double.toString(getHeightOfObject(Float.parseFloat(height), Integer.parseInt(anglepeak), Integer.parseInt(anglebase)));
            ((TextView) findViewById(R.id.text_answer)).setText(String.valueOf(answer));
        }
    }

    private double getHeightOfObject(float height, int peak, int base) {
        double peakBaseR = Math.toRadians(peak - base);
        double peakR = Math.toRadians(peak);
        double baseR = Math.toRadians(base);
        double top = height * Math.sin(peakBaseR);
        double bot = Math.cos(baseR) * Math.sin(peakR);
        return top / bot;

    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("WhichGyro Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}

