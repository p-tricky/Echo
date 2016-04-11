package me.loftythoughts.phloftus.echo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.v4.util.Pair;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Stack;

/**
 * Created by patrickloftus on 2/26/16.
 *
 * This view host all of the drawing logic in the UploaderFragment.
 *
 * The drawing touch events are handled in this view.
 *
 * Additionally, color selection and other other widget click events are
 * sent and ultimately resolved by this view. in to this view from the EchoEditIcons view.
 *
 */
public class DrawingView extends View {

    private Bitmap mEcho;  // The bitmap that was just captured by the EchoCameraFragment
    private Paint mPaint;  // The current paint being used. Paint object defines everything from
                           // color to line width, anti-aliasing, etc.
    private Path mScribble;    // Paths store the set of points that make up scribbles
    private Stack<Pair<Path,Paint>> mScribbles;  // History of scribbles (i.e., paths) and their colors.
    private Integer mColor;

    // constructor -- consider deleting unused constructors
    public DrawingView(Context context) {
        super(context);
        initView();
    }

    // Constructor
    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    // Constructor
    public DrawingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(5);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mScribble = new Path();
        mScribbles = new Stack<>();
    }


    public void setEcho(Bitmap b) {
        mEcho = b;
    }

    // Draw the current view (i.e., the original Echo picture and the scribbles) on a new
    // bitmap, and return the new bitmap.
    public Bitmap getEcho() {
        Bitmap b = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        draw(c);
        return b;
    }

    // Called everytime the view needs to be refreshed.
    public void onDraw(Canvas c) {
        // If an Echo picture has been defined, and it definitely should have been,
        // then draw the picture.
        if (mEcho != null) {
            Rect dest = new Rect(0, 0, getWidth(), getHeight());
            c.drawBitmap(mEcho, null, dest, new Paint());
        }
        // For each past scribble, trace it with its corresponding paint.
        for (Pair<Path, Paint> path : mScribbles)
            c.drawPath(path.first, path.second);
        // Trace the current scrible with its paint
        c.drawPath(mScribble, mPaint);
    }

    // This method is called when the user touches the canvas.
    // The method makes the appropriate changes to scribbles
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mColor != null) {
            // touch event location
            float pointX = event.getX();
            float pointY = event.getY();
            // Checks for the event that occurs
            switch (event.getAction()) {
                // screen touch
                case MotionEvent.ACTION_DOWN:
                    // Starts a new line in the path
                    mScribble.moveTo(pointX, pointY);
                    break;
                // screen swipe/drag
                case MotionEvent.ACTION_MOVE:
                    // Draws line between last point and this point
                    mScribble.lineTo(pointX, pointY);
                    break;
                default:
                    return false;
            }

            postInvalidate(); // Indicate view should be redrawn
        }
        return true; // Indicate we've consumed the touch
    }

    // This function is called by the UploaderFragment when a new color
    // is selected in the EchoEditIcons.
    // It pushes the current scribble on to the stack and starts a new empty
    // scribble with the newly selected color.
    public void onColorChanged(int color) {
        if (mColor != null)
            mScribbles.push(new Pair(mScribble, mPaint));
        mColor = color;
        mScribble = new Path();
        mPaint = new Paint(mPaint);
        mPaint.setColor(mColor);
    }

    // This function is called by the UploaderFragment when the undo signal is sent from
    // the EchoEditIcons.
    // The integer returned by this function will define the background of the EchoEditIcons.
    public Integer onUndo() {
        // If no scribbles have been saved, then the user has only drawn in one color.
        // Delete all the scribbles in that color and reset to start state.
        if (mScribbles.isEmpty()) {
            mColor = null;
            mScribble = new Path();
            mPaint = new Paint(mPaint);
            invalidate();
            return Color.WHITE; // This is the default color when no color is selected.
        }
        // We've changed colors before, so delete scribbles in current color and
        // reset color to previous.
        else {
            Pair previous = mScribbles.pop();
            mScribble = (Path) previous.first;
            mPaint = (Paint) previous.second;
            invalidate();
            return mPaint.getColor();
        }
    }
}
