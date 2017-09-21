package com.ateamventures.codeart.braiilepostcard;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by codeart on 21/09/2017.
 */

public class BrailleRequest {

    private class httpRequest extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try{
                JSONObject jsonObject = getJSONObjectFromURL("http://192.168.1.37:8080/api/json");
                //
                // Parse your json here
                //
                Log.e("TEST", "doInBackground: "+ jsonObject.toString() + " " + params[0]);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }


    public JSONObject getJSONObjectFromURL(String urlString) throws IOException, JSONException {

        URL url;
        HttpURLConnection connection = null;
        try {
            //Create connection
            url = new URL(urlString);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/json");

            connection.setUseCaches (false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            String rawString = "{\n" +
                    "  \"title\": \"Hello Braille Postcard Project\",\n" +
                    "  \"from\": {\n" +
                    "    \"name\": \"codeart\",\n" +
                    "    \"address\": \"heaven\"\n" +
                    "  },\n" +
                    "  \"to\": {\n" +
                    "    \"name\": \"you\",\n" +
                    "    \"address\": \"happy world\"\n" +
                    "  },\n" +
                    "  \"message\": [\"i\", \"love\", \"you\"],\n" +
                    "  \"number\": 123,\n" +
                    "  \"object\": {\n" +
                    "    \"a\": \"b\",\n" +
                    "    \"c\": \"d\",\n" +
                    "    \"e\": \"f\"\n" +
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

    public void sendRequsest(String urlString) {
        new httpRequest().execute(urlString);
    }
}

