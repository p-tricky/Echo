package me.loftythoughts.phloftus.echo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import me.loftythoughts.phloftus.echo.R;


public class DescriptionFragment extends Fragment {

    private final int POSITION_IN_PAGER = 0;

    public DescriptionFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_description, container, false);
        TextView tv = (TextView) v.findViewById(R.id.description);
        SpannableStringBuilder descString = new SpannableStringBuilder();
        descString.append(
                "Hi!  Thanks for downloading Echo.  Echo is a pseudo-anonymous, location-based picture sharing app. " +
                        "Take a picture, scribble on it, and upload it as an Echo.  Once uploaded anyone in your immediate vicinity " +
                        " will be able to view your Echo. Use the map to find Echoes in your area.  Echoes have a limited range. " +
                        "When you enter an echo's range, it will change from  "
        );
        descString.setSpan(new ImageSpan(getActivity(), R.drawable.echo_icon_red), descString.length() - 1, descString.length(), 0);
        descString.append(" to  ");
        descString.setSpan(new ImageSpan(getActivity(), R.drawable.echo_icon), descString.length() - 1, descString.length(), 0);
        descString.append(
                ". Once in range, you can click the Echo to open it, but be warned, once opened, Echoes will disappear forever."
        );
        tv.setText(descString);
        tv.setMovementMethod(new ScrollingMovementMethod());
        return v;
    }

}
