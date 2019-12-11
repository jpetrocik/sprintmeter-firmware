package com.bmxgates.logger;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.bmxgates.logger.TrackLocator.Track;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * A fragment representing a single Track detail screen. This fragment is either
 * contained in a {@link TrackListActivity} in two-pane mode (on tablets) or a
 * {@link TrackDetailActivity} on handsets.
 */
public class TrackDetailFragment extends Fragment {
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_TRACK = "track_id";

	private TrackLocator.Track track;

	private GoogleMap mMap;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public TrackDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().containsKey(ARG_TRACK)) {
			//TODO Change to look with trackId versus assume trackId is index
			track = (Track) TrackLocator.byTrackId(getArguments().getInt(ARG_TRACK));
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_track_detail, container, false);

		// Show the dummy content as text in a TextView.
		if (track != null) {
			((TextView) rootView.findViewById(R.id.track_detail)).setText(track.name);
			((EditText) rootView.findViewById(R.id.autoStopEditText)).setText(String.valueOf(track.autoStop));
		}

		((SupportMapFragment) this.getFragmentManager().findFragmentById(R.id.map)).getMapAsync(new OnMapReadyCallback() {
			@Override
			public void onMapReady(GoogleMap googleMap) {
				mMap = googleMap;
			}
		});

		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		setUpMapIfNeeded();
	}

	private void setUpMapIfNeeded() {
		if (mMap != null) {
//			mMap.setMyLocationEnabled(false);

			LatLng latLng = new LatLng(track.lat, track.log);
			
			CameraUpdate center = CameraUpdateFactory.newLatLng(latLng);
			CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);

			mMap.addMarker(new MarkerOptions().position(latLng));

			mMap.moveCamera(center);
			mMap.animateCamera(zoom);
			
		}
	}

}
