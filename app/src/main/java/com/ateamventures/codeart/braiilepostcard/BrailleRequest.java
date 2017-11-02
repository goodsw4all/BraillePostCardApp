package com.ateamventures.codeart.braiilepostcard;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by codeart on 21/09/2017.
 */

public class BrailleRequest {

    String mGcodeUrl = "";
    private static final String TAG = "BrailleRequest";
    private FullscreenActivity.BrailleRequestCallBack mCallBack;

    public String getGcodeUrl() {
        return mGcodeUrl;
    }
    public void registerCallBack(FullscreenActivity.BrailleRequestCallBack cb) {
        mCallBack = cb;
    }

    private void setGcodeUrl(String url) {
        mGcodeUrl = "http://192.168.1.37:8080" + url;
    }


    private String convertedMessage2JSON(String message)
    {
        String convertedMessage = "\"message\": [\"i\", \"test\", \"Braille\"],\n";
        boolean isFirstLine = true;
        String start = "\"message\": [";
        String end   = "],\n";
        convertedMessage = start;
        Scanner scanner = new Scanner(message);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!isFirstLine)
                convertedMessage += ",";
            else
                isFirstLine = false;
            convertedMessage += "\"" + line  + "\"";
        }
        scanner.close();
        convertedMessage += end;
        return convertedMessage;
    }
    private class httpRequest extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                String convertedMessage = convertedMessage2JSON(params[1]);
                
                JSONObject jsonObject = sendJSONRequest(params[0], convertedMessage);
                //
                // Parse your json here
                // Get your url

                String url = null; // = jsonObject.getString("gcodeURL");
                if(url == null)
                    url="/BraillePostCard.gcode";
                setGcodeUrl(url);

                Log.e(TAG, "doInBackground: "+ jsonObject.toString() + " " + params[0] +" " + params[1]);
                Log.d(TAG, "---Gcode "+ url);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            mCallBack.convertComplete();
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }


    public JSONObject sendJSONRequest(String urlString, String message) throws IOException, JSONException {

        URL url;
        HttpURLConnection connection = null;
        try {
            //Create connection
            url = new URL(urlString);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            //connection.setConnectTimeout(90000);

            connection.setUseCaches (false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //TODO : get message
            Log.d(TAG, "sendJSONRequest: "  + message);
//            message = "\"message\": [\"i\", \"test\", \"Braille\"],\n";
//            Log.d(TAG, "sendJSONRequest: "  + message);

            String rawString = "{\n" +
                    "  \"title\": \"Hello Braille Postcard Project\",\n" +
                    "  \"from\": {\n" +
                    "    \"name\": \"Justin\",\n" +
                    "    \"address\": \"heaven\"\n" +
                    "  },\n" +
                    "  \"to\": {\n" +
                    "    \"name\": \"you\",\n" +
                    "    \"address\": \"happy world\"\n" +
                    "  },\n" +
                    message +
                    "  \"number\": 1,\n" +
                    "  \"reserved\": {\n" +
                    "    \"0\": \"b\",\n" +
                    "    \"1\": \"d\",\n" +
                    "    \"2\": \"f\"\n" +
                    "  }\n" +
                    "}";

            //Send request
            DataOutputStream wr = new DataOutputStream (
                    connection.getOutputStream ());

            JSONObject jsonObj = new JSONObject(rawString);
            wr.write(jsonObj.toString().getBytes());
            wr.flush ();
            wr.close ();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return new JSONObject(response.toString());

        } catch (Exception e) {

            e.printStackTrace();
            return null;

        } finally {

            if(connection != null) {
                connection.disconnect();
            }
        }

    }

    public void sendRequsest(String... params) {
        Log.e(TAG, "sendRequest: "+ " " + params[0] +" " + params[1]);
        new httpRequest().execute(params);
    }
}
