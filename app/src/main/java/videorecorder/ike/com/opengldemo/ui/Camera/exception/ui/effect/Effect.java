package videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.effect;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.text.TextUtils;
import android.util.Log;

import java.nio.FloatBuffer;

import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.Camera.CameraData;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.Camera.CameraHolder;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.Utils.GlUtil;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.effect.beauty.Beauty;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.effect.beauty.MagicBeautyFilter;

/**
*@author：
@createTime:2018/6/9 15:25
@function: 特效的基类
**/

public class Effect {

    public static final String SHARDE_NULL_VERTEX = "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "\n" +
            "uniform   mat4 uPosMtx;\n" +
            "uniform   mat4 uTexMtx;\n" +
            "varying   vec2 textureCoordinate;\n" +
            "void main() {\n" +
            "  gl_Position = uPosMtx * position;\n" +
            "  textureCoordinate   = (uTexMtx * inputTextureCoordinate).xy;\n" +
            "}";

    public static final String SHARDE_NULL_FRAGMENT = "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 textureCoordinate;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "    vec4 tc = texture2D(sTexture, textureCoordinate);\n" +
            "    gl_FragColor = vec4(tc.r, tc.g, tc.b, 1.0);\n" +
            "}";
    private int effectTextureId;
    private String mVertex;
    private String mFragment;
    public int mProgram            = -1;
    private int maPositionHandle    = -1;
    private int maTexCoordHandle    = -1;
    private int muPosMtxHandle      = -1;
    private int muTexMtxHandle      = -1;

    private int mWidth  = -1;
    private int mHeight = -1;

    private final int[]       mFboId  = new int[]{0};
    private final int[]       mRboId  = new int[]{0};
    private final int[]       mTexId  = new int[]{0};

    private final FloatBuffer mVtxBuf = GlUtil.createSquareVtx();
    private final float[]     mPosMtx = GlUtil.createIdentityMtx();
    private MagicBeautyFilter beautyFilter;
    private String Tag="Effect";
    private Context context;

    public Effect(Context context) {
        this.context=context;
    }
    public void setShader(String vertex, String fragment) {
        mVertex = vertex;
        mFragment = fragment;
    }
    public void setTextureId(int textureId) {
        this.effectTextureId = textureId;
    }
    
    public void prepare() {
//        beautyFilter=new Beauty(context.getResources());
//        beautyFilter.create();
        loadShaderAndParams(mVertex,mFragment);
        initSize();
        createEffectTexture();
        createBeautyFrameBuffer();
        beautyFilter=new MagicBeautyFilter();
        beautyFilter.init();
        
       
    }

    

    /**
     * 创建opengl渲染程序
     * @param vertex
     * @param fragment
     */
    public void loadShaderAndParams(String vertex, String fragment) {
        if(TextUtils.isEmpty(vertex) || TextUtils.isEmpty(fragment)) {
            vertex = SHARDE_NULL_VERTEX;
            fragment = SHARDE_NULL_FRAGMENT;
        }
        GlUtil.checkGlError("initSH_S");
        mProgram = GlUtil.createProgram(vertex, fragment);
        //查找程序中的渲染句柄
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        maTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");

        muPosMtxHandle   = GLES20.glGetUniformLocation(mProgram, "uPosMtx");
        muTexMtxHandle   = GLES20.glGetUniformLocation(mProgram, "uTexMtx");
        GlUtil.checkGlError("initSH_E");
    }

    /**
     * 初始化绘制的图像大小
     */
    private void initSize() {
        if(CameraHolder.instance().getState() != CameraHolder.State.PREVIEW) {
            return;
        }
        CameraData cameraData = CameraHolder.instance().getCameraData();
        int width = cameraData.cameraWidth;
        int height = cameraData.cameraHeight;
        if(CameraHolder.instance().isLandscape()) {
            mWidth = Math.max(width, height);
            mHeight = Math.min(width, height);
        } else {
            mWidth = Math.min(width, height);
            mHeight = Math.max(width, height);
        }
        

    }

    /**
     * 创建特效的纹理渲染对象，并将创建的纹理对象和帧渲染区绑定到帧缓冲区(帧缓冲区（mFboId），帧渲染区（mRboId）)
     */
    private void createEffectTexture() {
        if(CameraHolder.instance().getState() != CameraHolder.State.PREVIEW) {
            return;
        }
        GlUtil.checkGlError("initFBO_S");
        GLES20.glGenFramebuffers(1, mFboId, 0);
        GLES20.glGenRenderbuffers(1, mRboId, 0);
        GLES20.glGenTextures(1, mTexId, 0);
        Log.e(Tag,"mTexId:"+mTexId[0]);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mRboId[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER,
                GLES20.GL_DEPTH_COMPONENT16, mWidth, mHeight);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId[0]);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mRboId[0]);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexId[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                mWidth, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mTexId[0], 0);

        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) !=
                GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("glCheckFramebufferStatus()");
        }
        GlUtil.checkGlError("initFBO_E");
    }
    private int[] beautyTextureId=new int[1];
    private void createBeautyEffectTexture() {
        if(CameraHolder.instance().getState() != CameraHolder.State.PREVIEW) {
            return;
        }
        Log.e(Tag,"beautyTextureId:"+beautyTextureId[0]);
        GlUtil.checkGlError("initBeauty_S");
        GLES20.glGenTextures(1, beautyTextureId, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, beautyTextureId[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                mWidth, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        
    }
    //创建FrameBuffer
    private int[] beautyFrame=new int[1];
    private int[] beautyRender=new int[1];
    private void createBeautyFrameBuffer() {
        GLES20.glGenFramebuffers(1, beautyFrame, 0);
        GLES20.glGenRenderbuffers(1,  beautyRender, 0);

        createBeautyEffectTexture();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, beautyFrame[0]);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, beautyRender[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, mWidth,
                mHeight);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, beautyTextureId[0], 0);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, beautyRender[0]);
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) !=
                GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("glCheckBeautyFramebufferStatus()");
        }
        GlUtil.checkGlError("initBeautyFBO_E");

    }
    public void draw(final float[] tex_mtx) {
        if (-1 == mProgram || effectTextureId == -1 || mWidth == -1) {
            return;
        }

        GlUtil.checkGlError("draw_S");
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId[0]);

        GLES20.glViewport(0, 0, mWidth, mHeight);
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);
        //runPendingOnDrawTasks();

        mVtxBuf.position(0);
        GLES20.glVertexAttribPointer(maPositionHandle,
                3, GLES20.GL_FLOAT, false, 4 * (3 + 2), mVtxBuf);
        GLES20.glEnableVertexAttribArray(maPositionHandle);

        mVtxBuf.position(3);
        GLES20.glVertexAttribPointer(maTexCoordHandle,
                2, GLES20.GL_FLOAT, false, 4 * (3 + 2), mVtxBuf);
        GLES20.glEnableVertexAttribArray(maTexCoordHandle);

        if(muPosMtxHandle>= 0){
            GLES20.glUniformMatrix4fv(muPosMtxHandle, 1, false, mPosMtx, 0);
        }
        
        if(muTexMtxHandle>= 0){
            GLES20.glUniformMatrix4fv(muTexMtxHandle, 1, false, tex_mtx, 0);
        }
          

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, effectTextureId);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GlUtil.checkGlError("draw_E");
       
    }
    
    public void drawBeauty(){
        GLES20.glViewport(0, 0, mWidth, mHeight);
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        //美颜绘制
        if (beautyFilter!=null){
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, beautyFrame[0]);
            beautyFilter.onDrawFrame(mTexId[0]);
//            beautyFilter.setSize(mWidth,mHeight);
//            beautyFilter.setTextureId(mTexId[0]);
//            beautyFilter.draw();
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }
    /**
     * 获取与FBo绑定的纹理对象那个，这个纹理对象上面绘制了特效处理后的画面
     * @return
     */
    public int getEffertedTextureId() {
         //return mTexId[0];
        return beautyTextureId[0];
    }
}
