package de.cstimming.konphidroid;

import java.io.CharArrayWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class LocationCollector implements LocationListener, SenderInterface {
	private ArrayList<Location> m_collectingLocations;
	private ArrayList<Location> m_sendingLocations;
	private boolean m_currentlySending;
	private String m_server;
	private SliceSenderResult m_result_cb;
	private SimpleDateFormat m_dateFormatter;
	private int m_instanceid;

	public LocationCollector(String serveraddress, SliceSenderResult result_cb, int instanceid) {
		m_currentlySending = false;
		m_server = serveraddress;
		m_result_cb = result_cb;
		m_instanceid = instanceid;
		m_dateFormatter = new SimpleDateFormat("HH:mm:ss");
		resetCollecting();
	}
	private void resetCollecting() {
		m_collectingLocations = new ArrayList<Location>();
	}

	@Override
	public void onLocationChanged(Location loc) {
		m_collectingLocations.add(loc);
		checkForSending();
	}

	private void checkForSending() {
		if (m_currentlySending || m_collectingLocations.size() <= 1) {
			return;
		}
		Location first = m_collectingLocations.get(0);
		Location last = m_collectingLocations.get(m_collectingLocations.size() - 1);
		long secdiff = (last.getTime() - first.getTime()) / 1000;
		long intervalSecs = 30; // FIXME getSenderIntervalSecs();
		if (intervalSecs > 0 && secdiff >= intervalSecs) {
			sendNow();
		}
	}

	private void sendNow() {
		m_sendingLocations = m_collectingLocations;
		m_currentlySending = true;
		scheduleSending();
		resetCollecting();
	}

	// FIXME: This must be tested!
	private static HttpEntity listToStreamEntity(ArrayList<Location> ul, int instanceid) {
		CharArrayWriter gpxoutput = new GpxCharArrayWriter(ul, instanceid);
		String gpxstring = gpxoutput.toString();
		try {
			return new StringEntity(gpxstring);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	private void scheduleSending() {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(m_server + "slices"); // FIXME: Different URL for GPX upload!
		final String displaytime = m_dateFormatter.format(new Date(m_sendingLocations.get(m_sendingLocations.size() - 1).getTime())) + ": ";
		HttpEntity entity = listToStreamEntity(m_sendingLocations, m_instanceid);
		httppost.setEntity(entity);

		// The network connection is moved into a separate task
		SliceSenderTask task = new SliceSenderTask(httpclient, displaytime, m_result_cb, this);

		// Execute HTTP Post Request
		task.execute(httppost);

	}

	@Override
	public void startedSending() {
		// TODO Auto-generated method stub

	}
	@Override
	public void stoppedSending(boolean successful) {
		if (successful) {
			m_currentlySending = false;
			m_sendingLocations = null;
		} else {
			scheduleSending(); // TODO: what now?
		}
	}


	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub

	}
	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}
	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub

	}

}
