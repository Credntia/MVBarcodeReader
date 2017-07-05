package devliving.online.mvbarcodereadersample;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.vision.barcode.Barcode;

import java.util.List;

import devliving.online.mvbarcodereader.BarcodeCaptureFragment;
import devliving.online.mvbarcodereader.MVBarcodeScanner;

/**
 * Created by Mehedi Hasan Khan <mehedi.mailing@gmail.com> on 6/8/17.
 */

public class ScannerDialog extends DialogFragment implements BarcodeCaptureFragment.BarcodeScanningListener {

    public interface DialogResultListener{
        void onScanned(Barcode... barcode);
        void onFailed(String reason);
    }

    public static ScannerDialog instantiate(MVBarcodeScanner.ScanningMode mode, DialogResultListener listener,
                                                     @MVBarcodeScanner.BarCodeFormat int... formats) {
        ScannerDialog fragment = new ScannerDialog();
        fragment.mListener = listener;
        Bundle args = new Bundle();
        if (mode != null) args.putSerializable(MVBarcodeScanner.SCANNING_MODE, mode);

        if (formats != null && formats.length > 0) {
            int barcodeFormats = formats[0];

            if (formats.length > 1) {
                for (int i = 1; i < formats.length; i++) {
                    barcodeFormats = barcodeFormats | formats[i];
                }
            }

            args.putInt(MVBarcodeScanner.BARCODE_FORMATS, barcodeFormats);
        }
        fragment.setArguments(args);
        return fragment;
    }

    private DialogResultListener mListener;
    MVBarcodeScanner.ScanningMode mMode = MVBarcodeScanner.ScanningMode.SINGLE_AUTO;
    int mFormats = Barcode.ALL_FORMATS;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if(context instanceof DialogResultListener){
            mListener = (DialogResultListener) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && !getArguments().isEmpty()) {
            if (getArguments().containsKey(MVBarcodeScanner.SCANNING_MODE))
                mMode = (MVBarcodeScanner.ScanningMode) getArguments().getSerializable(MVBarcodeScanner.SCANNING_MODE);

            if (getArguments().containsKey(MVBarcodeScanner.BARCODE_FORMATS))
                mFormats = getArguments().getInt(MVBarcodeScanner.BARCODE_FORMATS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.dialog_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BarcodeCaptureFragment fragment = BarcodeCaptureFragment.instantiate(mMode, mFormats);
        fragment.setListener(this);
        getChildFragmentManager().beginTransaction()
                .add(R.id.container, fragment)
                .commit();
    }

    @Override
    public void onBarcodeScanned(Barcode barcode) {
        if(mListener != null) mListener.onScanned(barcode);
        dismiss();
    }

    @Override
    public void onBarcodesScanned(List<Barcode> barcodes) {
        if(mListener != null){
            Barcode[] codes = new Barcode[barcodes.size()];
            barcodes.toArray(codes);
            mListener.onScanned(codes);
        }
        dismiss();
    }

    @Override
    public void onBarcodeScanningFailed(String reason) {
        if(mListener != null) mListener.onFailed(reason);
        dismiss();
    }
}
