package com.yandex.pownynotify;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class EventCache {
    private File mCacheFile;

    public EventCache(Context context) {
        mCacheFile = new File(context.getExternalCacheDir(), "events.json");
    }

    public void write(String data) {
        try {
            FileWriter file = new FileWriter(mCacheFile);
            file.write(data);
            file.flush();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String read() {
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(mCacheFile));

            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }
}
