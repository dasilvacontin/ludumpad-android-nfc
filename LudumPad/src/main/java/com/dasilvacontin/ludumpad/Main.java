package com.dasilvacontin.ludumpad;

import java.io.IOException;
import java.lang.Error;
import java.nio.charset.Charset;

import com.dasilvacontin.ludumpad.util.SystemUiHider;
import com.dejamobile.retailapi.core.RetailAPIManager;
import com.dejamobile.retailapi.core.Session;
import com.dejamobile.retailapi.model.*;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
@SuppressLint("NewApi")
public class Main extends Activity implements RetailAPIManager.onMEHolderListener {

    private RetailAPIManager retailManager;
    private final static String retailID = "ORI1";
    private Session s;
    private String pid;

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    
    public final static String GAMEPAD_URL = "com.example.myfirstapp.GAMEPAD_URL";
    
    private SystemUiHider mSystemUiHider;
    
    private Button read_qr;
    private String gamepadURL;
    
    private NfcAdapter mNfcAdapter;  
    private IntentFilter[] mWriteTagFilters;  
    private PendingIntent mNfcPendingIntent;
    
    private boolean silent=false;  
    private boolean writeProtect = false;
    Context context;
    public final static int NFCMODE_READ = 0;
    public final static int NFCMODE_WRITE = 1; 
    private int nfcMode = NFCMODE_READ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();

        retailManager = RetailAPIManager.getInstance(this);
        
        /* Typography things that doesn't work */
        TextView tx = (TextView)findViewById(R.id.title);
        Typeface custom_font;
        String assetPath = "CabinRegular.ttf";
        
        try {
        	custom_font = Typeface.createFromAsset(getAssets(), assetPath);
            tx.setTypeface(custom_font);
        } catch (Exception e) {
            Log.e("Font", "Could not get typeface '" + assetPath
                    + "' because " + e.getMessage());
        }
        
        
        /* NFC */
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);  
        mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,  
                  getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP  
                  | Intent.FLAG_ACTIVITY_CLEAR_TOP), 0);
        
        IntentFilter discovery = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);  
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);      
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);  
        // Intent filters for writing to a tag  
        mWriteTagFilters = new IntentFilter[] { discovery }; 
        
        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        read_qr = (Button) findViewById(R.id.read_qr_button);
        View contentView = (View) findViewById(R.id.title);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        
        findViewById(R.id.fullscreen_content_controls).setOnTouchListener(mDelayHideTouchListener);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
    
    public void readQR (View v) {
    	
    	try {

    	    Intent intent = new Intent("com.google.zxing.client.android.SCAN");
    	    intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes

    	    startActivityForResult(intent, 0);

    	} catch (Exception e) {

    	    Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
    	    Intent marketIntent = new Intent(Intent.ACTION_VIEW,marketUri);
    	    startActivity(marketIntent);

    	}
    	
    	//Intent qrIntent = new Intent(this, GamepadActivity.class);
    	//startActivity(qrIntent);
    	
    }

    private void startGamepad () {
        Intent intent = new Intent (this, GamepadActivity.class);
        intent.putExtra(GAMEPAD_URL, gamepadURL+"&pid="+pid);
        startActivity(intent);
        nfcMode = NFCMODE_READ; //In case after closing the gamepad wans to read an NFC tag
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {           
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {

            if (resultCode == RESULT_OK) {
                gamepadURL = data.getStringExtra("SCAN_RESULT");
                Log.d("QR Reader Result", gamepadURL);


                nfcMode = NFCMODE_WRITE;

                try {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    alertDialogBuilder.setTitle("Save connection in NFC tag");
                    alertDialogBuilder
                            .setMessage("Tap an NFC tag to save the game's connection properties in it!")
                            .setCancelable(false)
                            .setPositiveButton("Start Gamepad!", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    startGamepad();
                                }
                            });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                } catch (Error e) {
                    Log.d("Ludumpad", "Error creating dialog");
                }


            }
            if(resultCode == RESULT_CANCELED){
                //handle cancel
            }
        }
    }

    private void OpenOrReopenSession()
    {
        //setProgressBarIndeterminateVisibility(true);
        try {
            retailManager.setServiceStateListener(new RetailAPIManager.onServiceStateListener() {
                @Override
                public void onServiceStateAvailable(ServiceState state) {
                    if (state.getState() == ServiceState.SUBSCRIBED)
                    {
                        retailManager.setSessionListener(new RetailAPIManager.onSessionListener() {
                            @Override
                            public void onSessionOpened(Session session) {
                                s = session;
                                Log.d("Ludumpad", "Session opened");
                                if (pid == null) getUniqueId();
                                //tLog.setText("Session successfully started for retailer " + retailID);
                                //setProgressBarIndeterminateVisibility(false);
                            }

                            @Override
                            public void onSessionError(com.dejamobile.retailapi.model.Error e) {
                                Log.d("Ludumpad", "Session Error");
                                //tLog.setText("bad retailerId");
                                //setProgressBarIndeterminateVisibility(false);
                            }
                        });
                        try {
                            retailManager.doOpenSession(retailID);
                        } catch (Exception e) {
                            //tLog.setText("Exception : " + e.getClass().getName());
                        }
                    }
                    else if (state.getState() == ServiceState.UNSUBSCRIBED)
                    {
                        Log.d("Ludumpad", "ServiceState UNSUBSCRIBED");
                        //tLog.setText("RetailAPI " + retailID + " is not activated");
                        //setProgressBarIndeterminateVisibility(false);
                    }
                }

                @Override
                public void onServiceStateError(com.dejamobile.retailapi.model.Error e) {
                    //tLog.setText("onServiceStateError : " + e.getReason());
                }
            });
            retailManager.doGetServiceState(retailID);
        } catch (Exception e) {
            //tLog.setText("Exception : " + e.getClass().getName());
        }
    }

    @Override  
    protected void onResume() {  
         super.onResume();

         Log.d("LudumPad", "onResume");

         OpenOrReopenSession();

         if(mNfcAdapter != null) { 
        	 
              if (!mNfcAdapter.isEnabled()){
            	  Toast.makeText(context, "Oh noes! NFC is not enabled! Update your settings.", Toast.LENGTH_LONG).show();
            	  
              } else {
            	  mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
              }
	         
          } else {  
              Toast.makeText(context, "Sorry, No NFC Adapter found.", Toast.LENGTH_SHORT).show();  
         }




    }
    
    @Override  
    protected void onPause() {  
         super.onPause();
         CloseSession();
         if(mNfcAdapter != null) mNfcAdapter.disableForegroundDispatch(this);  
    }

    private void CloseSession()
    {
        if ((retailManager != null) && (s != null))
        {
            try {
                retailManager.closeSession(s);
            } catch (Exception e) {
                //tLog.setText("Exception : " + e.getClass().getName());
            }
        }
    }
    
    public WriteResponse writeTag(NdefMessage message, Tag tag) {  
		int size = message.toByteArray().length;  
		String mess = "";  
		try {  
			Ndef ndef = Ndef.get(tag);  
			if (ndef != null) {  
				ndef.connect();  
			    if (!ndef.isWritable()) {  
			    	return new WriteResponse(0,"Tag is read-only");  
			    }  
			    if (ndef.getMaxSize() < size) {  
			    	mess = "Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size  
			    			+ " bytes.";  
			    	return new WriteResponse(0,mess);  
			    }  
			    ndef.writeNdefMessage(message);  
			    //if(writeProtect) ndef.makeReadOnly();  We don't want to waste tags :)
			    mess = "Wrote game connection in NFC tag!";
			    return new WriteResponse(1,mess);  
			} else {  
				NdefFormatable format = NdefFormatable.get(tag);  
				if (format != null) {  
					try {  
						format.connect();  
						format.format(message);  
						mess = "Formatted tag and wrote message";  
						return new WriteResponse(1,mess);  
					} catch (IOException e) {  
						mess = "Failed to format tag.";  
						return new WriteResponse(0,mess);  
					}  
				} else {  
					mess = "Tag doesn't support NDEF.";  
					return new WriteResponse(0,mess);  
				}  
			}  
		} catch (Exception e) {  
			mess = "Failed to write tag";  
			return new WriteResponse(0,mess);  
		}  
    }

    @Override
    public void onMEHolderRead(MEHolderId meHolderId) {
        Log.d("Ludumpad", meHolderId.getMEHolderId().length+" bytes read");
        pid = new String(meHolderId.getMEHolderId());
    }

    @Override
    public void onMEHolderError(com.dejamobile.retailapi.model.Error error) {
        Intent intent = new Intent (this, GamepadActivity.class);
        intent.putExtra(GAMEPAD_URL, gamepadURL);
        startActivity(intent);
    }

    private class WriteResponse {  
    	int status;  
    	String message;  
    	WriteResponse(int Status, String Message) {  
    		this.status = Status;  
            this.message = Message;  
    	}  
    	public int getStatus() {  
    		return status;  
    	}  
    	public String getMessage() {  
    		return message;  
    	}	  
    }
    
    public static boolean supportedTechs(String[] techs) {  
    	boolean ultralight=false;  
    	boolean nfcA=false;  
    	boolean ndef=false;  
    	for(String tech:techs) {  
    		if(tech.equals("android.nfc.tech.MifareUltralight")) {  
    			ultralight=true;  
    		}else if(tech.equals("android.nfc.tech.NfcA")) {   
    			nfcA=true;  
    		} else if(tech.equals("android.nfc.tech.Ndef") || tech.equals("android.nfc.tech.NdefFormatable")) {  
    			ndef=true;  
            }  
    	}  
    	if(ultralight && nfcA && ndef) {  
    		return true;  
    	} else {  
    		return false;  
    	}  
    } 
    
    private boolean writableTag(Tag tag) {  
    	try {  
    		Ndef ndef = Ndef.get(tag);  
    		if (ndef != null) {  
    			ndef.connect();  
    			if (!ndef.isWritable()) {  
    				Toast.makeText(context,"Tag is read-only.",Toast.LENGTH_SHORT).show();  
    				//Sounds.PlayFailed(context, silent);  
    				ndef.close();   
    				return false;  
    			}  
    			ndef.close();  
    			return true;  
    		}   
    	} catch (Exception e) {  
    		Toast.makeText(context,"Failed to read tag",Toast.LENGTH_SHORT).show();  
    		//Sounds.PlayFailed(context, silent);  
    	}  
    	return false;  
    }
    
    @Override  
    protected void onNewIntent(Intent intent) {
         Log.d("Main", "Some intent");
         if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {  
        	 Log.d("Main", "Intent action tag discovered");
        	 // validate that this tag can be written  
        	 Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        	 if(supportedTechs(detectedTag.getTechList())) {  
        		 // check if tag is writable (to the extent that we can 
        		 
        		 
        		 switch (nfcMode) {
        		 
        		 	case NFCMODE_WRITE:
        		 		 if(writableTag(detectedTag)) {  
                			 //writeTag here  
                			 WriteResponse wr = writeTag(getTagAsNdef(), detectedTag);  
                			 String message = (wr.getStatus() == 1? "Success: " : "Failed: ") + wr.getMessage();  
                			 Toast.makeText(context,message,Toast.LENGTH_SHORT).show();  
                		 } else {  
                			 Toast.makeText(context,"This tag is not writable",Toast.LENGTH_SHORT).show();  
                			 //Sounds.PlayFailed(context, silent);  
                		 }
        		 	break;
        		 	
        		 	case NFCMODE_READ:
                        NdefMessage[] messages = null;
                        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                        if (rawMsgs != null) {
                            messages = new NdefMessage[rawMsgs.length];
                            for (int i = 0; i < rawMsgs.length; i++) {
                                messages[i] = (NdefMessage) rawMsgs[i];
                            }
                        }
                        try {
                            if(messages[0] != null) {
                                String result="";
                                byte[] payload = messages[0].getRecords()[0].getPayload();
                                // this assumes that we get back am SOH followed by host/code
                                for (int b = 0; b<payload.length; b++) { // skip SOH
                                    result += (char) payload[b];
                                }
                                //Toast.makeText(getApplicationContext(), "Tag Contains " + , Toast.LENGTH_SHORT).show();
                                gamepadURL = result;
                                startGamepad();
                            }
                        } catch (Error e) {

                        }
                        break;
        		 	
        		 }
        		 
        		
        		 
        		 
        	 } else {  
        		 Toast.makeText(context,"This tag type is not supported",Toast.LENGTH_SHORT).show();  
        		 //Sounds.PlayFailed(context, silent);  
        	 }  
         }
         super.onNewIntent(intent);
    }
    
    private NdefMessage getTagAsNdef() {  
    	boolean addAAR = false;
    	
    	/*
        String uniqueId = "kipos.me";      
        byte[] uriField = uniqueId.getBytes(Charset.forName("US-ASCII"));  
        byte[] payload = new byte[uriField.length + 1];       //add 1 for the URI Prefix  
        payload[0] = 0x01;                        //prefixes http://www. to the URI  
        System.arraycopy(uriField, 0, payload, 1, uriField.length); //appends URI to payload 
        */
    	
    	byte[] uriField = gamepadURL.getBytes(Charset.forName("US-ASCII"));
    	byte[] payload = new byte[uriField.length];
    	System.arraycopy(uriField, 0, payload, 0, uriField.length);
    	
        NdefRecord rtdUriRecord = new NdefRecord(  
        		NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, new byte[0], payload);  
        if(addAAR) {  
        	// note: returns AAR for different app (nfcreadtag)  
        	return new NdefMessage(new NdefRecord[] {  
        			rtdUriRecord, NdefRecord.createApplicationRecord("com.tapwise.nfcreadtag")  
        	});   
        } else {  
        	return new NdefMessage(new NdefRecord[] {  
        			rtdUriRecord});  
        }  
    }

    public void getUniqueId()
    {
        if (s != null)
        {
            //setProgressBarIndeterminateVisibility(true);
            s.setMEHolderIdListener(this);
            s.doGetMEHolderId();
        }
        else
        {
            Log.d("Ludumpad", "Session is not open");
           // tLog.setText("No session");
        }

    }

 }
