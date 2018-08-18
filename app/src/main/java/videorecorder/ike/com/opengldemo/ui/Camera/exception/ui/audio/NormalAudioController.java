package videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.audio;

import android.annotation.TargetApi;
import android.media.AudioRecord;
import android.media.MediaMuxer;

import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.configuration.AudioConfiguration;

/**
 * @Title: NormalAudioController
 * @Package com.laifeng.sopcastsdk.controller.audio
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 下午12:53
 * @Version
 */
public class NormalAudioController implements IAudioController {
    private OnAudioEncodeListener mListener;
    private AudioRecord mAudioRecord;
    private AudioProcessor mAudioProcessor;
    private boolean mMute;
    private AudioConfiguration mAudioConfiguration;
    private MediaMuxer mediaMuxer;

    public NormalAudioController() {
        mAudioConfiguration = AudioConfiguration.createDefault();
    }
    @Override
    public void setAudioConfiguration(AudioConfiguration audioConfiguration) {
        mAudioConfiguration = audioConfiguration;
    }
    
    @Override
    public void setAudioEncodeListener(OnAudioEncodeListener listener) {
        mListener = listener;
    }
    @Override
    public void start() {
       
        mAudioRecord = AudioUtils.getAudioRecord(mAudioConfiguration);
        try {
            mAudioRecord.startRecording();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mAudioProcessor = new AudioProcessor(mAudioRecord, mAudioConfiguration);
        mAudioProcessor.setMediaMuxer(mediaMuxer);
        mAudioProcessor.setAudioHEncodeListener(mListener);
        mAudioProcessor.start();
        mAudioProcessor.setMute(mMute);
       
    }
    @Override
    public void stop() {
        
        if(mAudioProcessor != null) {
            mAudioProcessor.stopEncode();
        }
        if(mAudioRecord != null) {
            try {
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public void pause() {
      
        if(mAudioRecord != null) {
            mAudioRecord.stop();
        }
        if (mAudioProcessor != null) {
            mAudioProcessor.pauseEncode(true);
        }
    }
    @Override
    public void resume() {
        if(mAudioRecord != null) {
            mAudioRecord.startRecording();
        }
        if (mAudioProcessor != null) {
            mAudioProcessor.pauseEncode(false);
        }
    }
    @Override
    public void mute(boolean mute) {
        mMute = mute;
        if(mAudioProcessor != null) {
            
            mAudioProcessor.setMute(mMute);
        }
    }

    @Override
    @TargetApi(16)
    public int getSessionId() {
        if(mAudioRecord != null) {
            return mAudioRecord.getAudioSessionId();
        } else {
            return -1;
        }
    }


    public void setMediaMuxer(MediaMuxer mediaMuxer) {
        this.mediaMuxer = mediaMuxer;
    }
}
