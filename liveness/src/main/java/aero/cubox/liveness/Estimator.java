package aero.cubox.liveness;

import android.graphics.Bitmap;

import java.util.Vector;

public class Estimator {

    public Estimator(float fps, float duration, int minBpm, int maxBpm, float bpmUpdatePeriod) {
        mNativeObj = nativeCreateObject(fps, duration, minBpm, maxBpm, bpmUpdatePeriod);
    }

    public void delete() {
        nativeDestroyObject(mNativeObj);
        mNativeObj = 0;
    }

    public void processFrame(Bitmap image, long time, boolean onStableMode) { nativeProcessFrame(mNativeObj, image, time, onStableMode); }

//    public void processSignal(){nativeProcessSignal(mNativeObj);}

    public Vector<Float> getSignal() { return nativeGetSignal(mNativeObj); }

    public Vector<Float> get_signal1(){ return native_get_signal1(mNativeObj); }
    public Vector<Float> get_signal2(){ return native_get_signal2(mNativeObj); }
    public Vector<Float> get_signal3(){ return native_get_signal3(mNativeObj); }
    public Vector<Float> get_signal4(){ return native_get_signal4(mNativeObj); }

    public int getBufferLength() { return nativeBufferLength(mNativeObj);}

    public int getAverageBpm() { return nativeGetHeartrate(mNativeObj); }

    public int getPpi() { return nativeGetPpi(mNativeObj); }

    public int getFps() { return nativeGetFps(mNativeObj); }

    public void reset() { nativeReset(mNativeObj); }

    public int predict(float t1, float t2, float t3, float t4, Vector<Float> thresholds) { return nativePredict(mNativeObj, t1, t2, t3, t4, thresholds); }

    private long mNativeObj = 0;

    private static native long nativeCreateObject(float fps, float duration, int minBpm, int maxBpm, float bpmUpdatePeriod);

    private static native void nativeProcessFrame(long mNativeObj, Bitmap image, long time, boolean onStableMode);

//    private static native void nativeProcessSignal(long mNativeObj);

    private static native void nativeDestroyObject(long mNativeObj);

    private static native Vector<Float> nativeGetSignal(long mNativeObj);

    private static native int nativeGetHeartrate(long mNativeObj);

    private static native int nativeGetPpi(long mNativeObj);

    private static native int nativeGetFps(long mNativeObj);

    public static native int nativePredict(long mNativeObj, float t1, float t2, float t3, float t4, Vector<Float> thresholds);

    public static native int nativeBufferLength(long mNativeObj);

    public static native void nativeReset(long mNativeObj);

    public static native Vector<Float> native_get_signal1(long mNativeObj);
    public static native Vector<Float> native_get_signal2(long mNativeObj);
    public static native Vector<Float> native_get_signal3(long mNativeObj);
    public static native Vector<Float> native_get_signal4(long mNativeObj);
}
