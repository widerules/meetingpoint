package pl.skifo.meetingpoint;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.catchnotes.integration.IntentIntegrator;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

public class MeetingPoint extends MapActivity implements OnClickListener, ActionListener {
    
    public static final int DIALOG_ID__ON_TAP = 0;
    public static final int DIALOG_ID__ON_EDIT = 1;
    public static final int DIALOG_ID__INFO = 2;
    public static final int DIALOG_ID__SEARCH_RESULT = 3;
    public static final int DIALOG_ID__LONLAT = 4;
    
    private static final String SAVED_STATE__currentMarkerId = "currentMarkerId";
    private static final String SAVED_STATE__markerNumberGenerator = "markerNumberGenerator";
    private static final String SAVED_STATE__centerLat = "centerLat";
    private static final String SAVED_STATE__centerLon = "centerLon";
    private static final String SAVED_STATE__isSatView = "isSatView";
    private static final String SAVED_STATE__zoom = "zoom";
    
    
    public static final int INITIAL_ZOOM_LEVEL = 18;

    public static final String ACTION_NEXT = "next";
    public static final String ACTION_PREV = "prev";
    public static final String ACTION_SEARCH = "search";
    
    private MapView mapView;
    
    private List<Overlay> mapOverlays;
    private Drawable markerPic;
    private MarkersOverlay markersOverlay;
    private CrosshairsOverlay crossOverlay;
    private MyLocationOverlay myLocationOverlay;
    private Geocoder gcoder;
    private SearchListAdapter searchAdapter;
    
    private Button getPosButton;
    
    private boolean isStreetView = false;
    private boolean isSatView = false;
    
    private int markerNumberGenerator = 1;
    
    private int currentMarkerId = -1;
    
    private IntentIntegrator notesIntent;
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MeetingPoint", "onDestroy");
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d("MeetingPoint", "onStart");
    }

    private void storeMyState(Bundle state) {
        int w = mapView.getWidth();
        int h = mapView.getHeight();
        GeoPoint gp = getGeoPoint(w/2, h/2);
        state.putInt(SAVED_STATE__currentMarkerId, currentMarkerId);
        state.putInt(SAVED_STATE__markerNumberGenerator, markerNumberGenerator);
        state.putInt(SAVED_STATE__centerLat, gp.getLatitudeE6());
        state.putInt(SAVED_STATE__centerLon, gp.getLongitudeE6());
        state.putInt(SAVED_STATE__zoom, mapView.getZoomLevel());
        state.putBoolean(SAVED_STATE__isSatView,isSatView);
        markersOverlay.saveState(state);
        Log.d("MeetingPoint", "saving state");
    }
    
    
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("MeetingPoint", "saving state");
        storeMyState(outState);
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.d("MeetingPoint", "onStop");
    }


    private LocationManager locMgr;
    private Location myLocation;
    private MPLocationListener locListener;
    
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Log.d("MeetingPoint", "onCreate, savedState == "+savedInstanceState);
        if (savedInstanceState != null) {
            Log.d("MeetingPoint", "onCreate, markers cnt = "+savedInstanceState.getInt("size"));
        }
        
        super.onCreate(savedInstanceState);
        
        boolean checkIntent = false;
        
        if (savedInstanceState == null) {
            savedInstanceState = new Bundle();
            checkIntent = true;
        }
        else
            Log.d("MeetingPoint", "onCreate, AGAIN markers cnt = "+savedInstanceState.getInt("size"));

        Log.d("MeetingPoint", "onCreate");
        setContentView(R.layout.main_panel);
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        mapOverlays = mapView.getOverlays();
        
        markerPic = getResources().getDrawable(R.drawable.marker);
        markersOverlay = new MarkersOverlay(this, markerPic);
        markersOverlay.restoreState(savedInstanceState);
        
        crossOverlay = new CrosshairsOverlay(this);
        crossOverlay.setActionListener(this);
        
        myLocationOverlay = new MyLocationOverlay(this, mapView);
        
        mapOverlays.add(markersOverlay);
        mapOverlays.add(myLocationOverlay);
        mapOverlays.add(crossOverlay);
        
        getPosButton = (Button) findViewById(R.id.getpos_button);
        //label = (TextView) findViewById(R.id.position_label);
        
        getPosButton.setOnClickListener(this);
        
        isSatView = savedInstanceState.getBoolean(SAVED_STATE__isSatView);
        mapView.setSatellite(isSatView);
        mapView.setStreetView(isStreetView);
        
        CompoundButton bSat = (CompoundButton) findViewById(R.id.sat_view);
        bSat.setChecked(isSatView);
        bSat.setOnClickListener(new SatButtonListener(this));
        
        ImageButton bMyPos = (ImageButton) findViewById(R.id.my_pos);
        bMyPos.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                if (myLocation != null) {
                    MapController ctrl = mapView.getController();
                    double latE6 = myLocation.getLatitude() * 1000000;
                    double lonE6 = myLocation.getLongitude() * 1000000;
                    //ctrl.setCenter(new GeoPoint((int)latE6, (int)lonE6));
                    ctrl.animateTo(new GeoPoint((int)latE6, (int)lonE6));
                }
            }});

        Button bSetLonLat = (Button) findViewById(R.id.ll_button);
        bSetLonLat.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_ID__LONLAT);
            }});
        
        
        locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location gpsLocation = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location netLocation = locMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (gpsLocation != null) {
            if (netLocation != null) {
                long tGps = gpsLocation.getTime();
                long tNet = netLocation.getTime();
                if (tNet > (tGps + 60000)) {
                    myLocation = netLocation;
                }
                else
                    myLocation = gpsLocation;
            }
            else
                myLocation = gpsLocation;
        }
        else {
            myLocation = netLocation; 
        }
        
        
        int centerLat = savedInstanceState.getInt(SAVED_STATE__centerLat, Integer.MAX_VALUE);
        int centerLon = savedInstanceState.getInt(SAVED_STATE__centerLon, Integer.MAX_VALUE);
        MapController ctrl = mapView.getController();
        if (centerLat != Integer.MAX_VALUE && centerLon != Integer.MAX_VALUE) {
            ctrl.setCenter(new GeoPoint(centerLat, centerLon)); 
        }
        else 
            if (myLocation != null) {
                Log.d("MeetingPoint", "myLocation "+myLocation.getLatitude()+", "+myLocation.getLongitude());
                double latE6 = myLocation.getLatitude() * 1000000;
                double lonE6 = myLocation.getLongitude() * 1000000;
                ctrl.setCenter(new GeoPoint((int)latE6, (int)lonE6));
            }
            else {
                Log.d("MeetingPoint", "myLocation not known");
            }
        
        ctrl.setZoom(savedInstanceState.getInt(SAVED_STATE__zoom, INITIAL_ZOOM_LEVEL));
        locListener = new MPLocationListener();
        notesIntent = new IntentIntegrator(this);
        
        currentMarkerId = savedInstanceState.getInt(SAVED_STATE__currentMarkerId, -1);
        markerNumberGenerator = savedInstanceState.getInt(SAVED_STATE__markerNumberGenerator, 1);
        
        gcoder = new Geocoder(this);
        searchAdapter = new SearchListAdapter(this);
        
        if (checkIntent)
            handleIntent(getIntent());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MeetingPoint", "onPause");
        locMgr.removeUpdates(locListener);
        //myLocationOverlay.disableCompass();
        myLocationOverlay.disableMyLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MeetingPoint", "onResume");
        locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5 * 61000, 500, locListener);
        locMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 500, locListener);

        myLocationOverlay.enableMyLocation();
        //myLocationOverlay.enableCompass();
        
        mapView.preLoad();
    }
    
    private class MPLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            Log.d("MeetingPoint", "location update: ("+location.getLatitude()+" "+location.getLongitude()+")");
            if (myLocation == null || LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
                myLocation = location;
            }
            else {
                if (LocationManager.GPS_PROVIDER.equals(myLocation.getProvider())) {
                    long tGps = myLocation.getTime();
                    long tNet = location.getTime();
                    if (tNet > (tGps + 3 * 60000)) {
                        myLocation = location;
                    }
                }
                else
                    myLocation = location;
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

    }
    
    
    private static class SatButtonListener implements OnClickListener {
        private MeetingPoint parent;
        public SatButtonListener(MeetingPoint p) {
            parent = p;
        }
        
        public void onClick(View v) {
            parent.isSatView = !parent.isSatView;
            parent.mapView.setSatellite(parent.isSatView);
            parent.crossOverlay.setSecondaryCross(parent.isSatView);
            parent.mapView.invalidate();
//            Log.d("MeetingPoint","satView = "+parent.mapView.isSatellite());
        }
    }
    
    
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    
    private GeoPoint getGeoPoint(int x, int y) {
        Projection p = mapView.getProjection();
        GeoPoint gp = p.fromPixels(x, y);
        
        int la, lo;
        la = gp.getLatitudeE6();
        lo = gp.getLongitudeE6();

        GeoPoint gp2 = new GeoPoint(la, lo);
        
        Point o2 = new Point();
        p.toPixels(gp2, o2);
        
        int dist = (o2.x - x) * (o2.x - x) + (o2.y - y) * (o2.y - y);
        int delta = 1;
        boolean found = false;
        
        if (dist > 0) {
            Log.d("MeetingPoint", "dist = ["+dist+"] calculating correction");
        
            for (int i=0; i<2; i++) {
                Log.d("MeetingPoint", "adjusting latitude delta = "+delta);
                int watchdog = 25;
                while (true) {
                    int prevLo = lo;
                    int prevLa = la;
                    
                    la += delta;
                    if (la > 90 * 1000000 - 1) {
                        delta = -delta;
                        if (lo < 0)
                            lo += 180 * 1000000;
                        else
                            lo -= 180 * 1000000;
                    }
                    GeoPoint cGp = new GeoPoint(la, lo);
                    p.toPixels(cGp, o2);
                    int newDist = (o2.x - x) * (o2.x - x) + (o2.y - y) * (o2.y - y);
                    Log.d("MeetingPoint", "newDist = "+newDist);
                    if (newDist == 0) {
                        found = true;
                        break;
                    }
                    if (newDist < dist) {
                        dist = newDist; 
                        continue;
                    }
                    if (newDist == dist) {
                        delta *= 2;
                        Log.d("MeetingPoint", "same distance, delta increased = "+delta);
                        watchdog--;
                        if (watchdog == 0) {
                            Log.d("MeetingPoint", "watchdog");
                            delta = (delta<0)?-1:1;
                        }
                        else
                            continue;
                    }
                    lo = prevLo;
                    la = prevLa;
                    break;
                }
                if (found) break;
                delta = -delta;
            }

            if (!found) {
                delta = 1;
                
                for (int i=0; i<2; i++) {
                    Log.d("MeetingPoint", "adjusting longitude delta = "+delta);
                    int watchdog = 25;
                    while (true) {
                        int prevLo = lo;
                        int prevLa = la;
                        
                        lo += delta;
                        if (lo > 180 * 1000000 -1)
                            lo = -180 * 1000000 + 1;
                        if (lo < -180 * 1000000 +1)
                            lo = 180 * 1000000 - 1;
                        
                        GeoPoint cGp = new GeoPoint(la, lo);
                        p.toPixels(cGp, o2);
                        int newDist = (o2.x - x) * (o2.x - x) + (o2.y - y) * (o2.y - y);
                        Log.d("MeetingPoint", "newDist = "+newDist);
                        if (newDist == 0) {
                            dist = newDist; 
                            found = true;
                            break;
                        }
                        if (newDist < dist) {
                            dist = newDist; 
                            continue;
                        }
                        if (newDist == dist) {
                            delta *= 2;
                            Log.d("MeetingPoint", "same distance, delta increased = "+delta);
                            watchdog--;
                            if (watchdog == 0) {
                                Log.d("MeetingPoint", "watchdog");
                                delta = (delta<0)?-1:1;
                            }
                            else
                                continue;
                        }
                        lo = prevLo;
                        la = prevLa;
                        break;
                    }
                    if (found) break;
                    delta = -delta;
                }
            }
            gp2 = new GeoPoint(la, lo);
            Log.d("MeetingPoint", "closest dist = "+dist);
        }
        return gp2;
    }
    
    public void onClick(View v) {
        int w = mapView.getWidth();
        int h = mapView.getHeight();
        int x = w/2;
        int y = h/2;

        GeoPoint gp2 = getGeoPoint(x, y);
        
        OverlayItem mark = new OverlayItem(gp2, "marker "+markerNumberGenerator, "");
        Log.d("MeetingPoint","add ["+mark.getTitle()+"]<"+gp2.getLatitudeE6()+","+gp2.getLongitudeE6()+">");
        markerNumberGenerator++;
        markersOverlay.addMarker(mark);
        setCurrentMarker(markersOverlay.size() - 1);
        mapView.invalidate();
        
    }
    
    
    public void setCurrentMarker(int id) {
        currentMarkerId = id;
    }
    
    
    private Location marker2Location(OverlayItem marker) {
        Location loc = new Location("MeetingPoint");
        GeoPoint gp = marker.getPoint();
        loc.setLatitude((double)gp.getLatitudeE6()/1e6);
        loc.setLongitude((double)gp.getLongitudeE6()/1e6);
        return loc;
    }
    
    private void addURL(StringBuilder ret, Location loc, String name) {
        ret.append("http://maps.google.com/maps?q=");
        ret.append(loc.getLatitude());
        ret.append(",");
        ret.append(loc.getLongitude());
        ret.append("+");
        try {
            ret.append(URLEncoder.encode("("+name+")", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            ret.append("no name");
        }
        //ret.append(")");
    }

    private String marker2CVSString(OverlayItem marker, int format) {
        
        String content = PreferenceManager.getDefaultSharedPreferences(this).getString(Setup.PREF_INCLUDE_LINK_SHARE, Setup.LL_AND_LINK);
        boolean both = Setup.LL_AND_LINK.equals(content);
        
        //Log.d("MeetingPoint", "pref = "+content+", both = "+both);
        
        StringBuilder ret = new StringBuilder(256);
        
        ret.append("");
        Location loc = marker2Location(marker);

        if (both || Setup.ONLY_LL.equals(content)) {
            //workaround for http://code.google.com/p/android/issues/detail?id=5734
            Locale oldDef = Locale.getDefault();
            Locale.setDefault(Locale.US);
            String lat = Location.convert(loc.getLatitude(), format);
            String lon = Location.convert(loc.getLongitude(), format);
            Locale.setDefault(oldDef);
            String snippet = marker.getSnippet();
            boolean hasSnippet = (snippet != null && snippet.length() > 0); 
            ret.append(lat);
            ret.append(",");
            ret.append(lon);
            ret.append(",\"");
            ret.append(marker.getTitle());
            ret.append("\"");
            
            if (hasSnippet) {
                ret.append(",\"");
                ret.append(marker.getSnippet());
                ret.append("\"");
            }
        }
        if (both || Setup.ONLY_LINK.equals(content)) {
            if (both)
                ret.append(",\"");
            addURL(ret, loc, marker.getTitle());
            if (both)
                ret.append("\"");
            //ret.append(" \"geo:").append(loc.getLatitude()).append(",").append(loc.getLongitude()).append("\"");
        }
        return ret.toString();
    }
    
    private String marker2VerboseString(String prefix, OverlayItem marker, int format) {
        String content = PreferenceManager.getDefaultSharedPreferences(this).getString(Setup.PREF_INCLUDE_LINK_SAVE, Setup.ONLY_LL);
        StringBuilder ret = new StringBuilder(256);
        if (prefix != null) {
            ret.append(prefix);
        }
        ret.append(marker.getTitle());
        Location loc = marker2Location(marker);
        
        boolean both = Setup.LL_AND_LINK.equals(content); 
        
        if (both || Setup.ONLY_LL.equals(content)) {
            //workaround for http://code.google.com/p/android/issues/detail?id=5734
            Locale oldDef = Locale.getDefault();
            Locale.setDefault(Locale.US);
            String lat = Location.convert(loc.getLatitude(), format);
            String lon = Location.convert(loc.getLongitude(), format);
            Locale.setDefault(oldDef);
            ret.append("\nLatitude: ");
            ret.append(lat);
            ret.append("\nLongitude: ");
            ret.append(lon);
            //ret.append("\ngeo:").append(loc.getLatitude()).append(",").append(loc.getLongitude());
        }
        if (both || Setup.ONLY_LINK.equals(content)) {
            ret.append("\n");
            addURL(ret, loc, marker.getTitle());
        }
        String desc = marker.getSnippet();
        if (desc != null && desc.length() > 0) {
            ret.append("\ndesc: ");
            ret.append(desc);
        }
        return ret.toString();
    }
    
    private int string2Format(String s) {
        if (Setup.FORMAT_DEGREES.equals(s)) {
            return Location.FORMAT_DEGREES;
        }
        if (Setup.FORMAT_MINUTES.equals(s)) {
            return Location.FORMAT_MINUTES;
        }
        if (Setup.FORMAT_SECONDS.equals(s)) {
            return Location.FORMAT_SECONDS;
        }
        return Location.FORMAT_DEGREES;
    }
    
    private void share() {
        String form = PreferenceManager.getDefaultSharedPreferences(this).getString(Setup.PREF_LONLAT_FORMAT_SHARE, null);
        OverlayItem marker = markersOverlay.createItem(currentMarkerId);
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, marker.getTitle());
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, marker2CVSString(marker, string2Format(form)));
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_with_title)));    
    }

    private void shareAll() {

        int s = markersOverlay.size();
        if (s == 0)
            return;
        
        String form = PreferenceManager.getDefaultSharedPreferences(this).getString(Setup.PREF_LONLAT_FORMAT_SHARE, null);
        int iform = string2Format(form);
        StringBuilder bldr = new StringBuilder(s * 256);
        OverlayItem marker = markersOverlay.createItem(0);

        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, marker.getTitle());

        for (int i=0; i<s; i++) {
            marker = markersOverlay.createItem(i);
            bldr.append(marker2CVSString(marker, iform));
            bldr.append("\n");
        }
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, bldr.toString());
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_with_title)));    
    }
    
    private void runExternal() {
        OverlayItem marker = markersOverlay.createItem(currentMarkerId);
        Location loc = marker2Location(marker);
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:"+loc.getLatitude()+","+loc.getLongitude()));
        startActivity(i);
    }
    
    private void quickSave() {
        String form = PreferenceManager.getDefaultSharedPreferences(this).getString(Setup.PREF_LONLAT_FORMAT_SAVE, null);
        OverlayItem marker = markersOverlay.createItem(currentMarkerId);
        Location loc = marker2Location(marker);
        notesIntent.createNote(marker.getTitle(), marker2VerboseString("#MeetingPoint\n", marker, string2Format(form)), loc, true);
    }
    
    private void quickSaveAll() {

        int s = markersOverlay.size();
        if (s == 0)
            return;

        String form = PreferenceManager.getDefaultSharedPreferences(this).getString(Setup.PREF_LONLAT_FORMAT_SAVE, null);
        int iform = string2Format(form);
        StringBuilder bldr = new StringBuilder(s * 256);
        OverlayItem marker = markersOverlay.createItem(0);
        Location loc = marker2Location(marker);
        String title = marker.getTitle();
        bldr.append("#MeetingPoint\n");
        for (int i=0; i<s; i++) {
            marker = markersOverlay.createItem(i);
            bldr.append(marker2VerboseString(null, marker, iform));
            bldr.append("\n");
        }
        notesIntent.createNote(title, bldr.toString(), loc, true);
    }
    
    private void startPrefs() {
        Intent stratPrefs = new Intent("pl.skifo.meetingpoint.action.PREFS");
        stratPrefs.setClassName("pl.skifo.meetingpoint", "pl.skifo.meetingpoint.Setup");
        startActivity(stratPrefs);
    }    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_options, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.m_setup:
                startPrefs();
                break;
            case R.id.m_about:
                showDialog(DIALOG_ID__INFO);
                break;
            case R.id.m_save:
                quickSaveAll();
                break;
            case R.id.m_share:
                shareAll();
                break;

        }
        return false;
    }


    private EditDialogHandler editHandler;
    
    private class MainDialogListener implements DialogInterface.OnClickListener {

        private int ids[];
        
        public MainDialogListener (int i[]) {
            ids = i;
        }
        
        public void onClick(DialogInterface d, int item) {
            if (currentMarkerId >= 0) {
                int id = ids[item];
                switch (id) {
                    case R.string.share:
                        share();
                        break;
                    case R.string.save:
                        quickSave();
                        break;
                    case R.string.edit:
                        showDialog(DIALOG_ID__ON_EDIT);
                        editHandler.update(currentMarkerId);
                        break;
                    case R.string.delete: {
                        markersOverlay.removeMarker(currentMarkerId);
                        mapView.invalidate();
                        break;
                    }
                    case R.string.openExt: {
                        runExternal();
                        break;
                    }
                }
            }
        }
    }
    
    private class EditDialogHandler implements DialogInterface.OnClickListener {

        private Dialog parent;
        private OverlayItem marker = null;
        
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                Dialog d = (Dialog)dialog;
                EditText name = (EditText) d.findViewById(R.id.marker_edit_name);
                EditText snippet = (EditText) d.findViewById(R.id.marker_edit_snippet);
                if (marker != null) {
                    markersOverlay.replaceAtIndex(new OverlayItem(marker.getPoint(), name.getText().toString(), snippet.getText().toString()), currentMarkerId);
                    mapView.invalidate();
                }
            }
            else if (which == DialogInterface.BUTTON_NEGATIVE) {
                dialog.cancel();
            }
        }

        public void update(int currentMarkerId) {
            marker = markersOverlay.createItem(currentMarkerId);
            EditText name = (EditText) parent.findViewById(R.id.marker_edit_name);
            EditText snippet = (EditText) parent.findViewById(R.id.marker_edit_snippet);
            String n = marker.getTitle();
            name.setText(n);
            if (n.length() > 0)
                name.setSelection(n.length());
            snippet.setText(marker.getSnippet());            
        }

        public void setParent(Dialog p) {
            parent = p;
        }
    }

    private class LonLatDialogHandler implements DialogInterface.OnClickListener, 
                                                 DialogInterface.OnShowListener,
                                                 RadioGroup.OnCheckedChangeListener, OnClickListener {
        
        private Dialog parent;
        private EditText latText;
        private EditText lonText;

        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                
                
                String lat = (latText.getText()).toString();
                String lon = (lonText.getText()).toString();
                Log.d("MeetingPoint", "lon = "+lon+", lat = "+lat);

                Locale oldDef = Locale.getDefault();
                Locale.setDefault(Locale.US);
                
                boolean latOK = false;
                boolean lonOK = false;
                double latD = 0, lonD = 0;
                
                if (lat != null && lat.length() > 0) {
                    try {
                        latD = Location.convert(lat);
                        latOK = true;
                    }
                    catch (Exception ignore) {
                    }
                }
                
                if (lon != null && lon.length() > 0) {
                    try {
                        lonD = Location.convert(lon);
                        lonOK = true;
                    }
                    catch (Exception ignore) {
                    }
                }
                Locale.setDefault(oldDef);
                if (latOK && lonOK)
                    jumpToLocation(latD, lonD);
                
            }
            else if (which == DialogInterface.BUTTON_NEGATIVE) {
                dialog.cancel();
            }
        }

        public void setParent(Dialog p) {
            parent = p;
        }


//        private int getCheckId() {
//            RadioGroup rg = (RadioGroup) parent.findViewById(R.id.coords_format_group);
//            int checkedId = rg.getCheckedRadioButtonId();
//            if (checkedId == -1) {
//                rg.check(R.id.coords_format_minutes);
//                checkedId = R.id.coords_format_minutes;
//            }
//            return checkedId;
//        }

        private int getRequestedFormat() {
            int requestedFormat = Location.FORMAT_MINUTES;
            RadioGroup rg = (RadioGroup) parent.findViewById(R.id.coords_format_group);
            int checkedId = rg.getCheckedRadioButtonId();
            if (checkedId == -1) {
                rg.check(R.id.coords_format_minutes);
                checkedId = R.id.coords_format_minutes;
            }
            if (checkedId == R.id.coords_format_degrees) {
                requestedFormat = Location.FORMAT_DEGREES;
            }
            return requestedFormat;
        }
        
        
        private void setHint(int requestedFormat) {
            int w = mapView.getWidth();
            int h = mapView.getHeight();
            GeoPoint gp = getGeoPoint(w/2, h/2);
            
            Locale oldDef = Locale.getDefault();
            Locale.setDefault(Locale.US);
            
            latText.setHint(Location.convert((double)(gp.getLatitudeE6())/1000000, requestedFormat));
            lonText.setHint(Location.convert((double)(gp.getLongitudeE6())/1000000, requestedFormat));
            
            Locale.setDefault(oldDef);
        }

        private void grabPosition(int requestedFormat) {
            int w = mapView.getWidth();
            int h = mapView.getHeight();
            GeoPoint gp = getGeoPoint(w/2, h/2);
            
            Locale oldDef = Locale.getDefault();
            Locale.setDefault(Locale.US);
            
            latText.setText(Location.convert((double)(gp.getLatitudeE6())/1000000, requestedFormat));
            lonText.setText(Location.convert((double)(gp.getLongitudeE6())/1000000, requestedFormat));
            
            Locale.setDefault(oldDef);
        }
        
        private String parseLat(int format, String lat) {

            if (format == Location.FORMAT_DEGREES) {
            }
            return lat;
        }
        
        
        @Override
        public void onShow(DialogInterface dialog) {
            // TODO Auto-generated method stub
            Log.d("MeetingPoint", "LonLat onShow");
            
            latText = (EditText) parent.findViewById(R.id.enter_latitude);
            lonText = (EditText) parent.findViewById(R.id.enter_longitude);
            
            latText.setText("");
            lonText.setText("");
            
            RadioGroup rg = (RadioGroup) parent.findViewById(R.id.coords_format_group);
            setHint(getRequestedFormat());
            rg.setOnCheckedChangeListener(this);
            
            Button getCurrent = (Button) parent.findViewById(R.id.get_current);
            getCurrent.setOnClickListener(this);
        }

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            Log.d("MeetingPoint", "onCheckedChanged = "+((checkedId == R.id.coords_format_degrees)?"degrees":"minutes"));
            setHint((checkedId == R.id.coords_format_degrees)?Location.FORMAT_DEGREES:Location.FORMAT_MINUTES);
            String lat = (latText.getText()).toString();
            String lon = (lonText.getText()).toString();
            Log.d("MeetingPoint", "onCheckedChanged lat = <"+lat+">, lon = <"+lon+">");
            
            int requestedFormat = (checkedId == R.id.coords_format_degrees) ? Location.FORMAT_DEGREES : Location.FORMAT_MINUTES;
            
            Locale oldDef = Locale.getDefault();
            Locale.setDefault(Locale.US);
            
            if (lat != null && lat.length() > 0) {
                try {
                    double latD = Location.convert(lat);
                    latText.setText(Location.convert(latD, requestedFormat));
                }
                catch (Exception ignore) {
                    ignore.printStackTrace();
                    latText.setText("");
                }
            }
            
            if (lon != null && lon.length() > 0) {
                try {
                    double lonD = Location.convert(lon);
                    lonText.setText(Location.convert(lonD, requestedFormat));
                }
                catch (Exception ignore) {
                    ignore.printStackTrace();
                    lonText.setText("");
                }
            }
            
            Locale.setDefault(oldDef);
        }

        @Override
        public void onClick(View v) {
            grabPosition(getRequestedFormat());
        }
    }
    
    
    private static class SearchListAdapter extends ArrayAdapter<String> implements android.content.DialogInterface.OnClickListener {

        private Address[] addr;
        private MeetingPoint parent;
        
        public SearchListAdapter(MeetingPoint context) {
            super(context, android.R.layout.select_dialog_item, android.R.id.text1);
            parent = context;
        }
        
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which > addr.length || which < 0)
                return;

            Address a = addr[which];
            
            if (!a.hasLatitude() || !a.hasLongitude())
                return;
            
            MapController ctrl = parent.mapView.getController();
            double latE6 = a.getLatitude() * 1000000;
            double lonE6 = a.getLongitude() * 1000000;
            ctrl.animateTo(new GeoPoint((int)latE6, (int)lonE6));
        }
        
        public void updateDataStore(String[] dStore, Address[] a) {
            addr = a;
            clear();
            for (int i=0; i < dStore.length; i++) {
                add(dStore[i]);
            }
            notifyDataSetChanged();
        }
        
    }
    
    
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog ret = null;
        switch (id) {
            case DIALOG_ID__ON_TAP: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                
                int[] ids = new int[5];
                ids[0] = R.string.share;
                ids[1] = R.string.save;
                ids[2] = R.string.edit;
                ids[3] = R.string.delete;
                ids[4] = R.string.openExt;
                
                CharSequence[] items = new CharSequence[ids.length];
                for (int i=0; i<ids.length; i++) {
                    items[i] = getResources().getText(ids[i]);
                }
                builder.setItems(items, new MainDialogListener(ids));
                ret = builder.create();                
                //Log.d("MeetingPoint", "adapter = "+((AlertDialog)ret).getListView().getAdapter());
            } break;
            case DIALOG_ID__ON_EDIT: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                editHandler = new EditDialogHandler(); 
                builder.setTitle(R.string.edit);
                LayoutInflater inflater = getLayoutInflater();
                builder.setView(inflater.inflate(R.layout.edit_marker_dialog, null));
                builder.setPositiveButton(R.string.ok, editHandler);
                builder.setNegativeButton(R.string.cancel, editHandler);
                ret = builder.create();
                editHandler.setParent(ret);
            } break;

            case DIALOG_ID__INFO: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this); 
                LayoutInflater inflater = getLayoutInflater();
                View about = inflater.inflate(R.layout.info_dialog, null);
                builder.setView(about);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((Dialog)dialog).dismiss();
                    }
                });
                ret = builder.create(); 
            } break;
            case DIALOG_ID__SEARCH_RESULT: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setAdapter(searchAdapter, searchAdapter);
                builder.setTitle(R.string.search_result_title);
                ret = builder.create();
                //Log.d("MeetingPoint", "adapter = "+((AlertDialog)ret).getListView().getAdapter());
            } break;

            case DIALOG_ID__LONLAT: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                LonLatDialogHandler h = new LonLatDialogHandler(); 
                builder.setTitle(R.string.enter_coords);
                LayoutInflater inflater = getLayoutInflater();
                builder.setView(inflater.inflate(R.layout.enter_ll_dialog, null));
                builder.setPositiveButton(R.string.ok, h);
                builder.setNegativeButton(R.string.cancel, h);
                ret = builder.create();
                h.setParent(ret);
                ret.setOnShowListener(h);
            } break;
        }
        return ret;
    }

    
    

    @Override
    public void doAction(String action) {

        if (action == ACTION_SEARCH) {
            onSearchRequested();
            return;
        }
        
        int s = markersOverlay.size();
        if (s == 0)
            return;
        boolean handled = false;
        
        //Log.d("MeetingPoint", "doAction<"+action+">, size = "+s+", curr idx = "+currentMarkerId);
        
        if (action == ACTION_NEXT) {
            currentMarkerId++;
            if (currentMarkerId >= s)
                currentMarkerId = 0;
            if (currentMarkerId < 0) // just for case
                currentMarkerId = 0;
            handled = true;
        }
        else if (action == ACTION_PREV) {
            currentMarkerId--;
            if (currentMarkerId < 0)
                currentMarkerId = s - 1;
            handled = true;
        }
        
        if (handled) {
            OverlayItem marker = markersOverlay.createItem(currentMarkerId);
            GeoPoint gp = marker.getPoint();
            
            //Log.d("MeetingPoint", "doAction, animating["+currentMarkerId+"] <"+gp+">");
            
            MapController ctrl = mapView.getController();
            ctrl.animateTo(gp);
        }
    }
    
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private Location geoURI2Location(String geoURI) {
        Location ret = null;
        if (geoURI != null && geoURI.startsWith("geo:")) {
            String path = geoURI.substring("geo:".length());
            if (path.length() > 0) {
                int idx0 = path.indexOf(',');
                if (idx0 > 0) {
                    String lat = path.substring(0, idx0);
                    int idx1 = path.indexOf(',', idx0 + 1);
                    Log.d("MeetingPoint", "geoURI2Location idx of , = "+idx1);
                    int idx2 = path.indexOf(';', idx0 + 1);
                    Log.d("MeetingPoint", "geoURI2Location idx of ; = "+idx2);
                    if (idx2 != -1 && idx2 < idx1)
                        idx1 = idx2;
                    idx2 = path.indexOf('?', idx0 + 1);
                    Log.d("MeetingPoint", "geoURI2Location idx of ? = "+idx2);
                    if (idx2 != -1 && idx2 < idx1)
                        idx1 = idx2;
                    
                    String lon;
                    if (idx1 == -1) {
                        lon = path.substring(idx0 + 1);
                    }
                    else {
                        lon = path.substring(idx0 + 1, idx1);
                    }
                    if (lon.length() > 0) {
                        Log.d("MeetingPoint", "geoURI2Location lat = <"+lat+">, lon = <"+lon+">");
                        ret = new Location("");
                        try {
                            ret.setLatitude(Double.parseDouble(lat));
                            ret.setLongitude(Double.parseDouble(lon));
                        }
                        catch (Exception e) {
                            ret = null;
                        }
                    }
                }
            }
        }
        return ret;
    }
    
    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
          String query = intent.getStringExtra(SearchManager.QUERY);
          doSearch(query);
        }
        else {
            Log.d("MeetingPoint", "new Intent <"+intent+">");
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                Log.d("MeetingPoint", "Intent data <"+intent.getDataString()+">");
                 Location loc = geoURI2Location(intent.getDataString());
                 if (loc != null)
                     jumpToLocation(loc.getLatitude(), loc.getLongitude());
            }
        }
    }

    
    private static class SearchQuery implements Runnable {

        
        private Geocoder gcoder;
        private MeetingPoint parent;
        private String query;
        private ProgressDialog dialog;
        
        public SearchQuery(MeetingPoint parent, Geocoder gc, String q, ProgressDialog dialog) {
            gcoder = gc;
            this.parent = parent;
            query = q;
            this.dialog = dialog;
        }
        
        @Override
        public void run() {
            List<Address> ret = null;
            try {
                ret = gcoder.getFromLocationName(query, 16);
            } catch (IOException e) {e.printStackTrace();}
            
            dialog.dismiss();
            
            int size = 0;
            Address aTmp[] = null; 
            
            if (ret != null) {
                int s2 = ret.size();
                ArrayList<Address> aList = new ArrayList<Address>(s2);
                for (int i = 0; i < s2; i++) {
                    Address a = ret.get(i);
                    if (a.hasLatitude() && a.hasLongitude()) {
                        aList.add(a);
                    }
                }
                aTmp = aList.toArray(new Address[aList.size()]);
                size = aTmp.length; 
            }
            
            if (size == 0) {
                parent.runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        Toast.makeText(parent,
                                R.string.search_result_notfound,
                                Toast.LENGTH_SHORT).show(); 
                    }});
                Log.d("MeetingPoint", "NOT FOUND");
            }
            else if (size == 1) {
                Address a = aTmp[0];
                if (a.hasLatitude() && a.hasLongitude()) {
                    final double lat = a.getLatitude();
                    final double lon = a.getLongitude();
                    parent.runOnUiThread(new Runnable(){
                        @Override
                        public void run() {
                            parent.jumpToLocation(lat, lon);
                        }});
                }
            }
            else {
                
                final String dStore[] = new String[size];
                final Address[] a = aTmp;
                
                for (int i = 0; i < size; i++) {
                    StringBuilder sb = new StringBuilder(128);
                    String locality = a[i].getLocality();
                    String aarea = a[i].getAdminArea();
                    String sarea = a[i].getSubAdminArea();
                    String country = a[i].getCountryName();
                    boolean hasPrev = false;
                    if (locality != null) {
                        sb.append(locality);
                        hasPrev = true;
                    }
                    if (sarea != null) {
                        if (hasPrev)
                            sb.append(", ");
                        sb.append(sarea);
                        hasPrev = true;
                    }
                    if (aarea != null) {
                        if (hasPrev)
                            sb.append(", ");
                        sb.append(aarea);
                        hasPrev = true;
                    }
                    if (country != null) {
                        if (hasPrev)
                            sb.append(", ");
                        sb.append(country);
                    }
                    dStore[i] = sb.toString();
                }
                
                parent.runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        parent.searchAdapter.updateDataStore(dStore, a);
                        parent.showDialog(DIALOG_ID__SEARCH_RESULT);
                    }});
            }
        }
        
    }

    private void doSearch(String query) {
        Log.d("MeetingPoint", "search for query <"+query+">");

        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mapView.getWindowToken(), 0);        
        
        ProgressDialog d = ProgressDialog.show(this, "", getResources().getString(R.string.searchin), true); 
        Thread t = new Thread(new SearchQuery(this, gcoder, query, d));
        t.start();
    }    
    
    public void jumpToLocation(double latitude, double longitude) {
        MapController ctrl = mapView.getController();
        double latE6 = latitude * 1000000;
        double lonE6 = longitude * 1000000;
        ctrl.animateTo(new GeoPoint((int)latE6, (int)lonE6));
    }
    
}