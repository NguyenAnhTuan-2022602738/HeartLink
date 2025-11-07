package vn.haui.heartlink.activities;

import vn.haui.heartlink.R;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class OtpVerificationActivity extends AppCompatActivity {

    private EditText[] otpFields;
    private int currentOtpFieldIndex = 0;
    private ImageView numpadActionButton;

    private TextView timerText;
    private ProgressBar progressBar;
    private CountDownTimer countDownTimer;
    private static final long START_TIME_IN_MILLIS = 42000; // 42 seconds as per design

    /**
     * Phương thức khởi tạo activity xác minh OTP.
     * Thiết lập giao diện và bắt đầu timer đếm ngược.
     *
     * @param savedInstanceState Trạng thái đã lưu của activity (có thể null)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        // Initialize Views
        timerText = findViewById(R.id.timer_text);
        progressBar = findViewById(R.id.progress_bar);
        numpadActionButton = findViewById(R.id.numpad_action_button);
        findViewById(R.id.back_button).setOnClickListener(v -> onBackPressed());

        otpFields = new EditText[]{
                findViewById(R.id.otp_box_1),
                findViewById(R.id.otp_box_2),
                findViewById(R.id.otp_box_3),
                findViewById(R.id.otp_box_4)
        };

        startTimer();
        updateOtpBoxUI(); // Set initial state
    }

    /**
     * Phương thức bắt đầu timer đếm ngược cho việc nhập OTP.
     */
    private void startTimer() {
        progressBar.setMax((int) (START_TIME_IN_MILLIS / 1000));
        countDownTimer = new CountDownTimer(START_TIME_IN_MILLIS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", secondsRemaining / 60, secondsRemaining % 60));
                progressBar.setProgress(progressBar.getMax() - (int) secondsRemaining);
            }

            @Override
            public void onFinish() {
                timerText.setText("00:00");
                progressBar.setProgress(progressBar.getMax());
            }
        }.start();
    }

    /**
     * Phương thức xử lý khi người dùng click vào numpad.
     *
     * @param view View được click
     */
    public void onNumpadClicked(View view) {
        if (view.getTag() == null) return;

        String tag = view.getTag().toString();
        if (tag.equals("backspace")) {
            handleBackspace();
        } else if (tag.equals("confirm")) {
            // Handle OTP confirmation
            Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to the next screen, e.g., ProfileInfoActivity
            // Intent intent = new Intent(OtpVerificationActivity.this, ProfileInfoActivity.class);
            // startActivity(intent);
        } else {
            handleNumberInput(tag);
        }
    }

    /**
     * Phương thức xử lý nhập số vào OTP.
     *
     * @param number Số được nhập
     */
    private void handleNumberInput(String number) {
        if (currentOtpFieldIndex < otpFields.length) {
            otpFields[currentOtpFieldIndex].setText(number);
            currentOtpFieldIndex++;
            updateOtpBoxUI();
        }
    }

    /**
     * Phương thức xử lý xóa ký tự cuối trong OTP.
     */
    private void handleBackspace() {
        if (currentOtpFieldIndex > 0) {
            currentOtpFieldIndex--;
            otpFields[currentOtpFieldIndex].setText("");
            updateOtpBoxUI();
        }
    }

    /**
     * Phương thức cập nhật giao diện các ô nhập OTP.
     */
    private void updateOtpBoxUI() {
        for (int i = 0; i < otpFields.length; i++) {
            EditText currentField = otpFields[i];
            if (i < currentOtpFieldIndex) {
                // State: Filled
                currentField.setBackgroundResource(R.drawable.otp_box_filled);
                currentField.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            } else if (i == currentOtpFieldIndex) {
                // State: Active (currently typing in)
                currentField.setBackgroundResource(R.drawable.otp_box_active);
                currentField.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            } else {
                // State: Inactive
                currentField.setBackgroundResource(R.drawable.otp_box_inactive);
                currentField.setTextColor(ContextCompat.getColor(this, R.color.textColorSecondary));
            }
        }

        // Update the numpad action button
        if (currentOtpFieldIndex == otpFields.length) {
            numpadActionButton.setImageResource(R.drawable.ic_check);
            numpadActionButton.setTag("confirm");
        } else {
            numpadActionButton.setImageResource(R.drawable.ic_backspace);
            numpadActionButton.setTag("backspace");
        }
    }

    /**
     * Phương thức được gọi khi activity bị hủy.
     * Hủy timer để tránh memory leak.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
