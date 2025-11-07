package vn.haui.heartlink.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import vn.haui.heartlink.R;
import vn.haui.heartlink.utils.UserRepository;

public class ProfileInfoActivity extends AppCompatActivity {

    private TextInputEditText lastNameInput;
    private TextInputEditText firstNameInput;
    private TextView birthdayButton;
    private String selectedBirthday = "";
    private Date selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_info);

        lastNameInput = findViewById(R.id.edit_text_lastname);
        firstNameInput = findViewById(R.id.edit_text_firstname);
        birthdayButton = findViewById(R.id.birthday_button);
        Button confirmButton = findViewById(R.id.confirm_button);
        TextView skipButton = findViewById(R.id.skip_button);

        birthdayButton.setOnClickListener(v -> showDatePicker());

        confirmButton.setOnClickListener(v -> saveProfileAndProceed());

        skipButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileInfoActivity.this, SeekingActivity.class);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.back_button_profile).setOnClickListener(v -> onBackPressed());
    }

    /**
     * Phương thức hiển thị dialog chọn ngày sinh với calendar view và year picker.
     * Cho phép người dùng chọn ngày sinh với ràng buộc tuổi tối thiểu 18.
     */
    private void showDatePicker() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.AppBottomSheetDialogTheme);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.dialog_date_picker, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        final CalendarView calendarView = bottomSheetView.findViewById(R.id.calendar_view);
        final RecyclerView yearRecyclerView = bottomSheetView.findViewById(R.id.year_recycler_view);
        final TextView selectedYearText = bottomSheetView.findViewById(R.id.selected_year);
        final TextView selectedMonthText = bottomSheetView.findViewById(R.id.selected_month);
        ImageView prevMonthButton = bottomSheetView.findViewById(R.id.prev_month_button);
        ImageView nextMonthButton = bottomSheetView.findViewById(R.id.next_month_button);
        Button saveButton = bottomSheetView.findViewById(R.id.save_button);

        final Calendar calendar = Calendar.getInstance();
        if (selectedDate != null) {
            calendar.setTime(selectedDate);
        } else {
            calendar.add(Calendar.YEAR, -25); // Default to 25 years ago
        }

        final Calendar maxDateCalendar = Calendar.getInstance();
        maxDateCalendar.add(Calendar.YEAR, -18);
        calendarView.setMaxDate(maxDateCalendar.getTimeInMillis());

        calendarView.setDate(calendar.getTimeInMillis(), true, true);
        updateDateHeader(calendar, selectedYearText, selectedMonthText);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
        });

        // Year Picker Logic
        selectedYearText.setOnClickListener(v -> {
            if (yearRecyclerView.getVisibility() == View.GONE) {
                calendarView.setVisibility(View.GONE);
                yearRecyclerView.setVisibility(View.VISIBLE);
            } else {
                calendarView.setVisibility(View.VISIBLE);
                yearRecyclerView.setVisibility(View.GONE);
            }
        });

        ArrayList<Integer> years = new ArrayList<>();
        int endYear = maxDateCalendar.get(Calendar.YEAR);
        int startYear = endYear - 100;
        for (int i = endYear; i >= startYear; i--) {
            years.add(i);
        }

        YearAdapter yearAdapter = new YearAdapter(years, selectedYear -> {
            calendar.set(Calendar.YEAR, selectedYear);
            calendarView.setDate(calendar.getTimeInMillis(), true, true);
            updateDateHeader(calendar, selectedYearText, selectedMonthText);
            calendarView.setVisibility(View.VISIBLE);
            yearRecyclerView.setVisibility(View.GONE);
        });

        yearRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        yearRecyclerView.setAdapter(yearAdapter);
        yearRecyclerView.scrollToPosition(years.indexOf(calendar.get(Calendar.YEAR)));


        // Month Navigation
        prevMonthButton.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            calendarView.setDate(calendar.getTimeInMillis(), true, true);
            updateDateHeader(calendar, selectedYearText, selectedMonthText);
        });

        nextMonthButton.setOnClickListener(v -> {
            Calendar tempCal = (Calendar) calendar.clone();
            tempCal.add(Calendar.MONTH, 1);
            if (tempCal.getTimeInMillis() <= maxDateCalendar.getTimeInMillis()) {
                calendar.add(Calendar.MONTH, 1);
                calendarView.setDate(calendar.getTimeInMillis(), true, true);
                updateDateHeader(calendar, selectedYearText, selectedMonthText);
            }
        });

        saveButton.setOnClickListener(v -> {
            selectedDate = calendar.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            selectedBirthday = sdf.format(selectedDate);
            birthdayButton.setText(selectedBirthday);
            birthdayButton.setTextColor(getResources().getColor(R.color.textColorPrimary));
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    /**
     * Phương thức cập nhật header hiển thị năm và tháng hiện tại trong date picker.
     *
     * @param calendar Đối tượng Calendar chứa thông tin ngày tháng
     * @param yearText TextView hiển thị năm
     * @param monthText TextView hiển thị tháng
     */
    private void updateDateHeader(Calendar calendar, TextView yearText, TextView monthText) {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.ENGLISH);
        yearText.setText(String.valueOf(calendar.get(Calendar.YEAR)));
        monthText.setText(monthFormat.format(calendar.getTime()));
    }

    /**
     * Phương thức lưu thông tin profile và chuyển sang activity tiếp theo.
     * Kiểm tra validation cho tên và ngày sinh, sau đó cập nhật lên Firebase.
     */
    private void saveProfileAndProceed() {
        String lastName = lastNameInput.getText().toString().trim();
        String firstName = firstNameInput.getText().toString().trim();

        if (TextUtils.isEmpty(firstName)) {
            Toast.makeText(this, "Vui lòng nhập tên của bạn", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(selectedBirthday)) {
            Toast.makeText(this, "Vui lòng chọn ngày sinh", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo tên đầy đủ
        String fullName = TextUtils.isEmpty(lastName) ? firstName : lastName + " " + firstName;

        // Cập nhật nhiều trường cùng lúc
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", fullName);
        updates.put("dateOfBirth", selectedBirthday);

        UserRepository.getInstance().updateUser(currentUser.getUid(), updates, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                Intent intent = new Intent(ProfileInfoActivity.this, SeekingActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ProfileInfoActivity.this, "Lỗi khi lưu thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Adapter for Year Picker
    class YearAdapter extends RecyclerView.Adapter<YearAdapter.YearViewHolder> {
        private final ArrayList<Integer> years;
        private final OnYearSelectedListener listener;

        YearAdapter(ArrayList<Integer> years, OnYearSelectedListener listener) {
            this.years = years;
            this.listener = listener;
        }

        /**
         * Tạo ViewHolder mới cho item năm trong RecyclerView.
         *
         * @param parent ViewGroup cha
         * @param viewType Loại view (không sử dụng)
         * @return YearViewHolder mới
         */
        @NonNull
        @Override
        public YearViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_year, parent, false);
            return new YearViewHolder(view);
        }

        /**
         * Bind dữ liệu năm vào ViewHolder tại vị trí cụ thể.
         *
         * @param holder ViewHolder chứa view
         * @param position Vị trí của item trong danh sách
         */
        @Override
        public void onBindViewHolder(@NonNull YearViewHolder holder, int position) {
            int year = years.get(position);
            holder.yearText.setText(String.valueOf(year));
            holder.itemView.setOnClickListener(v -> listener.onYearSelected(year));
        }

        /**
         * Trả về số lượng item trong danh sách năm.
         *
         * @return Số lượng năm có thể chọn
         */
        @Override
        public int getItemCount() {
            return years.size();
        }

        class YearViewHolder extends RecyclerView.ViewHolder {
            TextView yearText;
            YearViewHolder(View itemView) {
                super(itemView);
                yearText = itemView.findViewById(R.id.year_text);
            }
        }

    }

    interface OnYearSelectedListener {
        void onYearSelected(int year);
    }
}