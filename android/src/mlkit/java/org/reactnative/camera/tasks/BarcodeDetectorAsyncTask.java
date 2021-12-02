package org.reactnative.camera.tasks;

//import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.common.InputImage;

import org.reactnative.barcodedetector.BarcodeFormatUtils;
import org.reactnative.barcodedetector.RNBarcodeDetector;
import org.reactnative.camera.utils.ImageDimensions;

import android.graphics.Point;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

import java.util.List;

public class BarcodeDetectorAsyncTask extends android.os.AsyncTask<Void, Void, Void> {

  private byte[] mImageData;
  private int mWidth;
  private int mHeight;
  private int mRotation;
  private RNBarcodeDetector mBarcodeDetector;
  private BarcodeDetectorAsyncTaskDelegate mDelegate;
  private double mScaleX;
  private double mScaleY;
  private ImageDimensions mImageDimensions;
  private int mPaddingLeft;
  private int mPaddingTop;
  private String TAG = "RNCamera";

  public BarcodeDetectorAsyncTask(
      BarcodeDetectorAsyncTaskDelegate delegate,
      RNBarcodeDetector barcodeDetector,
      byte[] imageData,
      int width,
      int height,
      int rotation,
      float density,
      int facing,
      int viewWidth,
      int viewHeight,
      int viewPaddingLeft,
      int viewPaddingTop
  ) {
    mImageData = imageData;
    mWidth = width;
    mHeight = height;
    mRotation = rotation;
    mDelegate = delegate;
    mBarcodeDetector = barcodeDetector;
    mImageDimensions = new ImageDimensions(width, height, rotation, facing);
    mScaleX = (double) (viewWidth) / (mImageDimensions.getWidth() * density);
    mScaleY = 1 / density;
    mPaddingLeft = viewPaddingLeft;
    mPaddingTop = viewPaddingTop;
  }

  @Override
  protected Void doInBackground(Void... ignored) {
    if (isCancelled() || mDelegate == null || mBarcodeDetector == null) {
      return null;
    }

    InputImage image = InputImage.fromByteArray(mImageData, mWidth, mHeight, getFirebaseRotation(), InputImage.IMAGE_FORMAT_YV12);

    BarcodeScanner barcode = mBarcodeDetector.getDetector();
    barcode.process(image)
            .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
              @Override
              public void onSuccess(List<Barcode> barcodes) {
                WritableArray serializedBarcodes = convertBarcodes(barcodes);
                mDelegate.onBarcodesDetected(serializedBarcodes, mWidth, mHeight, mImageData);
                mDelegate.onBarcodeDetectingTaskCompleted();
              }
            })
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(Exception e) {
                Log.e(TAG, "Text recognition task failed" + e);
                mDelegate.onBarcodeDetectingTaskCompleted();
              }
            });
    return null;
  }

  private int getFirebaseRotation(){
    int result;
    switch (mRotation) {
      case 0:
        result = 0;
        break;
      case 90:
        result = 90;
        break;
      case 180:
        result = 180;
        break;
      case -90:
      case 270:
        result = 270;
        break;
      default:
        result = 0;
        Log.e(TAG, "Bad rotation value: " + mRotation);
    }
    return result;
  }


  public static WritableNativeArray convertToArray(@NonNull Point[] points) {
    WritableNativeArray array = new WritableNativeArray();

    for (Point point: points) {
      array.pushMap(convertToMap(point));
    }

    return array;
  }

  public static WritableNativeArray convertToArray(@NonNull String[] elements) {
    WritableNativeArray array = new WritableNativeArray();

    for (String elem: elements) {
      array.pushString(elem);
    }

    return array;
  }

  public static WritableNativeArray convertStringList(@NonNull List<String> elements) {
    WritableNativeArray array = new WritableNativeArray();

    for (String elem: elements) {
      array.pushString(elem);
    }

    return array;
  }

  public static WritableNativeArray convertAddressList(@NonNull List<Barcode.Address> addresses) {
    WritableNativeArray array = new WritableNativeArray();

    for (Barcode.Address address: addresses) {
      array.pushMap(convertToMap(address));
    }

    return array;
  }

  public static WritableNativeArray convertPhoneList(@NonNull List<Barcode.Phone> phones) {
    WritableNativeArray array = new WritableNativeArray();

    for (Barcode.Phone phone: phones) {
      array.pushMap(convertToMap(phone));
    }

    return array;
  }

  public static WritableNativeArray convertEmailList(@NonNull List<Barcode.Email> emails) {
    WritableNativeArray array = new WritableNativeArray();

    for (Barcode.Email email: emails) {
      array.pushMap(convertToMap(email));
    }

    return array;
  }

  public static WritableNativeMap convertToMap(@NonNull Point point) {
    WritableNativeMap map = new WritableNativeMap();

    map.putInt("x", point.x);
    map.putInt("y", point.y);

    return map;
  }

  public static WritableNativeMap convertToMap(@NonNull Barcode.Address address) {
    WritableNativeMap map = new WritableNativeMap();

    map.putArray("addressLines", convertToArray(address.getAddressLines()));
    map.putInt("type", address.getType());

    return map;
  }

  public static WritableNativeMap convertToMap(@NonNull Rect rect) {
    WritableNativeMap map = new WritableNativeMap();

    map.putInt("bottom", rect.bottom);
    map.putInt("left", rect.left);
    map.putInt("right", rect.right);
    map.putInt("top", rect.top);

    return map;
  }

  public static WritableNativeMap convertToMap(@NonNull Barcode.ContactInfo contactInfo) {
    WritableNativeMap map = new WritableNativeMap();

    map.putArray("addresses", convertAddressList(contactInfo.getAddresses()));
    map.putArray("emails", convertEmailList(contactInfo.getEmails()));
    map.putMap("name", convertToMap(contactInfo.getName()));
    map.putString("organization", contactInfo.getOrganization());
    map.putArray("phones", convertPhoneList(contactInfo.getPhones()));
    map.putString("title", contactInfo.getTitle());
    map.putArray("urls", convertStringList(contactInfo.getUrls()));

    return map;
  }

  public static WritableNativeMap convertToMap(@NonNull Barcode.PersonName name) {
    WritableNativeMap map = new WritableNativeMap();

    map.putString("first", name.getFirst());
    map.putString("formattedName", name.getFormattedName());
    map.putString("last", name.getLast());
    map.putString("middle", name.getMiddle());
    map.putString("prefix", name.getPrefix());
    map.putString("pronunciation", name.getPronunciation());
    map.putString("suffix", name.getSuffix());

    return map;
  }

  public static WritableNativeMap convertToMap(@NonNull Barcode.UrlBookmark url) {
    WritableNativeMap map = new WritableNativeMap();

    map.putString("title", url.getTitle());
    map.putString("url", url.getUrl());

    return map;
  }

  public static WritableNativeMap convertToMap(@NonNull Barcode.Email email) {
    WritableNativeMap map = new WritableNativeMap();

    map.putString("address", email.getAddress());
    map.putString("body", email.getBody());
    map.putString("subject", email.getSubject());
    map.putInt("type", email.getType());

    return map;
  }

  public static WritableNativeMap convertToMap(@NonNull Barcode.Phone phone) {
    WritableNativeMap map = new WritableNativeMap();

    map.putString("number", phone.getNumber());
    map.putInt("type", phone.getType());

    return map;
  }

  public static WritableNativeMap convertToMap(@NonNull Barcode.Sms sms) {
    WritableNativeMap map = new WritableNativeMap();

    map.putString("message", sms.getMessage());
    map.putString("phoneNumber", sms.getPhoneNumber());

    return map;
  }

  public static WritableNativeMap convertToMap(@NonNull Barcode.WiFi wifi) {
    WritableNativeMap map = new WritableNativeMap();

    map.putInt("encryptionType", wifi.getEncryptionType());
    map.putString("password", wifi.getPassword());
    map.putString("ssid", wifi.getSsid());

    return map;
  }

  public static WritableNativeMap convertToMap(@NonNull Barcode.GeoPoint geoPoint) {
    WritableNativeMap map = new WritableNativeMap();

    map.putDouble("lat", geoPoint.getLat());
    map.putDouble("lng", geoPoint.getLng());

    return map;
  }

  public static WritableNativeMap convertToMap(@NonNull Barcode.CalendarDateTime calendarDateTime) {
    WritableNativeMap map = new WritableNativeMap();

    map.putInt("day", calendarDateTime.getDay());
    map.putInt("hours", calendarDateTime.getHours());
    map.putInt("minutes", calendarDateTime.getMinutes());
    map.putInt("month", calendarDateTime.getMonth());
    map.putString("rawValue", calendarDateTime.getRawValue());
    map.putInt("year", calendarDateTime.getYear());
    map.putInt("seconds", calendarDateTime.getSeconds());
    map.putBoolean("isUtc", calendarDateTime.isUtc());

    return map;
  }

  public static WritableNativeMap convertToMap(@NonNull Barcode.CalendarEvent calendarEvent) {
    WritableNativeMap map = new WritableNativeMap();

    map.putString("description", calendarEvent.getDescription());
    map.putMap("end", convertToMap(calendarEvent.getEnd()));
    map.putString("location", calendarEvent.getLocation());
    map.putString("organizer", calendarEvent.getOrganizer());
    map.putMap("start", convertToMap(calendarEvent.getStart()));
    map.putString("status", calendarEvent.getStatus());
    map.putString("summary", calendarEvent.getSummary());

    return map;
  }

  public static WritableNativeMap convertToMap(@NonNull Barcode.DriverLicense driverLicense) {
    WritableNativeMap map = new WritableNativeMap();

    map.putString("addressCity", driverLicense.getAddressCity());
    map.putString("addressState", driverLicense.getAddressState());
    map.putString("addressStreet", driverLicense.getAddressStreet());
    map.putString("addressZip", driverLicense.getAddressZip());
    map.putString("birthDate", driverLicense.getBirthDate());
    map.putString("documentType", driverLicense.getDocumentType());
    map.putString("expiryDate", driverLicense.getExpiryDate());
    map.putString("firstName", driverLicense.getFirstName());
    map.putString("gender", driverLicense.getGender());
    map.putString("issueDate", driverLicense.getIssueDate());
    map.putString("issuingCountry", driverLicense.getIssuingCountry());
    map.putString("lastName", driverLicense.getLastName());
    map.putString("licenseNumber", driverLicense.getLicenseNumber());
    map.putString("middleName", driverLicense.getMiddleName());

    return map;
  }

  public static WritableNativeMap convertContent(@NonNull Barcode barcode) {
    WritableNativeMap map = new WritableNativeMap();

    int type = barcode.getValueType();

    map.putInt("type", type);

    switch (type) {
      case Barcode.TYPE_UNKNOWN:
      case Barcode.TYPE_ISBN:
      case Barcode.TYPE_TEXT:
      case Barcode.TYPE_PRODUCT:
        map.putString("content", barcode.getRawValue());
        break;
      case Barcode.TYPE_CONTACT_INFO:
        map.putMap("content", convertToMap(barcode.getContactInfo()));
        break;
      case Barcode.TYPE_EMAIL:
        map.putMap("content", convertToMap(barcode.getEmail()));
        break;
      case Barcode.TYPE_PHONE:
        map.putMap("content", convertToMap(barcode.getPhone()));
        break;
      case Barcode.TYPE_SMS:
        map.putMap("content", convertToMap(barcode.getSms()));
        break;
      case Barcode.TYPE_URL:
        map.putMap("content", convertToMap(barcode.getUrl()));
        break;
      case Barcode.TYPE_WIFI:
        map.putMap("content", convertToMap(barcode.getWifi()));
        break;
      case Barcode.TYPE_GEO:
        map.putMap("content", convertToMap(barcode.getGeoPoint()));
        break;
      case Barcode.TYPE_CALENDAR_EVENT:
        map.putMap("content", convertToMap(barcode.getCalendarEvent()));
        break;
      case Barcode.TYPE_DRIVER_LICENSE:
        map.putMap("content", convertToMap(barcode.getDriverLicense()));
        break;
    }

    return map;
  }

  public WritableMap processBounds(Rect frame) {
    WritableMap origin = Arguments.createMap();
    int x = frame.left;
    int y = frame.top;

    origin.putDouble("x", x);
    origin.putDouble("y", y);

    WritableMap frameSize = Arguments.createMap();
    frameSize.putDouble("width", mWidth);
    frameSize.putDouble("height", mHeight);

    WritableMap bounds = Arguments.createMap();
    bounds.putMap("origin", origin);
    bounds.putMap("frameSize", frameSize);

    return bounds;
  }

  public WritableNativeMap convertBarcode(@NonNull Barcode barcode) {
    WritableNativeMap map = new WritableNativeMap();

    Rect boundingBox = barcode.getBoundingBox();
    if (boundingBox != null) {
      map.putMap("boundingBox", convertToMap(boundingBox));
    }

    Point[] cornerPoints = barcode.getCornerPoints();
    if (cornerPoints != null) {
      map.putArray("cornerPoints", convertToArray(cornerPoints));
    }

    String displayValue = barcode.getDisplayValue();
    if (displayValue != null) {
      map.putString("displayValue", displayValue);
    }

    String rawValue = barcode.getRawValue();
    if (rawValue != null) {
      map.putString("rawValue", rawValue);
    }

    map.putMap("content", convertContent(barcode));
    map.putMap("bounds", processBounds(boundingBox));

    return map;
  }

  public WritableArray convertBarcodes(List<Barcode> barcodes) {
      try {
        WritableArray array = new WritableNativeArray();

        for (Barcode barcode: barcodes) {
          array.pushMap(convertBarcode(barcode));
        }

        return array;
      } catch (Exception e) {
        e.printStackTrace();
      }

      return null;
  }
}
