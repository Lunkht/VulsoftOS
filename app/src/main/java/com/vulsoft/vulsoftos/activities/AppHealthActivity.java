package com.vulsoft.vulsoftos.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vulsoft.vulsoftos.R;
import com.vulsoft.vulsoftos.health.AppHealthManager;
import com.vulsoft.vulsoftos.health.AppHealthReport;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppHealthActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_health);

        RecyclerView recyclerView = findViewById(R.id.recycler_view_reports);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<AppHealthReport> reports = AppHealthManager.loadReports(this);
        
        // Add a manual analysis trigger for testing/demo if list is empty
        if (reports.isEmpty()) {
            AppHealthManager.analyzeSystemHealth(this, false);
            reports = AppHealthManager.loadReports(this);
        }
        
        recyclerView.setAdapter(new HealthAdapter(reports));
    }

    private static class HealthAdapter extends RecyclerView.Adapter<HealthAdapter.ViewHolder> {
        private final List<AppHealthReport> reports;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

        HealthAdapter(List<AppHealthReport> reports) {
            this.reports = reports;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_health_report, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppHealthReport report = reports.get(position);
            holder.appName.setText(report.getAppName());
            holder.time.setText(dateFormat.format(new Date(report.getTimestamp())));
            holder.issueType.setText(report.getIssueType());
            holder.description.setText(report.getDescription());

            // Color coding based on severity
            if (report.getSeverity() == 2) { // Critical
                holder.issueType.setTextColor(0xFFFF5252); // Red
            } else if (report.getSeverity() == 1) { // Warning
                holder.issueType.setTextColor(0xFFFFD740); // Amber
            } else { // Info
                holder.issueType.setTextColor(0xFF69F0AE); // Green
            }

            holder.itemView.setOnClickListener(v -> {
                if ("PERMISSION_ERROR".equals(report.getIssueType())) {
                    android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    try {
                        v.getContext().startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return reports.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView appName, time, issueType, description;

            ViewHolder(View itemView) {
                super(itemView);
                appName = itemView.findViewById(R.id.text_app_name);
                time = itemView.findViewById(R.id.text_time);
                issueType = itemView.findViewById(R.id.text_issue_type);
                description = itemView.findViewById(R.id.text_description);
            }
        }
    }
}
