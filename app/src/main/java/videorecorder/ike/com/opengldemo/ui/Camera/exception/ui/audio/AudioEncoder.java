package videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.audio;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import java.nio.ByteBuffer;

import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.configuration.AudioConfiguration;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.mediacodec.AudioMediaCodec;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.widget.MyRecorder;

/**
 * @Title: AudioEncoder
 * @Package com.laifeng.sopcastsdk.audio
 * @Description:
 * @Author Jim
 * @Date 16/9/19
 * @Time 上午9:57
 * @Version
 */
@TargetApi(18)
public class AudioEncoder {
    private MediaCodec mMediaCodec;
    private OnAudioEncodeListener mListener;
    private AudioConfiguration mAudioConfiguration;
    MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private String Tag = "AudioEncoder";
    private MediaMuxer mediaMuxer;
    public static int mAudioTrackIndex;
    private boolean mMuxerStarted;
    private Object lock=new Object();

    public void setOnAudioEncodeListener(OnAudioEncodeListener listener) {
        mListener = listener;
    }

    public AudioEncoder(AudioConfiguration audioConfiguration) {
        mAudioConfiguration = audioConfiguration;
    }

    public void prepareEncoder() {
        mMediaCodec = AudioMediaCodec.getAudioMediaCodec(mAudioConfiguration);
        mMediaCodec.start();
    }

    synchronized public void stop() {
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
            mAudioTrackIndex=-1;
        }
    }

    public synchronized void offerEncoder(byte[] input) {
        if (mMediaCodec == null) {
            return;
        }
        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(12000);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(input);
            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, 0, 0);
        }

        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 12000);

        do {
            if (outputBufferIndex >= 0) {
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.e(Tag, "audio end");
                    mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    return;
                }
                ByteBuffer buffer = getOutputBuffer(mMediaCodec, outputBufferIndex);
                buffer.position(mBufferInfo.offset);
                if (mMuxerStarted) {
                    try {
                        mediaMuxer.writeSampleData(mAudioTrackIndex, buffer, mBufferInfo);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
//            if(mListener != null) {
//                mListener.onAudioEncode(outputBuffer, mBufferInfo);
//            }
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
            }else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                synchronized (lock) {
                    mAudioTrackIndex = mediaMuxer.addTrack(mMediaCodec.getOutputFormat());
                    Log.e(Tag, "add audio track-->" + mAudioTrackIndex);
                    if (mAudioTrackIndex >= 0 && MyRecorder.videoTrack >= 0) {
                        if (!MyRecorder.isMediaMuxerstart){
                            mediaMuxer.start();
                            MyRecorder.isMediaMuxerstart=true;
                            mMuxerStarted = true;
                        }


                    }
                   
                }
            }
        }while (outputBufferIndex >= 0);

    }

    private ByteBuffer getOutputBuffer(MediaCodec codec, int index) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return codec.getOutputBuffer(index);
        } else {
            return codec.getOutputBuffers()[index];

        }
    }

    public void setMediaMuxer(MediaMuxer mediaMuxer) {

        this.mediaMuxer = mediaMuxer;
    }
}
