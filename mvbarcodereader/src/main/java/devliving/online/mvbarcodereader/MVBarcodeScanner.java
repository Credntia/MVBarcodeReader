package devliving.online.mvbarcodereader;

import android.app.Activity;
import android.content.Intent;

/**
 * Created by user on 10/2/16.
 */
public class MVBarcodeScanner {
    public static final String SCANNING_MODE = "scanning_mode";
    public static final String BARCODE_FORMATS = "barcode_formats";

    BarcodeCaptureFragment.ScanningMode mMode = null;
    @BarcodeCaptureFragment.BarCodeFormat
    int[] mFormats = null;

    private MVBarcodeScanner(BarcodeCaptureFragment.ScanningMode mode, @BarcodeCaptureFragment.BarCodeFormat int[] formats) {
        mMode = mode;
        mFormats = formats;
    }

    public void launchScanner(Activity activity, int requestCode) {
        Intent i = new Intent(activity, BarcodeCaptureActivity.class);
        if (mMode != null) i.putExtra(SCANNING_MODE, mMode);
        if (mFormats != null) i.putExtra(BARCODE_FORMATS, mFormats);

        activity.startActivityForResult(i, requestCode);
    }

    public static class Builder {
        BarcodeCaptureFragment.ScanningMode mMode = null;
        @BarcodeCaptureFragment.BarCodeFormat
        int[] mFormats = null;

        public Builder setScanningMode(BarcodeCaptureFragment.ScanningMode mode) {
            mMode = mode;
            return this;
        }

        public Builder setFormats(@BarcodeCaptureFragment.BarCodeFormat int... formats) {
            mFormats = formats;
            return this;
        }

        public MVBarcodeScanner build() {
            return new MVBarcodeScanner(mMode, mFormats);
        }
    }
}
