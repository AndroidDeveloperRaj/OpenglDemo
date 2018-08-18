package videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.effect;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;


import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.Utils.GLSLFileUtils;

/**
 * @Title: GrayEffect
 * @Package com.laifeng.sopcastsdk.video.effect
 * @Description:
 * @Author Jim
 * @Date 16/9/18
 * @Time 下午2:11
 * @Version
 */
public class GrayEffect extends Effect{
    private String Tag="GrayEffect";

    private static final String GRAY_EFFECT_VERTEX = "gray/vertexshader.glsl";
    private static final String GRAY_EFFECT_FRAGMENT = "gray/fragmentshader.glsl";

    public GrayEffect(Context context) {
        super(context);
        String vertexShader = GLSLFileUtils.getFileContextFromAssets(context, GRAY_EFFECT_VERTEX);
        String fragmentShader = GLSLFileUtils.getFileContextFromAssets(context, GRAY_EFFECT_FRAGMENT);
        
        super.setShader(vertexShader, fragmentShader);
    }
}
