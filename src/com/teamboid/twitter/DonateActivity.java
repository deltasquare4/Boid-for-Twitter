package com.teamboid.twitter;

import net.robotmedia.billing.BillingController;
import net.robotmedia.billing.BillingRequest.ResponseCode;
import net.robotmedia.billing.helper.AbstractBillingObserver;
import net.robotmedia.billing.model.Transaction.PurchaseState;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

/**
 * This activity launches the Donate system.
 * 
 * It should be nearly invisible
 * @author kennydude
 *
 */
public class DonateActivity extends Activity {
	@Override
	public void onCreate(Bundle sis){
		super.onCreate(sis);
		
		// This callback must stay here, otherwise in-app billing doesn't work
		// for some reason.
		AbstractBillingObserver mBillingObserver = new AbstractBillingObserver(
				this) {
			@Override
			public void onBillingChecked(boolean supported) {
			}

			@Override
			public void onPurchaseStateChanged(String itemId,
					PurchaseState state) {
			}

			@Override
			public void onRequestPurchaseResponse(String itemId,
					ResponseCode response) {
				if(response != ResponseCode.RESULT_OK && response != ResponseCode.RESULT_USER_CANCELED){
					Toast.makeText(getApplicationContext(),
							R.string.error_str, Toast.LENGTH_SHORT).show();
				}
			}
		};
		BillingController.registerObserver(mBillingObserver);
		BillingController.checkBillingSupported(this);
		
		Toast.makeText(getApplicationContext(),
				R.string.donations_appreciated, Toast.LENGTH_SHORT).show();
		BillingController.requestPurchase(this,
				"com.teamboid.twitter.donate", true);
		finish();
	}
}
