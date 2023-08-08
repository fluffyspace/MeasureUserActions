package eu.kodba.measureuseractions;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import static android.content.Context.WINDOW_SERVICE;

interface DialogInterface{
    void buttonClicked();
}

public class Window {

    // declaring required variables
    private Context context;
    private View mView;
    private WindowManager.LayoutParams mParams;
    private WindowManager mWindowManager;
    private LayoutInflater layoutInflater;

    private Button button;
    private TextView vrijeme;

    public Window(Context context, DialogInterface dialogInterface){
        this.context=context;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // set the layout parameters of the window
            mParams = new WindowManager.LayoutParams(
                    // Shrink the window to wrap the content rather
                    // than filling the screen
                    WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                    // Display it on top of other application windows
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    // Don't let it grab the input focus
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    // Make the underlying application window visible
                    // through any transparent parts
                    PixelFormat.TRANSLUCENT);
        }
        // getting a LayoutInflater
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // inflating the view with the custom layout we created
        mView = layoutInflater.inflate(R.layout.popup_window, null);
        // set onClickListener on the remove button, which removes
        // the view from the window
        button = mView.findViewById(R.id.window_close);
        vrijeme = mView.findViewById(R.id.vrijeme);
        button.findViewById(R.id.window_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogInterface.buttonClicked();
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                //close();
            }
        });
        // Define the position of the
        // window within the screen
        mParams.gravity = Gravity.END|Gravity.TOP;
        mWindowManager = (WindowManager)context.getSystemService(WINDOW_SERVICE);
    }

    public void setButtonText(String text) {
        this.button.setText(text);
    }
    public void setVrijemeText(String text) {
        this.vrijeme.setText(text);
    }

    public void open() {
        try {
            // check if the view is already
            // inflated or present in the window
            if(mView.getWindowToken()==null) {
                if(mView.getParent()==null) {
                    mWindowManager.addView(mView, mParams);
                }
            }
        } catch (Exception e) {
            Log.d("Error1",e.toString());
        }
    }

    public void close() {
        try {
            // remove the view from the window
            ((WindowManager)context.getSystemService(WINDOW_SERVICE)).removeView(mView);
            // invalidate the view
            mView.invalidate();
            // remove all views
            ((ViewGroup)mView.getParent()).removeAllViews();
            // the above steps are necessary when you are adding and removing
            // the view simultaneously, it might give some exceptions
        } catch (Exception e) {
            Log.d("Error2",e.toString());
        }
    }
}