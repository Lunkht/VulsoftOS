package com.vulsoft.vulsoftos.activities;

import com.vulsoft.vulsoftos.*;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AssistantActivity extends BaseActivity {

    private RecyclerView recyclerChat;
    private EditText inputMessage;
    private ImageButton btnSend;
    private android.widget.ImageView btnBack;
    private android.widget.ImageView btnHistory;
    private LinearLayout welcomeSection;
    private ChatAdapter adapter;
    private List<ChatMessage> messages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant);

        recyclerChat = findViewById(R.id.recyclerChat);
        inputMessage = findViewById(R.id.inputMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        btnHistory = findViewById(R.id.btnHistory);
        welcomeSection = findViewById(R.id.welcomeSection);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> showHistory());
        }

        messages = new ArrayList<>();
        adapter = new ChatAdapter(messages);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerChat.setLayoutManager(layoutManager);
        recyclerChat.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String content = inputMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;

        if (welcomeSection.getVisibility() == View.VISIBLE) {
            welcomeSection.setVisibility(View.GONE);
        }

        // User message
        messages.add(new ChatMessage(content, true));
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerChat.smoothScrollToPosition(messages.size() - 1);
        inputMessage.setText("");

        // Simulate processing delay
        recyclerChat.postDelayed(() -> processUserMessage(content), 500);
    }

    private void addBotMessage(String content) {
        messages.add(new ChatMessage(content, false));
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerChat.smoothScrollToPosition(messages.size() - 1);
    }

    private void processUserMessage(String input) {
        String lowerInput = input.toLowerCase();

        if (lowerInput.contains("bonjour") || lowerInput.contains("salut")) {
            addBotMessage("Salut ! Prêt à vous aider.");
        } else if (lowerInput.contains("ouvrir") || lowerInput.contains("lance")) {
            // Extract app name attempt
            String appName = input.replace("ouvrir", "").replace("lance", "").trim();
            if (!appName.isEmpty()) {
                if (launchApp(appName)) {
                    addBotMessage("Ouverture de " + appName + "...");
                } else {
                    addBotMessage("Je ne trouve pas l'application " + appName + ".");
                }
            } else {
                addBotMessage("Quelle application voulez-vous ouvrir ?");
            }
        } else if (lowerInput.contains("heure")) {
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            addBotMessage("Il est " + time + ".");
        } else if (lowerInput.contains("date")) {
            String date = new SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(new Date());
            addBotMessage("Nous sommes le " + date + ".");
        } else {
            addBotMessage("Désolé, je ne comprends pas encore cette commande. Essayez 'ouvrir [app]' ou demandez l'heure.");
        }
    }

    private boolean launchApp(String appName) {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);

        for (ResolveInfo info : apps) {
            String label = info.loadLabel(pm).toString();
            if (label.toLowerCase().contains(appName.toLowerCase())) {
                Intent launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName);
                if (launchIntent != null) {
                    startActivity(launchIntent);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!messages.isEmpty()) {
            ConversationManager.saveSession(this, messages);
        }
    }

    private void showHistory() {
        List<List<ChatMessage>> history = ConversationManager.getHistory(this);
        if (history.isEmpty()) {
            Toast.makeText(this, "Aucun historique disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = new String[history.size()];
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        for (int i = 0; i < history.size(); i++) {
            List<ChatMessage> session = history.get(i);
            long timestamp = 0;
            String preview = "Conversation vide";
            if (!session.isEmpty()) {
                ChatMessage firstMsg = session.get(0);
                timestamp = firstMsg.getTimestamp();
                preview = firstMsg.getContent();
                if (preview.length() > 30) preview = preview.substring(0, 30) + "...";
            }
            String dateStr = (timestamp > 0) ? sdf.format(new Date(timestamp)) : "Date inconnue";
            items[i] = dateStr + "\n" + preview;
        }

        new com.vulsoft.vulsoftos.utils.ModernDialogHelper.Builder(this)
                .setTitle("Historique des conversations")
                .setItems(items, (index, value) -> {
                    showConversationDetails(history.get(index));
                })
                .setPositiveButton("Fermer", null)
                .setNeutralButton("Effacer tout", v -> {
                    ConversationManager.clearHistory(this);
                    Toast.makeText(this, "Historique effacé", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showConversationDetails(List<ChatMessage> sessionMessages) {
        new com.vulsoft.vulsoftos.utils.ModernDialogHelper.Builder(this)
                .setTitle("Détails de la conversation")
                .setMessage("Voulez-vous charger cette conversation ? Cela remplacera la conversation actuelle.")
                .setPositiveButton("Charger", v -> {
                    messages.clear();
                    messages.addAll(sessionMessages);
                    adapter.notifyDataSetChanged();
                    welcomeSection.setVisibility(View.GONE);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
}
