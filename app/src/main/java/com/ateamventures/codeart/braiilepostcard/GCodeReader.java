package com.ateamventures.codeart.braiilepostcard;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by codeart on 17/10/2017.
 */

public class GCodeReader {

    File sdcard = Environment.getExternalStorageDirectory();

    //Get the text file
    File file;
    static Boolean OKfromPrinter = true;
    static final String TAG = "MW";

    StringBuilder text = new StringBuilder();
    String nextGcode;

    public GCodeReader(String path) {
        file = new File(sdcard, "Download/waggleLogo_noheat.gcode");
        OKfromPrinter = true;
        Log.d(TAG, "GCodeReader: " + file.getAbsolutePath());
    }

    public void startGCodeReadThread() {
        new GcodeReadThread().start();
    }

    public static void setOKfromPrinter(Boolean OKfromPrinter) {
        GCodeReader.OKfromPrinter = OKfromPrinter;
    }

    public String getNextGcode() {
        return nextGcode;
    }

    public void setNextGcode(String nextGcode) {
        this.nextGcode = nextGcode;
    }

    public String extractGcode(String line) {
        if (line.length() <= 0 || !Character.isAlphabetic(line.charAt(0)) ) {
                return null;
        }

        System.out.println(line);

        int indexOfsemicollon = line.indexOf(';');

        if(indexOfsemicollon == -1) {
            return line;
        } else {
            return line.substring(0, indexOfsemicollon-1);
        }
    }

    private class GcodeReadThread extends Thread {
        @Override
        public void run() {

            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(file));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            while (OKfromPrinter) {
                String line;

                try {
                    if ((line = br.readLine()) != null) {
                        String temp = extractGcode(line);
                        if (temp == null)
                            continue;
                        ;
                        setNextGcode(temp + " \n");
                        OKfromPrinter = false;
                    } else {

                        Log.d(TAG, "run: File Read is done");
                        setNextGcode(null);
                        br.close();
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }
    }
}
