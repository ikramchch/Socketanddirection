package com.squalala.chatapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.squalala.chatapp.adapter.MessagesAdapter;
import com.squalala.chatapp.common.ChatConstant;
import com.squalala.chatapp.utils.ChatUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;


public class ChatActivity extends Activity  {

    private WebSocketConnection mConnection = new WebSocketConnection();

    private static final String TAG = ChatActivity.class.getSimpleName();

    private MessagesAdapter messagesAdapter;
    private ArrayList<Message> messages = new ArrayList<Message>();

    private String myName;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    private static final int PERMISSION_REQUEST_CODE_LOCATION = 1;

    int REQUEST_CHECK_SETTINGS  = 1000;

   // @Bind(R.id.editMessage)
    EditText editMessage;

   // @Bind(R.id.btnSendMessage)
    Button btnSendMessage;
    Location generalLocation;
   // @Bind(R.id.recyclerView_messages)
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        editMessage=(EditText)findViewById(R.id.editMessage);
        btnSendMessage=(Button)findViewById(R.id.btnSendMessage);
        recyclerView=(RecyclerView)findViewById(R.id.recyclerView_messages);
       // ButterKnife.bind(this);

        // On récupère le nom entré par l'utilisateur
        myName = getIntent().getStringExtra(ChatConstant.TAG_NAME);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(layoutManager);

        messagesAdapter = new MessagesAdapter(messages);

        recyclerView.setAdapter(messagesAdapter);
       // requestPermission(PERMISSION_REQUEST_CODE_LOCATION,getApplicationContext(), this);
      //  requestPermission(PERMISSION_REQUEST_CODE_LOCATION,getApplicationContext(), (Activity) getApplicationContext());

        try {

            mConnection.connect(ChatConstant.URL, new WebSocketHandler() {

                @Override
                public void onOpen() {
                    Log.d(TAG, "\n" + "Login successfully : " + ChatConstant.URL);
                }

                @Override
                public void onTextMessage(String payload) {
                    Log.d(TAG, " " + payload);

                    messages.add(ChatUtils.jsonToMessage(payload));
                    messagesAdapter.notifyDataSetChanged();

                    try {
                        JSONObject jsonObject = new JSONObject(payload);


                    }
                    catch (JSONException e)
                    {
                        e.printStackTrace();
                    }


                    scrollToBottom();
                }

                @Override
                public void onClose(int code, String reason) {
                    Log.d(TAG, "\n" + "Lost connection");
                }
            });

        } catch (WebSocketException e) {
            e.printStackTrace();
        }






        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // startRepeatingTask();
                /*
                 * On vérifie que notre edittext n'est pas vide
                 */
                if (!TextUtils.isEmpty(getMessage())) {

                    // On met "true" car c'est notre message
                    Message message = new Message(myName, getMessage(), true);

                    String json = ChatUtils.messageToJson(message);

                    // On envoie notre message
                    mConnection.sendTextMessage(json);

                    // On ajoute notre message à notre list
                    messages.add(message);

                    // On notifie notre adapter
                    messagesAdapter.notifyDataSetChanged();

                    scrollToBottom();

                    // On efface !
                    editMessage.setText("");
                }

            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();

    }
    @Override
    protected void onStop() {
        super.onStop();

        //Disconnect the google client api connection.
        if (googleApiClient != null) {
            googleApiClient.disconnect();
        }

    }

    @Override
    protected void onPause() {
        try {
            super.onPause();

            /*
            * Stop retrieving locations when we go out of the application.
            * */

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

    }


    private void scrollToBottom() {
        recyclerView.scrollToPosition(messages.size() - 1);
    }









    public static void requestPermission(int perCode, Context _c, Activity _a){

        String fineLocationPermissionString = Manifest.permission.ACCESS_FINE_LOCATION;
        String coarseLocationPermissionString = Manifest.permission.ACCESS_COARSE_LOCATION;

        if  (   ContextCompat.checkSelfPermission(_a, fineLocationPermissionString) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(_a, coarseLocationPermissionString) != PackageManager.PERMISSION_GRANTED
                ) {

            //user has already cancelled the permission prompt. we need to advise him.
            if (ActivityCompat.shouldShowRequestPermissionRationale(_a,fineLocationPermissionString)){
                Toast.makeText(_c,"We must need your permission in order to access your reporting location.",Toast.LENGTH_LONG).show();
            }

            ActivityCompat.requestPermissions(_a,new String[]{fineLocationPermissionString, coarseLocationPermissionString},perCode);

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        for (String per : permissions) {
            System.out.println("permissions are  " + per);
        }

        switch (requestCode) {

            case PERMISSION_REQUEST_CODE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(this, "Permission loaded...", Toast.LENGTH_SHORT).show();

                } else {

                    Toast.makeText(getApplicationContext(),"Permission Denied, You cannot access location data.",Toast.LENGTH_LONG).show();

                }
                break;
        }
    }



    private String getMessage() {
        return editMessage.getText().toString().trim();
    }




    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CHECK_SETTINGS){
            Toast.makeText(this, "Setting has changed...", Toast.LENGTH_SHORT).show();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }




}
