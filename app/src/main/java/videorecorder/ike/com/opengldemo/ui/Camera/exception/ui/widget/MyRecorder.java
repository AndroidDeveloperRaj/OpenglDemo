package videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.widget;

import android.annotation.TargetApi;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;


import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.audio.AudioUtils;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.audio.IAudioController;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.audio.OnAudioEncodeListener;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.configuration.AudioConfiguration;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.configuration.VideoConfiguration;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.mediacodec.AudioMediaCodec;
import videorecorder.ike.com.opengldemo.ui.Camera.exception.ui.mediacodec.VideoMediaCodec;

/**
*@author：
@createTime:2018/6/9 16:57
@function: 视频编码（合成MP4文件）
**/

public class MyRecorder {
    private Object lock=new Object();
    private MediaCodec mMediaCodec;
    private InputSurface mInputSurface;
    private final VideoConfiguration mConfiguration;
    private HandlerThread mHandlerThread;
    private Handler mEncoderHandler;
    private ReentrantLock encodeLock = new ReentrantLock();
    private volatile boolean isStarted;
    private MediaCodec.BufferInfo mBufferInfo;
    private boolean mPause;
    private MediaMuxer mMediaMuxer;
    public static int videoTrack=-1;
    public  int mAudioTrackIndex=-1;
    private String Tag="MyRecorder";
    public static boolean isMediaMuxerstart;
    private boolean isEnd=true;
    private long baseTimeStamp=-1;
    public NormalAudioController audioController;

    public MyRecorder(VideoConfiguration configuration) {
        mConfiguration = configuration;
    }
    public void prepareEncoder() {
        if (mMediaCodec != null || mInputSurface != null) {

            throw new RuntimeException("prepareEncoder called twice?");
        }
        mMediaCodec = VideoMediaCodec.getVideoMediaCodec(mConfiguration);
        mHandlerThread = new HandlerThread("SopCastEncode");
        mHandlerThread.start();
        mEncoderHandler = new Handler(mHandlerThread.getLooper());
        mBufferInfo = new MediaCodec.BufferInfo();
        isStarted = true;
        
        //配置MediaMuxer
        String savePath= Environment.getExternalStorageDirectory().getAbsolutePath()+"/openGlMovie";
        File file = new File(savePath);
        if (!file.exists()){
            file.mkdirs();
        }
        String fileName="openGlRecorder.mp4";
        savePath=savePath+"/"+fileName;
        File file1 = new File(savePath);
        if (file1.exists()){
            file1.delete();
        }
        //配置混合器
        try {
            mMediaMuxer = new MediaMuxer(savePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            Log.e(Tag,"mMediaMuxer出错了:"+e.getMessage());
            e.printStackTrace();
        }
        ////////////初始化音频采集
        audioController=new NormalAudioController();
        audioController.setAudioConfiguration(AudioConfiguration.createDefault());
        Log.e(Tag,"savePath:"+savePath);
    }
    public boolean firstTimeSetup() {
        if (mMediaCodec == null || mInputSurface != null) {
            return false;
        }
        try {
            mInputSurface = new InputSurface(mMediaCodec.createInputSurface());
            mMediaCodec.start();
        } catch (Exception e) {
            releaseEncoder();
            throw (RuntimeException)e;
        }
        return true;
    }
    private void releaseEncoder() {
        if (mMediaCodec != null) {
            mMediaCodec.signalEndOfInputStream();
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }
    }
    public void makeCurrent() {
        mInputSurface.makeCurrent();
    }
    public long vbaseTime=-1;
    public void swapBuffers() {
        if (mMediaCodec == null || mPause) {
            return;
        }
        if (vbaseTime==-1){
            vbaseTime=System.nanoTime();
        }
        mInputSurface.setPresentationTime(System.nanoTime()-vbaseTime);
        mInputSurface.swapBuffers();
    }
    public void startSwapData() {
        mEncoderHandler.post(swapDataRunnable);
    }
    private Runnable swapDataRunnable = new Runnable() {
        @Override
        public void run() {
            //drainEncoder();
            isEnd=false;
            isStarted=true;
            enCodeVideo();
        }
    };
    private void drainEncoder() {
        ByteBuffer[] outBuffers = mMediaCodec.getOutputBuffers();
        while (isStarted) {
            encodeLock.lock();
            if(mMediaCodec != null) {
                int outBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 12000);
                if (outBufferIndex >= 0) {
                    ByteBuffer bb = outBuffers[outBufferIndex];
//                    if (mListener != null) {
//                        mListener.onVideoEncode(bb, mBufferInfo);
//                    }
                    Log.e(Tag,"编码完成");
                    mMediaCodec.releaseOutputBuffer(outBufferIndex, false);
                } else {
                    try {
                        // wait 10ms
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                encodeLock.unlock();
            } else {
                encodeLock.unlock();
                break;
            }
        }
    }
    public void stopRecorde(){
        mMediaCodec.signalEndOfInputStream();
        Log.e(Tag,"发送结束命令");
        videoTrack=-1;
        mAudioTrackIndex=-1;
        if(isMediaMuxerstart){
            isMediaMuxerstart=false;
            mMediaMuxer.stop();
            audioController.stop();
        }
       
    }
    public void enCodeVideo() {
        ByteBuffer outputBuffer = null;
        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        while (isStarted) {
            int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 12000);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
               
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
               synchronized (lock){
                   //添加视频音轨
                   if (isMediaMuxerstart) {
                       throw new RuntimeException("视频混和器MediaMuxer已经开始工作,不能添加视频音轨");
                   }
                   MediaFormat newFormat = mMediaCodec.getOutputFormat();
                   videoTrack = mMediaMuxer.addTrack(newFormat);
                   Log.e(Tag, "add videoTrack track-->" + videoTrack);
                   Log.e(Tag, "输出的视频音轨格式:"+videoTrack+"," + newFormat);

                   if (videoTrack >= 0
                           && mAudioTrackIndex>=0
                           ){
                       if (!isMediaMuxerstart){
                           mMediaMuxer.start();
                           isMediaMuxerstart = true;
                           Log.e(Tag,"视频开始");
                       }
                   }
               }
             
               
            } else if (encoderStatus < 0) {
              
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                //获取编码数据成功
                outputBuffer=encoderOutputBuffers[encoderStatus];
                if (outputBuffer == null) {
                  

                } else if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                   
                    mBufferInfo.size = 0;
                }else {
                    if (outputBuffer!=null&&mBufferInfo.size > 0) {
                        if (isMediaMuxerstart) {
                            mMediaMuxer.writeSampleData(videoTrack, outputBuffer, mBufferInfo);
                            Log.e(Tag, "开始混合视频: " + mBufferInfo.size + " bytes to muxer, ts=" +
                                    mBufferInfo.presentationTimeUs);
                        }
                       
                    }
                    if (encoderStatus >= 0) {
                        mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                    }
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.e(Tag, "视屏到了结束的位置");
                        isStarted=false;
                        break;
                    }
                }
               
            }
            
        }
    }
    ///////////////////////音频录制相关
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
        public int getSessionId() {
            if(mAudioRecord != null) {
                return mAudioRecord.getAudioSessionId();
            } else {
                return -1;
            }
        }

        @Override
        public void setAudioConfiguration(AudioConfiguration audioConfiguration) {
            mAudioConfiguration = audioConfiguration;
        }


    }
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

       
    }

    public class AudioEncoder {
        private long aubaseTime=-1;
        private MediaCodec mMediaCodec;
        private OnAudioEncodeListener mListener;
        private AudioConfiguration mAudioConfiguration;
        MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
        private String Tag = "AudioEncoder";
        
      
        private boolean mMuxerStarted;
      

        public void setOnAudioEncodeListener(OnAudioEncodeListener listener) {
            mListener = listener;
        }

        public AudioEncoder(AudioConfiguration audioConfiguration) {
            mAudioConfiguration = audioConfiguration;
        }

        void prepareEncoder() {
            mMediaCodec = AudioMediaCodec.getAudioMediaCodec(mAudioConfiguration);
            mMediaCodec.start();
            aubaseTime=System.nanoTime();
        }

        synchronized public void stop() {
            if (mMediaCodec != null) {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
                mAudioTrackIndex=-1;
            }
        }

        synchronized void offerEncoder(byte[] input) {
            if (mMediaCodec == null) {
                return;
            }
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(12000);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(input);
                if (aubaseTime!=-1){
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, (System.nanoTime()-aubaseTime)/1000, 0);
                }else {
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, 0, 0);
                }
                
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
                            
                            mMediaMuxer.writeSampleData(mAudioTrackIndex, buffer, mBufferInfo);
                            Log.e(Tag, "开始混合音频: " + mBufferInfo.size + " bytes to muxer, ts=" +
                                    mBufferInfo.presentationTimeUs);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
//            if(mListener != null) {
//                mListener.onAudioEncode(outputBuffer, mBufferInfo);
//            }
                   // mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
                }else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    synchronized (lock) {
                        mAudioTrackIndex = mMediaMuxer.addTrack(mMediaCodec.getOutputFormat());
                        Log.e(Tag, "add audio track-->" + mAudioTrackIndex);
                        if (mAudioTrackIndex >= 0 && MyRecorder.videoTrack >= 0) {
                            if (!MyRecorder.isMediaMuxerstart){
                                mMediaMuxer.start();
                                MyRecorder.isMediaMuxerstart=true;
                                mMuxerStarted = true;
                                Log.e(Tag,"音频开始");
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
       
        
    }


}
