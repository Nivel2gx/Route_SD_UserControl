package com.example.samplemodule;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import com.artech.activities.ActivityHelper;
import com.artech.android.WithPermission;
import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.AvoidType;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.artech.android.api.GeoLocationAPI;
import com.artech.android.api.LocationHelper;
import com.artech.base.controls.IGxControlRuntime;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.ActionParameter;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.services.Services;
import com.artech.base.utils.GeoFormats;
import com.artech.controllers.ViewData;
import com.artech.controls.grids.GridAdapter;
import com.artech.controls.grids.GridHelper;
import com.artech.controls.maps.GxMapViewDefinition;
import com.artech.controls.maps.common.IGxMapView;
import com.artech.controls.maps.common.MapItemViewHelper;
import com.artech.externalapi.ExternalApi;
import com.artech.ui.Coordinator;
import com.example.genexusmodule.R;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

//import com.android.volley.Request;
//import com.android.volley.RequestQueue;
//import com.android.volley.Response;
//import com.android.volley.VolleyError;
//import com.android.volley.toolbox.JsonObjectRequest;
//import com.android.volley.toolbox.Volley;

import static android.content.Context.SENSOR_SERVICE;
//import android.provider.Settings;

@SuppressLint("ViewConstructor")
public class BasicUserControl extends MapView implements IGxControlRuntime, IGxMapView,
		SensorEventListener,LocationListener, GoogleMap.CancelableCallback {

	final static String NAME = "BasicUserControl";
	private final static String METHOD_SET_NAME = "routepoint";
	private final static String EVENT_ON_TAP = "OnTap";
	private float currentDegree = 0f;

	private LatLngBounds bounds = null;
	private final Coordinator mCoordinator;
	private final GxMapViewDefinition mDefinition;
	private boolean mIsAnimatedRunning = false;
	//private final LayoutItemDefinition mDefinition;

	private final SensorManager mSensorManager;

	private boolean mIsReady;
	private boolean mOnResumeInvoked;

	private boolean mIsReadyAndDraw;

	private GridHelper mHelper;
	private GridAdapter mAdapter;
	private MapItemViewHelper mItemViewHelper;

	private ViewData mPendingUpdate;
	private CameraUpdate mPendingCameraUpdate;

	private String mName;
	private int tapCount;
	GoogleMap mMap = null;

	//APUNTAR AL NORTE
	float mDeclination;
	float[] mRotationMatrix = new float[9];
	float[] mGeomagneticMatrix = new float[9];

	float[] mGravity;
	float[] mGeomagnetic;
	float[] mGravity2;
	float[] mGeomagnetic2;
	boolean success;

	LatLng startPointLatLong;
	LatLng endPointLatLong;
	LatLng  myLocation;
	private String PropertyGridVariableName;
	String PropertyApiKey;
	Boolean PropertyMyLocation;
	Boolean PropertyAutoRotate;
	Boolean PropertyDirectionLayer;

	private final static int ITEM_VIEW_WIDTH_MARGIN = 20; // dips
	private final static int MARKER_CAMERA_ANIMATION_DURATION = 500; // ms
	static final int CAMERA_MARGIN_DIPS = 40;

	@SuppressWarnings("UnusedParameters")
	public BasicUserControl(Context context, Coordinator coordinator, LayoutItemDefinition definition) {
		super(context, new GoogleMapOptions());
		Log.e("UC:", " Antes que nada");
		mCoordinator = coordinator;

		mDefinition = new GxMapViewDefinition(context, (GridDefinition) definition);
		mSensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);

		onCreate(new Bundle());

		Log.e("UC:", " REGISTERLISTENER ");
		initialize();
	}

	private void initialize() {
		mHelper = new GridHelper(this, mCoordinator, mDefinition.getGrid());
		mHelper.setReservedSpace(ITEM_VIEW_WIDTH_MARGIN);
		mAdapter = new GridAdapter(getContext(), mHelper, mDefinition.getGrid());
		mItemViewHelper = new MapItemViewHelper(this);

		PropertyGridVariableName = mDefinition.getItem().getControlInfo().optStringProperty("@BasicUserControlCodeAtt");
		PropertyGridVariableName.replace("&", "");   // NO ANDA , no saca el &
		PropertyApiKey = mDefinition.getItem().getControlInfo().optStringProperty("@BasicUserControlGoogleDirectionApi");
		PropertyMyLocation = mDefinition.getItem().getControlInfo().optBooleanProperty("@BasicUserControlmylocation");
		PropertyAutoRotate = mDefinition.getItem().getControlInfo().optBooleanProperty("@BasicUserControlAutoRotate");
		PropertyDirectionLayer = mDefinition.getItem().getControlInfo().optBooleanProperty("@BasicUserControlDirectionLayer");

		getMapAsync(new OnMapReadyCallback() {


			@Override
			public void onMapReady(final GoogleMap googleMap) {
				mMap = googleMap;

				Log.e("UC:", " ON MAP READY");
				LatLng MVD = new LatLng(-34.8800126,-56.0865618);
				CameraPosition pos = CameraPosition.builder().target(MVD).zoom(10f).build();
				mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos), 1000, null);

				if (mMap == null)
					Log.e("UC:", " MAP¨IS NULL ");
				getMyLocation();

				mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
					@Override
					public void onMapLoaded() {
						Log.e("UC:", " onMapLoaded ");

						if (bounds != null) {
							updateBound();
						}
					}
				});

				if(PropertyMyLocation)
				     setMyLocation();

				if(PropertyAutoRotate)
					startAutoRotate();

				makeMapReady();
			}
		});

	}

	private void updateMyLocation(){
		mMap.setOnMyLocationChangeListener(new GxMapOnMyLocationChangeListener());
	}

	private class GxMapOnMyLocationChangeListener implements GoogleMap.OnMyLocationChangeListener
	{
		Marker mPositionMarker;
		//BitmapDescriptor mBitmapDescriptor;

		@Override
		public void onMyLocationChange(Location location)
		{
			if (location != null){
				myLocation = stringToLatLng2(location.getLatitude() + "," + location.getLongitude());
				//Log.e(TAG, "MyLocation: " + myLocation);
			}else{
				return;
			}
		}
	}

	private void updateBound(){
		bounds.including(myLocation);
		mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80), 1000, null);
	}
	private void startAutoRotate(){
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);
	}

	 /// Para la navegación, deja de utilizar los sensores.
	private void stopAutoRotate(){
		mSensorManager.unregisterListener(this);
	}

	private void makeMapReady() {
		mIsReady = true;
		ViewData pendingUpdate = mPendingUpdate;
		mPendingUpdate = null;

		if (pendingUpdate != null)
			update(pendingUpdate);
	}

	@Override
	public void setEnabled(boolean enabled) {

	}

	@Override
	public void setProperty(String name, Object value) {

	}


	@Override
	public Object getProperty(String name) {
		return null;
	}

	public void runOnTapEvent() {
		ActionDefinition actionDef = mCoordinator.getControlEventHandler(this, EVENT_ON_TAP);

		for (ActionParameter param : actionDef.getEventParameters()) {
			String paramName = param.getValueDefinition().getName();
			mCoordinator.setValue(paramName, tapCount);
		}

		mCoordinator.runControlEvent(this, EVENT_ON_TAP);
	}

	private final View.OnClickListener mOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			tapCount++;
			runOnTapEvent();
		}
	};

	@Override
	public String getMapType() {
		return null;
	}

	@Override
	public void setMapType(String type) {

	}

	@Override
	public void addListener(GridEventsListener listener) {

	}

	void animateCamera(CameraUpdate update) {
		if (mMap != null) {
			try {
				mMap.animateCamera(update);
				return; // Done!
			} catch (IllegalStateException e) {
				// Map is not ready.
			}
		}

		mPendingCameraUpdate = update;
	}

	public static LatLng stringToLatLng2(String str) {
		Pair<Double, Double> coordinates = GeoFormats.parseGeolocation(str);
		if (coordinates != null)
			return new LatLng(coordinates.first, coordinates.second);
		else
			return null;
	}

	////////////////////////////////////////////////////////////////////

	@Override
	public void update(ViewData data) {
		Services.Log.debug("mDefinition: " + mDefinition.getItem().toString());
		Services.Log.debug("cantidad " + data.getCount());

		EntityList dataList = data.getEntities();

		Services.Log.debug("DataList " + dataList.toString());

		endPointLatLong = null;
		LatLngBounds.Builder builder = new LatLngBounds.Builder();

		boolean Inicio = true;

		if (dataList != null) {
			for (Entity dato : dataList) {
				String geolocation = dato.getProperty(PropertyGridVariableName).toString();
				Log.e("UC:", "dato.tostring " + dato.toString());
				MarkerOptions marker = new MarkerOptions();

				LatLng position = stringToLatLng2(geolocation);
				if (Inicio) {
					startPointLatLong = position;
					marker.icon(BitmapDescriptorFactory.fromResource(R.mipmap.taxidest));
					Inicio = false;
				} else {
					marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.locator));
				}

				marker.position(position);
				builder.include(position);

				if (mMap != null) {
					mMap.addMarker(marker);
				}
				endPointLatLong = position;
			}
		}

		bounds = builder.build();

		if (mIsReady) {
			if (!mOnResumeInvoked) {
				mOnResumeInvoked = true;

				if (PropertyDirectionLayer)
					//startRouting(startPointLatLong, endPointLatLong);
					startRouting();

					onResume();
			}
		} else {
			mPendingUpdate = data;
		}

		Services.Device.postOnUiThreadDelayed(new Runnable() {
			@Override
			public void run() {
				mIsReadyAndDraw = true;
			}
		}, 2000);

	}

	public void runMethod(String method, List<Object> parameters) {
		Log.e("UC:", " 1. onrunMethod ");
		Log.e("UC:", method);
		Log.e("UC:", "routepoint");
		if (METHOD_SET_NAME.equals(method)) {
			try {
				String startPoint = (String) parameters.get(0);
				startPointLatLong = new LatLng(Double.valueOf(startPoint.split(",")[1]), Double.valueOf(startPoint.split(",")[2]));
				Log.e("UC:", startPoint);
				String endPoint = (String) parameters.get(1);
				Log.e("UC:", startPoint);
				endPointLatLong = new LatLng(Double.valueOf(endPoint.split(",")[1]), Double.valueOf(endPoint.split(",")[2]));
				startRouting();
			} catch (Exception e) {
				e.printStackTrace();
				Log.e("UC:", e.toString());
			}
		}
	}

	public void startRouting() {
		Log.e("UC:", "2. StartRouting method");
		if (mMap != null) {
			Log.e("UC:", "3. Map is not null");
			GoogleDirection.withServerKey(PropertyApiKey)
					.from(startPointLatLong)
					.to(endPointLatLong)
					.avoid(AvoidType.FERRIES)
					.avoid(AvoidType.HIGHWAYS)
					.execute(new DirectionCallback() {
						@Override
						public void onDirectionSuccess(Direction direction, String rawBody) {
							Log.e("UC:", "4. On direction success");
							if (direction.isOK()) {
								Route route = direction.getRouteList().get(0);
								Log.e("UC:", "5. Create polyline");
								ArrayList<LatLng> directionPositionList = route.getLegList().get(0).getDirectionPoint();
								mMap.addPolyline(DirectionConverter.createPolyline(getContext(), directionPositionList, 5, Color.BLUE));
								//setCameraWithCoordinationBounds(route);
							} else {
								// Do something
							}
						}

						@Override
						public void onDirectionFailure(Throwable throwable) {

						}
					});
		} else {
			Log.d("BasicUserControl", "Map is not ready yet.");
		}
	}

	@SuppressWarnings({"deprecation", "MissingPermission"}) // Checked by updateFromData()
	private final Runnable mRunnableEnableMyLocationLayer = new Runnable() {
		@Override
		public void run() {
			Log.e("UC:", " Ejecuta el run de EnabledLocation");
			mMap.setMyLocationEnabled(true);

		}
	};

	private void getMyLocation() {
		Log.e("UC:", " SetMylocation pide permisos");
		WithPermission.Builder<Void> permisionBuilder;
		permisionBuilder = new WithPermission.Builder<Void>(ActivityHelper.getCurrentActivity())
				.needs(GeoLocationAPI.getRequiredPermissions())
				.setRequestCode(1010)
				.attachToActivityController()
				.onSuccess(new Runnable() {
					@Override
					public void run() {
						Log.e("UC:", " Busca mi posicion");
						Location location = LocationHelper.getLastKnownLocation();
						if (location != null) {
							myLocation = stringToLatLng2(location.getLatitude() + "," + location.getLongitude());
						} else
							Toast.makeText(getContext(), R.string.GXM_CouldNotGetLocationInformation, Toast.LENGTH_SHORT).show();
					}
				})
				.onFailure(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getContext(), "No se obtuvieron los permisos.", Toast.LENGTH_SHORT).show();
					}
				});
		permisionBuilder.build().run();
	}


	@SuppressLint("MissingPermission")
	private void setMyLocation(){
		if(PropertyMyLocation)
			mMap.setMyLocationEnabled(true);
	}

	public void RoutePoint(String startpoint, String endpoint) {
		mName = startpoint;
		//setText(getContext().getString(R.string.welcome_message, name));
	}


	@Override
	public void onLocationChanged(Location location) {
		Log.e("UC:", " ONLOCATION CHANGED");
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (PropertyAutoRotate)
		{
			float bearing = (float) Math.round(event.values[0]);
			Log.d("onSensorChanged", "Heading: " + Float.toString(bearing) + " degrees");
			if (Math.abs(currentDegree - bearing) > 4) {
				Log.e("UC:", " CAMBIO mas de 4");
				updateCamera(bearing);
				currentDegree = bearing; //-bearing;
			} //else
				//Log.e("UC:", " NO CAMBIO mas de 4");
		}

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {

	}

	private void updateCamera(float bearing)
	{
		Log.e("UC:"," UPDATECAMERA");
		if (mMap == null) {
			return;
		}
		CameraPosition oldPos = mMap.getCameraPosition();
		float zoomold = mMap.getMaxZoomLevel();     //.getCameraPosition().zoom;
		CameraPosition pos = CameraPosition.builder(oldPos).bearing(bearing).build();

		if (mIsAnimatedRunning)
		{
			mMap.stopAnimation();
			mIsAnimatedRunning = true;
			mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos),200,this);
		}
		else
		{
			mIsAnimatedRunning = true;
			mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos),200,this);
		}

	}
	@Override
	public void onFinish() {
		Log.e("onFinish:","onFinish:") ;
		mIsAnimatedRunning = false;
	}

	@Override
	public void onCancel() {
		Log.e("onCancel:"," onCancel" ) ;

	}
}