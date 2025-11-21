package vn.haui.heartlink.admin;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.haui.heartlink.R;
import vn.haui.heartlink.models.User;

/**
 * Fragment Dashboard quản trị viên
 * Hiển thị các thống kê về người dùng, matches, tin nhắn và biểu đồ phân tích
 */
public class AdminDashboardFragment extends Fragment {

    // TextViews hiển thị thống kê người dùng
    private TextView textTotalUsers, textNewUsers, textOnlineUsers, textActiveUsers, textBlockedUsers;
    // TextViews hiển thị thống kê matches và tin nhắn
    private TextView textTotalMatches, textMatchesPerUser, textTotalMessages, textMessagesPerUser;
    // Biểu đồ đường: Tăng trưởng người dùng theo ngày
    private LineChart userGrowthChart;
    // Biểu đồ tròn: Phân phối người dùng (hoạt động/bị khóa)
    private PieChart userDistributionChart;
    // Biểu đồ cột: Hoạt động hàng tuần (matches và tin nhắn)
    private BarChart weeklyActivityChart;
    // Tham chiếu Firebase Database
    private DatabaseReference usersRef, matchesRef, chatsRef;

    /**
     * Tạo view cho fragment
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    /**
     * Khởi tạo sau khi view được tạo
     * Liên kết views, khởi tạo Firebase và tải dữ liệu
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        initializeFirebaseReferences();
        loadDashboardData();
    }

    /**
     * Liên kết các view components với biến
     */
    private void bindViews(View view) {
        textTotalUsers = view.findViewById(R.id.text_total_users);
        textNewUsers = view.findViewById(R.id.text_new_users);
        textTotalMatches = view.findViewById(R.id.text_total_matches);
        textMatchesPerUser = view.findViewById(R.id.text_matches_per_user);
        textTotalMessages = view.findViewById(R.id.text_total_messages);
        textMessagesPerUser = view.findViewById(R.id.text_messages_per_user);
        textActiveUsers = view.findViewById(R.id.text_active_users);
        textOnlineUsers = view.findViewById(R.id.text_online_users);
        textBlockedUsers = view.findViewById(R.id.text_blocked_users);
        userGrowthChart = view.findViewById(R.id.chart_user_growth);
        userDistributionChart = view.findViewById(R.id.chart_user_distribution);
        weeklyActivityChart = view.findViewById(R.id.chart_weekly_activity);
    }

    /**
     * Khởi tạo các tham chiếu Firebase Database
     */
    private void initializeFirebaseReferences() {
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        matchesRef = FirebaseDatabase.getInstance().getReference("Matches");
        chatsRef = FirebaseDatabase.getInstance().getReference("Chats");
    }

    /**
     * Tải dữ liệu dashboard từ Firebase
     * Tính toán các thống kê về người dùng và thiết lập biểu đồ
     */
    private void loadDashboardData() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Đếm tổng số người dùng
                long totalUsersCurrent = snapshot.getChildrenCount();
                textTotalUsers.setText(String.valueOf(totalUsersCurrent));

                // Khởi tạo các biến đếm
                long newUsersTodayCount = 0;
                long onlineCount = 0;
                long activeCount = 0;
                long blockedCount = 0;
                Map<Long, Integer> dailyNewUsers = new HashMap<>(); // Lưu số người dùng mới theo ngày

                // Tính timestamp bắt đầu ngày hôm nay
                Calendar todayCal = Calendar.getInstance();
                todayCal.set(Calendar.HOUR_OF_DAY, 0); todayCal.set(Calendar.MINUTE, 0); todayCal.set(Calendar.SECOND, 0);
                long todayStartTimestamp = todayCal.getTimeInMillis();

                // Tính timestamp 7 ngày trước
                Calendar sevenDaysAgoCalForGrowth = Calendar.getInstance();
                sevenDaysAgoCalForGrowth.add(Calendar.DAY_OF_YEAR, -7);
                sevenDaysAgoCalForGrowth.set(Calendar.HOUR_OF_DAY, 0); sevenDaysAgoCalForGrowth.set(Calendar.MINUTE, 0); sevenDaysAgoCalForGrowth.set(Calendar.SECOND, 0); sevenDaysAgoCalForGrowth.set(Calendar.MILLISECOND, 0);
                long sevenDaysAgoTimestamp = sevenDaysAgoCalForGrowth.getTimeInMillis();

                // Duyệt qua tất cả người dùng
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user == null) continue;

                    // Đếm người dùng online
                    if (user.isOnline()) onlineCount++;
                    // Đếm người dùng bị khóa và đang hoạt động
                    if (user.isBlocked()) {
                        blockedCount++;
                    } else {
                        activeCount++;
                    }

                    // Xử lý thời gian tạo tài khoản
                    Object createdAtObj = userSnapshot.child("createdAt").getValue();
                    if (createdAtObj instanceof Long) {
                        long createdAt = (Long) createdAtObj;
                        // Đếm người dùng mới hôm nay
                        if (createdAt >= todayStartTimestamp) newUsersTodayCount++;

                        // Thu thập dữ liệu người dùng mới trong 7 ngày qua cho biểu đồ
                        if (createdAt >= sevenDaysAgoTimestamp) {
                            Calendar userCal = Calendar.getInstance();
                            userCal.setTimeInMillis(createdAt);
                            userCal.set(Calendar.HOUR_OF_DAY, 0); userCal.set(Calendar.MINUTE, 0); userCal.set(Calendar.SECOND, 0); userCal.set(Calendar.MILLISECOND, 0);
                            long dayStart = userCal.getTimeInMillis();
                            dailyNewUsers.put(dayStart, dailyNewUsers.getOrDefault(dayStart, 0) + 1);
                        }
                    }
                }

                // Cập nhật UI với các giá trị đã tính toán
                textNewUsers.setText(String.format("+%d người mới", newUsersTodayCount));
                textOnlineUsers.setText(String.valueOf(onlineCount));
                textActiveUsers.setText(String.valueOf(activeCount));
                textBlockedUsers.setText(String.valueOf(blockedCount));

                // Thiết lập các biểu đồ
                setupUserGrowthChart(dailyNewUsers);
                setupUserDistributionChart(activeCount, blockedCount);

                // Tải dữ liệu matches và messages
                fetchMatchAndMessageData(totalUsersCurrent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Không thể tải dữ liệu người dùng.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Tải dữ liệu matches từ Firebase
     * @param totalUsers Tổng số người dùng để tính trung bình
     */
    private void fetchMatchAndMessageData(long totalUsers) {
        matchesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long totalMatches = 0;
                Map<String, Integer> weeklyMatches = new HashMap<>();
                Calendar sevenDaysAgoCal = Calendar.getInstance();
                sevenDaysAgoCal.add(Calendar.DAY_OF_YEAR, -7);

                for(DataSnapshot userMatches : snapshot.getChildren()){
                    for (DataSnapshot match : userMatches.getChildren()) {
                        totalMatches++;
                        Long timestamp = match.child("timestamp").getValue(Long.class);
                        if(timestamp != null && timestamp >= sevenDaysAgoCal.getTimeInMillis()){
                            String day = new SimpleDateFormat("EEE", Locale.US).format(timestamp);
                            weeklyMatches.put(day, weeklyMatches.getOrDefault(day, 0) + 1);
                        }
                    }
                }
                textTotalMatches.setText(String.valueOf(totalMatches));
                if (totalUsers > 0) {
                    textMatchesPerUser.setText(String.format(Locale.getDefault(), "%.1f / người", (float) totalMatches / totalUsers));
                }

                fetchMessagesAndSetupChart(weeklyMatches);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Không thể tải dữ liệu match.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Tải dữ liệu tin nhắn và thiết lập biểu đồ hoạt động tuần
     * @param weeklyMatches Dữ liệu matches theo tuần
     */
    private void fetchMessagesAndSetupChart(Map<String, Integer> weeklyMatches) {
        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long totalMessages = 0;
                Map<String, Integer> weeklyMessages = new HashMap<>();
                Calendar sevenDaysAgoCal = Calendar.getInstance();
                sevenDaysAgoCal.add(Calendar.DAY_OF_YEAR, -7);

                for(DataSnapshot chatRoomSnapshot : snapshot.getChildren()){
                    DataSnapshot messagesNode = chatRoomSnapshot.child("messages");
                    if (messagesNode.exists()) {
                        for (DataSnapshot messageSnapshot : messagesNode.getChildren()) {
                            totalMessages++;
                            Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);
                            if(timestamp != null && timestamp >= sevenDaysAgoCal.getTimeInMillis()){
                                String day = new SimpleDateFormat("EEE", Locale.US).format(timestamp);
                                weeklyMessages.put(day, weeklyMessages.getOrDefault(day, 0) + 1);
                            }
                        }
                    }
                }
                textTotalMessages.setText(String.valueOf(totalMessages));
                long totalUsers = Long.parseLong(textTotalUsers.getText().toString());
                if (totalUsers > 0) {
                    textMessagesPerUser.setText(String.format(Locale.getDefault(), "%.1f / người", (float) totalMessages / totalUsers));
                }

                setupWeeklyActivityChart(weeklyMatches, weeklyMessages);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Không thể tải dữ liệu tin nhắn.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Thiết lập biểu đồ tăng trưởng người dùng theo ngày
     * @param dailyNewUsers Map chứa số người dùng mới theo ngày
     */
    private void setupUserGrowthChart(Map<Long, Integer> dailyNewUsers) {
        if (getContext() == null) return;
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());

        Calendar chartStartCal = Calendar.getInstance();
        chartStartCal.add(Calendar.DAY_OF_YEAR, -6);
        chartStartCal.set(Calendar.HOUR_OF_DAY, 0); chartStartCal.set(Calendar.MINUTE, 0); chartStartCal.set(Calendar.SECOND, 0); chartStartCal.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i < 7; i++) {
            cal.setTimeInMillis(chartStartCal.getTimeInMillis());
            cal.add(Calendar.DAY_OF_YEAR, i);

            long dayStartTimestamp = cal.getTimeInMillis();
            entries.add(new Entry(i, dailyNewUsers.getOrDefault(dayStartTimestamp, 0)));
            labels.add(sdf.format(cal.getTime()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Người dùng mới");
        dataSet.setColor(ContextCompat.getColor(getContext(), R.color.admin_stat_blue));
        dataSet.setCircleColor(ContextCompat.getColor(getContext(), R.color.admin_stat_blue));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillDrawable(ContextCompat.getDrawable(getContext(), R.drawable.gradient_admin_toolbar));

        userGrowthChart.setData(new LineData(dataSet));
        userGrowthChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        userGrowthChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        userGrowthChart.getXAxis().setDrawGridLines(false);
        userGrowthChart.getAxisRight().setEnabled(false);
        userGrowthChart.getAxisLeft().setAxisMinimum(0f);
        userGrowthChart.getDescription().setEnabled(false);
        userGrowthChart.getLegend().setEnabled(true);
        userGrowthChart.animateX(1000);
        userGrowthChart.invalidate();
    }

    /**
     * Thiết lập biểu đồ phân phối người dùng (hoạt động vs bị khóa)
     * @param active Số người dùng đang hoạt động
     * @param blocked Số người dùng bị khóa
     */
    private void setupUserDistributionChart(long active, long blocked) {
        if (getContext() == null || active + blocked == 0) {
            userDistributionChart.clear();
            return;
        }
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(active, "Hoạt động"));
        entries.add(new PieEntry(blocked, "Bị khóa"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{ContextCompat.getColor(getContext(), R.color.admin_stat_green), ContextCompat.getColor(getContext(), R.color.admin_stat_red)});
        dataSet.setValueFormatter(new PercentFormatter(userDistributionChart));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        dataSet.setSliceSpace(2f);

        userDistributionChart.setData(new PieData(dataSet));
        userDistributionChart.setUsePercentValues(true);
        userDistributionChart.getDescription().setEnabled(false);
        userDistributionChart.setDrawHoleEnabled(true);
        userDistributionChart.setHoleColor(Color.TRANSPARENT);
        userDistributionChart.setEntryLabelColor(Color.WHITE);
        userDistributionChart.setEntryLabelTextSize(10f);
        userDistributionChart.getLegend().setEnabled(true);
        userDistributionChart.animateY(1000);
        userDistributionChart.invalidate();
    }

    /**
     * Thiết lập biểu đồ hoạt động hàng tuần (matches và tin nhắn)
     * @param weeklyMatches Số lượng matches theo ngày trong tuần
     * @param weeklyMessages Số lượng tin nhắn theo ngày trong tuần
     */
    private void setupWeeklyActivityChart(Map<String, Integer> weeklyMatches, Map<String, Integer> weeklyMessages) {
        if (getContext() == null) return;
        ArrayList<BarEntry> matchesEntries = new ArrayList<>();
        ArrayList<BarEntry> messagesEntries = new ArrayList<>();
        final String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        final String[] displayDays = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

        for (int i = 0; i < days.length; i++) {
            matchesEntries.add(new BarEntry(i, weeklyMatches.getOrDefault(days[i], 0)));
            messagesEntries.add(new BarEntry(i, weeklyMessages.getOrDefault(days[i], 0)));
        }

        BarDataSet matchesDataSet = new BarDataSet(matchesEntries, "Matches");
        matchesDataSet.setColor(ContextCompat.getColor(getContext(), R.color.admin_card_border_pink));

        BarDataSet messagesDataSet = new BarDataSet(messagesEntries, "Tin nhắn");
        messagesDataSet.setColor(ContextCompat.getColor(getContext(), R.color.admin_card_border_purple));

        float groupSpace = 0.4f;
        float barSpace = 0.05f;
        float barWidth = 0.25f;

        BarData data = new BarData(matchesDataSet, messagesDataSet);
        data.setBarWidth(barWidth);

        weeklyActivityChart.setData(data);
        weeklyActivityChart.groupBars(0, groupSpace, barSpace);
        weeklyActivityChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(displayDays));
        weeklyActivityChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        weeklyActivityChart.getXAxis().setGranularity(1f);
        weeklyActivityChart.getXAxis().setCenterAxisLabels(true);
        weeklyActivityChart.getXAxis().setDrawGridLines(false);
        weeklyActivityChart.getAxisRight().setEnabled(false);
        weeklyActivityChart.getAxisLeft().setAxisMinimum(0f);
        weeklyActivityChart.getDescription().setEnabled(false);
        weeklyActivityChart.getLegend().setEnabled(true);
        weeklyActivityChart.animateY(1000);
        weeklyActivityChart.invalidate();
    }
}
