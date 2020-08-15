package uit.com.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private Client client;
    static private boolean showNotice = false;
    private volatile boolean isReady = false;
    static boolean isSending = false;
    String ADDRESSIP;
    EditText portNum,addrConnect,portVideo;
    int PORTNUMBER,PORTVIDEO;
    ImageView streamView;
    Switch PW;
    Button CW, CCW;
    Thread clientThread = null;
    JoystickView joystickRight;
    Joystick joystickLeft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ADDRESSIP  = "192.168.1.115";
        PORTNUMBER = 5533;
        PORTVIDEO =5555;

        PW = findViewById(R.id.powerButton);
        CW = findViewById(R.id.motorCW);
        CCW = findViewById(R.id.motorCCW);
        joystickLeft = findViewById(R.id.joystick1);
        joystickRight = findViewById(R.id.joystick2);
        streamView =findViewById(R.id.streamView);
        showDialogInput();
    }

    @SuppressLint("SetTextI18n")
    private void showDialogInput() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = LayoutInflater.from(this);
        final View dialogView =inflater.inflate(R.layout.alert_dialog_view,null);
        addrConnect=dialogView.findViewById(R.id.address_input);
        portNum=dialogView.findViewById(R.id.port_num);
        portVideo =dialogView.findViewById(R.id.port_video);
        addrConnect.setText(ADDRESSIP);
        portNum.setText(Integer.toString(PORTNUMBER));
        portVideo.setText(Integer.toString(PORTVIDEO));
        builder.setView(dialogView);
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        if (!addrConnect.getText().toString().isEmpty() && !portNum.getText().toString().isEmpty()) {
                            if (isCheckedInput(addrConnect.getText().toString(), portNum.getText().toString())) {
                                ADDRESSIP = addrConnect.getText().toString();

                                PORTNUMBER = Integer.parseInt(portNum.getText().toString());
                                try {
                                    clientThread = new Thread(new ClientThread());
                                    clientThread.start();
                                    while (!isReady) ;
                                    if (!showNotice || !isReady) {
                                        isReady = false;
                                        showDialogInput();
                                    }
                                }catch (Exception e)
                                {
                                    e.printStackTrace();
                                    showDialogInput();
                                }
                                //Toast.makeText(MainActivity.this, "Enter successful!!", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Please enter the IP and PORT!!", Toast.LENGTH_LONG).show();
                                showDialogInput();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Please enter the IP and PORT!!", Toast.LENGTH_LONG).show();
                            showDialogInput();
                        }
                    }
                }
        );
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                Toast.makeText(MainActivity.this, "Please enter the IP and PORT!!", Toast.LENGTH_SHORT).show();
                finish();
                System.exit(0);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();


    }



    private void playStream(final String src) {
        final Timer timer =new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                LoadImage loadImage = new LoadImage(streamView);
                loadImage.execute(src);
                //your method
                loadImage.cancel(true);
                if(!PW.isChecked()) timer.cancel();
            }
        }, 0, 30);//put here time 1000 milliseconds=1 second

//        Uri UriSrc = Uri.parse(src);
//        if (UriSrc == null) {
//            Toast.makeText(MainActivity.this, "Please enter the Uri.", Toast.LENGTH_LONG).show();
//        } else {
//            streamView.setBackgroundResource(0);
//            //streamView.setVideoPath(src);
//            //streamView.setVideoURI(UriSrc);
//            mediaController = new MediaController(this);
//            streamView.setMediaController(mediaController);
//            streamView.start();
//
//            Toast.makeText(this, "Connect" + src, Toast.LENGTH_LONG).show();
//        }
    }

    public void switchClick(View view) {
        switchClickAction();
    }

    private void switchClickAction() {
        if(PW.isChecked()) {
            //if(client.getStatus())
                checkSocket();
                sendMessage(instructionSend("power", "on"));
                sendSpeedData();
                controlMotorCut();
                //playStream("http://" + ADDRESSIP + ":" + PORTVIDEO + "/html/cam_pic.php");
           // }2
          //else {


            //}
        }else
        {
            sendMessage(instructionSend("power","off"));
            Toast.makeText(MainActivity.this, "Turn off", Toast.LENGTH_LONG).show();
        }
    }

    private void checkSocket() {
        final Timer timer =new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
               
                System.out.println("Test : "+client.getStatus());
               if((client.getStatus())&&PW.isChecked())
               {
                   try {
                       clientThread = new Thread(new ClientThread());
                       clientThread.start();
                   }catch (Exception i)
                   {
                       i.printStackTrace();
                   }
               }
            }
        }, 0, 1000);//put here time 1000 milliseconds=1 second
    }
    private boolean waitConnection() throws InterruptedException {
            TimeUnit.SECONDS.sleep(1);
            clientThread = new Thread(new ClientThread());
            clientThread.start();
            return client.getStatus();
    }

    private void sendMessage(String instructionSend) {
        new Thread(new SendMessage(instructionSend)).start();
    }

    private void sendSpeedData() {
        joystickLeft.setOnMoveListener(new Joystick.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                if(angle<0) angle = 1500-5*Math.abs(angle); else  angle = 1500+5*Math.abs(angle);
                strength = 1000+strength/100;
                sendMessage(instructionSend("JoyL",strength,angle));


            }
        });
        joystickRight.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int pitch, int roll) {
                if(pitch<0) pitch = 1500-5*Math.abs(pitch); else  pitch = 1500+5*Math.abs(pitch);
                if(roll>0) roll = 1500-5*Math.abs(roll); else  roll = 1500+5*Math.abs(roll);
                sendMessage(instructionSend("JoyR",pitch,roll));
            }
        });
    }

    private String instructionSend(String device, int value1,int value2)
    {
        return (device.length()+"/"+device+"4/"+value1+"4/"+value2);
    }
    private String instructionSend(String device, String valueName)
    {
        return (device.length()+"/"+device+valueName.length()+"/"+valueName);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void controlMotorCut() {
        CW.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.i("TAG", "touched down");
                        sendMessage((instructionSend("motor","on")));
                        sendMessage(instructionSend("motor","cw"));
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.i("TAG", "touched up");
                        sendMessage(instructionSend("motor","off"));

                        break;
                }

                return true;
            }

        });
        CCW.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.i("TAG", "touched down");
                        sendMessage((instructionSend("motor","on")));
                        sendMessage(instructionSend("motor","ccw"));
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.i("TAG", "touched up");
                        sendMessage(instructionSend("motor","off"));

                        break;
                }

                return true;
            }

        });
    }

    private boolean isCheckedInput(String scr, String port) {
        int count = 0, last_i = 0 ;  // count variable to count the value of point //
        for (int i = 0; i < scr.length() ; i++)
        {

            if(scr.charAt(i)=='.'&& i- last_i <=3 && i-last_i > 0) // if the distance of point (.) must >0 and < 4
            {
                count++;

                // check the num
                int num=0;
                for (int j =last_i ; j < i;j++)
                {
                    if('0'>scr.charAt(j)||scr.charAt(j)>'9') return false;
                    num = num*10+(scr.charAt(j)-'0');
                }
                if(num>255) return false; // must be < 255 //
                last_i =i+1;
            }

        }
        for (int i = 0; i < port.length() ; i++)
        {
            if('0'>port.charAt(i)||port.charAt(i)>'9') return false;

        }

        return (count==3);
    }
    class SendMessage implements Runnable
    {
        private String line;
        public SendMessage(String line)
        {
            this.line = line;
        }
        @Override
        public void run() {
            while (isSending);
            isSending = true;
            client.sendMessage(line);
            isSending = false;
        }
    }
    class ClientThread implements Runnable {
        public ClientThread() {
            client = new Client(ADDRESSIP,PORTNUMBER);
        }

        @Override



        public void run() {

            if(client.UNit()) {
                showNotice = true;
                //System.out.println("Test");
            }
            else showNotice = false;
            isReady = true;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        try {sendMessage(instructionSend("power","off"));
            closeSocket();} catch (Exception e) { e.printStackTrace();}
        showDialogInput();
        switchClickAction();
//        if(PW.isChecked()) {
//            this.clientThread = new Thread(new ClientThread());
//            clientThread.start();
//        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        //switchClickAction();
    }

    @Override
    protected void onStop() {
        super.onStop();

        //showDialogInput();

        try {sendMessage(instructionSend("power","off"));
            closeSocket();} catch (Exception e) { e.printStackTrace();}
//        try {
//            client.closeSocket();
//        }catch (Exception u)
//        {
//            System.out.println(u);
//        }
    }

    private void closeSocket() {
       new Thread(new CloseSocket(client)).start();
    }
    static class CloseSocket implements Runnable
    {
        Client client;
        public CloseSocket(Client client)
        {
            this.client = client;
        }
        @Override
        public void run() {
            try {
                client.closeSocket();
            }catch (Exception i)
            {
                i.printStackTrace();
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //streamView.stopPlayback();


    }

    @SuppressLint("StaticFieldLeak")
    private class LoadImage extends AsyncTask<String,Void,Bitmap>{
        ImageView imageView;
        public LoadImage(ImageView streamView) {
            this.imageView= streamView;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            String urlLink= strings[0];
            Bitmap bitmap = null;
            try{
                InputStream inputStream = new java.net.URL(urlLink).openStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            streamView.setImageBitmap(bitmap);
        }

    }
}

