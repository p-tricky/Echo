package me.loftythoughts.phloftus.echo;

/**
 * Created by Patrick on 3/20/2016.
 *
 * Fosters communication between the EchoEditIcons view,
 * and the DrawingView
 *
 */
public interface EchoEditListener {
    void onColorChanged(int color);
    int onUndo();
    void editing(boolean isEditing);
}
