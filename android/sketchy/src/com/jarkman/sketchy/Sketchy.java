package com.jarkman.sketchy;


import java.io.File;
import java.util.Random;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


/*************************************************************************
 * 
 * 
 * 	Sketchy
 * 
 *  This is the Android app which is the input end of an Android->Bluetooth->Arduino drawing robot:
 *  http://jarkman.co.uk/catalog/robots/sketchy.htm
 * 
 *  
 * 	
 *
 */
public class Sketchy extends Activity {
    
	final static String TAG = "com.jarkman.sketchy";
   private static final boolean D = true;

	
	int mImageWidth = 128;
	int mImageHeight = 128;
 	static final int RESULT_PICK_AND_CROP_IMAGE = 3;
 	static final int RESULT_CAMERA_IMAGE = 4;
 	static final int RESULT_CROP = 5;
 	

 	static final int MODE_PICTURE = 1;
 	static final int MODE_SNOWFLAKE = 2;
	int mMode;
	
	ImageView mPhotoImageView;
	ImageView mOutputImageView;
	ImageButton mProcessButton;
	Button mSnowflakeButton;
	TextView mStatusText;
	TextView mBluetoothText;
	//TextView mBluetoothSentText;
	
	int mBluetoothState = BluetoothChatService.STATE_NONE;
	
	String mStatus;
	int mNLines = 0;
	int mNTries = 0;
	
	Bitmap mInputBitmap;
	Bitmap mOutputBitmap;
	   
	Bitmap mEdgeBitmap;
	Bitmap mVectorBitmap;
	   	   
	int mWidth;
	int mHeight;
	//int mStep;
	
	boolean mWorking = false;
	boolean mStopping = false;
    byte[] mSend = null;

	
	
	Random mRandom = new Random(); 
    
	
	// From the BluetoothChat sample:
	// Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
    
    
    
    
    
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    
    
       	Settings.setInitialPrefs( this, false );
        
	 	
    mPhotoImageView = (ImageView) findViewById(R.id.photo_image);
    mOutputImageView = (ImageView) findViewById(R.id.output_image);

    mOutputImageView.setVisibility(View.GONE);
    
    ImageButton picButton = (ImageButton) findViewById(R.id.take_picture_button);
    picButton.setOnClickListener(mTakePictureListener);

    ImageButton albumButton = (ImageButton) findViewById(R.id.album_button);
    albumButton.setOnClickListener(mAlbumListener);

    
    mProcessButton = (ImageButton) findViewById(R.id.process_picture_button);
    mProcessButton.setOnClickListener(mProcessPictureListener);
    
    mSnowflakeButton = (Button) findViewById(R.id.snowflake_button);
    mSnowflakeButton.setOnClickListener(mSnowflakeListener);

    mStatusText = (TextView) findViewById(R.id.status_text);
    mBluetoothText = (TextView) findViewById(R.id.bluetooth_text);
    //mBluetoothSentText = (TextView) findViewById(R.id.bluetooth_sent_text);
    
    // Get local Bluetooth adapter
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    // If the adapter is null, then Bluetooth is not supported
    if (mBluetoothAdapter == null) {
        Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        finish();
        return;
    }
    
    
    }
    
    
    
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        
       	SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);

	   	String imageSizeS = prefs.getString("imageSize", "128");  // real default is set in Settings

	   	mImageWidth = Integer.valueOf(imageSizeS);
	   	mImageHeight = Integer.valueOf(imageSizeS);

	   	
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
    }
    
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
        
        connectToDefaultDevice();
    }
 
    private void connectToDefaultDevice()
    {
    	if( mBluetoothState == BluetoothChatService.STATE_CONNECTED )
    		return;
    	
	    String address = Settings.getDeviceMacAddress(this);
	    if( address != null)
	    {
	        // Get the BLuetoothDevice object
	        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
	        // Attempt to connect to the device
	        mChatService.connect(device);
	    }
	}
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            //mBluetoothSentText.setText(message); // so we can see what we are sending
        }
    }
    
    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendVector() {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (mSend != null && mSend.length > 0) {
            // Get the message bytes and tell the BluetoothChatService to write

            //mChatService.write(mSend);

            // send the bytes one at a time because we're usign a slow software serial implementaiton on Arduino
        	for( int i = 0; i < mSend.length; i ++ )
        	{
        		byte[] b = new byte[1];
        		b[0] = mSend[i];
        		mChatService.write(b);
        		
        	}
            this.mBluetoothText.setText("Sent " + mSend.length +" bytes"); // so we can see what we are sending
        }
    }
    
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                
                mBluetoothState = msg.arg1;
                
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    mBluetoothText.setText(R.string.title_connected_to);
                    mBluetoothText.append(mConnectedDeviceName);
                   
                    //mBluetoothSentText.setText("");
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                	mBluetoothText.setText(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                	mBluetoothText.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                //todo
                  break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    Uri uriToCrop()
    {
    	File file = new File(Environment.getExternalStorageDirectory(), "sketchytocrop.jpg");
    	Uri outputFileUri = Uri.fromFile(file);
    	return outputFileUri;
    }
    void launchCamera()
    {
    	Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		
    	// see http://stackoverflow.com/questions/1910608/android-action-image-capture-intent
    	
    	
    	Uri outputFileUri = uriToCrop();
    	intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
    	 
    	// always crashes when we ask to crop - need to go to using a separate cropper in the old Ringo way
    	
		//intent.putExtra("crop", "true");
		
		intent.putExtra("outputX", mImageWidth * 2);
		intent.putExtra("outputY", mImageHeight * 2);
		intent.putExtra("aspectX", mImageWidth);
		intent.putExtra("aspectY", mImageHeight);
		intent.putExtra("scale", true);
        

		intent.putExtra("noFaceDetection", false);
		
		intent.putExtra("setWallpaper",    false);
		
		intent.putExtra("return-data", true);
		
		startActivityForResult( intent, RESULT_CAMERA_IMAGE);
    }
    
    private void performCrop(Uri picUri)
    {
    	//take care of exceptions
    	try {
    		//call the standard crop action intent (the user device may not support it)
	    	Intent cropIntent = new Intent("com.android.camera.action.CROP"); 
	    	//indicate image type and Uri
	    	cropIntent.setDataAndType(picUri, "image/*");
	    	//set crop properties
	    	cropIntent.putExtra("crop", "true");
	    	//indicate aspect of desired crop
	    	cropIntent.putExtra("outputX", mImageWidth);
	    	cropIntent.putExtra("outputY", mImageHeight);
	    	cropIntent.putExtra("aspectX", mImageWidth);
	    	cropIntent.putExtra("aspectY", mImageHeight);
			
	    	//retrieve data on return
	    	cropIntent.putExtra("return-data", true);
	    	//start the activity - we handle returning in onActivityResult
	        startActivityForResult(cropIntent, RESULT_CROP);  
    	}
    	//respond to users whose devices do not support the crop action
    	catch(ActivityNotFoundException anfe){
    		//display an error message
    		String errorMessage = "Whoops - your device doesn't support the crop action!";
    		Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
    		toast.show();
    	}
    }
    void launchImagePicker()
    {
    	Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		intent.putExtra("crop", "true");
		
		intent.putExtra("outputX", mImageWidth);
		intent.putExtra("outputY", mImageHeight);
		intent.putExtra("aspectX", mImageWidth);
		intent.putExtra("aspectY", mImageHeight);
		intent.putExtra("scale", true);
        

		intent.putExtra("noFaceDetection", false);
		
		intent.putExtra("setWallpaper",    false);
		
		intent.putExtra("return-data", true);
		
		startActivityForResult( intent, RESULT_PICK_AND_CROP_IMAGE);
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        
        
        case RESULT_PICK_AND_CROP_IMAGE:
        	// new recipe
        	if (resultCode == Activity.RESULT_CANCELED)
			{
			}
			else
			{

				if(  data != null && data.getExtras() != null)
				{
					final Bundle extras = data.getExtras();

					Bitmap bitmap = extras.getParcelable("data");
					
					if( bitmap == null )
					{
						//EPLog.i( TAG, "No bitmap - can't save");				
					}
					else
					{
						mOutputImageView.setVisibility(View.GONE);
						
						//EPLog.i( TAG, "Storing pick & cropped photo from bitmap");
						mInputBitmap = bitmap;
						mPhotoImageView.setImageBitmap(mInputBitmap);

					}
					
				}
					

			}
        	break;
        	
        case RESULT_CAMERA_IMAGE:
        	// new recipe
        	if (resultCode == Activity.RESULT_CANCELED)
			{
			}
			else
			{
				

    			performCrop( uriToCrop() ); //data.getData());

    			/*
				//http://developer.android.com/training/camera/photobasics.html

				if(  data != null && data.getExtras() != null)
				{
					final Bundle extras = data.getExtras();

					Bitmap bitmap = extras.getParcelable("data");
					
					if( bitmap == null )
					{
						//EPLog.i( TAG, "No bitmap - can't save");				
					}
					else
					{
						mOutputImageView.setVisibility(View.GONE);
						
						//EPLog.i( TAG, "Storing pick & cropped photo from bitmap");
						mInputBitmap = bitmap;
						mPhotoImageView.setImageBitmap(mInputBitmap);

					}
					
				}
				*/	

			}
        	break;
        
        case RESULT_CROP:
        	{
        		final Bundle extras = data.getExtras();

        		if( extras == null )
        		{
        			//seeing this on Nexus 6    
        			//EPLog.i( TAG, "No extras for bitmap - can't save");
        		}
        		else
        		{
					Bitmap bitmap = extras.getParcelable("data");
					
					if( bitmap == null )
					{
						//EPLog.i( TAG, "No bitmap - can't save");				
					}
					else
					{
						mOutputImageView.setVisibility(View.GONE);
						
						//EPLog.i( TAG, "Storing pick & cropped photo from bitmap");
						mInputBitmap = bitmap;
						mPhotoImageView.setImageBitmap(mInputBitmap);
	
					}
        		}
    		}
    		break;
    		
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                
                Settings.setDeviceMacAddress( this, address );
                
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mChatService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        case R.id.settings:
        	Intent i = new Intent(this, Settings.class);
     		startActivity(i);
     		return true;
        
        case R.id.about:
        	Intent a = new Intent(this, About.class);
     		startActivity(a);
     		return true;
        }
        return false;
    }
    
    View.OnClickListener mTakePictureListener = new View.OnClickListener()
    {
        public void onClick(View v)
        {
        	
        	launchCamera();
        	

        }
    };
    
    View.OnClickListener mAlbumListener = new View.OnClickListener()
    {
        public void onClick(View v)
        {
        	
        	launchImagePicker();
        	

        }
    };
    
   
    
    View.OnClickListener mProcessPictureListener = new View.OnClickListener()
    {
        public void onClick(View v)
        {
        	startWork( MODE_PICTURE );
        	
        	
        }
    };
    
    
    View.OnClickListener mSnowflakeListener = new View.OnClickListener()
    {
        public void onClick(View v)
        {
           	startWork( MODE_SNOWFLAKE );

        	
        }
    };
    
    private void startWork( int mode )
    {
    	mMode = mode;
    	
    	connectToDefaultDevice();
    	
    	doProcess();

    }
    void updateProgress(String status)
	{
    	if( status != null )
    		mStatus = status;
    	
		// Update the progress bar
	    mHandler.post(new Runnable() {
	      public void run() {
	    	  
	    	  /*
	    	  if( mWorking )	    		
	    		  mProcessButton.setText("Stop");
	    	  else
	    		  if( mStopping )
	    			  mProcessButton.setText("Stopping");
	    		  else
	    			  mProcessButton.setText("Sketch");
	    	  */
	    	  if( mOutputBitmap != null )
	    		  mOutputImageView.setImageBitmap(mOutputBitmap);
	    	  
	    	  mStatusText.setText( mStatus );
	      }
	    });
	}
    
    private void doProcess()
    {
    	if( mStopping )
    		return;
    	
    	if( mWorking )
    	{
    		mWorking = false;
    		mStopping = true;
    		updateProgress("Stopping");
    	}
    	else
    	{
    		mBluetoothText.setText("");
    		
    	    if( mMode == MODE_PICTURE && mInputBitmap == null )
    	      	return;
    	 
	    	mWorking = true;
	    	mOutputImageView.setVisibility(View.VISIBLE);
	    	   
	    	Thread r = new Thread() {
		      public void run() {  processWorker(); 
		      						mWorking = false; 
		      						mStopping = false;
		      						updateProgress("Done: " + mStatus);}
		      };
		      
		     r.start();
	    	}
	}
    
    private void processWorker()
    {
    	switch( mMode)
    	{
    	case MODE_PICTURE:
    		processWorkerPicture();
    		break;
    		
    	case MODE_SNOWFLAKE:
    		processWorkerSnowflake();
    		break;
    		
    	}
    }
    
    private void processWorkerSnowflake()
    {
    	mSend = null;
    	
        updateProgress("start");
        
        
    	mWidth = mImageWidth;
    	mHeight = mImageHeight;

    	mVectorBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		Canvas vectorCanvas = new Canvas(mVectorBitmap);

		
		
		RectF drawRect = new RectF();
	
		drawRect.set(0,0, mWidth, mHeight);
	
		

		
		
		int translucent = Color.argb(128, 255,255,255);

		vectorCanvas.drawColor(translucent);
	
	    
		if( ! mWorking )
			return;

		
	    mOutputBitmap = mVectorBitmap;
	    //doVectorWalker( mEdgeBitmap, vectorCanvas );
	    
	    SnowflakeWalker v = new SnowflakeWalker( this, mVectorBitmap, vectorCanvas );
    	v.generate();
    	mSend = v.mSend;
    	
    	 try {
 			Thread.sleep(2000);
 		} catch (InterruptedException e) {
 		}
 	    
 		if( ! mWorking )
 			return;
 		
	    mHandler.post(new Runnable() {
		      public void run() {
		    	  
		    	  //sendMessage("abcdef");
		    	  sendVector();
		      }
		    });
	    
	    
	  
        
        
    }
    
    private void processWorkerPicture()
    {
    	mSend = null;
    	
        updateProgress("start");
        
        
    	mWidth = mInputBitmap.getWidth();
    	mHeight = mInputBitmap.getHeight();
    	//mStep = mWidth / 20;
    	
    	mEdgeBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		Canvas edgeCanvas = new Canvas(mEdgeBitmap);

    	mVectorBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		Canvas vectorCanvas = new Canvas(mVectorBitmap);

		
		
		RectF drawRect = new RectF();
	
		drawRect.set(0,0, mWidth, mHeight);
	
		

		
		
		int translucent = Color.argb(128, 255,255,255);
		edgeCanvas.drawColor(translucent);
		vectorCanvas.drawColor(translucent);
		
		mOutputBitmap = mEdgeBitmap;
	    doCanny( edgeCanvas );
	    
		if( ! mWorking )
			return;

	    try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}
	    
		if( ! mWorking )
			return;

		
	    mOutputBitmap = mVectorBitmap;
	    doVectorWalker( mEdgeBitmap, vectorCanvas );
	    
	    mHandler.post(new Runnable() {
		      public void run() {
		    	  
		    	  //sendMessage("abcdef");
		    	  sendVector();
		      }
		    });
	    
	    
	  
        
        
    }
    
    private void doCanny( Canvas canvas )
    {
	    CannyEdgeDetector detector = new CannyEdgeDetector(this);
	    //detector.setLowThreshold(0.8f);
	    //detector.setHighThreshold(1.0f);
	    detector.setSourceImage(mInputBitmap);
	    detector.setEdgesImage(mEdgeBitmap);
	    detector.process();
	    detector.getEdgesImage();
    }
    
    
    private void doVectorWalker( Bitmap input, Canvas canvas )
    {
    	VectorWalker v = new VectorWalker( this, input, mVectorBitmap, canvas );
    	v.raster2vector();
    	mSend = v.mSend;
    	
    }
    
  
    /*
    private void doVectoriser( Bitmap input, Canvas canvas )
    {
    	Vectoriser v = new Vectoriser( this, input, mVectorBitmap, canvas );
    	v.raster2vector();
    	
    }
    */
 
}