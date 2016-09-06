package com.beanie.stepsensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {
	private static boolean isResetStepCounts = false;

	@Override
	public void onReceive(Context context, Intent intent) {
		
		isResetStepCounts = true;
		Toast.makeText(context, "Alarm Triggered: reset current step counts ",
				Toast.LENGTH_LONG).show();

	}

	public static boolean isResetStep() {
		return isResetStepCounts;
	}

}
