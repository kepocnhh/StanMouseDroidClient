package stan.mouse.ui.activities;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import stan.mouse.R;

public class Main
        extends StanActivity
        implements SensorEventListener
{
    //_______________VIEWS
    private TextView xyView;
    private TextView xzView;
    private TextView zyView;
    Button testButton;

    //_______________FIELDS
    private SensorManager msensorManager; //Менеджер сенсоров аппрата
//    BufferedReader inFromUser;
    DatagramSocket clientSocket;
    InetAddress IPAddress;
    int serverPort = 9876;
    String serverAddress =
//            "192.168.1.161"
            "10.0.1.16"
            ;
    List<Integer> xTemp;
    int tempListSize = 25;

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
        testButton = (Button) findViewById(R.id.testButton);
        testButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try
                {
                    test();
                } catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    private void test()
            throws IOException
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                String sentence = "hello from droid";
                byte[] sendData = sentence.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, serverPort);
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                try
                {
                    clientSocket.send(sendPacket);
//                    clientSocket.receive(receivePacket);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                String modifiedSentence = new String(receivePacket.getData());
                Log.e("FROM SERVER", modifiedSentence);
            }
        }).start();
    }

    private void sendOrientationData(final String x, final String y, final String z)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                String sentence = x+"/"+y+"/"+z;
                byte[] sendData = sentence.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, serverPort);
                try
                {
                    clientSocket.send(sendPacket);
                    Log.e("Sucess send", x+"\t"+y+"\t"+z);
                }
                catch (IOException e)
                {
                    Log.e("Error send", e.getMessage());
                }
            }
        }).start();
    }

    @Override
    protected void init()
    {
        msensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        rotationMatrix = new float[16];
        accelData = new float[3];
        magnetData = new float[3];
        OrientationData = new float[3];
        //
//        inFromUser = new BufferedReader(new InputStreamReader(System.in));
        try
        {
            clientSocket = new DatagramSocket();
            IPAddress = InetAddress.getByName(serverAddress);
        }
        catch (SocketException e)
        {
            e.printStackTrace();
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        }

        xTemp = new ArrayList<>();
        for(int i=0; i<tempListSize-1; i++)
        {
            xTemp.add(0);
        }
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

        //Выводим результат
//        long x = Math.round(Math.toDegrees(OrientationData[0]));
//        long y = Math.round(Math.toDegrees(OrientationData[1]));
//        long z = Math.round(Math.toDegrees(OrientationData[2]));
        int x = (int)(OrientationData[0] * 1000);
        int y = (int)(OrientationData[1] * 1000);
        int z = (int)(OrientationData[2] * 1000);
        xyView.setText(String.valueOf(x));
        xzView.setText(String.valueOf(y));
        zyView.setText(String.valueOf(z));

        //
//        sendOrientationData(x/2, y/2, z/2);
//        sendOrientationData(OrientationData[0]+"", OrientationData[1]+"", OrientationData[2]+"");

        int xForMouse = x;
        xTemp.add(x);
        for(int i=0; i<xTemp.size(); i++)
        {
            xForMouse += xTemp.get(i);
        }
        xTemp.remove(0);
        xForMouse /= tempListSize;
        sendOrientationData(xForMouse+"", y+"", z+"");
//        sendOrientationData(x+"", y+"", z+"");
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