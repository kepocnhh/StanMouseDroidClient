package stan.mouse.ui.activities;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;

import stan.mouse.R;

public class Main
        extends StanActivity
        implements SensorEventListener
{
    //_______________VIEWS
    private TextView xyView;
    private TextView xzView;
    private TextView zyView;

    //_______________FIELDS
    private SensorManager msensorManager; //Менеджер сенсоров аппрата

    private float[] rotationMatrix;     //Матрица поворота
    private float[] accelData;           //Данные с акселерометра
    private float[] magnetData;       //Данные геомагнитного датчика
    private float[] OrientationData; //Матрица положения в пространстве


    public Main()
    {
        super(R.layout.main, R.id.main_frame);
    }

    @Override
    protected void initFragments()
    {

    }

    @Override
    protected void initViews()
    {
        xyView = (TextView) findViewById(R.id.xyValue);
        xzView = (TextView) findViewById(R.id.xzValue);
        zyView = (TextView) findViewById(R.id.zyValue);
    }

    @Override
    protected void init()
    {
        msensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        rotationMatrix = new float[16];
        accelData = new float[3];
        magnetData = new float[3];
        OrientationData = new float[3];
    }
    @Override
    protected void onResume()
    {
        super.onResume();
        msensorManager.registerListener(this, msensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI );
        msensorManager.registerListener(this, msensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI );
    }
    @Override
    protected void onPause()
    {
        super.onPause();
        msensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        loadNewSensorData(event); // Получаем данные с датчика
        SensorManager.getRotationMatrix(rotationMatrix, null, accelData, magnetData); //Получаем матрицу поворота
        SensorManager.getOrientation(rotationMatrix, OrientationData); //Получаем данные ориентации устройства в пространстве

        if((xyView==null)||(xzView==null)||(zyView==null)){  //Без этого работать отказалось.
            xyView = (TextView) findViewById(R.id.xyValue);
            xzView = (TextView) findViewById(R.id.xzValue);
            zyView = (TextView) findViewById(R.id.zyValue);
        }

        //Выводим результат
        xyView.setText(String.valueOf(Math.round(Math.toDegrees(OrientationData[0]))));
        xzView.setText(String.valueOf(Math.round(Math.toDegrees(OrientationData[1]))));
        zyView.setText(String.valueOf(Math.round(Math.toDegrees(OrientationData[2]))));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    private void loadNewSensorData(SensorEvent event)
    {
        final int type = event.sensor.getType(); //Определяем тип датчика
        if (type == Sensor.TYPE_ACCELEROMETER)
        { //Если акселерометр
            accelData = event.values.clone();
        }

        if (type == Sensor.TYPE_MAGNETIC_FIELD)
        { //Если геомагнитный датчик
            magnetData = event.values.clone();
        }
    }
}