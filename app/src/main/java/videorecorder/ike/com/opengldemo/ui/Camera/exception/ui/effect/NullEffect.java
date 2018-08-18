package videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.effect;

import android.content.Context;

import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.Utils.GLSLFileUtils;

/**
*@author：
@createTime:2018/6/9 15:26
@function: 默认使用的效果（没有特效）
**/

public class NullEffect extends Effect {
    private static final String NULL_EFFECT_VERTEX = "null/vertexshader.glsl";
    private static final String NULL_EFFECT_FRAGMENT = "null/fragmentshader.glsl";

    public NullEffect(Context context) {
        super(context);
        String vertexShader = GLSLFileUtils.getFileContextFromAssets(context, NULL_EFFECT_VERTEX);
        String fragmentShader = GLSLFileUtils.getFileContextFromAssets(context, NULL_EFFECT_FRAGMENT);
        super.setShader(vertexShader, fragmentShader);
    }
}
