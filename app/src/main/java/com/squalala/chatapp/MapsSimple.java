package com.squalala.chatapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.squalala.chatapp.common.ChatConstant;
import com.squalala.chatapp.utils.ChatUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;


public class MapsSimple extends AppCompatActivity implements OnMapReadyCallback,com.android.volley.Response.Listener<String>, com.android.volley.Response.ErrorListener,LocationListener {
    private GoogleMap mMap;
    SupportMapFragment mapFragment;
    private WebSocketConnection mConnection = new WebSocketConnection();

    private static final String TAG = ChatActivity.class.getSimpleName();
    public LatLng camera_latlng;
    String check_request;
    ArrayList<String> locality_arr;
    LatLng servicProvide;
    Marker m2;
    private ProgressDialog mProgressDialog;
    Boolean isMarkerRotating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_simple);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        isMarkerRotating=false;
        GPSTracker gpsTracker = new GPSTracker(this);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        if (gpsTracker != null) {

            double userCurrent_Latitude = gpsTracker.getLatitude();
            double userCurrent_Longtude = gpsTracker.getLongitude();
      /*     if(userCurrent_Latitude!=0.0)
           {
               //gpsTracker._getLocation();
               Toast.makeText(getApplicationContext()," "+gpsTracker.getGPS(),Toast.LENGTH_LONG).show();
           }
           else
           {
               //._getLocation();
               Toast.makeText(getApplicationContext()," "+gpsTracker.getGPS(),Toast.LENGTH_LONG).show();
           }*/
            Log.d("lattlong", String.valueOf(userCurrent_Latitude));
            camera_latlng = new LatLng(userCurrent_Latitude, userCurrent_Longtude);
            //genderConfirmationAlert(camera_latlng);
            // genderAlert(camera_latlng);

        }

        startSocket();
    }


    public void startSocket()
    {
        try {

            mConnection.connect(ChatConstant.URL, new WebSocketHandler() {
                String[] separated;
                @Override
                public void onOpen() {
                    Log.d(TAG, "\n" + "Login successfully : " + ChatConstant.URL);
                }

                @Override
                public void onTextMessage(String payload) {
                    Log.d(TAG, " " + payload);

                   // messages.add(ChatUtils.jsonToMessage(payload));
                   // messagesAdapter.notifyDataSetChanged();

                    try {
                        JSONObject jsonObject = new JSONObject(payload);
                        String tempLatlng=jsonObject.getString(ChatConstant.TAG_MESSAGE);
                        Log.d("messagereceieved",tempLatlng);
                        if(tempLatlng.contains(","))
                        {
                             separated = tempLatlng.split(",");
                        }

                        Double lat=Double.parseDouble(separated[0]);
                        Double lng=Double.parseDouble(separated[1]);



                         LatLng oldMarkerLatlng=m2.getPosition();
                         Double oldLat=oldMarkerLatlng.latitude;
                         Double oldLng=oldMarkerLatlng.longitude;


                        Log.d("messagereceieved",lat.toString());
                        LatLng newLatlng=new LatLng(lat,lng);
                      //  float bearing = (float) bearingBetweenLocations(oldMarkerLatlng, newLatlng);
                        float bearing = (float) getBearing(oldMarkerLatlng, newLatlng);

                        Float distanceTwoLatLng=distance(oldLat,oldLng,lat,lng);
                        Log.d("messagereceieved",""+distanceTwoLatLng);
                      /*  if(distanceTwoLatLng<5)   //distance in meters
                        {
                            rotateMarker(m2, bearing);
                        }
                        else {
                            moveVechile(m2, newLatlng);
                        }*/

                      //moveVechile(m2,newLatlng);
                        animateMarkerNew(newLatlng,m2);

                     //   m2.setPosition(newLatlng);

                    }
                    catch (JSONException e)
                    {
                        e.printStackTrace();
                    }


                    //scrollToBottom();
                }

                @Override
                public void onClose(int code, String reason) {
                    Log.d(TAG, "\n" + "Lost connection");
                }
            });

        } catch (WebSocketException e) {
            e.printStackTrace();
        }

    }


    public float distance (Double lat_a, Double lng_a, Double lat_b, Double lng_b )
    {
        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(lat_b-lat_a);
        double lngDiff = Math.toRadians(lng_b-lng_a);
        double a = Math.sin(latDiff /2) * Math.sin(latDiff /2) +
                Math.cos(Math.toRadians(lat_a)) * Math.cos(Math.toRadians(lat_b)) *
                        Math.sin(lngDiff /2) * Math.sin(lngDiff /2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = earthRadius * c;

        int meterConversion = 1609;

        return new Float(distance * meterConversion).floatValue();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.car);
        m2=  mMap.addMarker(new MarkerOptions()
                               .icon(icon)
                               .position(camera_latlng)
                               .title("Sevice Seeker"));
       // Location bearing = prevLoc.bearingTo(newLoc);
         servicProvide=new LatLng(33.645576,73.040896);
        mMap.addMarker(new MarkerOptions().position(servicProvide).title("Sevice Provider"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(camera_latlng.latitude, camera_latlng.longitude), 19.0f));
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return true;
            }
        });

    }

    private void showProgressDialog() {
        mProgressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
        mProgressDialog.setMessage("Please Wait..");
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.d("errror", error.toString());
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();// or alert.dismiss() it
        }
        Toast.makeText(this, "Volley error", Toast.LENGTH_LONG).show();
        // myCoustomToast.mycoustomToast(this,""+getResources().getString(R.string.weakinternet));
    }


    @Override
    public void onResponse(String response) {
        Log.d("responce", response.toString());
        try {


            switch (check_request) {

                case "getAdressReq":
                   /* if(mProgressDialog != null && mProgressDialog.isShowing()){
                        mProgressDialog.dismiss();// or alert.dismiss() it
                    }*/
                    // mProgressDialog.dismiss();
                    Log.d("getAdressReq", response);
                    JSONObject jsonObject = new JSONObject(response);
                    String status = jsonObject.getString("status");
                    if (status.equals("ZERO_RESULTS")) {
                        //tv_address.setText("Location not found");
                        locality_arr = new ArrayList<>();
                        // serviceAvailability();

                    } else {

                        JSONArray jsonArray = jsonObject.getJSONArray("results");
                        JSONObject resultobj = jsonArray.getJSONObject(0);
                        JSONObject resultobj1 = jsonArray.getJSONObject(1);

                        String formate_address = resultobj.getString("formatted_address");
                        String formate_address1 = resultobj1.getString("formatted_address");
                        String address = "";

                        Log.d("formate_address", formate_address);
                        Log.d("formate_address1", formate_address1);

                      /*  if (formate_address.contains("Unnamed")){
                            tv_address.setText(formate_address1);
                        }else {
                            tv_address.setText(formate_address);
                        }*/

                        JSONArray address_components = resultobj.getJSONArray("address_components");
                        locality_arr = new ArrayList<>();
                        for (int i = 0; i < address_components.length(); i++) {
                            JSONObject address_componentsobj = address_components.getJSONObject(i);
                            JSONArray types = address_componentsobj.getJSONArray("types");
                            for (int k = 0; k < types.length(); k++) {
                                if (types.getString(k).equals("locality") || types.getString(k).equals("administrative_area_level_2")) {
                                    locality_arr.add(address_componentsobj.getString("long_name"));
                                }
                                if (types.getString(k).equals("street_number") || types.getString(k).equals("route") || types.getString(k).equals("sublocality") || types.getString(k).equals("locality") || types.getString(k).equals("country")) {
                                    if (!address_componentsobj.getString("long_name").contains("Unnamed ")) {
                                        address = address + address_componentsobj.getString("long_name") + ",";
                                    }

                                }
                            }
                        }
                        address = address.substring(0, address.length() - 1);
                        Toast.makeText(this, "address" + address, Toast.LENGTH_LONG).show();
                        // tv_address.setText(address);
                        // serviceAvailability();
                    }
                    //getServiceProviderLatLngReq(camera_latlng);
                    break;


            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }




    public void getAdressReq(LatLng latLng) {
        try {
            //showProgressDialog();
            check_request = "getAdressReq";
            Log.d("geeeeeettttt", ".......................");
            // tv_address.setText("loading...");

            String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latLng.latitude + "," + latLng.longitude + "&key=AIzaSyC8vtf3QcyASazUV9_zMCQcuS3ZUcCfcC0";
            Log.d("urlerefwesfewfewfwe", "" + url);

            StringRequest jsObjRequest = new StringRequest(Request.Method.GET, url, this, this) {
            };
            jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(5000,
                    DefaultRetryPolicy
                            .DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            MySingleton.getInstance(MapsSimple.this).addToRequestQueue(jsObjRequest);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }




    public void moveVechile(final Marker myMarker, final LatLng finalPosition) {

        final LatLng startPosition = myMarker.getPosition();

        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();

        final Interpolator interpolator = new AccelerateDecelerateInterpolator();
        final float durationInMs = 3000;
        final boolean hideMarker = false;

        handler.post(new Runnable() {
            long elapsed;
            float t;
            float v;

            @Override
            public void run() {
                // Calculate progress using interpolator
                elapsed = SystemClock.uptimeMillis() - start;
                t = elapsed / durationInMs;
                v = interpolator.getInterpolation(t);

                LatLng currentPosition = new LatLng(
                        startPosition.latitude * (1 - t) + (finalPosition.latitude) * t,
                        startPosition.longitude * (1 - t) + (finalPosition.longitude) * t);
                myMarker.setPosition(currentPosition);
                // myMarker.setRotation(finalPosition.getBearing());


                // Repeat till progress is completeelse
                if (t < 1) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                    // handler.postDelayed(this, 100);
                } else {
                    if (hideMarker) {
                        myMarker.setVisible(false);
                    } else {
                        myMarker.setVisible(true);
                    }
                }
            }
        });


    }
//another technique to move marker smoothly

    private void animateMarkerNew(final LatLng destination, final Marker marker) {

        if (marker != null) {

            final LatLng startPosition = marker.getPosition();
            final LatLng endPosition = new LatLng(destination.latitude, destination.longitude);

            final float startRotation = marker.getRotation();
            final LatLngInterpolatorNew latLngInterpolator = new LatLngInterpolatorNew.LinearFixed();

            ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
            valueAnimator.setDuration(3000); // duration 3 second
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    try {
                        float v = animation.getAnimatedFraction();
                     LatLng newPosition = latLngInterpolator.interpolate(v, startPosition, endPosition);
                        marker.setPosition(newPosition);
              /*          mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                                .target(newPosition)
                                .zoom(15.5f)
                                .build()));

                        marker.setRotation(getBearing(startPosition, new LatLng(destination.latitude, destination.longitude)));*/
                    } catch (Exception ex) {
                        //I don't care atm..
                    }
                }
            });
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);

                    // if (mMarker != null) {
                    // mMarker.remove();
                    // }
                    // mMarker = googleMap.addMarker(new MarkerOptions().position(endPosition).icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_car)));

                }
            });
            valueAnimator.start();
        }
    }


    private interface LatLngInterpolatorNew {
        LatLng interpolate(float fraction, LatLng a, LatLng b);

        class LinearFixed implements LatLngInterpolatorNew {
            @Override
            public LatLng interpolate(float fraction, LatLng a, LatLng b) {
                double lat = (b.latitude - a.latitude) * fraction + a.latitude;
                double lngDelta = b.longitude - a.longitude;
                // Take the shortest path across the 180th meridian.
                if (Math.abs(lngDelta) > 180) {
                    lngDelta -= Math.signum(lngDelta) * 360;
                }
                double lng = lngDelta * fraction + a.longitude;
                return new LatLng(lat, lng);
            }
        }
    }


    private double bearingBetweenLocations(LatLng latLng1,LatLng latLng2) {

        double PI = 3.14159;
        double lat1 = latLng1.latitude * PI / 180;
        double long1 = latLng1.longitude * PI / 180;
        double lat2 = latLng2.latitude * PI / 180;
        double long2 = latLng2.longitude * PI / 180;

        double dLon = (long2 - long1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;

        return brng;
    }


//another bearing technique
    private float getBearing(LatLng begin, LatLng end) {
        double lat = Math.abs(begin.latitude - end.latitude);
        double lng = Math.abs(begin.longitude - end.longitude);

        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
        return -1;
    }

    private void rotateMarker(final Marker marker, final float toRotation) {
        if(!isMarkerRotating) {
            final Handler handler = new Handler();
            final long start = SystemClock.uptimeMillis();
            final float startRotation = marker.getRotation();
            final long duration = 2000;

            final Interpolator interpolator = new LinearInterpolator();

            handler.post(new Runnable() {
                @Override
                public void run() {
                    isMarkerRotating = true;

                    long elapsed = SystemClock.uptimeMillis() - start;
                    float t = interpolator.getInterpolation((float) elapsed / duration);

                    float rot = t * toRotation + (1 - t) * startRotation;

                    float bearing =  -rot > 180 ? rot / 2 : rot;

                    marker.setRotation(bearing);

                    if (t < 1.0) {
                        // Post again 16ms later.
                        handler.postDelayed(this, 16);
                    } else {
                        isMarkerRotating = false;
                    }
                }
            });
        }
    }






/*

    public void rotateMarker(final Marker marker, final float toRotation, final float st) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final float startRotation = st;
        final long duration = 1555;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);

                float rot = t * toRotation + (1 - t) * startRotation;


                marker.setRotation(-rot > 180 ? rot / 2 : rot);
               // start_rotation = -rot > 180 ? rot / 2 : rot;
                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                }
            }
        });
    }





    public void animateMarker(final LatLng startPosition, final LatLng toPosition,
                              final boolean hideMarker) {


        final Marker marker = mMap.addMarker(new MarkerOptions()
                .position(startPosition)
                .title("Yeghar Car")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
        makeUrlformpoints(camera_latlng,servicProvide);

        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();

        final long duration = 1000;
        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startPosition.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startPosition.latitude;

                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

*/




    public void makeUrlformpoints(LatLng seeker, LatLng provider) {
        String str_origin = "origin=" + seeker.latitude + "," + seeker.longitude;
        String str_dest = "destination=" + provider.latitude + "," + provider.longitude;
        String sensor = "sensor=false";
        String mode = "mode=driving";
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;
        //String parameters = str_origin + "&" + str_dest + "&" + sensor+"&"+mode;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + "AIzaSyC8vtf3QcyASazUV9_zMCQcuS3ZUcCfcC0";
        //return url;
        FetchUrl FetchUrl = new FetchUrl();
        FetchUrl.execute(url);
    }

    @Override
    public void onLocationChanged(Location location) {


        Double tempLat=location.getLatitude();
        Double tempLng=location.getLongitude();
        LatLng templatlng=new LatLng(tempLat,tempLng);
      //  mMap.clear();
        //Toast.makeText(this,"Location Changed",Toast.LENGTH_LONG).show();
        //m2=  mMap.addMarker(new MarkerOptions().position(templatlng).title("Sevice Seeker"));
       // makeUrlformpoints(templatlng,servicProvide);


    }

    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {
            String data = "";

            try {
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);

        }
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();
        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        ArrayList<LatLng> points;

        public void remove()
        {

        }
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask", jsonData[0].toString());
                JSONParserTask parser = new JSONParserTask();
                Log.d("ParserTask", parser.toString());
                routes = parser.parse(jObject);
                JSONArray routeArray = jObject.getJSONArray("routes");
                JSONObject routeObject = routeArray.getJSONObject(0);
                JSONArray legsArray = routeObject.getJSONArray("legs");
                JSONObject timeandDistanceObj = legsArray.getJSONObject(0);
                JSONObject timeobj = timeandDistanceObj.getJSONObject("duration");
                String time = timeobj.getString("text");
                JSONObject distanceobj = timeandDistanceObj.getJSONObject("distance");
                // approxTime =time.toString();
                // approxDis =distanceobj.getString("text").toString();
                Log.d("ParserTask", "Executing routes");
                Log.d("ParserTask", routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask", e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {

            PolylineOptions lineOptions = null;

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = result.get(i);
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));

                    double lng = Double.parseDouble(point.get("lng"));
                    Log.d("latlng", "latlog" + point.get("lat"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }

                lineOptions.addAll(points);

                lineOptions.width(7);
                lineOptions.color(getResources().getColor(R.color.colorPrimaryDark));
                Log.d("onPostExecute", "onPostExecute lineoptions decoded");

            }
            if (lineOptions != null) {
                mMap.addPolyline(lineOptions);
                // sp_Onway.append(" and will reach in approximately "+ approxTime+"("+approxDis+")");
            } else {
                Log.d("onPostExecute", "without Polylines drawn");
            }
        }
    }


    private class ReverseGeocodingTask extends AsyncTask<LatLng, Void, String> {
        Context mContext;
        public ReverseGeocodingTask(Context context) {
            super();
            mContext = context;
        }
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }
        // Finding address using reverse geocoding
        @Override
        protected String doInBackground(LatLng... params) {
            //Geocoder geocoder = new Geocoder(mContext);
            double latitude = params[0].latitude;
            double longitude = params[0].longitude;
            String strAdd="";
            Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null) {
                    Address returnedAddress = addresses.get(0);
                    StringBuilder strReturnedAddress = new StringBuilder("");

                    for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
                        strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
                    }
                    strAdd = strReturnedAddress.toString();
                    Log.d("My Current loction add", strReturnedAddress.toString());
                } else {
                    Log.d("My Current loction add", "No Address returned!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("My Current loction addr", "Canont get Address!");
            }
            return strAdd;
        }

        @Override
        protected void onPostExecute(String addressText) {
           /*Setting adress in us_location textview.*/
          //  tv_address.setText(addressText);

        }
    }

}
