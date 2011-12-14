package pl.skifo.meetingpoint;

import android.content.Context;
import android.graphics.Canvas;
//import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;

import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class CrosshairsOverlay extends Overlay {

    private Drawable cross;
    

    private Drawable arrowLeft_noGlow, arrowLeft_Glow;
    private Drawable arrowRight_noGlow, arrowRight_Glow;
    
    private Drawable searchIcon;
    
    private Drawable crossOrange;
    private boolean isSecondary;

    private InputArea prev, next;
    private InputArea search;
    
    
    private int lastW, lastH;

    private ActionListener actionListener;
    
    
//    private Paint paint = new Paint();
    
    
    public CrosshairsOverlay(Context ctx) {
        cross = ctx.getResources().getDrawable(R.drawable.cross);
        crossOrange = ctx.getResources().getDrawable(R.drawable.cross_orange);
        
        arrowLeft_noGlow = ctx.getResources().getDrawable(R.drawable.ic_menu_back);
        arrowRight_noGlow = ctx.getResources().getDrawable(R.drawable.ic_menu_forward);
        
        arrowLeft_Glow = ctx.getResources().getDrawable(R.drawable.ic_menu_back_glow);
        arrowRight_Glow = ctx.getResources().getDrawable(R.drawable.ic_menu_forward_glow);
        
        searchIcon = ctx.getResources().getDrawable(R.drawable.search1);
        
        prev = new InputArea(MeetingPoint.ACTION_PREV);
        next = new InputArea(MeetingPoint.ACTION_NEXT);
        search = new InputArea(MeetingPoint.ACTION_SEARCH);
        
        lastW = -1;
        lastH = -1;
        isSecondary = false;
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        
        Drawable currentCross; 
        Drawable arrowLeft;
        Drawable arrowRight;
        
        if (isSecondary) {
            currentCross = crossOrange;
            arrowLeft = arrowLeft_Glow;
            arrowRight = arrowRight_Glow;
        }
        else {
            currentCross = cross;
            arrowLeft = arrowLeft_noGlow;
            arrowRight = arrowRight_noGlow;
        }

        int cw, ch;
        cw = currentCross.getIntrinsicWidth();
        ch = currentCross.getIntrinsicHeight();
        int h1 = mapView.getHeight();
        int w1 = mapView.getWidth();
        
        if (lastW != w1 || lastH != h1) {
            lastW = w1;
            lastH = h1;

            //Log.d("MeetingPoint","view changed <"+lastW+"x"+lastH+">");
            
            int areaW, areaH;
            areaW = arrowLeft.getIntrinsicWidth();
            areaH = arrowLeft.getIntrinsicHeight();
            prev.updateBounds(0.0f, 0.0f, ((float)areaW)/((float)lastW), ((float)areaH)/((float)lastH));
            arrowLeft.setBounds(0, 0, areaW, areaH);
            //Log.d("MeetingPoint","a left <"+areaW+"x"+areaH+">");
            
            areaW = arrowRight.getIntrinsicWidth();
            areaH = arrowRight.getIntrinsicHeight();
            //Log.d("MeetingPoint","a right <"+areaW+"x"+areaH+">");
            
            next.updateBounds(1.0f - ((float)areaW)/((float)lastW), 0.0f, ((float)areaW)/((float)lastW), ((float)areaH)/((float)lastH));
            arrowRight.setBounds((lastW -1) - areaW, 0, (lastW -1), areaH);

            areaW = searchIcon.getIntrinsicWidth();
            areaH = searchIcon.getIntrinsicHeight();
            
            search.updateBounds(1.0f - ((float)areaW)/((float)lastW), 1.0f - ((float)areaH)/((float)lastH), ((float)areaW)/((float)lastW), ((float)areaH)/((float)lastH));
            searchIcon.setBounds((lastW -1) - areaW, (lastH -1) - areaH, (lastW -1), (lastH -1));
        }
        
        currentCross.setBounds((w1 - cw)/2, (h1 - ch)/2, (w1 - cw)/2 + cw, (h1 - ch)/2 + ch);
        currentCross.draw(canvas);
        arrowLeft.draw(canvas);
        arrowRight.draw(canvas);
        searchIcon.draw(canvas);
        
        //Log.d("MeetingPoint","cw = "+cw+", ch = "+ch+", h = "+h+", w = "+w+", h1 = "+h1+", w1 = "+w1);
        
    }
    
    public void setSecondaryCross(boolean setSec) {
        isSecondary = setSec;
        lastW = -1;
        lastH = -1;
    }


    public void setActionListener(ActionListener listener) {
        actionListener = listener;
    }
    
    
    private static class InputArea {

        private float x, y, xPrim, yPrim;
        protected int pointerId = -1; // tracking pointer id
        private String actionName;

        public InputArea(String actionName) {
                this.actionName = actionName;
        }    
    
        public void updateBounds(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.xPrim = x + w;
            this.yPrim = y + h;
        }
        
        protected boolean checkRangeAndSetPointer(int pointer, float x2, float y2) {
            boolean ret = (x2 >= x && y2 >= y && x2 <= xPrim && y2 <= yPrim);
            
            if (ret) {
                //Log.d("MeetingPoint", x2+","+y2+" in range <"+x+", "+y+"> <"+xPrim+", "+yPrim+">");
                pointerId = pointer;
            }
            else {
                //Log.d("MeetingPoint", x2+","+y2+" outside range <"+x+", "+y+"> <"+xPrim+", "+yPrim+">");
            }
            return ret;
        }
    }
    
    
    @Override
    public boolean onTouchEvent(MotionEvent e, MapView mapView) {

        int rawAction = e.getAction();
        int action = e.getAction() & MotionEvent.ACTION_MASK;
        boolean ret = false;


        int w = mapView.getWidth();
        int h = mapView.getHeight();

        //int index = e.getActionIndex();
        int index = (rawAction & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
            int pointerId = e.getPointerId(index);
            if (action != MotionEvent.ACTION_MOVE) {
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {

                    //Log.d("MeetingPoint", "onTouchEvent = "+e);

                    float x = e.getX(pointerId)/w;
                    float y = e.getY(pointerId)/h;

                    if (prev.checkRangeAndSetPointer(pointerId, x, y)) {
                        //Log.d("MeetingPoint", "PREV");
                        if (actionListener != null)
                            actionListener.doAction(MeetingPoint.ACTION_PREV);
                        ret = true;
                    }
                    else if (next.checkRangeAndSetPointer(pointerId, x, y)) {
                        //Log.d("MeetingPoint", "NEXT");
                        if (actionListener != null)
                            actionListener.doAction(MeetingPoint.ACTION_NEXT);
                        ret = true;
                    }
                    else if (search.checkRangeAndSetPointer(pointerId, x, y)) {
                        //Log.d("MeetingPoint", "SEARCH");
                        if (actionListener != null)
                            actionListener.doAction(MeetingPoint.ACTION_SEARCH);
                        ret = true;
                    }
                }
            }
            else {
                float x = e.getX(pointerId)/w;
                float y = e.getY(pointerId)/h;
                ret = prev.checkRangeAndSetPointer(pointerId, x, y) || next.checkRangeAndSetPointer(pointerId, x, y); 
            }
            return ret;        
    }
    
}
