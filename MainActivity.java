package com.abhro.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> urlList = new ArrayList<>();

    ArrayAdapter arrayAdapter;

    SQLiteDatabase articleDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView)findViewById(R.id.listView);
        arrayAdapter= new ArrayAdapter(MainActivity.this,android.R.layout.simple_list_item_1,titles);

        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(),article.class);
                intent.putExtra("url",urlList.get(i));

                startActivity(intent);
            }
        });

        articleDb = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);

        articleDb.execSQL("CREATE TABLE IF NOT EXISTS articles ( id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, url VARCHAR)");

        updateList();

        DownloadTask task = new DownloadTask();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }
        catch (Exception e){
            e.printStackTrace();
        }

        //https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty
    }

    public void updateList (){

        Cursor c = articleDb.rawQuery("SELECT * FROM articles",null);

        int urlIndex = c.getColumnIndex("url");
        int titleIndex = c.getColumnIndex("title");

        if(c.moveToFirst()){

            titles.clear();
            urlList.clear();

            do {
                titles.add(c.getString(titleIndex));
                urlList.add(c.getString(urlIndex));
            }while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }
    }

    public class DownloadTask extends AsyncTask<String, Void ,String> {

        @Override
        protected String doInBackground(String... strings) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(strings[0]);

                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = urlConnection.getInputStream();

                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();

                while (data != -1) {

                    char current = (char) data;

                    result += current;

                    data = reader.read();

                }
                Log.i("result",result);

                JSONArray jsonArray = new JSONArray(result);

                int numberOfItem = 20;
                if (numberOfItem > jsonArray.length()) {
                    numberOfItem = jsonArray.length();
                }
                articleDb.execSQL("DELETE FROM articles");

                for (int i = 0; i < numberOfItem; i++) {

                    String articleId = jsonArray.getString(i);

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" +articleId+".json?print=pretty");

                    urlConnection = (HttpURLConnection) url.openConnection();

                    in = urlConnection.getInputStream();

                    reader = new InputStreamReader(in);

                    data = reader.read();

                    String articleInfo = "";
                    while (data != -1) {

                        char current = (char) data;

                        articleInfo += current;

                        data = reader.read();
                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        String title = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

                        String sql = "INSERT INTO articles (articleId, title, url) VALUES (?, ? , ?)";

                        SQLiteStatement statement = articleDb.compileStatement(sql);

                        statement.bindString(1, articleId);
                        statement.bindString(2, title);
                        statement.bindString(3, articleUrl);

                        statement.execute();

                    }

                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateList();
        }
    }
}
