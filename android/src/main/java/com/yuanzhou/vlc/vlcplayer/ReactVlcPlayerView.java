package com.yuanzhou.vlc.vlcplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.uimanager.ThemedReactContext;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCUtil;
import java.util.ArrayList;

@SuppressLint("ViewConstructor")
class ReactVlcPlayerView extends SurfaceView implements
        LifecycleEventListener,
        AudioManager.OnAudioFocusChangeListener{

    private static final String TAG = "ReactExoplayerView";


    private final VideoEventEmitter eventEmitter;

    private Handler mainHandler;

    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoHeight = 0;
    private int mVideoWidth = 0;
    private int mVideoVisibleHeight;
    private int mVideoVisibleWidth;
    private int mSarNum;
    private int mSarDen;


    SurfaceView surfaceView;

    private int counter = 0;

    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;
    private static final int SURFACE_FIT_VERTICAL = 2;
    private static final int SURFACE_FILL = 3;
    private static final int SURFACE_16_9 = 4;
    private static final int SURFACE_4_3 = 5;
    private static final int SURFACE_ORIGINAL = 6;
    private int mCurrentSize = SURFACE_BEST_FIT;
    private int screenWidth;
    private int screenHeight;

    private boolean isPaused = true;
    private boolean isBuffering;
    private float rate = 1f;

    private String src;

    // Props from React
    private Uri srcUri;
    private String extension;
    private boolean repeat;
    private boolean disableFocus;
    private boolean playInBackground = false;
    // \ End props

    // React
    private final ThemedReactContext themedReactContext;
    private final AudioManager audioManager;

    public ReactVlcPlayerView(ThemedReactContext context) {
        super(context);
        this.eventEmitter = new VideoEventEmitter(context);
        this.themedReactContext = context;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        themedReactContext.addLifecycleEventListener(this);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenHeight = dm.heightPixels;
        screenWidth = dm.widthPixels;
    }


    @Override
    public void setId(int id) {
        super.setId(id);
        eventEmitter.setViewId(id);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //createPlayer();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopPlayback();
    }

    // LifecycleEventListener implementation

    @Override
    public void onHostResume() {
        if (playInBackground) {
            return;
        }
        setPlayWhenReady(!isPaused);
    }

    @Override
    public void onHostPause() {
        if (playInBackground) {
            return;
        }
        setPlayWhenReady(false);
    }

    @Override
    public void onHostDestroy() {
        stopPlayback();
    }

    public void cleanUpResources() {
        stopPlayback();
    }

    private void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(callback);
        vout.detachViews();
        surfaceView.removeOnLayoutChangeListener(onLayoutChangeListener);
        libvlc.release();
        libvlc = null;
        //mVideoWidth = 0;
        //mVideoHeight = 0;
    }

    private boolean requestAudioFocus() {
        if (disableFocus) {
            return true;
        }
        int result = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void setPlayWhenReady(boolean playWhenReady) {
    }


    private void stopPlayback() {
        onStopPlayback();
        releasePlayer();
    }

    private void onStopPlayback() {
        setKeepScreenOn(false);
        audioManager.abandonAudioFocus(this);
    }

    // AudioManager.OnAudioFocusChangeListener implementation

    @Override
    public void onAudioFocusChange(int focusChange) {
       /* switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                eventEmitter.audioFocusChanged(false);
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                eventEmitter.audioFocusChanged(true);
                break;
            default:
                break;
        }

        if (player != null) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                // Lower the volume
                player.setVolume(0.8f);
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // Raise it back to normal
                player.setVolume(1);
            }
        }*/
    }

    public void setPlayInBackground(boolean playInBackground) {
        this.playInBackground = playInBackground;
    }

    public void setDisableFocus(boolean disableFocus) {
        this.disableFocus = disableFocus;
    }



    private void createPlayer(boolean autoplay) {
        releasePlayer();
        try {
            // Create LibVLC
            ArrayList<String> options = new ArrayList<String>(50);
            libvlc = new LibVLC(getContext(), options);
            // Create media player
            mMediaPlayer = new MediaPlayer(libvlc);
            mMediaPlayer.setEventListener(mPlayerListener);
            surfaceView = this;
            surfaceView.addOnLayoutChangeListener(onLayoutChangeListener);
            this.getHolder().setKeepScreenOn(true);
            IVLCVout vlcOut =  mMediaPlayer.getVLCVout();
            if(mVideoWidth > 0 && mVideoHeight > 0){
                vlcOut.setWindowSize(mVideoWidth,mVideoHeight);
            }
            vlcOut.addCallback(callback);
            vlcOut.setVideoSurface(this.getHolder().getSurface(), this.getHolder());
            vlcOut.attachViews(onNewVideoLayoutListener);
            DisplayMetrics dm = getResources().getDisplayMetrics();
            Uri uri = Uri.parse(this.src);
            Media m = new Media(libvlc, uri);
            mMediaPlayer.setMedia(m);
            mMediaPlayer.setScale(0);
            if(autoplay){
                mMediaPlayer.play();
            }
        } catch (Exception e) {
            //Toast.makeText(getContext(), "Error creating player!", Toast.LENGTH_LONG).show();
        }
    }

    private static int getDeblocking(int deblocking) {
        int ret = deblocking;
        if (deblocking < 0) {
            /**
             * Set some reasonable sDeblocking defaults:
             *
             * Skip all (4) for armv6 and MIPS by default
             * Skip non-ref (1) for all armv7 more than 1.2 Ghz and more than 2 cores
             * Skip non-key (3) for all devices that don't meet anything above
             */
            VLCUtil.MachineSpecs m = VLCUtil.getMachineSpecs();
            if (m == null)
                return ret;
            if ((m.hasArmV6 && !(m.hasArmV7)) || m.hasMips)
                ret = 4;
            else if (m.frequency >= 1200 && m.processors > 2)
                ret = 1;
            else if (m.bogoMIPS >= 1200 && m.processors > 2) {
                ret = 1;
            } else
                ret = 3;
        } else if (deblocking > 4) { // sanity check
            ret = 3;
        }
        return ret;
    }

    /*************
     * Events
     *************/

    private View.OnLayoutChangeListener onLayoutChangeListener = new View.OnLayoutChangeListener(){

        @Override
        public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
            mVideoWidth = view.getWidth(); // 获取宽度
            mVideoHeight = view.getHeight(); // 获取高度
            IVLCVout vlcOut =  mMediaPlayer.getVLCVout();
            vlcOut.setWindowSize(mVideoWidth,mVideoHeight);
        }
    };

    /**
     * 播放过程中的时间事件监听
     */
    private MediaPlayer.EventListener mPlayerListener = new MediaPlayer.EventListener(){
        long currentTime = 0;
        long totalLength = 0;
        @Override
        public void onEvent(MediaPlayer.Event event) {
            switch (event.type) {
                case MediaPlayer.Event.EndReached:
                    eventEmitter.end();
                    break;
                case MediaPlayer.Event.Playing:
                    break;
                case MediaPlayer.Event.Paused:
                    break;
                case MediaPlayer.Event.Buffering:
                    System.out.println("--------Buffering---------->>>");
                    eventEmitter.buffering(true);
                    break;
                case MediaPlayer.Event.Stopped:
                    break;
                case MediaPlayer.Event.EncounteredError:
                    break;
                case MediaPlayer.Event.TimeChanged:
                    //event.
                    System.out.println("---------progressChanged------------>");
                    currentTime = mMediaPlayer.getTime();
                    totalLength = mMediaPlayer.getLength();
                    eventEmitter.progressChanged(currentTime, totalLength);
                    break;
                default:
                    break;
            }
        }
    };


    private IVLCVout.OnNewVideoLayoutListener onNewVideoLayoutListener = new IVLCVout.OnNewVideoLayoutListener(){
        @Override
        public void onNewVideoLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
            System.out.println("---------onNewVideoLayout------------>");
            if (width * height == 0)
                return;

            // store video size
            /*mVideoWidth = width;
            mVideoHeight = height;
            mVideoVisibleWidth  = visibleWidth;
            mVideoVisibleHeight = visibleHeight;
            mSarNum = sarNum;
            mSarDen = sarDen;*/
        }
    };


    IVLCVout.Callback callback = new IVLCVout.Callback() {
        @Override
        public void onSurfacesCreated(IVLCVout ivlcVout) {

        }

        @Override
        public void onSurfacesDestroyed(IVLCVout ivlcVout) {
        }

    };


    public void seekTo(long time) {
        mMediaPlayer.setTime(time);
        mMediaPlayer.isSeekable();
    }

    public void setSrc(String uri, String extension) {
        this.src = uri;
        createPlayer(true);
    }

    public void setRateModifier(float rateModifier) {
        mMediaPlayer.setRate(rateModifier);
    }


    public void setVolumeModifier(int volumeModifier) {
        mMediaPlayer.setVolume(volumeModifier);
    }

    public void setPausedModifier(boolean paused){
        if(paused){
            mMediaPlayer.pause();
        }else{
            mMediaPlayer.play();
        }
    }

    public void doResume(boolean autoplay){
        createPlayer(autoplay);
    }

    public void setRepeatModifier(boolean repeat){
    }

    private void changeSurfaceSize(boolean message) {

        if (mMediaPlayer != null) {
            final IVLCVout vlcVout = mMediaPlayer.getVLCVout();
            vlcVout.setWindowSize(screenWidth, screenHeight);
        }

        double displayWidth = screenWidth, displayHeight = screenHeight;

        if (screenWidth < screenHeight) {
            displayWidth = screenHeight;
            displayHeight = screenWidth;
        }

        // sanity check
        if (displayWidth * displayHeight <= 1 || mVideoWidth * mVideoHeight <= 1) {
            return;
        }

        // compute the aspect ratio
        double aspectRatio, visibleWidth;
        if (mSarDen == mSarNum) {
            /* No indication about the density, assuming 1:1 */
            visibleWidth = mVideoVisibleWidth;
            aspectRatio = (double) mVideoVisibleWidth / (double) mVideoVisibleHeight;
        } else {
            /* Use the specified aspect ratio */
            visibleWidth = mVideoVisibleWidth * (double) mSarNum / mSarDen;
            aspectRatio = visibleWidth / mVideoVisibleHeight;
        }

        // compute the display aspect ratio
        double displayAspectRatio = displayWidth / displayHeight;

        counter ++;

        switch (mCurrentSize) {
            case SURFACE_BEST_FIT:
                if(counter > 2)
                    Toast.makeText(getContext(), "Best Fit", Toast.LENGTH_SHORT).show();
                if (displayAspectRatio < aspectRatio)
                    displayHeight = displayWidth / aspectRatio;
                else
                    displayWidth = displayHeight * aspectRatio;
                break;
            case SURFACE_FIT_HORIZONTAL:
                Toast.makeText(getContext(), "Fit Horizontal", Toast.LENGTH_SHORT).show();
                displayHeight = displayWidth / aspectRatio;
                break;
            case SURFACE_FIT_VERTICAL:
                Toast.makeText(getContext(), "Fit Horizontal", Toast.LENGTH_SHORT).show();
                displayWidth = displayHeight * aspectRatio;
                break;
            case SURFACE_FILL:
                Toast.makeText(getContext(), "Fill", Toast.LENGTH_SHORT).show();
                break;
            case SURFACE_16_9:
                Toast.makeText(getContext(), "16:9", Toast.LENGTH_SHORT).show();
                aspectRatio = 16.0 / 9.0;
                if (displayAspectRatio < aspectRatio)
                    displayHeight = displayWidth / aspectRatio;
                else
                    displayWidth = displayHeight * aspectRatio;
                break;
            case SURFACE_4_3:
                Toast.makeText(getContext(), "4:3", Toast.LENGTH_SHORT).show();
                aspectRatio = 4.0 / 3.0;
                if (displayAspectRatio < aspectRatio)
                    displayHeight = displayWidth / aspectRatio;
                else
                    displayWidth = displayHeight * aspectRatio;
                break;
            case SURFACE_ORIGINAL:
                Toast.makeText(getContext(), "Original", Toast.LENGTH_SHORT).show();
                displayHeight = mVideoVisibleHeight;
                displayWidth = visibleWidth;
                break;
        }

        // set display size
        int finalWidth = (int) Math.ceil(displayWidth * mVideoWidth / mVideoVisibleWidth);
        int finalHeight = (int) Math.ceil(displayHeight * mVideoHeight / mVideoVisibleHeight);

        SurfaceHolder holder = this.getHolder();
        holder.setFixedSize(finalWidth, finalHeight);

        ViewGroup.LayoutParams lp = this.getLayoutParams();
        lp.width = finalWidth;
        lp.height = finalHeight;
        this.setLayoutParams(lp);
        this.invalidate();
    }
}
