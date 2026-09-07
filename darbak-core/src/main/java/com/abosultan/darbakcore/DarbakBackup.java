package com.abosultan.darbakcore;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Generic preferences backup/restore layer. App databases and media remain app-specific. */
public final class DarbakBackup {
    private DarbakBackup() {}

    public static File exportPreferences(Context context, String preferencesName) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
        JSONObject root = new JSONObject();
        root.put("schema", 1);
        root.put("package", context.getPackageName());
        root.put("preferences", preferencesName);
        JSONObject values = new JSONObject();

        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            Object value = entry.getValue();
            JSONObject item = new JSONObject();
            if (value instanceof String) {
                item.put("type", "string"); item.put("value", value);
            } else if (value instanceof Boolean) {
                item.put("type", "boolean"); item.put("value", value);
            } else if (value instanceof Integer) {
                item.put("type", "int"); item.put("value", value);
            } else if (value instanceof Long) {
                item.put("type", "long"); item.put("value", value);
            } else if (value instanceof Float) {
                item.put("type", "float"); item.put("value", ((Float) value).doubleValue());
            } else if (value instanceof Set) {
                item.put("type", "strings");
                JSONArray a = new JSONArray();
                for (Object s : (Set<?>) value) a.put(String.valueOf(s));
                item.put("value", a);
            } else {
                continue;
            }
            values.put(entry.getKey(), item);
        }
        root.put("values", values);

        File out = new File(DarbakCore.reportsDir(context), "settings_backup.json");
        try (FileWriter writer = new FileWriter(out, false)) {
            writer.write(root.toString(2));
        }
        return out;
    }

    public static void importPreferences(Context context, String preferencesName, File file) throws Exception {
        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) json.append(line);
        }

        JSONObject root = new JSONObject(json.toString());
        if (!context.getPackageName().equals(root.optString("package"))) {
            throw new IllegalArgumentException("Backup belongs to a different package");
        }
        JSONObject values = root.getJSONObject("values");
        SharedPreferences.Editor editor = context
                .getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().clear();

        java.util.Iterator<String> keys = values.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject item = values.getJSONObject(key);
            String type = item.getString("type");
            switch (type) {
                case "string": editor.putString(key, item.optString("value", "")); break;
                case "boolean": editor.putBoolean(key, item.optBoolean("value")); break;
                case "int": editor.putInt(key, item.optInt("value")); break;
                case "long": editor.putLong(key, item.optLong("value")); break;
                case "float": editor.putFloat(key, (float) item.optDouble("value")); break;
                case "strings":
                    JSONArray a = item.getJSONArray("value");
                    Set<String> set = new HashSet<>();
                    for (int i = 0; i < a.length(); i++) set.add(a.getString(i));
                    editor.putStringSet(key, set);
                    break;
                default: break;
            }
        }
        if (!editor.commit()) throw new IllegalStateException("Could not restore settings");
    }
}
