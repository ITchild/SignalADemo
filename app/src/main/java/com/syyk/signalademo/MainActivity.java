package com.syyk.signalademo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

import com.zsoft.signala.hubs.HubConnection;
import com.zsoft.signala.hubs.HubInvokeCallback;
import com.zsoft.signala.hubs.HubOnDataCallback;
import com.zsoft.signala.hubs.IHubProxy;
import com.zsoft.signala.transport.StateBase;
import com.zsoft.signala.transport.longpolling.LongPollingTransport;

import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements OnDisconnectionRequestedListener,
	ConnectionFragment.OnConnectionRequestedListener,
	CalculatorFragment.ShowAllListener,
	CalculatorFragment.OnCalculationRequestedListener
{
	protected static final String TAG_CONNECTION_FRAGMENT = "connection";	
	protected static final String TAG_CALCULATION_FRAGMENT = "calculation";
	
	protected HubConnection con = null;
	protected IHubProxy hub = null;
	protected TextView tvStatus = null;
	private Boolean mShowAll = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		tvStatus = (TextView) findViewById(R.id.connection_status);
		
		ChangeFragment(new ConnectionFragment(), false, TAG_CONNECTION_FRAGMENT);
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void ConnectionRequested(Uri address) {
		
		con = new HubConnection(address.toString(), this, new LongPollingTransport())
		{
			@Override
			public void OnStateChanged(StateBase oldState, StateBase newState) {
				tvStatus.setText(oldState.getState() + " -> " + newState.getState());
				
				switch(newState.getState())
				{
					case Connected:
						CalculatorFragment fragment = new CalculatorFragment();
						ChangeFragment(fragment, true, TAG_CALCULATION_FRAGMENT);
						break;
					case Disconnected:
						Fragment f = getSupportFragmentManager().findFragmentByTag(TAG_CALCULATION_FRAGMENT);
						if (f!=null && f.isVisible()) {
							getSupportFragmentManager().popBackStackImmediate();
						}
						break;
					default:
						break;
				}
			}
				
			@Override
			public void OnError(Exception exception) {
	            Toast.makeText(MainActivity.this, "On error: " + exception.getMessage(), Toast.LENGTH_LONG).show();
			}

		};
		
		try {
			hub = con.CreateHubProxy("serverHub");
		} catch (OperationApplicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		hub.On("broadcastMessageone", new HubOnDataCallback()
		{
			@Override
			public void OnReceived(JSONArray args) {
				if(mShowAll)
				{
					for(int i=0; i<args.length(); i++)
					{
						Toast.makeText(MainActivity.this, args.opt(i).toString()+"11111111", Toast.LENGTH_SHORT).show();
					}
				}
			}
		});
		
		con.Start();
	}

	@Override
	public void calculate(int value1, int value2, String operator) {

		List<String> args = new ArrayList<String>(2);
		args.add("mac");
		args.add("[{\"k1\":\"v1\",\"k2\":\"v2\"},{\"k3\":\"v3\",\"k4\":\"v4\"}]");
		hub.Invoke("send", args, new HubInvokeCallback() {
			@Override
			public void OnResult(boolean succeeded, String response) {
				Toast.makeText(MainActivity.this, succeeded+response+"2222222", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void OnError(Exception ex) {
				Toast.makeText(MainActivity.this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
			}
		});
		
	}

	protected void ChangeFragment(Fragment fragment, Boolean addToBackstack, String tag)
	{
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		trans.replace(R.id.fragment_container, fragment, tag);
		if(addToBackstack)
			trans.addToBackStack(null);
		trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		trans.commit();
		
	}

	@Override
	public void setShowAll(Boolean value) {
		mShowAll = value;
	}

	@Override
	public Boolean getShowAll() {
		return mShowAll;
	}

	@Override
	public void DisconnectionRequested() {
		if(con!=null)
		{
			con.Stop();
		}
		
	}

}



