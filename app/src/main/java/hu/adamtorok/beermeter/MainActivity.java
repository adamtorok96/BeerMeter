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
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;

    private boolean mStarted = false;

    private double mCounter = 0;

    private long mStartTime, mEndTime;

    private LineGraphSeries<DataPoint> mSeries;

    private Thread mThread;

    private TextView mTvTime;

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

        GraphView graph = findViewById(R.id.graph);

        mSeries = new LineGraphSeries<>();
        graph.addSeries(mSeries);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(40);

        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while( true ) {
                    try {
                        Thread.sleep(10);
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

        mTvTime.setText(String.valueOf(getElapsedMilliseconds()));
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

        if( length < 11 )
            return;

        Log.i("DEBUG", String.valueOf(length));

        if( mStarted ) {
            if( getElapsedSeconds() > 1 ) {
                mEndTime = System.nanoTime();
                mStarted = false;

                Log.i("DEBUG", "STOPPED");

                mTvTime.setText("Time: " + String.valueOf(nanoTimeToMilliSeconds(mEndTime - mStartTime)));
            }
        } else {
            if( getElapsedSecondsFromEnd() > 1 ) {
                mStartTime = System.nanoTime();
                mStarted = true;

                Log.i("DEBUG", "STARTED");
            }
        }

        mSeries.appendData(
                new DataPoint(
                        mCounter++, // Calendar.getInstance().getTime(),
                        length
                ),
                true,
                40
        );
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
}
