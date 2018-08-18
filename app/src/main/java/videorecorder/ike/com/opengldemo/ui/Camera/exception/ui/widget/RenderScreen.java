package videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.widget;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import videorecorder.ike.com.opengldemo.R;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.Camera.CameraData;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.Camera.CameraHolder;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.Utils.GlUtil;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.configuration.VideoConfiguration;

/**
 * @author：
 * @createTime:2018/6/9 15:22
 * @function: 将相机的预览界面（FBo）绘制倒手机物理屏幕上
 **/

public class RenderScreen {
    private final int mFboTexId;
    private int mProgram = -1;

    private int maPositionHandle = -1;
    private int maTexCoordHandle = -1;
    private int muPosMtxHandle = -1;
    private int muSamplerHandle = -1;
    private int mScreenW;
    private int mScreenH;

    private final FloatBuffer mNormalVtxBuf = GlUtil.createVertexBuffer();
    private final FloatBuffer mNormalTexCoordBuf = GlUtil.createTexCoordBuffer();
    private final float[] mPosMtx = GlUtil.createIdentityMtx();
    //水印
    public static Bitmap watermaker;
    private FloatBuffer waterMakerByteBuffer;
    private int mWaterTexureId=-1;
    public static Bitmap maker;
    private String Tag="RenderScreen";


    public RenderScreen(int id) {
        mFboTexId = id;
        initGL();
    }

    /**
     * 初始化EGL上下文
     */
    private void initGL() {
        GlUtil.checkGlError("initGL_S");
        final String vertexShader =
                //
                "attribute vec4 position;\n" +
                        "attribute vec4 inputTextureCoordinate;\n" +
                        "uniform   mat4 uPosMtx;\n" +
                        "varying   vec2 textureCoordinate;\n" +
                        "void main() {\n" +
                        "  gl_Position = uPosMtx * position;\n" +
                        "  textureCoordinate   = inputTextureCoordinate.xy;\n" +
                        "}\n";
        final String fragmentShader =
                //
                "precision mediump float;\n" +
                        "uniform sampler2D uSampler;\n" +
                        "varying vec2  textureCoordinate;\n" +
                        "void main() {\n" +
                        "  gl_FragColor = texture2D(uSampler, textureCoordinate);\n" +
                        "}\n";
        mProgram = GlUtil.createProgram(vertexShader, fragmentShader);
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        maTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        muPosMtxHandle = GLES20.glGetUniformLocation(mProgram, "uPosMtx");
        muSamplerHandle = GLES20.glGetUniformLocation(mProgram, "uSampler");
        GlUtil.checkGlError("initGL_E");
        watermaker= BitmapFactory.decodeResource(CameraRender.context.getResources(), R.drawable.watermark);
        initWaterMakerBuffer();
    }
     
    private void initWaterMakerBuffer() {
//        mScreenW=1080;
//        mScreenH=1686;
//        scale=mScreenW/VideoConfiguration.DEFAULT_WIDTH*1.0f;
//        int scalewidth = (int) (128*scale);
//        int scaleheight = (int) (64*scale);
//        int scalevMargin = (int) (30*scale);
//        int scalehMargin = (int) (30*scale);
//        float leftX = (mScreenW/2.0f - scalehMargin - scalewidth)/(mScreenW/2.0f);
//        float rightX = (mScreenW/2.0f - scalehMargin)/(mScreenW/2.0f);
//
//        float topY = (mScreenH/2.0f - scalevMargin)/(mScreenH/2.0f);
//        float bottomY = (mScreenH/2.0f - scalevMargin -scaleheight)/(mScreenH/2.0f);
         float[] waterMakerFloat=new float[]{
                 -1f, -1f,0,
                 -1f, 1f,0,
                 1f, -1f,0,
                 1f, 1f,0,

        };
//        Log.e(Tag,leftX+","+rightX+","+bottomY+"，"+topY);
//        float[] waterMakerFloat=new float[]{
//                -leftX ,-bottomY,0,
//                -leftX,topY,0,
//                rightX,-bottomY,
//                rightX,topY,
//        };
        ByteBuffer bb = ByteBuffer.allocateDirect(waterMakerFloat.length * 4);
        bb.order(ByteOrder.nativeOrder());
        waterMakerByteBuffer=bb.asFloatBuffer();
        waterMakerByteBuffer.put(waterMakerFloat);
        waterMakerByteBuffer.position(0);
    }

    public void draw() {
        if (mScreenW <= 0 || mScreenH <= 0) {
            return;
        }
        
        GlUtil.checkGlError("draw_S");

        GLES20.glViewport(0, 0, mScreenW, mScreenH);
        
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);

        mNormalVtxBuf.position(0);
        GLES20.glVertexAttribPointer(maPositionHandle,
                3, GLES20.GL_FLOAT, false, 4 * 3, mNormalVtxBuf);
        GLES20.glEnableVertexAttribArray(maPositionHandle);

        mCameraTexCoordBuffer.position(0);
        GLES20.glVertexAttribPointer(maTexCoordHandle,
                2, GLES20.GL_FLOAT, false, 4 * 2, mCameraTexCoordBuffer);
        GLES20.glEnableVertexAttribArray(maTexCoordHandle);
        GLES20.glUniformMatrix4fv(muPosMtxHandle, 1, false, mPosMtx, 0);
        GLES20.glUniform1i(muSamplerHandle, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFboTexId);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        //绘制水印纹理
       drawWatermark();

        GlUtil.checkGlError("draw_E");
    }

//    private void drawWatermark() {
//        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1f);
//        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT|GLES20.GL_COLOR_BUFFER_BIT);
//        GLES20.glViewport(0,0,mScreenW,mScreenH);
//        GLES20.glUseProgram(mProgram);
//        waterMakerByteBuffer.position(0);
//        GLES20.glVertexAttribPointer(maPositionHandle,3,GLES20.GL_FLOAT,
//                false,12,waterMakerByteBuffer);
//        GLES20.glEnableVertexAttribArray(maPositionHandle);
//        mNormalTexCoordBuf.position(0);
//        GLES20.glVertexAttribPointer(maTexCoordHandle,3,GLES20.GL_FLOAT,
//                false,12,mNormalTexCoordBuf);
//        GLES20.glEnableVertexAttribArray(maTexCoordHandle);
//        if (mWaterTexureId==-1){
//             int[] texTd=new int[1];
//            GLES20.glGenTextures(1,texTd,0);
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texTd[0]);
//            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,watermaker,0);
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
//                    GLES20.GL_LINEAR);
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
//                    GLES20.GL_LINEAR);
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
//                    GLES20.GL_CLAMP_TO_EDGE);
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
//                    GLES20.GL_CLAMP_TO_EDGE);
//            mWaterTexureId = texTd[0];
//        }
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,mWaterTexureId);
//        GLES20.glEnable(GLES20.GL_BLEND);
//        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
//        GLES20.glDisable(GLES20.GL_BLEND);
//        
//        
//        
//    }
private void drawWatermark() {
    GLES20.glViewport(30,30,maker.getWidth()/2,maker.getHeight()/2);
    waterMakerByteBuffer.position(0);
    GLES20.glVertexAttribPointer(maPositionHandle,
            3, GLES20.GL_FLOAT, false, 4 * 3, waterMakerByteBuffer);
    GLES20.glEnableVertexAttribArray(maPositionHandle);

    mNormalTexCoordBuf.position(0);
    GLES20.glVertexAttribPointer(maTexCoordHandle,
            2, GLES20.GL_FLOAT, false, 4 * 2, mNormalTexCoordBuf);
    GLES20.glEnableVertexAttribArray(maTexCoordHandle);
        
    if(mWaterTexureId == -1) {
        
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, maker, 0);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        mWaterTexureId = textures[0];
    }
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mWaterTexureId);
    GlUtil.createIdentityMtx();
    GLES20.glUniformMatrix4fv(muPosMtxHandle, 1, false, mPosMtx, 0);
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    GLES20.glEnable(GLES20.GL_BLEND);

    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    GLES20.glDisable(GLES20.GL_BLEND);
    GLES20.glViewport(0,0,mScreenW,mScreenH);
}
    private float scale;
    public void setScreenSize(int width, int height) {
        mScreenW = width;
        mScreenH = height;
        Log.e(Tag,"屏幕宽高："+width+",height："+height);
        Log.e(Tag,"视频宽高："+ VideoConfiguration.DEFAULT_WIDTH+",height："+VideoConfiguration.DEFAULT_HEIGHT);
 
        initCameraTexCoordBuffer();
    }
    public void setVideoSize(int width,int height){
       
    }
    private FloatBuffer mCameraTexCoordBuffer;
    private void initCameraTexCoordBuffer() {
        int cameraWidth, cameraHeight;
        CameraData cameraData = CameraHolder.instance().getCameraData();
        int width = cameraData.cameraWidth;
        int height = cameraData.cameraHeight;
        Log.e(Tag,"相机预览的宽高:"+width+",height:"+height);
        if(CameraHolder.instance().isLandscape()) {
            cameraWidth = Math.max(width, height);
            cameraHeight = Math.min(width, height);
        } else {
            cameraWidth = Math.min(width, height);
            cameraHeight = Math.max(width, height);
        }

        float hRatio = mScreenW / ((float)cameraWidth);
        float vRatio = mScreenH / ((float)cameraHeight);

        float ratio;
        if(hRatio > vRatio) {
            ratio = mScreenH / (cameraHeight * hRatio);
            final float vtx[] = {
                    //UV
                    0f, 0.5f + ratio/2,
                    0f, 0.5f - ratio/2,
                    1f, 0.5f + ratio/2,
                    1f, 0.5f - ratio/2,
            };
            ByteBuffer bb = ByteBuffer.allocateDirect(4 * vtx.length);
            bb.order(ByteOrder.nativeOrder());
            mCameraTexCoordBuffer = bb.asFloatBuffer();
            mCameraTexCoordBuffer.put(vtx);
            mCameraTexCoordBuffer.position(0);
        } else {
            ratio = mScreenW/ (cameraWidth * vRatio);
            final float vtx[] = {
                    //UV
                    0.5f - ratio/2, 1f,
                    0.5f - ratio/2, 0f,
                    0.5f + ratio/2, 1f,
                    0.5f + ratio/2, 0f,
            };
            ByteBuffer bb = ByteBuffer.allocateDirect(4 * vtx.length);
            bb.order(ByteOrder.nativeOrder());
            mCameraTexCoordBuffer = bb.asFloatBuffer();
            mCameraTexCoordBuffer.put(vtx);
            mCameraTexCoordBuffer.position(0);
            
        }

    }

    public void setMaker(Bitmap maker) {
        this.maker = maker;
    }
}
