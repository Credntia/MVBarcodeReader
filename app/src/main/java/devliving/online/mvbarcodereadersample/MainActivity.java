package devliving.online.mvbarcodereadersample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import devliving.online.mvbarcodereader.BarcodeCaptureActivity;
import devliving.online.mvbarcodereader.MVBarcodeScanner;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    final int REQ_CODE = 12;

    TextView result;
    Button scanButton;

    Barcode mBarcode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = (TextView) findViewById(R.id.result);
        scanButton = (Button) findViewById(R.id.scan);

        scanButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        new MVBarcodeScanner.Builder().build()
                .launchScanner(this, REQ_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE) {
            if (resultCode == CommonStatusCodes.SUCCESS && data != null
                    && data.getExtras() != null && data.getExtras().containsKey(BarcodeCaptureActivity.BarcodeObject)) {
                mBarcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                updateBarcodeInfo();
            } else {
                mBarcode = null;
                updateBarcodeInfo();
            }
        }
    }

    void updateBarcodeInfo() {
        if (mBarcode != null) {
            result.setText("Type: " + getBarcodeFormatName(mBarcode.format) +
                    "\nData: " + mBarcode.rawValue);
        } else result.setText("");
    }

    String getBarcodeFormatName(int format) {
        switch (format) {
            case Barcode.AZTEC:
                return "Aztec";

            case Barcode.CALENDAR_EVENT:
                return "Calendar Event";

            case Barcode.CODABAR:
                return "Codabar";

            case Barcode.CODE_39:
                return "Code 39";

            case Barcode.CODE_93:
                return "Code 93";

            case Barcode.CODE_128:
                return "Code 128";

            case Barcode.DATA_MATRIX:
                return "Data Matrix";

            case Barcode.DRIVER_LICENSE:
                return "Driver License";

            case Barcode.EAN_8:
                return "EAN 8";

            case Barcode.EAN_13:
                return "EAN 13";

            case Barcode.GEO:
                return "GEO";

            case Barcode.ISBN:
                return "ISBN";

            case Barcode.ITF:
                return "ITF";

            case Barcode.PDF417:
                return "PDF417";

            case Barcode.QR_CODE:
                return "QR Code";

            case Barcode.SMS:
                return "SMS";

            case Barcode.TEXT:
                return "TEXT";

            case Barcode.UPC_A:
                return "UPC A";

            case Barcode.UPC_E:
                return "UPC E";

            case Barcode.WIFI:
                return "WIFI";
        }

        return "Unknown";
    }
}
