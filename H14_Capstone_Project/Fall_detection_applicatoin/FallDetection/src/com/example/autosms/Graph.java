package com.example.autosms;

import java.text.DecimalFormat;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer.LegendAlign;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

public class Graph extends Activity {
	//declare UI components and public variables
	private GraphView Plot;
	private final Handler mhandler = new Handler();
	private Runnable Timer1;
	private double graph2LastXValue = 5d;
	private LineGraphSeries<DataPoint> series;
	private LineGraphSeries<DataPoint> series2;
	private LineGraphSeries<DataPoint> series3;
	private LineGraphSeries<DataPoint> series4;

	
	
	/** Called when the activity is first created. */
	//initilatize UI components
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.graph);
		//create four line series
		series = new LineGraphSeries<DataPoint>(new DataPoint[] {});
		series2 = new LineGraphSeries<DataPoint>(new DataPoint[] {});
		series3 = new LineGraphSeries<DataPoint>(new DataPoint[] {});
		series4 = new LineGraphSeries<DataPoint>(new DataPoint[] {});
		//iniatlize a graph and link it to the UI
		Plot = (GraphView) findViewById(R.id.graph);
		//add series to the graph
		Plot.addSeries(series);
		Plot.addSeries(series2);
		//configure X and Y axis to fixed values
		Plot.getViewport().setScrollable(true);
		Plot.getViewport().setYAxisBoundsManual(true);
		Plot.getViewport().setXAxisBoundsManual(true);
		Plot.getViewport().setMinX(0);
		Plot.getViewport().setMaxX(50);
		Plot.getViewport().setMinY(0);
		Plot.getViewport().setMaxY(4);
		//configure plotting style
		series.setDrawDataPoints(true);
		series.setDataPointsRadius(5);
		series2.setColor(Color.RED);
		series2.setDrawDataPoints(true);
		series2.setDataPointsRadius(5);
		//set legends
		series.setTitle("Phone Magnitude");
		series2.setTitle("Arduino Magnitude");
		series3.setTitle("Pitch Magnitude");
		series4.setTitle("Roll Magnitude");
		Plot.getLegendRenderer().setVisible(true);
		Plot.getLegendRenderer().setAlign(LegendAlign.TOP);

		//add series to the graph
		Plot.getSecondScale().addSeries(series3);
		Plot.getSecondScale().addSeries(series4);
		Plot.getSecondScale().setMinY(0);
		Plot.getSecondScale().setMaxY(90);
		series3.setColor(Color.GREEN);
		series4.setColor(Color.BLACK);
		//setup tap listener for data point view
		series.setOnDataPointTapListener(new OnDataPointTapListener(){
			
			@Override
			public void onTap(Series series, DataPointInterface dataPoint) {
				// TODO Auto-generated method stub
				Toast.makeText(getApplicationContext(), "Phone sensor datapoint: "+dataPoint, Toast.LENGTH_SHORT).show();
			}
		});
		
		series2.setOnDataPointTapListener(new OnDataPointTapListener(){
			
			@Override
			public void onTap(Series series2 , DataPointInterface dataPoint) {
				// TODO Auto-generated method stub
				Toast.makeText(getApplicationContext(), "Arduino sensor datapoint: "+dataPoint, Toast.LENGTH_SHORT).show();
			}
		});

		
	}
	
	
	//keep plotting graph by a UI background runnable while activity is on
	public void onResume(){
		super.onResume();
		Timer1 = new Runnable(){
			public void run(){
				//X axis incremental every 0.2s
				graph2LastXValue += 1d;
				//get sensor values for Y values from main activity
				double y = MainActivity.getSensorV();
				double yy = MainActivity.getSensorZ();
				double d = MainActivity.getSensorD();
				double dd = MainActivity.getSensorDD();
				//append data to existing plot
				series4.appendData(new DataPoint(graph2LastXValue,dd), true, 300);
				series3.appendData(new DataPoint(graph2LastXValue,d), true, 300);
				series2.appendData(new DataPoint(graph2LastXValue,yy), true, 300);
				series.appendData(new DataPoint(graph2LastXValue,y), true, 300);
				Plot.refreshDrawableState();
				//set sample rate to 0.2s
				mhandler.postDelayed(this, 200);
			}
		};
		//stall 1s when resumed
		mhandler.postDelayed(Timer1, 1000);
	}

	
	//Pause plotting when switched out
	public void onPause(){
		mhandler.removeCallbacks(Timer1);
		super.onPause();
	}
	
	//click the toggle to pause/resume plotting
	public void onToggleClicked(View view){
		boolean on = ((ToggleButton) view).isChecked();
		if(on){
			mhandler.removeCallbacks(Timer1);
		}
		else{
			mhandler.postDelayed(Timer1, 800);
		}
	}
	


	
}
