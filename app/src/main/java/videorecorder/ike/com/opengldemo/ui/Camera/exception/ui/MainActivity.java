package videorecorder.ike.com.opengldemo.ui.Camera.exception.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;


import videorecorder.ike.com.opengldemo.R;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.widget.CameraSurfaceView;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.widget.RenderScreen;

public class MainActivity extends AppCompatActivity {
    private CameraSurfaceView cameraSurfaceView;
    private Button btn_record;
    private boolean isStartRecorde;
    private int totaolTime=0;
    private long startTime=0;
    private String Tag="MainActivity";
    private ImageView iv;
    private Handler handler=new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraSurfaceView=findViewById(R.id.camera_surface);
        btn_record=findViewById(R.id.btn_record);
        iv=findViewById(R.id.iv);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.watermark);
        cameraSurfaceView.setMarker(bitmap);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                iv.setImageBitmap(RenderScreen.maker);
            }
        },3000);
        btn_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isStartRecorde){
                    isStartRecorde=true;
                    cameraSurfaceView.startRecorde();
                    btn_record.setText("停止录制");
                    startTime=System.currentTimeMillis();
                }else {
                    isStartRecorde=false;
                    cameraSurfaceView.stopRecorde();
                    btn_record.setText("开始录制");
                    Log.e(Tag,"录制时间:"+(System.currentTimeMillis()-startTime)/1000);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
       
       
    }
}
