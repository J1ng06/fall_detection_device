package com.beanie.stepsensors;

import java.util.Calendar;

import java.util.GregorianCalendar;


import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.text.format.Time;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;



public class MainActivity extends Activity {

	private TextView textViewStepCounter;
	private TextView textViewCurrentStep;
	private TextView textViewCalorie;
	private TextView textViewDistance;
	
	//save daily logger
	Time today = new Time(Time.getCurrentTimezone());
	DBAdapter myDb;

	public int previousCount=0;
	public int dayCount;
	public int currentCount;
	public int tempSteps;
	public String tempDataString = null;
	public int tempData = 0;
	
	public int xAxisVal=1;
	
	public static double WalkingFactor =0.4827;
	//shared preference for user data input
	 public static final String MyPREFERENCES = "MyPrefs" ;
	 public static final String STEPLENGTH = "steplengthKey"; 
	 public static final String WEIGHT = "weightKey"; 
	 SharedPreferences sharedpreferences;
	TextView StepLength;
	TextView Weight;
	LineGraphSeries<DataPoint> seriestep;
	GraphView graph;
	// **********************************
	// initialized total step counts to be zero
	protected int steps = 0;
	
	//saving daily record on the list view
	
	private void populateListView(){
		
	Cursor cursor = myDb.getAllRows();
	String[] fromFieldNames = new String[]{DBAdapter.KEY_ROWID,DBAdapter.KEY_TASK,DBAdapter.KEY_CALORIE,DBAdapter.KEY_DATE};
	int[] toViewIDs = new int[]{R.id.number,R.id.tasks,R.id.calories,R.id.dates};
	SimpleCursorAdapter myCursorAdapter;
	myCursorAdapter = new SimpleCursorAdapter(getBaseContext(),R.layout.items_layout,cursor,fromFieldNames,toViewIDs,0);
	ListView myList =(ListView)findViewById(R.id.datarecordlist);
	myList.setAdapter(myCursorAdapter);
		
	}
	
	
	//initiate graph view 
	
	
	
	private void openDB(){
		
		myDb = new DBAdapter(this);
		myDb.open();
		
	}
	
	//add steps to DB
	public void onClick_AddTask(View v){
		today.setToNow();
		String timestamp = today.format("%Y-%m-%d %H:%M:%S");
		if(!TextUtils.isEmpty(textViewCurrentStep.getText().toString())){
			
			myDb.insertRow(textViewCurrentStep.getText().toString(), textViewCalorie.getText().toString(), timestamp);
		
		}
		
		populateListView();
		
		//generate the graph view with data points
		seriestep.appendData(new DataPoint(xAxisVal,Double.parseDouble(textViewCurrentStep.getText().toString())), true,300);
		xAxisVal++;
	}
	
	//delete steps record from listview
	


	public void onClick_DeleteTasks(View v){
		
		myDb.deleteAll();
		populateListView();
	}

	
	private void listViewItemLongClick(){
		ListView myList = (ListView)findViewById(R.id.datarecordlist);
		myList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				
				myDb.deleteRow(id);
				populateListView();
				return false;
			}
			
		});
	}
	
	
	//generate barchart
	
/*	public void  barGenerate(){
		//generate a bar chart
	
				//GraphView graph = (GraphView) findViewById(R.id.graph);
				LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[] {
				       //   new DataPoint(0, Integer.valueOf(textViewCurrentStep.getText().toString())),
				          new DataPoint(0, 0),        
				         new DataPoint(2, 3),
				          new DataPoint(3, 5),
				          new DataPoint(4, 6)
				          
				});
				graph.addSeries(series);
				graph.setTitle("Track Your Steps");
				graph.setTitleTextSize(32);
				
				
	

	}
	*/

	
	//method to save user preference
    public void run(View view){
        String s  = StepLength.getText().toString();
        String w  = Weight.getText().toString();
        
      
        Editor editor2 = sharedpreferences.edit();
        editor2.putString(STEPLENGTH, s);
        editor2.putString(WEIGHT, w);

        editor2.commit(); 

     } 
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		this.scheduleAlarm();

		textViewStepCounter = (TextView) findViewById(R.id.totalstep);
		
		//textViewDate = (TextView) findViewById(R.id.textView4);
		textViewCurrentStep = (TextView) findViewById(R.id.currentstep);
		
		textViewCalorie = (TextView) findViewById(R.id.Calorie);
		textViewDistance= (TextView) findViewById(R.id.Distance);
		
		//textViewStepCounter.setText((int) hours + "");
		Calendar d = Calendar.getInstance(); 
		
		//textViewDate.setText(String.valueOf(d.getTime()));

		registerForSensorEvents();

		//run open Database when oncreate
		
		openDB();
		
		//populate list
		populateListView();	
		
		//when want to delete a record
		listViewItemLongClick();
		
		//initiate a graphview with current count plot
		seriestep = new LineGraphSeries<DataPoint>(new DataPoint[] {
				 new DataPoint(0, 0),
			       //   new DataPoint(xAxisVal, Double.valueOf(textViewCurrentStep.getText().toString())),
	
			          
			});
		graph = (GraphView) findViewById(R.id.graph);
		graph.addSeries(seriestep);
		graph.getViewport().setScalable(true);
		//graph.getViewport().setXAxisBoundsManual(true);
		//graph.getViewport().setYAxisBoundsManual(true);
		graph.setTitle("Track Your Steps");
		graph.setTitleTextSize(32);
		
		

		// *********************
		
		//saving data before onBack pressed
		SharedPreferences prefs = getSharedPreferences("com.beanie.stepsensors", Context.MODE_PRIVATE); 
		this.tempDataString = prefs.getString("tempData", "0");
		this.tempData = Integer.valueOf(this.tempDataString);
		this.textViewCurrentStep.setText(tempDataString);
		
		
		//
		 //shared preference for user data input
       StepLength = (TextView) findViewById(R.id.step_length);
       Weight = (TextView) findViewById(R.id.weight_kg);


        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        if (sharedpreferences.contains(STEPLENGTH))
        {
           StepLength.setText(sharedpreferences.getString(STEPLENGTH, ""));

        }
        if (sharedpreferences.contains(WEIGHT))
        {
           Weight.setText(sharedpreferences.getString(WEIGHT, ""));

        }
	  

		 
		// constantly waiting for the alarm goes off, then run the onReceive() method
		 BroadcastReceiver test = new BroadcastReceiver() {
			    @Override
			    public void onReceive(Context context, Intent intent) {
			    	textViewCurrentStep.setText("received alarm");
			    	// reset data once a new day is reached
			    	currentCount = 0;
			    	saveData("0");
			    	tempData = 0;
			    	previousCount = steps;
			    	textViewCurrentStep.setText("0");
			    	
			    }           
			};
			registerReceiver(test, new IntentFilter("com.beanie.stepsensors"));
			
			
	}

	public void registerForSensorEvents() {
		SensorManager sManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		// Step Counter
		sManager.registerListener(new SensorEventListener() {

			@Override
			public void onSensorChanged(SensorEvent event) {
				tempSteps = steps;
				if (tempSteps == 0) {
					// first time start up
					steps = (int) event.values[0];
					currentCount = tempData;
				}
				
				else { // read values from sensor
					steps = (int) event.values[0];
					int diff = steps - tempSteps;
					tempData = tempData + diff;
					currentCount = tempData;
				}
				
//				steps = (int) event.values[0];
//				 currentCount = steps - previousCount;
//				currentCount = steps - tempData;
				 textViewStepCounter.setText((int) steps + "");
				 textViewCurrentStep.setText("" + currentCount);
				 
				 //Distance Calculation
				 int sl = Integer.parseInt((StepLength.getText().toString()));
				 textViewDistance.setText(""+(currentCount*sl/100000.0));
				 
				 //Calorie Calculation
				 Double ww = Double.parseDouble((Weight.getText().toString()));
				 Double calorieCount =  currentCount*sl*ww*WalkingFactor/100000.0;

				 textViewCalorie.setText(""+calorieCount.shortValue());
//				//if
//				Calendar c = Calendar.getInstance(); 
//				int hours = c.get(Calendar.HOUR_OF_DAY);
//				int minutes = c.get(Calendar.MINUTE);
//				int seconds = c.get(Calendar.SECOND);
//				if (hours==16 && minutes==43 && seconds==0){
//					
//					 //int currentCount = (int) event.values[0];
//					
//					 //textViewStepCounter.setText("0");
//					 textViewCurrentStep.setText("0");
//					
//					
//					 previousCount = currentCount;
//	
//				}
//			
//				else {
//					
//					textViewStepCounter.setText((int) steps + "");
//					
//					//textViewCurrentStep.setText((int) currentCount + "");
//				}
			
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {

			}
		}, sManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
				SensorManager.SENSOR_DELAY_UI);

		// Step Detector
		sManager.registerListener(new SensorEventListener() {

			@Override
			public void onSensorChanged(SensorEvent event) {
				// Time is in nanoseconds, convert to millis
			//	timestamp = event.timestamp / 1000000;
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {

			}
		}, sManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
				SensorManager.SENSOR_DELAY_UI);
	}

	// *****************************************
	 public void scheduleAlarm()
	    {
	            // time at which alarm will be scheduled here alarm is scheduled at 1 day from current time, 
	     
	            // i.e. 24*60*60*1000= 86,400,000   milliseconds in a day        
	            Long time = new GregorianCalendar().getTimeInMillis();
	            Long resetTime = 86400000-time;
	            //set the reset time to be the time left for current day
	          //  Long resetTime = time + 5000;

	            // create an Intent and set the class which will execute when Alarm triggers, here we have
	            // given AlarmReciever in the Intent, the onRecieve() method of this class will execute when
	            // alarm triggers and 
	            //we will write the code to send SMS inside onRecieve() method pf Alarmreciever class
//	            Intent intentAlarm = new Intent(this, AlarmReceiver.class);
	       
	            // create the object
	            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
	            PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent("com.beanie.stepsensors"), 0);

	            //set the alarm for particular time
                alarmManager.set(AlarmManager.RTC_WAKEUP,resetTime, pi);
//	            alarmManager.set(AlarmManager.RTC_WAKEUP,time, PendingIntent.getBroadcast(this,1,  intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
	            Toast.makeText(this, "Alarm Scheduled for 10 seconds from now", Toast.LENGTH_LONG).show();
	            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 86400000, pi); 
	            
	            //repeat the alarm every 24 hrs
	    }
	 
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	public void onBackPressed(){
		
		
		super.onBackPressed();
		String string = textViewCurrentStep.getText().toString();
		 saveData(string);
		
	}
	
	public void saveData(String string) {
		
		 Editor editor = getSharedPreferences("com.beanie.stepsensors", Context.MODE_PRIVATE).edit();
		 editor.putString("tempData", string);
		 editor.commit();
	}
	
	
}