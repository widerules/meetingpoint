/**
 * 
 */
package pl.skifo.meetingpoint;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

/**
 * @author master
 *
 */
public class MarkersOverlay extends ItemizedOverlay {

    private static final int FONT_SIZE = 24;
    private static final int TITLE_MARGIN = 10;    
    
    private MeetingPoint ctx;
    private ArrayList<OverlayItem> markersList = new ArrayList<OverlayItem>();
    
    private int markerHeight;
    private TextPaint paintText = new TextPaint();
    
    /**
     * @param defaultMarker
     */
    public MarkersOverlay(MeetingPoint c, Drawable defaultMarker) {
        super(boundCenterBottom(defaultMarker));
        markerHeight = ((BitmapDrawable) defaultMarker).getBitmap().getHeight();
        ctx = c;
        
        paintText.setTextSize(FONT_SIZE);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(FONT_SIZE);
        paintText.setARGB(255, 255, 255, 255);
        int flags = paintText.getFlags();
        flags |= Paint.ANTI_ALIAS_FLAG;
        flags |= Paint.DEV_KERN_TEXT_FLAG;
        flags |= Paint.DITHER_FLAG;
        flags |= Paint.SUBPIXEL_TEXT_FLAG;
        paintText.setFlags(flags);
        paintText.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        
        populate();
    }

    @Override
    protected OverlayItem createItem(int index) {
//        Log.d("MeetingPoint","createItem "+index);
        return markersList.get(index);
    }
    
    public void replaceAtIndex(OverlayItem newItem, int indexOfMarkerToBeReplaced) {
        markersList.remove(indexOfMarkerToBeReplaced);
        markersList.add(indexOfMarkerToBeReplaced, newItem);
        populate();
    }

    @Override
    public int size() {
        //Log.d("MeetingPoint", "size update ret = "+markersList.size());
        return markersList.size();
    }

    public void addMarker(OverlayItem overlay) {
        markersList.add(overlay);
        populate();
    }
    
    public void removeMarker(int idx) {
        if (idx >= 0 && idx < markersList.size()) {
            markersList.remove(idx);
            setLastFocusedIndex(-1);
            populate();
        }
    }

    @Override
    protected boolean onTap(int index) {
      //Log.d("MeetingPoint", "ont tap @ idx = "+index);
      ctx.setCurrentMarker(index);
      ctx.showDialog(MeetingPoint.DIALOG_ID__ON_TAP);
      //Log.d("MeetingPoint", "ont tap done");
      return true;
    }
    
    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        super.draw(canvas, mapView, shadow);

        // go through all OverlayItems and draw title for each of them
        for (OverlayItem item:markersList) {
            GeoPoint point = item.getPoint();
            Point markerBottomCenterCoords = new Point();
            mapView.getProjection().toPixels(point, markerBottomCenterCoords);

            /* Find the width and height of the title*/
            Paint paintRect = new Paint();
            Rect rect = new Rect();
            
            paintText.getTextBounds(item.getTitle(), 0, item.getTitle().length(), rect);
            rect.inset(-TITLE_MARGIN, -TITLE_MARGIN);
            rect.offsetTo(markerBottomCenterCoords.x - rect.width()/2, markerBottomCenterCoords.y - markerHeight - rect.height());

            paintRect.setARGB(60, 0, 0, 0);
            canvas.drawRoundRect(new RectF(rect), 4, 4, paintRect);
            canvas.drawText(item.getTitle(), rect.left + rect.width() / 2, rect.bottom - TITLE_MARGIN, paintText);
        }
    }

    public void saveState(Bundle state) {
        
        int size = markersList.size();
        state.putInt("size", size);
        Log.d("MeetingPoint", "saving state, markers cnt = "+size+", stored "+state.getInt("size"));
        
        
        for (int i=0; i<size; i++) {
            OverlayItem marker = markersList.get(i);
            GeoPoint gp = marker.getPoint();
            state.putInt("m"+i+"_lat", gp.getLatitudeE6());
            state.putInt("m"+i+"_lon", gp.getLongitudeE6());
            state.putString("m"+i+"_title", marker.getTitle());
            state.putString("m"+i+"_snippet", marker.getSnippet());
        }
    }
    
    public void restoreState(Bundle state) {
        markersList.clear();
        int size = state.getInt("size");
        
        Log.d("MeetingPoint", "restoring state, markers cnt = "+size);
        
        for (int i=0; i<size; i++) {
            int lat = state.getInt("m"+i+"_lat");
            int lon = state.getInt("m"+i+"_lon");
            String title = state.getString("m"+i+"_title");
            String snippet = state.getString("m"+i+"_snippet");
            GeoPoint gp = new GeoPoint(lat, lon);
            OverlayItem marker = new OverlayItem(gp, title!=null?title:"marker "+i, snippet!=null?snippet:"");
            markersList.add(marker);
        }
        populate();
    }
    
//    public void setFocus(OverlayItem item) {
//        Log.d("MeetingPoint", "set focus to marker: "+item.getTitle());
//        super.setFocus(item);
//    }
    
}
