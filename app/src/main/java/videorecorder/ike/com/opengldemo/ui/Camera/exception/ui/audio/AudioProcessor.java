package videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.audio;

import android.media.AudioRecord;
import android.media.MediaMuxer;

import java.util.Arrays;

import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.configuration.AudioConfiguration;

/**
 * @Title: AudioProcessor
 * @Package com.laifeng.sopcastsdk.audio
 * @Description:
 * @Author Jim
 * @Date 16/9/19
 * @Time 上午9:56
 * @Version
 */
public class AudioProcessor extends Thread {
    private volatile boolean mPauseFlag;
    private volatile boolean mStopFlag;
    private volatile boolean mMute;
    private AudioRecord mAudioRecord;
    private AudioEncoder mAudioEncoder;
    private byte[] mRecordBuffer;
    private int mRecordBufferSize;
    private MediaMuxer mediaMuxer;

    public AudioProcessor(AudioRecord audioRecord, AudioConfiguration audioConfiguration) {
        mRecordBufferSize = AudioUtils.getRecordBufferSize(audioConfiguration);
        mRecordBuffer =  new byte[mRecordBufferSize];
        mAudioRecord = audioRecord;
        mAudioEncoder = new AudioEncoder(audioConfiguration);
        mAudioEncoder.setMediaMuxer(mediaMuxer);
        mAudioEncoder.prepareEncoder();
       
    }

    public void setAudioHEncodeListener(OnAudioEncodeListener listener) {
        mAudioEncoder.setOnAudioEncodeListener(listener);
    }

    public void stopEncode() {
        mStopFlag = true;
        if(mAudioEncoder != null) {
            mAudioEncoder.stop();
            mAudioEncoder = null;
        }
    }

    public void pauseEncode(boolean pause) {
        mPauseFlag = pause;
    }

    public void setMute(boolean mute) {
        mMute = mute;
    }
    @Override
    public void run() {
        while (!mStopFlag) {
            while (mPauseFlag) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            int readLen = mAudioRecord.read(mRecordBuffer, 0, mRecordBufferSize);
            if (readLen > 0) {
                if (mMute) {
                    byte clearM = 0;
                    Arrays.fill(mRecordBuffer, clearM);
                }
                if(mAudioEncoder != null) {
                    mAudioEncoder.offerEncoder(mRecordBuffer);
                    
                }
            }
        }
    }

    public void setMediaMuxer(MediaMuxer mediaMuxer) {
        this.mediaMuxer = mediaMuxer;
    }
}
