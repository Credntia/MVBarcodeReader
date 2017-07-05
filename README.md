# MVBarcodeReader
[ ![Download](https://api.bintray.com/packages/iammehedi/MVBarcodeReader/online.devliving%3Amvbarcodereader/images/download.svg) ](https://bintray.com/iammehedi/MVBarcodeReader/online.devliving%3Amvbarcodereader/_latestVersion)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-MVBarcodeReader-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/4457)

A Barcode scanning library for Android. Uses the Google Play Services' mobile vision api for barcode detection.

## Setup
### Maven
```xml
<dependency>
  <groupId>online.devliving</groupId>
  <artifactId>mvbarcodereader</artifactId>
  <version>LATEST_VERSION</version>
  <type>pom</type>
</dependency>
```

### Gradle
```groovy
compile 'online.devliving:mvbarcodereader:LATEST_VERSION'
```
Add following dependencies to your app's gradle file
```groovy
compile 'com.android.support:appcompat-v7:25.3.1'
compile 'com.google.android.gms:play-services-basement:11.0.1'
compile 'com.google.android.gms:play-services-vision:11.0.1'
compile 'com.android.support:design:25.3.1'
compile 'com.android.support:support-annotations:25.3.1'
```

## Usage
### Scanning Modes
- `SINGLE_AUTO`: The fastest mode. Returns the first barcode it can detect as soon as possible.
- `SINGLE_MANUAL`: Detects and highlights all the barcode it can find but returns only the one that user chooses by tapping.
- `MULTIPLE`: Detects and highlights all the barcode it can find. Returns all the barcodes on tap.

### Barcode Types
You can view [this link](https://developers.google.com/vision/barcodes-overview) for a list of supported barcode formats.

### Use the standalone scanner
launch the scanner from your `Activity` like this:
```java
new MVBarcodeScanner.Builder()
                    .setScanningMode(mMode)
                    .setFormats(mFormats)
                    .build()
                    .launchScanner(this, REQ_CODE);
```
You'll receive the scanned barcode/barcodes in your Activity's `onActivityResult`
```java
if (requestCode == REQ_CODE) {
            if (resultCode == RESULT_OK && data != null
                    && data.getExtras() != null) {
              
                if (data.getExtras().containsKey(MVBarcodeScanner.BarcodeObject)) {
                    Barcode mBarcode = data.getParcelableExtra(MVBarcodeScanner.BarcodeObject);
                } else if (data.getExtras().containsKey(MVBarcodeScanner.BarcodeObjects)) {
                    List<Barcode> mBarcodes = data.getParcelableArrayListExtra(MVBarcodeScanner.BarcodeObjects);
                }
            }
        }
```

### Use the scanner fragment
You can use the `BarcodeCaptureFragment` to scan barcodes. Just add the fragment to your `Activity`
```java
MVBarcodeScanner.ScanningMode mode = null;
@MVBarcodeScanner.BarCodeFormat int[] formats = null;

BarcodeCaptureFragment fragment = BarcodeCaptureFragment.instantiate(mode, formats);
getSupportFragmentManager().beginTransaction()
                .add(R.id.container, fragment)
                .commit();
```
Then make the the `Activity` implement the `BarcodeCaptureFragment.BarcodeScanningListener` so that you can receive results from the fragment or you can set the listener directly to the fragment
```java
        fragment.setListener(new BarcodeCaptureFragment.BarcodeScanningListener() {
            @Override
            public void onBarcodeScanned(Barcode barcode) {
                
            }

            @Override
            public void onBarcodesScanned(List<Barcode> barcodes) {

            }

            @Override
            public void onBarcodeScanningFailed(String reason) {

            }
        });
```
