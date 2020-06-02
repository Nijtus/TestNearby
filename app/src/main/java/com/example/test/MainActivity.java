package com.example.test;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import static com.google.android.gms.nearby.connection.Strategy.P2P_CLUSTER;

public class MainActivity extends AppCompatActivity {
    private static final String SERVICE_ID = "com.iut.covidtracker";
    private static final Strategy STRATEGY = P2P_CLUSTER;
    private static Activity context;
    private static TextView centerText ;
    private static final String[] permission = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.BLUETOOTH,Manifest.permission.BLUETOOTH_ADMIN,Manifest.permission.ACCESS_WIFI_STATE,Manifest.permission.CHANGE_WIFI_STATE};

    MessageListener mMessageListener ;
    Message mMessage;
    static Button call;
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void askForPermission()
    {

        requestPermissions(permission, 2);

    }

   @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
      if(requestCode == 2)
      {
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            centerText.setText("perm atribuer");

        }
        else
        {
            centerText.setText("perm refuser");
        }
      }

      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        centerText = findViewById(R.id.centerText);
        centerText.setText("Création");
        call = (Button) findViewById(R.id.call);
        call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startAdvertising();
                startDiscovery();
            }
        });
        askForPermission();
    }


    static class ReceiveBytesPayloadListener extends PayloadCallback {

        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            // This always gets the full data of the payload. Will be null if it's not a BYTES
            // payload. You can check the payload type with payload.getType().
            byte[] receivedBytes = payload.asBytes();
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
            // Bytes payloads are sent as a single chunk, so you'll receive a SUCCESS update immediately
            // after the call to onPayloadReceived().
        }
    }

    ReceiveBytesPayloadListener payloadCallback = new ReceiveBytesPayloadListener();

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    centerText.setText("connexion innité");
                    // Automatically accept the connection on both sides.
                    Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            centerText.setText("conecté");
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            centerText.setText("connextion rejeté");
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            centerText.setText("connextion erreur");
                            break;
                        default:
                            // Unknown status code
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                }
            };


    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    // An endpoint was found. We request a connection to it.
                    //centerText.setText("point de connextion trouvé");
                    Nearby.getConnectionsClient(context)
                            .requestConnection(getUserNickname(), endpointId, connectionLifecycleCallback)
                            .addOnSuccessListener(
                                    new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            centerText.setText("connextion demandé");
                                        }
                                    })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            centerText.setText("echec de demande de connexion");
                                        }
                                    });
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    // A previously discovered endpoint has gone away.
                }
            };


   private void startAdvertising() {
       AdvertisingOptions advertisingOptions =
      new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
    Nearby.getConnectionsClient(context)
      .startAdvertising(
          getUserNickname(), SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
      .addOnSuccessListener(
              new OnSuccessListener<Void>() {
                  @Override
                  public void onSuccess(Void unused) {
                      centerText.setText("advertising !");
                  }
              })
      .addOnFailureListener(
              new OnFailureListener() {
                  @Override
                  public void onFailure(@NonNull Exception e) {
                      // We were unable to start advertising.
                  }
              });
   }

    private void startDiscovery() {
      DiscoveryOptions discoveryOptions =
          new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();
      Nearby.getConnectionsClient(context)
          .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
          .addOnSuccessListener(
                  new OnSuccessListener<Void>() {
                      @Override
                      public void onSuccess(Void unused) {
                          centerText.setText("dicovery !");
                      }
                  })
          .addOnFailureListener(
                  new OnFailureListener() {
                      @Override
                      public void onFailure(@NonNull Exception e) {
                          centerText.setText("error dicovery !: "+e);
                      }
                  });
    }


    private String getUserNickname(){
        return "Test0001";
    }



}
