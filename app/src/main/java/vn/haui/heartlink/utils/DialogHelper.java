package vn.haui.heartlink.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import vn.haui.heartlink.R;

public class DialogHelper {

    public static void showStatusDialog(Context context, String title, String message, boolean isSuccess, Runnable onDismiss) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_status, null);
        builder.setView(dialogView);

        ImageView statusIcon = dialogView.findViewById(R.id.status_icon);
        TextView statusTitle = dialogView.findViewById(R.id.status_title);
        TextView statusMessage = dialogView.findViewById(R.id.status_message);
        Button okButton = dialogView.findViewById(R.id.ok_button);

        statusTitle.setText(title);
        statusMessage.setText(message);

        if (isSuccess) {
            statusIcon.setImageResource(R.drawable.ic_success);
        } else {
            statusIcon.setImageResource(R.drawable.ic_error);
        }

        final AlertDialog dialog = builder.create();

        okButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (onDismiss != null) {
                onDismiss.run();
            }
        });

        dialog.show();
    }
}
