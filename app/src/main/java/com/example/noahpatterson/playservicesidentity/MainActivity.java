package com.example.noahpatterson.playservicesidentity;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, ResultCallback<People.LoadPeopleResult>
{

    private static final String TAG = "main_activity";
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_USES_CREDENTIALS = 1;
    private GoogleApiClient googleApiClient;
    private SignInButton signInButton;
    private Button signOutButton;
    private Button revokeButton;
    private TextView signInStatusTextView;
    private int signInProgress;
    private PendingIntent signInIntent;
    private int signInError;

    private static final int STATE_SIGNED_IN = 0;
    private static final int STATE_SIGN_IN = 1;
    private static final int STATE_PROGRESS = 2 ;

    private static final int RC_SIGN_IN = 0;

    private static final int DIALOG_PLAY_SERVICES_ERROR = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        signInButton = (SignInButton) findViewById(R.id.signInButton);
        signOutButton= (Button) findViewById(R.id.signOutButton);
        revokeButton = (Button) findViewById(R.id.revokeAccessButton);
        signInStatusTextView = (TextView) findViewById(R.id.signInStatusTextView);

        signOutButton.setOnClickListener(this);
        signOutButton.setOnClickListener(this);
        revokeButton.setOnClickListener(this);

    }

    private GoogleApiClient buildGoogleApiClient() {
        return new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(new Scope(Scopes.PLUS_LOGIN))
                .addScope(new Scope(Scopes.PLUS_ME))
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getPermission();
    }

    @Override
    protected void onStop() {
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
//        getPermission();
        signInButton.setEnabled(false);
        signOutButton.setEnabled(true);
        revokeButton.setEnabled(true);

        signInProgress = STATE_SIGNED_IN;

//        Plus.PeopleApi.loadVisible(googleApiClient, null).setResultCallback(this);
        Person currentUser = Plus.PeopleApi.getCurrentPerson(googleApiClient);
        String userName = "unknown user";
        if (currentUser != null) {
            userName = currentUser.getDisplayName();
        }
        signInStatusTextView.setText("Signed in as: " + userName);
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (signInProgress != STATE_PROGRESS) {
            signInIntent = connectionResult.getResolution();
            signInError = connectionResult.getErrorCode();

            if (signInProgress == STATE_SIGNED_IN) {
                resolveSignInError();
            }
        }

        signOut();
    }

    private void signOut() {
        signInButton.setEnabled(true);
        signOutButton.setEnabled(false);
        revokeButton.setEnabled(false);

        signInStatusTextView.setText("Signed out");

    }

    public void onClick(View v) {
        if (!googleApiClient.isConnecting()) {
            switch (v.getId()) {
                case R.id.signInButton:
                    signInStatusTextView.setText("Signing In");
                    resolveSignInError();
                    break;
                case R.id.signOutButton:
                    Plus.AccountApi.clearDefaultAccount(googleApiClient);
                    googleApiClient.disconnect();
                    googleApiClient.connect();
                    break;
                case R.id.revokeAccessButton:
                    Plus.AccountApi.clearDefaultAccount(googleApiClient);
                    Plus.AccountApi.revokeAccessAndDisconnect(googleApiClient);
                    googleApiClient = buildGoogleApiClient();
                    googleApiClient.connect();
                    break;
            }
        }
    }

    private void resolveSignInError() {
        if (signInIntent != null) {
            try {
                signInProgress = STATE_PROGRESS;
                startIntentSenderForResult(signInIntent.getIntentSender(), RC_SIGN_IN, null, 0,0,0);
            } catch (IntentSender.SendIntentException e) {
                signInProgress = STATE_SIGN_IN;
                googleApiClient.connect();
            }
        } else {
            showDialog(DIALOG_PLAY_SERVICES_ERROR);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                signInProgress = STATE_SIGN_IN;
            } else {
                signInProgress = STATE_SIGNED_IN;
            }

            if (!googleApiClient.isConnecting()) {
                googleApiClient.connect();
            }
        }
    }

        public void getPermission() {
        Log.d(TAG, "in getPermission");
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.GET_ACCOUNTS)) {
                Log.d(TAG, "in shouldShowRationale");

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.GET_ACCOUNTS},
                        MY_PERMISSIONS_REQUEST_ACCESS_USES_CREDENTIALS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            googleApiClient = buildGoogleApiClient();
            googleApiClient.connect();
            return;

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d(TAG, "in onRequestPermissionsResult");
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_USES_CREDENTIALS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    googleApiClient = buildGoogleApiClient();
                    googleApiClient.connect();
                    return;


                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onResult(People.LoadPeopleResult loadPeopleResult) {

    }
}
