package me.loftythoughts.phloftus.echo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

/**
 * Created by Patrick on 3/19/2016.
 *
 * This class encapsulates all of the logic for the edit, color select, and undo icons
 * in the top left of the UploaderFragment
 *
 * If you want this view to affect the hosting fragment, have the hostint fragment implement
 * the EchoEditListener interface and pass the fragment to the setEchoEditListener method
 */
public class EchoEditIcons extends LinearLayout {
    // instance variables
    private boolean editing = false; // Keeps track of view state.
                                    // If in editting state, then the done button,
                                    // the color selector, and the undo button should be visible.
                                    // Otherwise, the edit button should be visible
    private ImageButton mToggleEditButton; // Start/stop editing
    private View mColorSelector;           // Vertical array of colors to select from
    private ImageButton mUndoButton;       // Send undo signal to host fragment
    private EchoEditListener mEchoEditListener;  // Listens for undo or new color selection, and
                                                // and makes appropriate change to host fragment
    private Integer mColor;

    // constructor
    public EchoEditIcons(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    // constructor
    public EchoEditIcons(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    // constructor
    public EchoEditIcons(Context context) {
        super(context);
        initView();
    }

    // Wire up the view.
    private void initView() {
        View view = inflate(getContext(), R.layout.view_echo_edit_icons, null); // Inflate from layout

        mToggleEditButton = (ImageButton) view.findViewById(R.id.toggle_edit); // Get button
        mToggleEditButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                editing = !editing;
                mEchoEditListener.editing(editing);
                if (editing) { // switched into edit mode so put UI in edit state
                    mToggleEditButton.setImageResource(R.drawable.ic_done_48dp);
                    mColorSelector.setVisibility(VISIBLE);
                    mUndoButton.setVisibility(VISIBLE);
                } else {  // switched out of edit mode, so set mToggleEditButton to edit icon and hide everything else
                    mToggleEditButton.setImageResource(R.drawable.ic_edit_48dp);
                    mColorSelector.setVisibility(INVISIBLE);
                    mUndoButton.setVisibility(INVISIBLE);
                }
            }
        });
        mToggleEditButton.setImageResource(R.drawable.ic_edit_48dp);

        mColorSelector = view.findViewById(R.id.color_selector);

        // Undo button should call undo method
        mUndoButton = (ImageButton) view.findViewById(R.id.undo);
        mUndoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                undo();
            }
        });

        // The color selector actually consists of 5 color blocks displayed vertically.
        // Each time any of the blocks is clicked, the color is
        view.findViewById(R.id.red).setOnClickListener(new View.OnClickListener(){
            public void onClick (View v){
                Integer c = ((ColorDrawable) v.getBackground()).getColor();
                if (mColor == null)
                    colorChanged(c);
                else if (!mColor.equals(c))
                    colorChanged(c);
            }
        });
        view.findViewById(R.id.green).setOnClickListener(new View.OnClickListener(){
            public void onClick (View v){
                Integer c = ((ColorDrawable) v.getBackground()).getColor();
                if (mColor == null)
                    colorChanged(c);
                else if (!mColor.equals(c))
                    colorChanged(c);
            }
        });
        view.findViewById(R.id.blue).setOnClickListener(new View.OnClickListener(){
            public void onClick (View v){
                Integer c = ((ColorDrawable) v.getBackground()).getColor();
                if (mColor == null)
                    colorChanged(c);
                else if (!mColor.equals(c))
                    colorChanged(c);
            }
        });
        view.findViewById(R.id.purple).setOnClickListener(new View.OnClickListener(){
            public void onClick (View v){
                Integer c = ((ColorDrawable) v.getBackground()).getColor();
                if (mColor == null)
                    colorChanged(c);
                else if (!mColor.equals(c))
                    colorChanged(c);
            }
        });
        view.findViewById(R.id.yellow).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Integer c = ((ColorDrawable) v.getBackground()).getColor();
                if (mColor == null)
                    colorChanged(c);
                else if (!mColor.equals(c))
                    colorChanged(c);
            }
        });
        addView(view);
    }

    public void setEchoEditListener(EchoEditListener listener) {
        mEchoEditListener = listener;
    }

    // Called when a user presses the undo button.
    // The listener rolls the paint color back to the previously selected
    // paint color, and erases any scribbles that were made in the current paint
    // color.
    // All of the undo logic can be found in the DrawingView
    public void undo() {
        Integer color;
        if (mEchoEditListener != null) {
            color = mEchoEditListener.onUndo();
            mColor = color;
        }
        else {
            color = Color.WHITE;
            mColor = null;
        }
        mToggleEditButton.setBackgroundColor(color);
        mToggleEditButton.getBackground().setAlpha(100);
        mUndoButton.setBackgroundColor(color);
        mUndoButton.getBackground().setAlpha(100);
    }

    // Called when a user selects a new color from the list.
    //
    // If a new color has been selected, then set the icon backgrounds
    // to the newly selected color, so that the user can tell what color
    // is currently selected.
    //
    // Should call the listener if there is one.
    // The call to the edit listener will change the color of
    // the paint in the hosting fragment.
    public void colorChanged(Integer c) {
        mToggleEditButton.setBackgroundColor(c);
        mToggleEditButton.getBackground().setAlpha(100);
        mUndoButton.setBackgroundColor(c);
        mUndoButton.getBackground().setAlpha(100);
        if (mEchoEditListener != null)
            mEchoEditListener.onColorChanged(c);
        mColor = c;
    }
}
