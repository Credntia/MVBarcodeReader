package devliving.online.mvbarcodereadersample;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.vision.barcode.Barcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import devliving.online.mvbarcodereader.MVBarcodeScanner;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    final int REQ_CODE = 12;

    TextView result;
    Button scanButton, scanDialogButton;
    EditText barcodeTypes;
    Spinner modeSpinner;

    MVBarcodeScanner.ScanningMode mMode = null;
    @MVBarcodeScanner.BarCodeFormat
    int[] mFormats = null;

    Barcode mBarcode;
    List<Barcode> mBarcodes;

    final static HashMap<Integer, String> TYPE_MAP;
    final static String[] barcodeTypeItems;

    static {
        TYPE_MAP = new HashMap<>();

        TYPE_MAP.put(Barcode.ALL_FORMATS, "All Formats");
        TYPE_MAP.put(Barcode.AZTEC, "Aztec");
        TYPE_MAP.put(Barcode.CALENDAR_EVENT, "Calendar Event");
        TYPE_MAP.put(Barcode.CODABAR, "Codabar");
        TYPE_MAP.put(Barcode.CODE_39, "Code 39");
        TYPE_MAP.put(Barcode.CODE_93, "Code 93");
        TYPE_MAP.put(Barcode.CODE_128, "Code 128");
        TYPE_MAP.put(Barcode.CONTACT_INFO, "Contact Info");
        TYPE_MAP.put(Barcode.DATA_MATRIX, "Data Matrix");
        TYPE_MAP.put(Barcode.DRIVER_LICENSE, "Drivers License");
        TYPE_MAP.put(Barcode.EAN_8, "EAN 8");
        TYPE_MAP.put(Barcode.EAN_13, "EAN 13");
        TYPE_MAP.put(Barcode.EMAIL, "Email");
        TYPE_MAP.put(Barcode.GEO, "Geo");
        TYPE_MAP.put(Barcode.ISBN, "ISBN");
        TYPE_MAP.put(Barcode.ITF, "ITF");
        TYPE_MAP.put(Barcode.PDF417, "PDF 417");
        TYPE_MAP.put(Barcode.PHONE, "Phone");
        TYPE_MAP.put(Barcode.QR_CODE, "QR Code");
        TYPE_MAP.put(Barcode.PRODUCT, "Product");
        TYPE_MAP.put(Barcode.SMS, "SMS");
        TYPE_MAP.put(Barcode.UPC_A, "UPC A");
        TYPE_MAP.put(Barcode.UPC_E, "UPC E");
        TYPE_MAP.put(Barcode.TEXT, "Text");
        TYPE_MAP.put(Barcode.URL, "URL");

        List<String> items = new ArrayList<>(TYPE_MAP.values());
        Collections.sort(items);
        String[] tempArray = new String[items.size()];
        tempArray = items.toArray(tempArray);
        barcodeTypeItems = tempArray;
    }

    boolean[] checkedStates = new boolean[TYPE_MAP.size()];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = (TextView) findViewById(R.id.result);
        scanButton = (Button) findViewById(R.id.scan);
        scanDialogButton = (Button) findViewById(R.id.scan_dialog);
        barcodeTypes = (EditText) findViewById(R.id.barcode_types);
        modeSpinner = (Spinner) findViewById(R.id.scanner_mode);

        ArrayAdapter adapter = new SpinnerAdapter(this, R.layout.simple_spinner_item);
        modeSpinner.setAdapter(adapter);
        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        mMode = MVBarcodeScanner.ScanningMode.SINGLE_AUTO;
                        break;

                    case 1:
                        mMode = MVBarcodeScanner.ScanningMode.SINGLE_MANUAL;
                        break;

                    case 2:
                        mMode = MVBarcodeScanner.ScanningMode.MULTIPLE;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        scanButton.setOnClickListener(this);
        scanDialogButton.setOnClickListener(this);
        barcodeTypes.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == scanButton.getId()) {
            new MVBarcodeScanner.Builder()
                    .setScanningMode(mMode)
                    .setFormats(mFormats)
                    .build()
                    .launchScanner(this, REQ_CODE);
        } else if (view.getId() == barcodeTypes.getId()) {
            showBarcodeTypesPicker();
        }
        else if(view.getId() == scanDialogButton.getId()) {
            ScannerDialog dialog = ScannerDialog.instantiate(mMode, new ScannerDialog.DialogResultListener() {
                @Override
                public void onScanned(Barcode... barcode) {
                    if(barcode.length > 1) {
                        mBarcode = null;
                        mBarcodes = new ArrayList<Barcode>();

                        for(Barcode b:barcode){
                            mBarcodes.add(b);
                        }
                    }
                    else {
                        mBarcodes = null;
                        mBarcode = barcode[0];
                    }

                    updateBarcodeInfo();
                }

                @Override
                public void onFailed(String reason) {

                }
            }, mFormats);

            Log.d("MAIN", "showing scanner dialog");
            dialog.show(getSupportFragmentManager(), "SCANNER");
        }
    }

    void showBarcodeTypesPicker() {
        final boolean[] checkedItems = Arrays.copyOf(checkedStates, checkedStates.length);

        new AlertDialog.Builder(this)
                .setTitle("Select Types")
                .setMultiChoiceItems(barcodeTypeItems, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checkedItems[which] = isChecked;
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checkedStates = checkedItems;
                        mFormats = getUpdatedFormats();
                    }
                })
                .show();
    }

    @MVBarcodeScanner.BarCodeFormat int[] getUpdatedFormats() {
        List<Integer> formats = new ArrayList<>();
        String barcodes = "";
        int count = 0;
        for (int i = 0; i < checkedStates.length; i++) {
            if (checkedStates[i]) {
                int format = getFormatForValue(barcodeTypeItems[i]);
                if (format != -1) {
                    formats.add(format);
                    barcodes = barcodes + (count > 0 ? ", " : "") + barcodeTypeItems[i];
                    count++;
                }
            }
        }

        if (count > 0) {
            @MVBarcodeScanner.BarCodeFormat int[] iFormats = new int[formats.size()];
            int i = 0;
            for (Integer f : formats) {
                iFormats[i] = f;
                i++;
            }

            barcodeTypes.setText(barcodes);
            return iFormats;
        } else {
            barcodeTypes.setText(null);
            return null;
        }
    }

    @MVBarcodeScanner.BarCodeFormat
    int getFormatForValue(String value) {
        for (Map.Entry<Integer, String> entry : TYPE_MAP.entrySet()) {
            if (entry.getValue().equals(value)) return entry.getKey();
        }

        return -1;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE) {
            if (resultCode == RESULT_OK && data != null
                    && data.getExtras() != null) {
                Log.d("BARCODE-SCANNER", "onActivityResult inside block called");
                if (data.getExtras().containsKey(MVBarcodeScanner.BarcodeObject)) {
                    mBarcode = data.getParcelableExtra(MVBarcodeScanner.BarcodeObject);
                    mBarcodes = null;
                } else if (data.getExtras().containsKey(MVBarcodeScanner.BarcodeObjects)) {
                    mBarcodes = data.getParcelableArrayListExtra(MVBarcodeScanner.BarcodeObjects);
                    mBarcode = null;
                }
                updateBarcodeInfo();
            } else {
                mBarcode = null;
                mBarcodes = null;
                updateBarcodeInfo();
            }
        }
    }

    void updateBarcodeInfo() {
        StringBuilder builder = new StringBuilder();

        if (mBarcode != null) {
            Log.d("BARCODE-SCANNER", "got barcode");
            builder.append("Type: " + getBarcodeFormatName(mBarcode.format) +
                    "\nData: " + mBarcode.rawValue + "\n\n");
        }

        if (mBarcodes != null) {
            Log.d("BARCODE-SCANNER", "got barcodes");
            for (Barcode barcode : mBarcodes) {
                builder.append("Type: " + getBarcodeFormatName(barcode.format) +
                        "\nData: " + barcode.rawValue + "\n\n");
            }
        }

        if (builder.length() > 0)
            result.setText(builder.toString());
        else result.setText("");
    }

    String getBarcodeFormatName(int format) {
        return TYPE_MAP.get(format);
    }

    class ModeData {
        String name;
        String description;

        ModeData(String title, String message) {
            name = title;
            description = message;
        }
    }

    class SpinnerAdapter extends ArrayAdapter<ModeData> {
        final String[] Modes = {"Single - Auto", "Single - Manual", "Multiple"};
        final String[] Descriptions = {"Returns the first barcode it detects", "Returns the single barcode user tapped on",
                "Returns all the detected barcodes after tap"};

        /**
         * Constructor
         *
         * @param context  The current context.
         * @param resource The resource ID for a layout file containing a TextView to use when
         */
        public SpinnerAdapter(Context context, int resource) {
            super(context, resource);
        }

        @Override
        public int getCount() {
            return Modes.length;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null)
                view = LayoutInflater.from(getContext()).inflate(R.layout.simple_spinner_item, parent, false);

            TextView title = (TextView) view.findViewById(R.id.title);
            title.setText(Modes[position]);
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null)
                view = LayoutInflater.from(getContext()).inflate(R.layout.simple_spinner_dropdown_item, parent, false);

            TextView title = (TextView) view.findViewById(R.id.title);
            TextView message = (TextView) view.findViewById(R.id.message);

            title.setText(Modes[position]);
            message.setText(Descriptions[position]);
            return view;
        }
    }
}
