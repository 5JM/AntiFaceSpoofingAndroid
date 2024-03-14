package aero.cubox.communication;

import android.graphics.Bitmap;
import android.graphics.Rect;

public class Detector {
    private long selfAddr;

    public Detector(String model_path, String model_weight_path, int inWidth, int inHeight, float scoreThresh, float iouThresh, boolean useTracker) {
        selfAddr = newSelf(model_path, model_weight_path, inWidth, inHeight, scoreThresh, iouThresh, useTracker);
    }

    public void delete() {
        deleteSelf(selfAddr);
        selfAddr = 0;
    }

    @Override
    protected void finalize() throws Throwable {
        delete();
    }

    public long getSelfAddr() {
        return selfAddr;
    }

    public Rect run(Bitmap input) { return run(selfAddr, input);}

    private static native long newSelf(String model_path, String model_weight_path, int inWidth, int inHeight, float scoreThresh, float iouThresh, boolean useTracker);
    private static native void deleteSelf(long selfAddr);
    private static native Rect run(long selfAddr, Bitmap input);
}
