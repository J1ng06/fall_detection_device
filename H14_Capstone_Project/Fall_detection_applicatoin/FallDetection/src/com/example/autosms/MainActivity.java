package com.example.autosms;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


import java.util.Timer;
import java.util.TimerTask;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
//import android.os.ParcelUuid;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomButton;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;



public class MainActivity extends ActionBarActivity implements SensorListener{
		//Declare global variables and UI components
	     private SensorManager sensorManager;
		 Button buttonReadContact;
		 Button buttonSendSMS;
		 Button SavePreference;
		 Button Cancel;
		 ZoomButton buttonScanScreen;
		 TextView textPhone;
		 EditText textMessage;
		 TextView textShake;
		 TextView BTstate;
		 TextView Console;
		 TextView Fstate;
		 TextView Counter;
		 CountDownTimer cdt;
		 double max = 0;
		 //Declare a FIFO queue
		 Queue<String> qe=new LinkedList<String>();
		 static Queue<Float> queue=new LinkedList<Float>();

		 String prevString = "";
		 boolean nextTimeReadFromPrevString = false;
		 //Sensor stuff
		 private float x, y, z;
		 int sec = 8;
		 private static float vv;
		 private static float zz;
		 private static float dd;
		 private static float aa;
		 
		 //Setting shared preferences to store phone number and help message
		 public static final String MyPREFERENCES = "MyPrefs" ;
		 public static final String PhoneNumber = "PhoneN"; 
		 public static final String TextMessage = "TestM";
		 SharedPreferences sharedpreferences;
		 
		 public boolean FallConfirmation, phone, external;
		 Runnable Timer;
		 SendSMS worker = new SendSMS();
		 boolean fallflag = false;
		 //threads for bluetooth
		 private ConnectThread mConnectThread;
		 private ConnectedThread mConnectedThread;
		 
		 //Bluetooth stuff
		 private BluetoothAdapter btAdapter = null;
		 private BluetoothSocket btSocket = null;
		 private OutputStream outStream = null;
		 private InputStream inStream = null;
		  
		 // Intent request codes
		 private static final int REQUEST_CONNECT_DEVICE_SECURE = 2;
		 private static final int REQUEST_ENABLE_BT = 3;
		  
		 public static final String DEVICE_NAME = "device_name";		 
		    public static final int MESSAGE_STATE_CHANGE = 1;
		    public static final int MESSAGE_READ = 2;
		    public static final int MESSAGE_WRITE = 3;
		    public static final int MESSAGE_DEVICE_NAME = 4;
		    public static final int MESSAGE_TOAST = 5;

		    public static final int STATE_NONE = 0;       // we're doing nothing
		    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
		    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
		    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
		    public static final int ALARM =4;
		    private int mState;
		    public int CounterX = 0;
		 
		  // Well known SPP UUID
		 private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
		    //private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
		  // Insert your bluetooth devices MAC address
		 private static String address = "e0:63:e5:d9:14:a6";
		 
		 private static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 1; // in Meters	 
		 private static final long MINIMUM_TIME_BETWEEN_UPDATES = 1000; // in Milliseconds
		 protected LocationManager locationManager;
		 private String LocationCood;
		 

		 final int RQS_PICKCONTACT = 1;
		 
		 
		 
		 @Override
		 protected void onCreate(Bundle savedInstanceState) {
		//UI components and sensors will be iniatited in Oncreare
		  super.onCreate(savedInstanceState);
		  setContentView(R.layout.main);
		  
		  btAdapter = BluetoothAdapter.getDefaultAdapter();
		  mState = STATE_NONE;
		  BTstate = (TextView)findViewById(R.id.textView4);
		  Fstate =  (TextView)findViewById(R.id.textView3);
		  Counter =  (TextView)findViewById(R.id.textView5);
		  
		   //check if Bluetooth is available when start the program
		  checkBTState();
		  
		  Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
		  
		  Cancel = (Button)findViewById(R.id.button1);
		  Cancel.setEnabled(false);
		  buttonReadContact = (Button)findViewById(R.id.picker);
		  textPhone = (TextView)findViewById(R.id.editPhoneNum);
		  
		  textMessage = (EditText)findViewById(R.id.editSMS);
		  Console = (TextView)findViewById(R.id.console);
		  
		  sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
		  
		  //load saved phone number and distress message
		  if (sharedpreferences.contains(PhoneNumber))
		  {
			  textPhone.setText(sharedpreferences.getString(PhoneNumber, ""));
			  textMessage.setText(sharedpreferences.getString(TextMessage, ""));
		  }
		  
		  //register accelerometer and GPS service
		  sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		  boolean accelSupported = sensorManager.registerListener(this,
					SensorManager.SENSOR_ACCELEROMETER,
					SensorManager.SENSOR_DELAY_GAME);
		  
		  locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		  locationManager.requestLocationUpdates(
				
				                  LocationManager.GPS_PROVIDER,
				  
				                  MINIMUM_TIME_BETWEEN_UPDATES,
				  
				                  MINIMUM_DISTANCE_CHANGE_FOR_UPDATES,
				  
				                  new MyLocationListener()
				  
				          );

		  
		  if (!accelSupported) {
			    // on accelerometer on this device
			    sensorManager.unregisterListener(this,
		                SensorManager.SENSOR_ACCELEROMETER);
			}

		  
		  buttonReadContact.setOnClickListener(new OnClickListener(){
		   @Override
		   public void onClick(View arg0) {
		    //Start activity to get contact
		    final Uri uriContact = ContactsContract.Contacts.CONTENT_URI;
		    Intent intentPickContact = new Intent(Intent.ACTION_PICK, uriContact);
		    startActivityForResult(intentPickContact, RQS_PICKCONTACT);
		   }});
		



		 }
		 //End of OnCreare
		 


		 //Read intent data when finishing request or return from other activity
		 @Override
		 protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		  // If result is ok from contacts request, extract phone number
			 
		  if(resultCode == RESULT_OK){
		   if(requestCode == RQS_PICKCONTACT){
		    Uri returnUri = data.getData();
		    Cursor cursor = getContentResolver().query(returnUri, null, null, null, null);
		    
		    if(cursor.moveToNext()){
		     int columnIndex_ID = cursor.getColumnIndex(ContactsContract.Contacts._ID);
		     String contactID = cursor.getString(columnIndex_ID);
		     
		     int columnIndex_HASPHONENUMBER = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
		     String stringHasPhoneNumber = cursor.getString(columnIndex_HASPHONENUMBER);
		     
		     if(stringHasPhoneNumber.equalsIgnoreCase("1")){
		      Cursor cursorNum = getContentResolver().query(
		        ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
		        null, 
		        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactID, 
		        null, 
		        null);
		      
		      //Get the first phone number
		      if(cursorNum.moveToNext()){
		       int columnIndex_number = cursorNum.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
		       String stringNumber = cursorNum.getString(columnIndex_number);
		       textPhone.setText(stringNumber);
		      }
		      
		     }else{
		      textPhone.setText("NO Phone Number");
		     }
		     
		     
		    }else{
		     Toast.makeText(getApplicationContext(), "NO data!", Toast.LENGTH_LONG).show();
		    }
		    
		    //when returned from bluetooth connect activity
		   }else if (requestCode == REQUEST_CONNECT_DEVICE_SECURE){
			   //get MAC address from target bluetooth device
			   address = data.getExtras().getString(BTscan.EXTRA_DEVICE_ADDRESS);
			   BluetoothDevice device = btAdapter.getRemoteDevice(address);
			   //attempt to connect to target bluetooth device
			   connect(device);
		   }
		  }

		 }
		 
		 //to send a text message by providing phone number and text body
		 private void sendSMS()
		    {        
				 	String phoneNumber = textPhone.getText().toString();
				 	String message = textMessage.getText().toString(); 
				 	if (phoneNumber.length()>0 && message.length()>0){ 
			        String SENT = "SMS_SENT";
			        String DELIVERED = "SMS_DELIVERED";

			        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
			            new Intent(SENT), 0);
			 
			        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
			            new Intent(DELIVERED), 0);
			 
			        //when the SMS has been sent, will return different results
			        registerReceiver(new BroadcastReceiver(){
			            @Override
			            public void onReceive(Context arg0, Intent arg1) {
			                switch (getResultCode())
			                {
			                    case Activity.RESULT_OK:
			                        Toast.makeText(getBaseContext(), "SMS sent", 
			                                Toast.LENGTH_SHORT).show();
			                        break;
			                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
			                        Toast.makeText(getBaseContext(), "Generic failure", 
			                                Toast.LENGTH_SHORT).show();
			                        break;
			                    case SmsManager.RESULT_ERROR_NO_SERVICE:
			                        Toast.makeText(getBaseContext(), "No service", 
			                                Toast.LENGTH_SHORT).show();
			                        break;
			                    case SmsManager.RESULT_ERROR_NULL_PDU:
			                        Toast.makeText(getBaseContext(), "Null PDU", 
			                                Toast.LENGTH_SHORT).show();
			                        break;
			                    case SmsManager.RESULT_ERROR_RADIO_OFF:
			                        Toast.makeText(getBaseContext(), "Radio off", 
			                                Toast.LENGTH_SHORT).show();
			                        break;
			                }
			            }
	
			        }, new IntentFilter(SENT));
		        
			 	
		 
			        //when the SMS has been delivered, will notify user by toasting 
			        registerReceiver(new BroadcastReceiver(){
			            @Override
			            public void onReceive(Context arg0, Intent arg1) {
			                switch (getResultCode())
			                {
			                    case Activity.RESULT_OK:
			                        Toast.makeText(getBaseContext(), "SMS delivered", 
			                                Toast.LENGTH_SHORT).show();
			                        break;
			                    case Activity.RESULT_CANCELED:
			                        Toast.makeText(getBaseContext(), "SMS not delivered", 
			                                Toast.LENGTH_SHORT).show();
			                        break;                        
			                }
			            }
			        }, new IntentFilter(DELIVERED));        
			 
			        SmsManager sms = SmsManager.getDefault();
			        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);   
		        
				 	}
		    }

		 
		 //Phone sensor monitoring
		 public void onSensorChanged(int sensor, float[] values) {

			 
			//for  accelerometer readings
			if (sensor == SensorManager.SENSOR_ACCELEROMETER) {

				//divide raw data by 9.8 to get G
				x = (float) (values[SensorManager.DATA_X]/9.8);
				y = (float) (values[SensorManager.DATA_Y]/9.8);
				z = (float) (values[SensorManager.DATA_Z]/9.8);
				//Calculate the magnitude of acceleration
				float vector = (float) Math.sqrt(x*x + y*y + z*z);
				//save the magnitude to a global method
				setSensorV(vector);


			    }
			}
			
		

		 @Override
		 public void onAccuracyChanged(int sensor, int accuracy) {
			// TODO Auto-generated method stub
		 }
		 
		 
		 
		 protected void showCurrentLocation() {
			 		//was GPS_PROVIDER
			         Location location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);			 			  
			 
			         //append the location as a Google map link to the message
			         if (location != null) {			 
		
			             String message = String.format(			 
			                     "\n Current Location \n http://maps.google.com/maps?z=12&t=m&q=loc:" + 			 
			                      location.getLatitude()+ "+" + location.getLongitude()		 
			             );	
			             LocationCood = message;

			         }

			     }  

		 
		 //Monitoring GPS service status
		 private class MyLocationListener implements LocationListener {
			 
			  
			 @Override
			         public void onLocationChanged(Location location) {
			 
			 
			         }
			 
			  
			 @Override
			         public void onStatusChanged(String s, int i, Bundle b) {
			 
			             Toast.makeText(MainActivity.this, "Provider status changed",
			 
			                     Toast.LENGTH_LONG).show();
			 
			         }
			 
			  
			 @Override
			         public void onProviderDisabled(String s) {
			 
			             Toast.makeText(MainActivity.this,
			 
			                     "Provider disabled by the user. GPS turned off",
			 
			                     Toast.LENGTH_LONG).show();
			 
			         }
			 
			  
			 @Override
			         public void onProviderEnabled(String s) {
			 
			             Toast.makeText(MainActivity.this,
			 
			                     "Provider enabled by the user. GPS turned on",
			 
			                     Toast.LENGTH_LONG).show();
			 
			         }
			 
			  
			 
			     }
		 
		 //Bluetooth methods below
		 private void errorExit(String title, String message){
			    Toast msg = Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_SHORT);
			    msg.show();
			    finish();
			  }
		 
		 private synchronized void setState(int state) {
		        

		        // Give the new state to the Handler so the UI Activity can update
		        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
		    }
		 
		 private void checkBTState() {
			    // Check for Bluetooth support and then check to make sure it is turned on
			 	setState(STATE_NONE);
			    // Emulator doesn't support Bluetooth and will return null
			    if(btAdapter==null) { 
			      errorExit("Fatal Error", "Bluetooth Not supported. Aborting.");
			    } else {
			      if (btAdapter.isEnabled()) {
			        Log.d("", "...Bluetooth is enabled...");
			      } else {
			        //Prompt user to turn on Bluetooth
			        Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
			        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			      }
			    }
			  }
	 
		  
		 		  
			  
			    public synchronized void connect(BluetoothDevice device) {

			        // Cancel any thread currently running a connection
			        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

			        // Start the thread to connect with the given device
			        mConnectThread = new ConnectThread(device);
			        mConnectThread.start();
			        setState(STATE_CONNECTING);
			    }
			    
			    
			    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
			        // Cancel the thread that completed the connection
			        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

			        // Cancel any thread currently running a connection
			        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

			        // Start the thread to manage the connection and perform transmissions
			        mConnectedThread = new ConnectedThread(socket);
			        mConnectedThread.start();
			        //Change BT state to Connected
			        setState(STATE_CONNECTED);
			    }
			    
			    
			    //Start a thread to establish a BT socket connection 
			    private class ConnectThread extends Thread {
			        private final BluetoothSocket mmSocket;
			        private final BluetoothDevice mmDevice;
			        public ConnectThread(BluetoothDevice device) {
			            mmDevice = device;
			            BluetoothSocket tmp = null;
			            Method m;
			            try {
			            	m = device.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
			            	tmp = (BluetoothSocket) m.invoke(device, 1);
			            }
			            catch (SecurityException e1) {
			                e1.printStackTrace();
			            } catch (NoSuchMethodException e1) {
			                e1.printStackTrace();
			            } catch (IllegalArgumentException e) {
			                e.printStackTrace();
			            } catch (IllegalAccessException e) {
			                e.printStackTrace();
			            } catch (InvocationTargetException e) {
			                e.printStackTrace();
			            }
			            mmSocket = tmp;
			        }
			        
			        
			        public void run() {
			            Log.i("", "BEGIN mConnectThread");
			            setName("ConnectThread");

			            // Always cancel discovery because it will slow down a connection
			            btAdapter.cancelDiscovery();

			            // Make a connection to the BluetoothSocket
			            try {
			                // This is a blocking call and will only return on a
			                // successful connection or an exception
			                mmSocket.connect();
			                Log.i("", "Socket connected");
			            } catch (IOException e) {
			                //connectionFailed();
			                // Close the socket
			                try {
			                	Log.i("", "Socket conneection failed and closed");
			                    mmSocket.close();
			                } catch (IOException e2) {
			                    Log.e("", "unable to close() socket during connection failure", e2);
			                }
			                return;
			            }

			            //reset the thread when done
			                mConnectThread = null;


			            // Start the connected thread to maintain data streaming
			            connected(mmSocket, mmDevice);
			        }
			        
			        public void cancel() {
			            try {
			                mmSocket.close();
			            } catch (IOException e) {
			                Log.e("", "close() of connect socket failed", e);
			            }
			        }
			    }
			    
			    
			    //Thread to keep BT socket running all the time in the background
			    private class ConnectedThread extends Thread {
			        private final BluetoothSocket mmSocket;
			        private final InputStream mmInStream;
			        private final OutputStream mmOutStream;
			        

			        public ConnectedThread(BluetoothSocket socket) {
			            Log.d("", "createed ConnectEDThread");
			            mmSocket = socket;
			            InputStream tmpIn = null;
			            OutputStream tmpOut = null;

			            // Get the BluetoothSocket input and output streams
			            try {
			                tmpIn = socket.getInputStream();
			                tmpOut = socket.getOutputStream();
			            } catch (IOException e) {
			                Log.e("", "temp sockets not created", e);
			            }

			            mmInStream = tmpIn;
			            mmOutStream = tmpOut;
			        }
			        public void run() {
			            Log.i("", "BEGIN mConnectedThread");
			            final int BUFFER_SIZE = 1024;
			            byte[] buffer = new byte[BUFFER_SIZE];
			            int bytes = 0;
			            write("8".getBytes());

			            // Keep listening to the InputStream while connected
			            while (true) {
			                try {

			                    // Read from the InputStream
			                	bytes = mmInStream.read(buffer);

			                    // Send the obtained bytes to the Handler
			                	if (buffer != null){
			                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
			                            .sendToTarget();
			                	}
			                } catch (IOException e) {
			                    Log.e("", "disconnected", e);
			                    e.printStackTrace();
			                    connectionLost();
			                    break;
			                } 
			            	
			            }
			        }
			        
			        private void connectionLost() {
			            setState(STATE_LISTEN);

			            // Send a failure message back to the Activity
			            Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
			            Bundle bundle = new Bundle();
			            bundle.putString("toast", "Device connection was lost");
			            msg.setData(bundle);
			            mHandler.sendMessage(msg);
			        }

			        /**
			         * Write to the connected OutStream.
			         * @param buffer  The bytes to write
			         */
			        //Send off characters through BT socket
			        public void write(byte[] buffer) {
			            try {
			                mmOutStream.write(buffer);
			            } catch (IOException e) {
			                Log.e("", "Exception during write", e);
			            }
			        }

			        public void cancel() {
			            try {
			                mmSocket.close();
			            } catch (IOException e) {
			                Log.e("", "close() of connect socket failed", e);
			            }
			        }
			    }
			    
			    
			    //handler to do data processing and fall dectection
			    @SuppressLint("UseValueOf")
				private final Handler mHandler = new Handler() {
			        @SuppressLint("NewApi")
					@Override
			        public void handleMessage(Message msg) {
			            switch (msg.what) {
			            //Set status for BT or fall detection
			            case MESSAGE_STATE_CHANGE:
			                switch (msg.arg1) {
			                case STATE_CONNECTED:
			                	BTstate.setTypeface(null, Typeface.BOLD);
			                	BTstate.setTextColor(Color.GREEN);
			                    BTstate.setText("Connected");
			                    Fstate.setTypeface(null, Typeface.BOLD);
			                	Fstate.setTextColor(Color.GREEN);
			                	Fstate.setText("Working");
			                    break;
			                case STATE_CONNECTING:
			                	BTstate.setTypeface(null, Typeface.BOLD);
			                	BTstate.setTextColor(Color.YELLOW);
			                	BTstate.setText("Connecting");
			                    break;
			                case STATE_LISTEN:
			                case STATE_NONE:
			                	BTstate.setTypeface(null, Typeface.BOLD);
			                	BTstate.setTextColor(Color.RED);
			                	BTstate.setText("Not Connected");
			                	Fstate.setTypeface(null, Typeface.BOLD);
			                	Fstate.setTextColor(Color.RED);
			                	Fstate.setText("Not Started");
			                
			                    break;
			                }
			                break;
			            //the case when received in streamed data from BT
			            case MESSAGE_READ:
			                byte[] readBuf = (byte[]) msg.obj;
			                
			                // construct a string from the valid bytes in the buffer
			                String readMessage = new String(readBuf, 0, msg.arg1);
			                //Split received string by "/"
			                String[] temp = readMessage.split("/", -1);
			                String patternx = "(\\D)";
			                

			                //Make sure each part on the split array is a complete set of data
			                
			                	
				                temp[0] = prevString + temp[0];
				                //if the received message was perfectly split, put everything to the queue
				                if (readMessage.endsWith("/") && readMessage.contains("/")){
				                	for (int i = 0 ; i < temp.length ; i ++){

				                		qe.add(temp[i]);
				                	}
				                	prevString = "";
				                }
				              //if the received message is not end with "/", save the last part to a temp, append the temp with its missing part on next message
				                else if (!readMessage.endsWith("/") && readMessage.contains("/")){
				                	for (int i = 0 ; i < temp.length - 1 ; i ++){
				                		qe.add(temp[i]);

				                		
				                	}
				                	prevString="";
				                	prevString = temp[temp.length - 1];
				                }
				                //if received message has no "/", put to temp 
				                else {
				                	prevString = prevString + readMessage;
				                }
			             
				                //while queue is not empty
				                if (qe.isEmpty()==false){
			                	
				                	//get the first set of data from the queue and split it with ","
				    				String[] temp1 = qe.remove().split(",") ;
				    				boolean isEmpty = isAnyNull(temp1);
				    				boolean isShort = true;
				    				boolean checked = false;
				    				String pp = "";

	    							
	    						//check if the first element which is checksum value of the set if empty or null
			    				if (temp1[0]!=null && !temp1[0].isEmpty()){  
			    					pp = temp1[0].replaceAll(patternx,"");
			    				}
			    				
			    				//check if the length of the array is 6
			    				if (temp1.length == 6){
			    					isShort = false; 
			    				}	
			    				
			    				//if this set data is good and checksum value is not null
			    				if (isEmpty == false && isShort == false && pp.isEmpty() == false && !pp.contains(".") && !pp.equals("0")){
			    					try{
			    						//calculate the checksum of received data
					    				double sum_raw = Double.parseDouble(temp1[1])+Double.parseDouble(temp1[2])+Double.parseDouble(temp1[3])+Double.parseDouble(temp1[4])+Double.parseDouble(temp1[5]) ;
					    				//round the value to integer
					    				int sum = Math.abs((int) sum_raw);
					    				
					    				int sumR = Integer.parseInt(pp);
					    				
					    				//consider the data is good if the difference between actual checksum and received checksum is within 5%
					    				if(((sum - sumR)/sumR < 0.05) && sumR !=0){
					    					checked = true;
					    					
					    				}
					    				
			    					}
			    					catch(NumberFormatException exe){
			    					}
			    					catch(ArithmeticException eee){
			    						
			    					}
			    				}
			    		
			    				if (checked == true){

			    					try {
			    						//if the fall confirming thread is running, stop the thread and cancel the fall because the headset resumed data streaming
			    						if (worker !=null){
			    							if (worker.isAlive()&& fallflag==true){
				    							stopThread();
				    							Toast.makeText(getApplicationContext(), "Fall Cancelled!", Toast.LENGTH_SHORT).show();
				    							fallflag = false;
			    							}
			    						}
			    						
			    						//convert received sensor date
				    					float a = Math.abs(Float.parseFloat(temp1[1]));
				    					
				    					float b = Math.abs(Float.parseFloat(temp1[2]));
				    					
				    					float x = Float.parseFloat(temp1[3])/8192;
				    					
				    					float y = Float.parseFloat(temp1[4])/8192;
				    					
				    					float z = Float.parseFloat(temp1[5])/8192;
				    					
				    					//calculate acceleration magnitude of the headset
				    					float v = (float) Math.sqrt(x*x + y*y + z*z);
				    					//save angles and magnitude to global methods
				    					setSensorZ(v);
				    					setSensorD(a);
				    					setSensorDD(b);				    					
				    					
				    					
				    					if (FallConfirmation == false){
				    						//if thresholds are met for both ends
				    						if (v > 2.4 && (a > 45 || b > 45) && (Benddown() == false)){
				    							//notify headset that a fall is detected
				    								for (int k = 0; k<5; k++){
				    									mConnectedThread.write("f".getBytes());	
				    								}
				    								fallflag = true;
				    								//start the fall confirming thread
				    								startThread();				    						
					    							Thread.sleep(300);
					    							//clear current queue
					    							qe.clear();
					    							temp1 = new String[temp1.length];				    							
					    							v = 0;
				    							
					    					}
				    						//if only the threshold of the phone is met
				    						else if (Benddown()== false){
				    							phone = true;
				    				    		fallcheck go = new fallcheck();
				    				    		//start a countdown thread
				    				    		go.start();
				    						}
				    						//if only the thresholds of headset are met
				    						else if (v > 2.4 && (a > 45 || b > 45)){
				    							external = true;
				    				    		fallcheck go = new fallcheck();
				    				    		//start a countdown thread
				    				    		go.start();
				    						}
		
				    					}
				    					
				    					//while the countdown thread is running
				    					if (FallConfirmation == true){
				    						//if the thresholds of the headset are also met within the window
				    						if (v > 2.4 && (a > 45 || b > 45) && (phone == true)){
				    							//notify headset a fall is detected
			    								for (int k = 0; k<5; k++){
		    									mConnectedThread.write("f".getBytes());	
			    								}
		    								fallflag = true;
		    								//start the fall confirming thread
		    								startThread();
			    							qe.clear();
			    							temp1 = new String[temp1.length];
			    							v = 0;
			    							Thread.sleep(300);

				    						}
			    						//if the threshold of the phone is also met within the window
			    						else if (Benddown() == false && external == true){
		    								for (int k = 0; k<5; k++){
		    									mConnectedThread.write("f".getBytes());	

		    								}
		    								fallflag = true;
		    								//start the fall confirming thread
		    								startThread();
			    						    qe.clear();
			    						    temp1 = new String[temp1.length];
			    							v = 0;
			    							Thread.sleep(300);
			    						}
			    					}
				    					
			    					}catch (NumberFormatException e){
			    					}
			    					catch (ArrayIndexOutOfBoundsException ww){
			    					}			    					
			    					catch (InterruptedException e) {
										e.printStackTrace();
									}

			    					
		    				}
			    				temp1 = new String[temp1.length];
			    				
			    			}

			                break;
			            case MESSAGE_TOAST:
			                Toast.makeText(getApplicationContext(), msg.getData().getString("toast"),
			                               Toast.LENGTH_SHORT).show();
			                break;
			            }
			        }
			    };
			    
			    //check if a array contains any null element
			    public boolean isAnyNull (String[] list){
			    	boolean flag = false;
			    	for (int i =0; i<list.length; i++){
    					if (list[i]==null){
    						flag = true;
    						break;
    					}
    					
			    	}
			    	return flag;
			    }
			    
			  
			    
	
			    //the counting down thread
			    public class fallcheck extends Thread{
			    	//within 0.6 seconds, set FallConfirmation to true
			    	public void run() {
						long starttime = System.currentTimeMillis();
						while (true){
							FallConfirmation = true;
							long currtime = System.currentTimeMillis();
							 long elapsedtime = currtime - starttime;
							 //after 0.6s, set FallConfirmation back to false
						     if(elapsedtime > 600)
						      {
						    	 FallConfirmation = false;
						    	 phone = false;
						    	 external = false;
						    	 break;
						      }
						}
						
					}
			    	
			    }
			    
			    //check if the magnitude of phone meets threshold
			    protected boolean Benddown() {
			    	
			    	boolean flag = false;
			    	float magnitude = getSensorV();
			    	if (magnitude < 1.8){
			    		flag = true;
			    	}
			    	return flag;
			    }

			    //the 6 functions below are for global sensor data saving and getting
			    //mainly for graphing
			    public static float getSensorV(){
			    	return vv;
			    }
			    
			    public void setSensorV(float value){
			    	vv = value;
			    }
			    
			    public static float getSensorZ(){
			    	String s = Float.toString(zz);
		    	if (s == null){
			    		return 0;
			    	}
			    	else{
			    		return zz;
			    	}
			    }
			    
			    public static void setSensorZ(float value){
			    	zz = value;
			    }
			    
			    public static float getSensorD(){
			    	
			    	String s = Float.toString(dd);
			    	if (s == null){
			    		return 0;
			    	}
			    	else{
			    		return dd;
			    	}
			    }
			    
			    public void setSensorD(float value){
			    	dd = value;
			    }
			    public static float getSensorDD(){
			    	
			    	String s = Float.toString(aa);
			    	if (s == null){
			    		return 0;
			    	}
			    	else{
			    		return aa;
			    	}
			    }
			    
			    public void setSensorDD(float value){
			    	aa = value;
			    }

			    //the fall confirming thread
				Handler handler = new Handler();

			    class SendSMS extends Thread{
			    	volatile boolean running = true;
			    	volatile boolean flagg;
			    	
				    public void run(){
				    	long starttime = System.currentTimeMillis();
				    	long temp = 0;
				    	sec = 5;
				    	flagg = true;
			    		runOnUiThread(new Runnable(){
							public void run(){
								Cancel.setEnabled(true);
								Fstate.setTypeface(null, Typeface.BOLD);
			                	Fstate.setTextColor(Color.YELLOW);
			                	Fstate.setText("Possible Fall!");
			                	Counter.setText("Fall will be confirmed in 5 seconds");
			                	//when cancel button is clicked from the phone within the window, cancel fall, stop thread
			                	Cancel.setOnClickListener(new OnClickListener(){
									@Override
									public void onClick(View v) {
										running = false;
										flagg = false;
										//notify headset that fall is cancelled
										for (int k = 0; k<5; k++){
	    									mConnectedThread.write("c".getBytes());	

	    								}
										
									}
					    			
					    		});
							}
						})  ; 
			    		
			    		
			    		//Looping for 5 seconds before sending message
				    	while(running){

				    		long currtime = System.currentTimeMillis();
							 long elapsedtime = currtime - starttime;
							 if (elapsedtime%1000 == 0 && elapsedtime/1000 != temp){
								 temp = elapsedtime/1000;
								 final int time = (int) (5 - temp);
								 
								 runOnUiThread(new Runnable(){
										public void run(){
											//print time count down
											Counter.setTypeface(null, Typeface.BOLD);
						                	Counter.setText("Fall will be confirmed in "+ Integer.toString(time) +" seconds");
										}
									})  ;
							 }
							 //when time reaches 5s, stop loop
						     if(elapsedtime > 5000)
						      {
						    	 running = false;
						      }
		   
	    		
				    	}
						     //if fall is not cancelled, go ahead and send message
							 if (flagg == true){
								 //change fall status to FALL
								 runOnUiThread(new Runnable(){
										public void run(){
											Fstate.setTypeface(null, Typeface.BOLD);
						                	Fstate.setTextColor(Color.RED);
						                	Fstate.setText("Fall!");
										}
									})  ; 

								 //append GPS location to distress message
					    		   MainActivity.this.showCurrentLocation();
								   if (LocationCood != null && !LocationCood.isEmpty()) {
									   runOnUiThread(new Runnable(){
											public void run(){
												 MainActivity.this.textMessage.append(LocationCood);
											}
										})  ; 
									  
								   }
								   else
								   {
									   handler.post(new Runnable(){
							                public void run() {
							                   Toast.makeText(getApplicationContext(), "Cannot obtain location", Toast.LENGTH_LONG).show();
							            }
							         });
									   
								   }

									//send message
									handler.post(new Runnable(){
						                public void run() {
						                	MainActivity.this.sendSMS(); 
						            }
						         });
								   
						    }

						
							 runOnUiThread(new Runnable(){
									public void run(){
										setState(STATE_CONNECTED);
										Counter.setText("");
										Cancel.setEnabled(false);
									}
								})  ;
				    	
							
					 
					    	
				    	}
			    
				    public synchronized void onPause(){
				    	running = false;
				    	flagg = false;
				    	
				    }
				    
				    public synchronized void Wait(){
				    	flagg = true;
				    	
				    }
			    }
			    
			    //method to safely stop thread externally
			    public synchronized void startThread(){
			    		worker = new SendSMS();
			    		worker.start();
			    }
			    
			    public synchronized void stopThread(){
			    	if (worker != null){
			    		worker.onPause();
			    		worker = null;
			    	}
			    }

				
			    //iniate the top menu bar on the main screen
			    public boolean onCreateOptionsMenu(Menu menu) {
			        // Inflate the menu items for use in the action bar
			        MenuInflater inflater = getMenuInflater();
			        inflater.inflate(R.menu.main, menu);
			        return super.onCreateOptionsMenu(menu);
			    }    
			    
			    //iniatlize each selection on the menu
			    public boolean onOptionsItemSelected(MenuItem item) {
			        // Handle presses on the action bar items
			        switch (item.getItemId()) {
			            case R.id.action_search:
			            	searchBT();
			                return true;
			            case R.id.action_graph:
			            	plot();
			                return true;
			            case R.id.action_settings:
			            	save();
			                return true;
			            default:
			                return super.onOptionsItemSelected(item);
			        }
			    }
			    
			    //switch to BT search activity when selected on the menu
			    public void searchBT(){
			    	final Context context = this;

						          Intent serverIntent = new Intent(context, BTscan.class);
						          startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);

			    }
			    //switch to Graph search activity when selected on the menu
			    public void plot(){
				 
			          Intent plotIntent = new Intent(this, Graph.class);
			          startActivity(plotIntent);
			    }
			    //save current phone number and message input when selected on the menu
			    public void save(){
			    	String n  = textPhone.getText().toString();
				     String ph  = textMessage.getText().toString();
				     Editor editor = sharedpreferences.edit();
				     editor.putString(PhoneNumber, n);
				     editor.putString(TextMessage, ph);
				     editor.commit();
			    }

}


