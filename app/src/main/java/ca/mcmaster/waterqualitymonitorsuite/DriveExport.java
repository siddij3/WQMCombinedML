package ca.mcmaster.waterqualitymonitorsuite;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveResourceClient;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;

import android.content.IntentSender;
import android.support.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.CreateFileActivityOptions;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

import java.util.Set;

import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.android.gms.drive.OpenFileActivityOptions;

/**
 * Created by DK on 2017-11-09.
 */

public class DriveExport extends Activity {

    private static final String TAG = DriveExport.class.getSimpleName();

    //Extras Definitions
    public static final String EXTRAS_FILENAME = "FILE_NAME";
    public static final String EXTRAS_DATA_ARRAY = "DATA_ARRAY";
    public static final String EXTRAS_FAIL_DESCRIPTION = "FAIL_REASON";
    public static final String EXTRAS_DRIVE_ID = "DRIVE_ID";

    private String filename;
    private ArrayList<String> dataArray;

    //Drive Defines
    private static final int REQUEST_CODE_SIGN_IN = 0;
    private static final int REQUEST_CODE_CREATE_FILE = 2;

    public static final int RESULT_CODE_SAVE_FAILED = 10;
    public static final int RESULT_CODE_SAVE_SUCCESS = 1;

    //Drive API
    private DriveClient mDriveClient;
    private DriveResourceClient mDriveResourceClient;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Intent and extra data
        final Intent intent = getIntent();
        filename = intent.getStringExtra(EXTRAS_FILENAME);
        dataArray = intent.getStringArrayListExtra(EXTRAS_DATA_ARRAY);

        //Check provided extras contain data
        if (!(filename != null && dataArray != null && filename.length()>0 && dataArray.size() >0)){
            activityFailed("Data provided for drive export invalid!");
        }

        for (String s:
             dataArray) {
            Log.i(TAG, "onCreate: " + s);
        }
        signIn();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                Log.i(TAG, "Sign in request code");
                // Called after user is signed in.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Sign in Result OK");
                    Task<GoogleSignInAccount> getAccountTask =
                            GoogleSignIn.getSignedInAccountFromIntent(data);
                    if (getAccountTask.isSuccessful()) {
                        initializeDriveClient(getAccountTask.getResult());
                    } else {
                        activityFailed("Sign-in failed.");
                    }
                } else {
                    // Sign-in may fail or be cancelled by the user.
                    activityFailed("Sign-in failed.");
                }
                break;
            case REQUEST_CODE_CREATE_FILE:
                if (resultCode != RESULT_OK) {
                    Log.e(TAG, "Unable to create file");
                    activityFailed("Error: File was not successfully uploaded to Drive");
                } else {
                    Log.d(TAG, "onActivityResult: File Created!");
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRAS_DRIVE_ID,data.getParcelableExtra(OpenFileActivityOptions.EXTRA_RESPONSE_DRIVE_ID));
                    setResult(RESULT_CODE_SAVE_SUCCESS,resultIntent);
                    finish();
                }
                break;
        }
    }
    /**
     * Starts the sign-in process and initializes the Drive client.
     */
    protected void signIn() {
        Set<Scope> requiredScopes = new HashSet<>(2);
        requiredScopes.add(Drive.SCOPE_FILE);
        //requiredScopes.add(Drive.SCOPE_APPFOLDER);
        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (signInAccount != null && signInAccount.getGrantedScopes().containsAll(requiredScopes)) {
            initializeDriveClient(signInAccount);
        } else {
            GoogleSignInOptions signInOptions =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestScopes(Drive.SCOPE_FILE)
                            //.requestScopes(Drive.SCOPE_APPFOLDER)
                            .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, signInOptions);
            startActivityForResult(googleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
        }
    }


    private void initializeDriveClient(GoogleSignInAccount signInAccount) {
        mDriveClient = Drive.getDriveClient(getApplicationContext(), signInAccount);
        mDriveResourceClient = Drive.getDriveResourceClient(getApplicationContext(), signInAccount);
        createFileWithIntent();
    }

    private void createEmptyFile() {
        // [START create_empty_file]
        getDriveResourceClient()
                .getRootFolder()
                .continueWithTask(new Continuation<DriveFolder, Task<DriveFile>>() {
                    @Override
                    public Task<DriveFile> then(@NonNull Task<DriveFolder> task) throws Exception {
                        DriveFolder parentFolder = task.getResult();
                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle("New file")
                                .setMimeType("application/vnd.google-apps.spreadsheet")
                                .setStarred(true)
                                .build();
                        return getDriveResourceClient().createFile(parentFolder, changeSet, null);
                    }
                })
                .addOnSuccessListener(this,
                        new OnSuccessListener<DriveFile>() {
                            @Override
                            public void onSuccess(DriveFile driveFile) {
                                //showMessage(getString(R.string.file_created,
                                        //driveFile.getDriveId().encodeToString()));

                            }
                        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Unable to create file", e);
                        //showMessage(getString(R.string.file_create_error));

                    }
                });
        // [END create_empty_file]
    }

    private void createFileWithIntent() {
        // [START create_file_with_intent]
        Task<DriveContents> createContentsTask = getDriveResourceClient().createContents();
        createContentsTask
                .continueWithTask(new Continuation<DriveContents, Task<IntentSender>>() {
                    @Override
                    public Task<IntentSender> then(@NonNull Task<DriveContents> task)
                            throws Exception {
                       DriveContents contents = task.getResult();
                        OutputStream outputStream = contents.getOutputStream();
                        try (Writer writer = new OutputStreamWriter(outputStream)) {
                            for(String str: dataArray) {
                                writer.write(str);
                                writer.write('\n');
                            }
                            writer.close();
                        }

                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle(filename)
                                .setMimeType("text/tsv")
                                .setStarred(true)
                                .build();

                        CreateFileActivityOptions createOptions =
                                new CreateFileActivityOptions.Builder()
                                        .setInitialDriveContents(contents)
                                        .setInitialMetadata(changeSet)
                                        .build();
                        return getDriveClient().newCreateFileActivityIntentSender(createOptions);
                    }
                })
                .addOnSuccessListener(this,
                        new OnSuccessListener<IntentSender>() {
                            @Override
                            public void onSuccess(IntentSender intentSender) {
                                try {
                                    startIntentSenderForResult(
                                            intentSender, REQUEST_CODE_CREATE_FILE, null, 0, 0, 0);
                                } catch (IntentSender.SendIntentException e) {
                                    Log.e(TAG, "Unable to create file, SendIntentException: " + e.toString());
                                    activityFailed("Unable to create file, SendIntentException");
                                }
                            }
                        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Unable to create file " + e.toString());
                        activityFailed("Unable to create file contents");
                    }
                });
        // [END create_file_with_intent]
    }

    protected DriveClient getDriveClient() {
        return mDriveClient;
    }

    protected DriveResourceClient getDriveResourceClient() {
        return mDriveResourceClient;
    }

    private void activityFailed(String reason){
        Log.e(TAG, "activityFailed: " + reason);
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRAS_FAIL_DESCRIPTION,reason);
        setResult(RESULT_CODE_SAVE_FAILED,resultIntent);
        finish();
    }

}
