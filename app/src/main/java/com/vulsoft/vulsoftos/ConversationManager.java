package com.vulsoft.vulsoftos;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ConversationManager {
    private static final String PREF_NAME = "chat_history";
    private static final String KEY_SESSIONS = "sessions";

    public static void saveSession(Context context, List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String sessionsJson = prefs.getString(KEY_SESSIONS, "[]");

        try {
            JSONArray sessionsArray = new JSONArray(sessionsJson);
            JSONObject currentSession = new JSONObject();
            currentSession.put("timestamp", System.currentTimeMillis());

            JSONArray msgsArray = new JSONArray();
            for (ChatMessage msg : messages) {
                JSONObject msgObj = new JSONObject();
                msgObj.put("content", msg.getContent());
                msgObj.put("isUser", msg.isUser());
                msgObj.put("timestamp", msg.getTimestamp());
                msgsArray.put(msgObj);
            }
            currentSession.put("messages", msgsArray);

            // Add new session at the beginning
            JSONArray newSessionsArray = new JSONArray();
            newSessionsArray.put(currentSession);
            for (int i = 0; i < sessionsArray.length(); i++) {
                newSessionsArray.put(sessionsArray.get(i));
            }

            prefs.edit().putString(KEY_SESSIONS, newSessionsArray.toString()).apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static List<List<ChatMessage>> getHistory(Context context) {
        List<List<ChatMessage>> history = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String sessionsJson = prefs.getString(KEY_SESSIONS, "[]");

        try {
            JSONArray sessionsArray = new JSONArray(sessionsJson);
            for (int i = 0; i < sessionsArray.length(); i++) {
                JSONObject session = sessionsArray.getJSONObject(i);
                JSONArray msgsArray = session.getJSONArray("messages");
                List<ChatMessage> sessionMessages = new ArrayList<>();
                for (int j = 0; j < msgsArray.length(); j++) {
                    JSONObject msgObj = msgsArray.getJSONObject(j);
                    ChatMessage msg = new ChatMessage(
                            msgObj.getString("content"),
                            msgObj.getBoolean("isUser"),
                            msgObj.optLong("timestamp", System.currentTimeMillis())
                    );
                    sessionMessages.add(msg);
                }
                history.add(sessionMessages);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return history;
    }
    
    public static void clearHistory(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().remove(KEY_SESSIONS).apply();
    }
}
