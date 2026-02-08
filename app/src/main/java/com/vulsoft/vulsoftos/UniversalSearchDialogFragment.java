package com.vulsoft.vulsoftos;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UniversalSearchDialogFragment extends DialogFragment {

    private EditText editSearch;
    private RecyclerView recyclerSearchResults;
    private SearchAdapter searchAdapter;
    private List<SearchResultItem> allItems = new ArrayList<>();
    private List<SearchResultItem> filteredItems = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_universal_search, container, false);

        editSearch = view.findViewById(R.id.editSearch);
        recyclerSearchResults = view.findViewById(R.id.recyclerSearchResults);

        recyclerSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        searchAdapter = new SearchAdapter(filteredItems, item -> {
            if (item.type == SearchResultItem.Type.APP) {
                try {
                    startActivity(item.intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (item.type == SearchResultItem.Type.CONTACT) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(item.contactUri);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (item.type == SearchResultItem.Type.WEB) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://www.google.com/search?q=" + Uri.encode(item.query)));
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            dismiss();
        });
        recyclerSearchResults.setAdapter(searchAdapter);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        editSearch.requestFocus();
        
        loadData();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        }
    }

    private void loadData() {
        new Thread(() -> {
            if (!isAdded()) return;
            
            List<SearchResultItem> items = new ArrayList<>();

            // Load Apps
            PackageManager pm = requireContext().getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);
            for (ResolveInfo app : apps) {
                SearchResultItem item = new SearchResultItem();
                item.label = app.loadLabel(pm).toString();
                item.icon = app.loadIcon(pm);
                item.type = SearchResultItem.Type.APP;
                item.intent = pm.getLaunchIntentForPackage(app.activityInfo.packageName);
                items.add(item);
            }

            // Load Contacts
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                try {
                    Cursor cursor = requireContext().getContentResolver().query(
                            ContactsContract.Contacts.CONTENT_URI,
                            null, null, null, null);
                    if (cursor != null) {
                        int idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                        int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                        
                        if (idIndex >= 0 && nameIndex >= 0) {
                            while (cursor.moveToNext()) {
                                String id = cursor.getString(idIndex);
                                String name = cursor.getString(nameIndex);
                                if (name != null) {
                                    SearchResultItem item = new SearchResultItem();
                                    item.label = name;
                                    item.type = SearchResultItem.Type.CONTACT;
                                    item.contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id);
                                    
                                    // Set Contact Icon (Themed)
                                    android.graphics.drawable.Drawable defaultContactIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_theme_phone); // Fallback
                                    item.icon = IconPackManager.getIcon(requireContext(), "com.android.contacts", defaultContactIcon);
                                    
                                    items.add(item);
                                }
                            }
                        }
                        cursor.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Collections.sort(items, (o1, o2) -> o1.label.compareToIgnoreCase(o2.label));

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    allItems.clear();
                    allItems.addAll(items);
                    filter(editSearch.getText().toString());
                });
            }
        }).start();
    }

    private void filter(String query) {
        filteredItems.clear();
        if (query.isEmpty()) {
            filteredItems.addAll(allItems);
        } else {
            String lower = query.toLowerCase();
            for (SearchResultItem item : allItems) {
                if (item.label.toLowerCase().contains(lower)) {
                    filteredItems.add(item);
                }
            }
            
            // Add Web Search Option
            SearchResultItem webItem = new SearchResultItem();
            webItem.label = "Rechercher \"" + query + "\" sur Internet";
            webItem.type = SearchResultItem.Type.WEB;
            webItem.query = query;
            filteredItems.add(webItem);
        }
        searchAdapter.notifyDataSetChanged();
    }

    private static class SearchResultItem {
        enum Type { APP, CONTACT, WEB }
        String label;
        android.graphics.drawable.Drawable icon;
        Type type;
        Intent intent;
        Uri contactUri;
        String query;
    }

    private static class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
        private List<SearchResultItem> items;
        private OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(SearchResultItem item);
        }

        SearchAdapter(List<SearchResultItem> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SearchResultItem item = items.get(position);
            holder.text.setText(item.label);
            if (item.type == SearchResultItem.Type.APP) {
                holder.icon.setImageDrawable(item.icon);
            } else if (item.type == SearchResultItem.Type.CONTACT) {
                holder.icon.setImageDrawable(IconPackManager.loadSafeDrawable(holder.itemView.getContext(), R.drawable.afro_contact));
            } else if (item.type == SearchResultItem.Type.WEB) {
                holder.icon.setImageDrawable(androidx.core.content.ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_web_search));
            }
            holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView text;

            ViewHolder(View itemView) {
                super(itemView);
                this.icon = itemView.findViewById(R.id.imgIcon);
                this.text = itemView.findViewById(R.id.txtLabel);
            }
        }
    }
}