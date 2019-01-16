package ke.co.techmata.mytktadmin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.*;
import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import ke.co.techmata.mytktadmin.utils.MyApplication;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {


    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1213;
    private static final String URL = "http://my-tkt.com/checkin.php";
    private SurfaceView cameraView;
    private TextView tvCode,tvInfo;
    private Button btnCheckIn, btnCancel;
    private LinearLayout lytTicketInfo;
    private ProgressBar pbTicketInfo;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private String ticketNo;
    private Boolean reading = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }


        lytTicketInfo = (LinearLayout) findViewById(R.id.lytTicketInfo);
        tvCode = (TextView)findViewById(R.id.code_info);
        pbTicketInfo = (ProgressBar) findViewById(R.id.pbTicketInfo);
        tvInfo = (TextView) findViewById(R.id.tvTicketInfo);
        tvInfo.setMovementMethod(new ScrollingMovementMethod());
        ((NestedScrollView) findViewById(R.id.nsvTicket)).setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                tvInfo.getParent().requestDisallowInterceptTouchEvent(false);
                return false;
            }
        });

        tvInfo.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                tvInfo.getParent().requestDisallowInterceptTouchEvent(true);

                return false;
            }
        });

        btnCancel = (Button) findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reading = true;
                lytTicketInfo.setVisibility(View.GONE);
                tvCode.setText("");
            }
        });
        btnCheckIn = (Button) findViewById(R.id.btnCheckIn);
        btnCheckIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkIn(ticketNo);
            }
        });

        cameraView = (SurfaceView)findViewById(R.id.camera_view);


        barcodeDetector =
                new BarcodeDetector.Builder(this)
                        .setBarcodeFormats(Barcode.QR_CODE)
                        .build();

        cameraSource = new CameraSource
                .Builder(this, barcodeDetector)
                .setRequestedPreviewSize(640, 480)
                .build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    cameraSource.start(cameraView.getHolder());
                } catch (IOException ie) {
                    Log.e("CAMERA SOURCE", ie.getMessage());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();

                if (barcodes.size() != 0) {
                    tvCode.post(new Runnable() {    // Use the post method of the TextView
                        public void run() {
                            if(reading){
                                reading = false;
                                ticketNo = barcodes.valueAt(0).displayValue;
                                tvCode.setText(ticketNo);
                                checkUp(ticketNo);
                            }
                        }
                    });
                }
            }
        });

    }

    private void checkUp(String ticket_no){
        pbTicketInfo.setVisibility(View.VISIBLE);
        lytTicketInfo.setVisibility(View.GONE);
        btnCheckIn.setVisibility(View.GONE);
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST,
                MainActivity.URL+"?tkt_no="+ticket_no,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(response != null){

                            try {
                                Log.i("myResp",response.toString(2));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                if(response.has("checkin")){
                                    int checkin = response.getInt("checkin");
                                    String stTV = response.toString(1);
                                    if(checkin == 0){
                                        btnCheckIn.setVisibility(View.VISIBLE);
                                        //valid ticket
                                        stTV =  "VALID TICKET\n";
                                        stTV += response.getString("ticket_number")+"\n";
                                        stTV += response.getString("subscriber_names")+"\n";

                                    }else if(checkin == 1){
                                        // ticket has been used
                                        stTV =  "TICKET HAS BEEN USED\n";
                                        stTV += response.getString("ticket_number")+"\n";
                                        stTV += response.getString("subscriber_names")+"\n";

                                    }else {
                                        //invalid ticket
                                        stTV =  "INVALID TICKET "+response.getString("checkin")+"\n";
                                        stTV += response.getString("ticket_number")+"\n";
                                        stTV += response.getString("subscriber_names")+"\n";
                                    }
                                    Log.d("myCheckin", stTV);
                                    tvInfo.setText(stTV);
                                    //tvInfo.setText(response.toString(1));
                                }else{
                                    tvInfo.setText("Invalid Ticket");
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }else{
                            Toast.makeText(MainActivity.this, "Error, null response", Toast.LENGTH_SHORT).show();
                        }
                        pbTicketInfo.setVisibility(View.GONE);
                        lytTicketInfo.setVisibility(View.VISIBLE);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("myerr",error.toString());
                error.printStackTrace();
                pbTicketInfo.setVisibility(View.GONE);
                lytTicketInfo.setVisibility(View.VISIBLE);

            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String,String>();
                params.put("Content-Type", "application/json");
                params.put("Accept", "application/json");
                return params;
            }
        };
        MyApplication myApp = new MyApplication(MainActivity.this);
        jsonRequest.setRetryPolicy(new DefaultRetryPolicy());
        MyApplication.getInstance().addToRequestQueue(jsonRequest);
    }

    private void checkIn(String ticket_no){
        pbTicketInfo.setVisibility(View.VISIBLE);
        btnCheckIn.setVisibility(View.GONE);
        tvInfo.setVisibility(View.GONE);
        //lytTicketInfo.setVisibility(View.GONE);
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST,
                MainActivity.URL+"?tkt_no="+ticket_no+"&checkin=1",
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(response != null){
                            try {
                                Log.i("myResp",response.toString(2));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                tvInfo.setText(response.toString(1));
                                if(!response.has("error")){
                                    Toast.makeText(MainActivity.this, "Checked In", Toast.LENGTH_SHORT).show();

                                }else {
                                    Toast.makeText(MainActivity.this, response.getString("error"), Toast.LENGTH_SHORT).show();
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }else{
                            Toast.makeText(MainActivity.this, "Error, null response", Toast.LENGTH_SHORT).show();
                        }
                        pbTicketInfo.setVisibility(View.GONE);
                        btnCheckIn.setVisibility(View.VISIBLE);
                        tvInfo.setVisibility(View.VISIBLE);
                        lytTicketInfo.setVisibility(View.GONE);
                        tvCode.setText("");
                        reading = true;
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("myerr",error.toString());
                error.printStackTrace();
                pbTicketInfo.setVisibility(View.GONE);
                tvInfo.setVisibility(View.VISIBLE);
                btnCheckIn.setVisibility(View.VISIBLE);
                lytTicketInfo.setVisibility(View.GONE);
                tvCode.setText("");
                reading = true;

            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String,String>();
                params.put("Content-Type", "application/json");
                params.put("Accept", "application/json");
                return params;
            }
        };
        MyApplication myApp = new MyApplication(MainActivity.this);
        jsonRequest.setRetryPolicy(new DefaultRetryPolicy());
        MyApplication.getInstance().addToRequestQueue(jsonRequest);
    }

}

