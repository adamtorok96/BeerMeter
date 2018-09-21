package hu.adamtorok.beermeter;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SensorEventListener, SeekBar.OnSeekBarChangeListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;

    private boolean mStarted = false;

    private double mSensitivity = 12;
    private double mCounter = 0;

    private long mStartTime, mEndTime;

    private LineGraphSeries<DataPoint> mSeries;

    private Thread mThread;

    private SeekBar sbSensitivity;
    private TextView mTvTime, tvSensitivity;

    private DecimalFormat mDecimalFormat = new DecimalFormat("#.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        assert mSensorManager != null;

        for (Sensor sensor : mSensorManager.getSensorList(Sensor.TYPE_ALL)) {
            Log.i("DEBUG", sensor.getName() + ": " + sensor.toString());
        }

        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);


        mTvTime = findViewById(R.id.tvTime);
        tvSensitivity = findViewById(R.id.tvSensivity);

        sbSensitivity = findViewById(R.id.sbSensivity);

        GraphView graph = findViewById(R.id.graph);
        graph.getGridLabelRenderer().setPadding(100);

        mSeries = new LineGraphSeries<>();
        graph.addSeries(mSeries);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(100);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(100);

        sbSensitivity.setOnSeekBarChangeListener(this);

        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while( true ) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if( mStarted )
                        updateTimeText();
                }
            }
        });

        mThread.start();
    }

    private void updateTimeText() {
        if( !isMainThread() ) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateTimeText();
                }
            });

            return;
        }

        mTvTime.setText(mDecimalFormat.format(getElapsedMilliseconds()));
    }

    private boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    private long nanoTimeToSeconds(long duration) {
        return TimeUnit.SECONDS.convert(duration, TimeUnit.NANOSECONDS);
    }

    private float nanoTimeToMilliSeconds(long duration) {
        return duration / 1000000000.0f;
    }

    private long getElapsedSeconds() {
        return nanoTimeToSeconds(System.nanoTime() - mStartTime); // / 1000000000000L;
    }

    private float getElapsedMilliseconds() {
        return nanoTimeToMilliSeconds(System.nanoTime() - mStartTime);
    }

    private long getElapsedSecondsFromEnd() {
        return nanoTimeToSeconds(System.nanoTime() - mEndTime);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
//        Log.i(getClass().toString(), event.sensor.getName() +":" + Arrays.toString(event.values));

        double length = getVectorLength(event.values);

        if( System.currentTimeMillis() % 100 == 0 ) {
            mSeries.appendData(
                    new DataPoint(
                            mCounter++, // Calendar.getInstance().getTime(),
                            length
                    ),
                    true,
                    60
            );
        }

        if( length < mSensitivity )
            return;

        Log.i("DEBUG", String.valueOf(length));

        if( mStarted ) {
            if( getElapsedSeconds() > 0 ) {
                mEndTime = System.nanoTime();
                mStarted = false;

                Log.i("DEBUG", "STOPPED");



                mTvTime.setText("Time: " + mDecimalFormat.format(nanoTimeToMilliSeconds(mEndTime - mStartTime)));
            }
        } else {
            if( getElapsedSecondsFromEnd() > 0 ) {
                mStartTime = System.nanoTime();
                mStarted = true;

                Log.i("DEBUG", "STARTED");
            }
        }

//        mSeries.appendData(
//                new DataPoint(
//                        mCounter++, // Calendar.getInstance().getTime(),
//                        length
//                ),
//                true,
//                40
//        );
    }

    double getVectorLength(float[] vector) {
        return Math.sqrt(
                vector[0] * vector[0] +
                vector[1] * vector[1] +
                vector[2] * vector[2]
        );
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
//        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mSensitivity = progress + 5;

        tvSensitivity.setText("Sensivity: " + mSensitivity);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
