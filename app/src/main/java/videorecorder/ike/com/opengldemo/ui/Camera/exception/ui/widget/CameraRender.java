package videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.Camera.CameraHolder;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.Camera.CameraUtils;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.Camera.exception.CameraDisabledException;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.Camera.exception.CameraHardwareException;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.Camera.exception.CameraNotSupportException;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.Camera.exception.NoCameraException;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.Utils.GlUtil;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.configuration.VideoConfiguration;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.effect.Effect;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.effect.GrayEffect;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.effect.NullEffect;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.effect.beauty.MagicBeautyFilter;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.mediacodec.VideoMediaCodec;

/**
*@author：
@createTime:2018/6/9 14:55
@function: 相机画面 的绘制render 
**/

public class CameraRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    /**相机是否打开**/
    private boolean isCameraOpen;
    private int mSurfaceTextureId;
    private SurfaceTexture mSurfaceTexture;
    /**是否应当跟新界面进行重新绘制**/
    private boolean updateSurface;
    //创建一个单元矩阵
    private final float[] mTexMtx = GlUtil.createIdentityMtx();
    private Effect effect;
    private RenderScreen mRenderScreen;
    private int mEffectTextureId;
    private RenderSrfTex mRenderSrfTex;

    private int mVideoWidth;
    private int mVideoHeight;
    private String Tag="CameraRender";
    public static Context context;
    private MagicBeautyFilter beautyFilter;

    public CameraRender(GLSurfaceView view) {
        this.view = (CameraSurfaceView) view;
        effect=new NullEffect(view.getContext());
        context=view.getContext();
       
    }

    private CameraSurfaceView view;
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        initSurfaceTexture();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        startCameraPreview();
        if (isCameraOpen){
            if (mRenderScreen==null){
                initRenderScreen();
            }
            mRenderScreen.setScreenSize(width, height);
        }
    }

    private void initRenderScreen() {
        effect.setTextureId(mSurfaceTextureId);
        effect.prepare();
        mEffectTextureId = effect.getEffertedTextureId();
        mRenderScreen = new RenderScreen(mEffectTextureId);
        mRenderScreen.setMaker(view.marker);
        Log.e(Tag,"初始化initRenderScreen");
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        synchronized(this) {
            if (updateSurface) {
                mSurfaceTexture.updateTexImage();
                mSurfaceTexture.getTransformMatrix(mTexMtx);
                updateSurface = false;
            }
        }
        effect.draw(mTexMtx);
       effect.drawBeauty();
        if(mRenderScreen != null) {
            mRenderScreen.draw();
        }
        if (mRenderSrfTex != null) {
            mRenderSrfTex.draw();
        }

    }

    /**
     * 创建相机预览所需的SurfaceTexture
     */
    private void initSurfaceTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mSurfaceTextureId = textures[0];
        mSurfaceTexture = new SurfaceTexture(mSurfaceTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mSurfaceTextureId);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    /**
     * 开启相机预览
     */
    private void startCameraPreview() {
        try {
            CameraUtils.checkCameraService(view.getContext());
        } catch (CameraDisabledException e) {
           
            e.printStackTrace();
            return;
        } catch (NoCameraException e) {
            e.printStackTrace();
            return;
        }
        CameraHolder.State state = CameraHolder.instance().getState();
        CameraHolder.instance().setSurfaceTexture(mSurfaceTexture);
        if (state != CameraHolder.State.PREVIEW) {
            try {
                CameraHolder.instance().openCamera();
                CameraHolder.instance().startPreview();
                isCameraOpen = true;
            } catch (CameraHardwareException e) {
                e.printStackTrace();
            } catch (CameraNotSupportException e) {
                e.printStackTrace();
            }
        }
    }
    //相机画面更新
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized(this) {
            updateSurface = true;
        }
        view.requestRender();
    }

    /**
     * 设置视频编码器
     * @param recorder
     */
    public void setRecorder(MyRecorder recorder) {
        synchronized(this) {
            if (recorder != null) {
                mRenderSrfTex = new RenderSrfTex(mEffectTextureId, recorder);
                mRenderSrfTex.setVideoSize(mVideoWidth, mVideoHeight);
                Log.e(Tag,"mRenderSrfTex初始化:");
            } else {
                mRenderSrfTex = null;
            }
        }
    }

    /**
     * 设置视频配置信息
     * @param videoConfiguration
     */
    public void setVideoConfiguration(VideoConfiguration videoConfiguration) {
       
        mVideoWidth = VideoMediaCodec.getVideoSize(videoConfiguration.width);
        mVideoHeight = VideoMediaCodec.getVideoSize(videoConfiguration.height);
       if (mRenderScreen!=null){
           mRenderScreen.setVideoSize(mVideoWidth,mVideoHeight);
       }
    }
}
