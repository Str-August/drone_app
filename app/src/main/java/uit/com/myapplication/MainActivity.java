package uit.com.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.VideoView;
import com.github.niqdev.mjpeg.DisplayMode;
import com.github.niqdev.mjpeg.Mjpeg;
import com.github.niqdev.mjpeg.MjpegInputStream;
import com.github.niqdev.mjpeg.MjpegView;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;


import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;

import java.util.concurrent.TimeUnit;

import rx.functions.Action1;

public class MainActivity extends Activity {
    private Client client;
    static private boolean showNotice = false;
    private volatile boolean isReady = false;
    static boolean isSending = false;
    //boolean goneFlag = false;
    int throttle_d =1500, yaw_d=1500, pitch_d=1500, roll_d = 1500;
    int throttle_max, yaw_max, pitch_max, roll_max ;
    int throttle_up, yaw_up, pitch_up, roll_up ;
    //boolean send_status;
    Button _lUp, _lDown, _lRight, _lLeft, _rUp, _rDown, _rLeft, _rRight;
    String ADDRESSIP;
    EditText portNum, addrConnect, portVideo;
    int PORTNUMBER, PORTVIDEO;
    ImageView streamView;
    Switch Power, Motor, Mode1;
    Thread clientThread = null;
    JoystickView joystickRight;
    Joystick joystickLeft;
    final Timer timer = new Timer();
    LoadImage loadImage;
    VideoView videoView;
    MediaController mediaController;
    private SimpleExoPlayer player;
    private PlayerView simpleExoPlayerView;
    //private TextView resolutionTextView;
    private MjpegView mjpegView;
    private static final int TIMEOUT = 5;
    //Put this into the class
    final Handler handler = new Handler();
    Runnable mLongPressedLD = new Runnable() {
        public void run() {
            //goneFlag = true;
            sendMessage(instructionSend("throttle",throttle_d-throttle_max ));
            //Toast.makeText(MainActivity.this,"press ", Toast.LENGTH_SHORT).show();
        }
    };
    Runnable mLongPressedLU = new Runnable() {
        public void run() {
            //goneFlag = true;
            //Toast.makeText(MainActivity.this,"press " , Toast.LENGTH_SHORT).show();
            sendMessage(instructionSend("throttle",throttle_d+throttle_max ));
        }
    };
    Runnable mLongPressedLL = new Runnable() {
        public void run() {
            //goneFlag = true;
            //Toast.makeText(MainActivity.this,"press " , Toast.LENGTH_SHORT).show();
            sendMessage(instructionSend("yaw",yaw_d-yaw_max ));
        }
    };
    Runnable mLongPressedLR = new Runnable() {
        public void run() {
            //goneFlag = true;
            sendMessage(instructionSend("yaw",yaw_d+yaw_max ));
            //Toast.makeText(MainActivity.this,"press " , Toast.LENGTH_SHORT).show();
        }
    };
    Runnable mLongPressedRU = new Runnable() {
        public void run() {
            //goneFlag = true;
            sendMessage(instructionSend("pitch",pitch_d+pitch_max ));
            //Toast.makeText(MainActivity.this,"press " , Toast.LENGTH_SHORT).show();
        }
    };
    Runnable mLongPressedRD = new Runnable() {
        public void run() {
            //goneFlag = true;
            sendMessage(instructionSend("pitch",pitch_d-pitch_max ));
            //Toast.makeText(MainActivity.this,"press " , Toast.LENGTH_SHORT).show();
        }
    };
    Runnable mLongPressedRR = new Runnable() {
        public void run() {
            //goneFlag = true;
            sendMessage(instructionSend("roll",roll_d+roll_max ));
            //Toast.makeText(MainActivity.this,"press " , Toast.LENGTH_SHORT).show();
        }
    };
    Runnable mLongPressedRL = new Runnable() {
        public void run() {
            //goneFlag = true;
            sendMessage(instructionSend("roll",roll_d-roll_max ));
            //Toast.makeText(MainActivity.this,"press " , Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ADDRESSIP = "192.168.12.1";
        PORTNUMBER = 5533;
        PORTVIDEO = 5554;

        Power = findViewById(R.id.powerButton);
        //Motor = findViewById(R.id.motor_control);
        //Mode1 = findViewById(R.id.flight_mode);
        //joystickLeft = findViewById(R.id.joystick1);
        //joystickRight = findViewById(R.id.joystick2);
        //streamView = findViewById(R.id.streamView);
        //videoView = findViewById(R.id.streamView);
        mjpegView = findViewById(R.id.mjpeg_view);
        _lUp = findViewById(R.id.up_button);
        _lDown = findViewById(R.id.down_button);
        _lLeft = findViewById(R.id.left_button);
        _lRight = findViewById(R.id.right_button);
        _rUp = findViewById(R.id.up_buttonR);
        _rDown = findViewById(R.id.down_buttonR);
        _rRight = findViewById(R.id.right_buttonR);
        _rLeft = findViewById(R.id.left_buttonR);

        SharedPreferences settings = getSharedPreferences("UserInfo", 0);
        ADDRESSIP = settings.getString("ip_addr","");
        PORTNUMBER = settings.getInt("port_sock",0);
        PORTVIDEO =settings.getInt("port_video",1);
        throttle_max = settings.getInt("throttle_max",2);
        throttle_up =settings.getInt("throttle_up",3);
        yaw_max =settings.getInt("yaw_max",4);
        yaw_up =settings.getInt("yaw_up",5);
        pitch_up =settings.getInt("pitch_up",6);
        pitch_max =settings.getInt("pitch_max",7);
        roll_max =settings.getInt("roll_max",8);
        roll_up =settings.getInt("roll_up",9);


        //mjpegView.setCustomBackgroundColor(Color.WHITE);
        //droneAppBackground = findViewById(R.id.drone_app_background);

        showDialogInput();
    }

    private void showDialogInput() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = LayoutInflater.from(this);
        final View dialogView = inflater.inflate(R.layout.alert_dialog_view, null);
        addrConnect = dialogView.findViewById(R.id.address_input);
        portNum = dialogView.findViewById(R.id.port_num);
        portVideo = dialogView.findViewById(R.id.port_video);
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
                                PORTVIDEO = Integer.parseInt(portVideo.getText().toString());
                                //System.out.println("Test Here is true thing zzzz");
                                try {
                                    clientThread = new Thread(new ClientThread());
                                    clientThread.start();
                                    //System.out.println("Test Here is true thing zzzz ");
                                    while (!isReady) {
                                        TimeUnit.NANOSECONDS.sleep(100);
                                    }
                                    ;
                                    if (!showNotice || !isReady) {
                                        isReady = false;
                                        Toast.makeText(MainActivity.this, "Can't make the connection...", Toast.LENGTH_SHORT).show();
                                        showDialogInput();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    showDialogInput();
                                    Toast.makeText(MainActivity.this, "Can't make the connection...", Toast.LENGTH_SHORT).show();
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

    private void dialogWaiting() {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater1 = LayoutInflater.from(this);
        final View dialogLoadingView = inflater1.inflate(R.layout.waiting_layout, null);
        ImageView loading = dialogLoadingView.findViewById(R.id.animation_loading);
        AnimationDrawable animation = (AnimationDrawable) loading.getDrawable();
        builder1.setView(dialogLoadingView)
                .setCancelable(false);
        AlertDialog dialog1 = builder1.create();
        dialog1.show();
        animation.start();
    }


    private DisplayMode calculateDisplayMode() {
        int orientation = getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE ?
                DisplayMode.FULLSCREEN : DisplayMode.BEST_FIT;
    }

    private void playStream(final String src) {
        try {
            Mjpeg.newInstance()
                    .open(src, TIMEOUT)
                    .subscribe(
                            new Action1<MjpegInputStream>() {
                                @Override
                                public void call(MjpegInputStream inputStream) {
                                    mjpegView.setSource(inputStream);
                                    mjpegView.setDisplayMode(MainActivity.this.calculateDisplayMode());
                                    mjpegView.showFps(true);
                                }
                            },
                            new Action1<Throwable>() {
                                @Override
                                public void call(Throwable throwable) {
                                    Log.e(MainActivity.this.getClass().getSimpleName(), "mjpeg error", throwable);
                                    Toast.makeText(MainActivity.this, "Error Stream", Toast.LENGTH_LONG).show();
                                }
                            });
        } catch (Exception error_stream) {
            System.out.println("Drone application error : " + error_stream.toString());
            //Toast.makeText(MainActivity.this, "Error Stream", Toast.LENGTH_LONG).show();
        }
//
    }
    public void settingDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = LayoutInflater.from(this);
        final View dialogSettiongView = inflater.inflate(R.layout.setting_layout, null);
        final EditText throttleUP = dialogSettiongView.findViewById(R.id.throttle_normal);
        final EditText throttleMAX = dialogSettiongView.findViewById(R.id.throttle_max);
        final EditText rollMAX = dialogSettiongView.findViewById(R.id.roll_max);
        final EditText rollUP = dialogSettiongView.findViewById(R.id.roll_normal);
        final EditText pitchMAX = dialogSettiongView.findViewById(R.id.pitch_max);
        final EditText pitchUP = dialogSettiongView.findViewById(R.id.pitch_normal);
        final EditText yawMAX = dialogSettiongView.findViewById(R.id.yaw_max);
        final EditText yawUP = dialogSettiongView.findViewById(R.id.yaw_normal);

        throttleMAX.setText(Integer.toString(throttle_max));
        throttleUP.setText(Integer.toString(throttle_up));
        rollMAX.setText(Integer.toString(roll_max));
        rollUP.setText(Integer.toString(roll_up));
        pitchMAX.setText(Integer.toString(pitch_max));
        pitchUP.setText(Integer.toString(pitch_up));
        yawMAX.setText(Integer.toString(yaw_max));
        yawUP.setText(Integer.toString(yaw_up));

        builder.setView(dialogSettiongView);
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        throttle_up = Integer.parseInt(throttleUP.getText().toString());
                        throttle_max = Integer.parseInt(throttleMAX.getText().toString());
                        roll_max = Integer.parseInt(rollMAX.getText().toString());
                        roll_up = Integer.parseInt(rollUP.getText().toString());
                        pitch_max = Integer.parseInt(pitchMAX.getText().toString());
                        pitch_up = Integer.parseInt(pitchUP.getText().toString());
                        yaw_max = Integer.parseInt(yawMAX.getText().toString());
                        yaw_up = Integer.parseInt(yawUP.getText().toString());

                    }
                }
        );
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    public void showMessDialog(String mess)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(true)
                .setMessage(mess)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void switchClick(View view) {
        switchClickAction();
    }

    private void switchClickAction() {
        if (Power.isChecked()) {
            Toast.makeText(MainActivity.this, "The Application is ON", Toast.LENGTH_SHORT).show();
            sendMessage(instructionSend("power", "on"));
            sendSpeedData();
            mjpegView.resetTransparentBackground();
            controlMotorCut();
            playStream("http://" + ADDRESSIP + ":" + PORTVIDEO + "/?action=stream");
        } else {
            sendMessage(instructionSend("power", "off"));
            mjpegView.setTransparentBackground();
            mjpegView.stopPlayback();
            mjpegView.clearStream();
            Toast.makeText(MainActivity.this, "You have turned off this app", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkSocket() {
//        final Timer timer1 = new Timer();
//        timer1.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//
//                //System.out.println("Test : " + client.getStatus());
//                if ( Power.isChecked()&& !send_status) {
//                    try {
//                        clientThread = new Thread(new ClientThread());
//                        clientThread.start();
//                    } catch (Exception i) {
//                        i.printStackTrace();
//                    }
//                }
//            }
//        }, 0, 10000);//put here time 1000 milliseconds=1 second
    }

    private boolean waitConnection() throws InterruptedException {
        TimeUnit.NANOSECONDS.sleep(100);
        clientThread = new Thread(new ClientThread());
        clientThread.start();
        return client.getStatus();
    }

    private boolean sendMessage(String instructionSend) {
        try {
            new Thread(new SendMessage(instructionSend)).start();
            return true;
        } catch (Exception s_ex) {
            Toast.makeText(this, "Socket not connected...", Toast.LENGTH_SHORT).show();
            s_ex.printStackTrace();
            return false;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void sendSpeedData() {
        _lUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        handler.postDelayed(mLongPressedLU, 1000);
                        //Toast.makeText(MainActivity.this,"down ", Toast.LENGTH_SHORT).show();
                        //This is where my code for movement is initialized to get original location.
                        sendMessage(instructionSend("throttle",throttle_d+throttle_up ));
                        break;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(mLongPressedLU);
                        //Toast.makeText(MainActivity.this, "up " , Toast.LENGTH_SHORT).show();
                        sendMessage(instructionSend("throttle",throttle_d));
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        handler.removeCallbacks(mLongPressedLU);
                        break;
                }
                return true;
            }
        });
        _lDown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        handler.postDelayed(mLongPressedLD, 1000);
                        //Toast.makeText(MainActivity.this,"down " , Toast.LENGTH_SHORT).show();
                        sendMessage(instructionSend("throttle",throttle_d-throttle_up ));
                        //This is where my code for movement is initialized to get original location.
                        break;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(mLongPressedLD);
                            //Toast.makeText(MainActivity.this, "up " , Toast.LENGTH_SHORT).show();
                        sendMessage(instructionSend("throttle",throttle_d));
                            return false;
                    case MotionEvent.ACTION_MOVE:
                        handler.removeCallbacks(mLongPressedLD);
                        break;
                }
                return true;
            }
        });

        _lLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        handler.postDelayed(mLongPressedLL, 1000);
                        sendMessage(instructionSend("yaw",yaw_d-yaw_up ));
                        //Toast.makeText(MainActivity.this,"down ", Toast.LENGTH_SHORT).show();
                        //This is where my code for movement is initialized to get original location.
                        break;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(mLongPressedLL);
                        sendMessage(instructionSend("yaw",yaw_d));
                        //Toast.makeText(MainActivity.this, "up " , Toast.LENGTH_SHORT).show();
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        handler.removeCallbacks(mLongPressedLL);
                        break;
                }
                return true;
            }
        });

        _lRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        handler.postDelayed(mLongPressedLR, 1000);
                        sendMessage(instructionSend("yaw",yaw_d+yaw_up ));
                        //This is where my code for movement is initialized to get original location.
                        break;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(mLongPressedLR);
                        sendMessage(instructionSend("yaw",yaw_d));
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        handler.removeCallbacks(mLongPressedLR);
                        break;
                }
                return true;
            }
        });
        _rUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        handler.postDelayed(mLongPressedRU, 1000);
                        //Toast.makeText(MainActivity.this,"down " , Toast.LENGTH_SHORT).show();
                        sendMessage(instructionSend("pitch",pitch_d+pitch_up));
                        //This is where my code for movement is initialized to get original location.
                        break;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(mLongPressedRU);
                        //Toast.makeText(MainActivity.this, "up " , Toast.LENGTH_SHORT).show();
                        sendMessage(instructionSend("pitch",pitch_d));
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        handler.removeCallbacks(mLongPressedRU);
                        break;
                }
                return true;
            }
        });
        _rDown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        handler.postDelayed(mLongPressedRD, 1000);
                        sendMessage(instructionSend("pitch",pitch_d-pitch_up));
                        //This is where my code for movement is initialized to get original location.
                        break;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(mLongPressedRD);
                        sendMessage(instructionSend("pitch",pitch_d));
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        handler.removeCallbacks(mLongPressedRD);
                        break;
                }
                return true;
            }
        });
        _rLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        handler.postDelayed(mLongPressedRL, 1000);
                        sendMessage(instructionSend("roll",roll_d-roll_up));
                        //This is where my code for movement is initialized to get original location.
                        break;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(mLongPressedRL);
                        sendMessage(instructionSend("roll",roll_d));
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        handler.removeCallbacks(mLongPressedRL);
                        break;
                }
                return true;
            }
        });
        _rRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        handler.postDelayed(mLongPressedRR, 1000);
                        sendMessage(instructionSend("roll",roll_d+roll_up));
                        //This is where my code for movement is initialized to get original location.
                        break;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(mLongPressedRR);
                        sendMessage(instructionSend("roll",roll_d));
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        handler.removeCallbacks(mLongPressedRR);
                        break;
                }
                return true;
            }
        });
//        joystickLeft.setOnMoveListener(new Joystick.OnMoveListener() {
//            @Override
//            public void onMove(int angle, int strength) {
//                if (angle < 0) angle = 1500 - 5 * Math.abs(angle);
//                else angle = 1500 + 5 * Math.abs(angle);
//                strength = 1000 + strength / 100;
//                sendMessage(instructionSend("JoyL", strength, angle));
//
//
//            }
//        });
//        joystickRight.setOnMoveListener(new JoystickView.OnMoveListener() {
//            @Override
//            public void onMove(int pitch, int roll) {
//                if (pitch < 0) pitch = 1500 - 5 * Math.abs(pitch);
//                else pitch = 1500 + 5 * Math.abs(pitch);
//                if (roll > 0) roll = 1500 - 5 * Math.abs(roll);
//                else roll = 1500 + 5 * Math.abs(roll);
//                sendMessage(instructionSend("JoyR", pitch, roll));
//            }
//        });
    }

    private String instructionSend(String device, int value1, int value2) {
        return (device.length() + "/" + device + "4/" + value1 + "4/" + value2);
    }

    private String instructionSend(String device, String valueName) {
        return (device.length() + "/" + device + valueName.length() + "/" + valueName);
    }
    private String instructionSend(String device, int value1) {
        return (device.length() + "/" + device + "4/" + value1 );
    }

    private void controlMotorCut() {
//        Motor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//                if (b) {
//                    //if(!sendMessage(instructionSend("motor", "on"))) send_status = false; else send_status=true;
//                    sendMessage(instructionSend("motor", "on"));
//                } else {
//                    //if(!sendMessage(instructionSend("motor", "off"))) send_status = false; else send_status=true;
//                    sendMessage(instructionSend("motor", "off"));
//                }
//            }
//        });
//        Mode1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//                if (b) {
//
//                } else {
//
//                }
//            }
//        });
//        CCW.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                switch (motionEvent.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        Log.i("TAG", "touched down");
//                        sendMessage((instructionSend("motor", "on")) + "!" + instructionSend("motor", "ccw"));
//
//                        //sendMessage(instructionSend("motor","ccw"));
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        Log.i("TAG", "touched up");
//                        sendMessage(instructionSend("motor", "off"));
//
//                        break;
//                }
//
//                return true;
//            }
//
//        });
    }

    private boolean isCheckedInput(String scr, String port) {
        int count = 0, last_i = 0;  // count variable to count the value of point //
        for (int i = 0; i < scr.length(); i++) {

            if (scr.charAt(i) == '.' && i - last_i <= 3 && i - last_i > 0) // if the distance of point (.) must >0 and < 4
            {
                count++;

                // check the num
                int num = 0;
                for (int j = last_i; j < i; j++) {
                    if ('0' > scr.charAt(j) || scr.charAt(j) > '9') return false;
                    num = num * 10 + (scr.charAt(j) - '0');
                }
                if (num > 255) return false; // must be < 255 //
                last_i = i + 1;
            }

        }
        for (int i = 0; i < port.length(); i++) {
            if ('0' > port.charAt(i) || port.charAt(i) > '9') return false;

        }

        return (count == 3);
    }

    public void setting_onclick(View view) {
        settingDialog();
    }


    class SendMessage implements Runnable {
        private String line;
        public boolean statusM = true;

        public SendMessage(String line) {
            this.line = line;
        }

        @Override
        public void run() {
            try {
                while (isSending) {
                    TimeUnit.NANOSECONDS.sleep(100);
                }
                ;
                isSending = true;
                try {
                    client.sendMessage(line);
                    statusM = true;
                } catch (Exception sendex) {
                    sendex.printStackTrace();
                    statusM = false;
                }
                isSending = false;
            } catch (Exception error) {
                error.printStackTrace();
            }
        }
    }

    class ClientThread implements Runnable {
        public ClientThread() {
            client = new Client(ADDRESSIP, PORTNUMBER);
        }

        @Override


        public void run() {
            // System.out.println("Test Here is true thing ");
            if (client.UNit()) {
                showNotice = true;
            } else showNotice = false;


            //System.out.println("Test That be out of client thread");
            isReady = true;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        try {
            sendMessage(instructionSend("power", "off"));
            Power.setChecked(false);
            closeSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Intent mStartActivity = new Intent(MainActivity.this, MainActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(MainActivity.this, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) MainActivity.this.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
        //showDialogInput();
        //switchClickAction();
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
        SharedPreferences settings = getSharedPreferences("UserInfo", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.putInt("throttle_max",throttle_max);
        editor.putInt("throttle_max",throttle_up);
        editor.putInt("yaw_max",yaw_max);
        editor.putInt("yaw_up",yaw_up);
        editor.putInt("pitch_up",pitch_up);
        editor.putInt("pitch_max",pitch_max);
        editor.putInt("roll_max",roll_max);
        editor.putInt("roll_up",roll_up);

        editor.putString("ip_addr",ADDRESSIP);
        editor.putInt("port_sock",PORTNUMBER);
        editor.putInt("port_video",PORTVIDEO);
        editor.apply();
        //showDialogInput();
//
//        try {
//            sendMessage(instructionSend("power", "off"));
//            closeSocket();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
////        try {
//            client.closeSocket();
//        }catch (Exception u)
//        {
//            System.out.println(u);
//        }
    }

    private void closeSocket() {
        new Thread(new CloseSocket(client)).start();
    }

    static class CloseSocket implements Runnable {
        Client client;

        public CloseSocket(Client client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                client.closeSocket();
            } catch (Exception i) {
                i.printStackTrace();
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //streamView.stopPlayback();


    }


    private class LoadImage extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;

        public LoadImage(ImageView streamView) {
            this.imageView = streamView;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            String urlLink = strings[0];
            Bitmap bitmap = null;
            try {
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

