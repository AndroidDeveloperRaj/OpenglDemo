package videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.Camera.CameraHolder;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.configuration.VideoConfiguration;

/**
*@author：
@createTime:2018/6/9 14:52
@function:相机预览的surfaceView界面 
**/

public class CameraSurfaceView extends GLSurfaceView {
    private String Tag="CameraSurfaceView";
    private CameraRender cameraRender;
    private MyRecorder mRecorder;
    public Bitmap marker;

    public CameraSurfaceView(Context context) {
        this(context,null);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * 初始化surface
     */
    private void init() {
        cameraRender=new CameraRender(this);
        setEGLContextClientVersion(2);
        setRenderer(cameraRender);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(surfaceCallback);
    }
    public void startRecorde(){
        cameraRender.setVideoConfiguration(VideoConfiguration.createDefault());
        mRecorder = new MyRecorder(VideoConfiguration.createDefault());
        mRecorder.prepareEncoder();
        mRecorder.audioController.start();
        cameraRender.setRecorder(mRecorder);
    }
    private SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            Log.e(Tag,"surfaceCreated");
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            //停止相机预览并释放相机资源
            CameraHolder.instance().stopPreview();
            CameraHolder.instance().releaseCamera();
        }
    };

    public void stopRecorde() {
        mRecorder.stopRecorde();
        
    }

    public void setMarker(Bitmap marker) {
        this.marker = marker;
    }
}
