package cn.blesolution.resistance;

import java.util.Calendar;

import android.view.View;
import android.view.View.OnClickListener;

public abstract class JEscapeDoubleClickListener implements OnClickListener {

	public static final int MIN_CLICK_DELAY_TIME = 1000;
	private long lastClickTime = 0;

	@Override
	public void onClick(final View v) {
		final long currentTime = Calendar.getInstance().getTimeInMillis();
		if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
			lastClickTime = currentTime;
			onNoDoubleClick(v);
		}
	}
	
	public abstract void onNoDoubleClick(View v);
}
