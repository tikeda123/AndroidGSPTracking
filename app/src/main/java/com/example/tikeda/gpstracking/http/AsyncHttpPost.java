package com.example.tikeda.gpstracking.http;

import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.io.IOException;

import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * Created by tikeda on 2017/01/05.
 */
/******************************************************************
 AsyncHttpPost Class : Send Tracking Data to ElasticSearch Server in Cloud.
 ******************************************************************/
public class AsyncHttpPost extends AsyncTask<String, Integer, String>
{
    private final String TAG = "AsyncTask";

    // 接続のタイムアウト（ミリ秒）
    private static final int CONNECT_TIMEOUT_MS = 3000;
    // 読み込みのタイムアウト（ミリ秒）
    private static final int READ_TIMEOUT_MS = 5000;

    // 接続先URL
    private String m_url;
    // HTTPステータスコード
    private int status;
    // レスポンスの入力ストリーム
    private InputStream in;
    private String m_PostData;

    // Class for Constructor
    /******************************************************************
     AsyncHttpPost : Class for Constructor
     :Parameters:
     :url     : Setting Elastic Server URL in this class.
     :Returns Type: void
     ******************************************************************/
    public  AsyncHttpPost
    (
            String url // Set URL of Elastic Search server.
    )
    {
        super();
        this.m_url = url;
    }
    /******************************************************************
     setPostData : Set Post Data in Class
     :Parameters:
     :data     : String Data .
     :Returns Type: void
     ******************************************************************/
    public void setPostData
            (
                    String data
            )
    {
        m_PostData = data;
    }
    /******************************************************************
     doInBackground : Backgraound thread for sending Post HTTP
     :Parameters:
     :params     : No JSON Data .
     :Returns Type: void
     ******************************************************************/
    @Override
    protected String doInBackground
            (
                    String... params
            )
    {
        HttpURLConnection con = null;

        try
        {
            URL url = new URL( this.m_url );
            String buffer = "";

            // 通信の設定を行う
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST"); // メソッド
            con.setDoOutput(true);
            con.setConnectTimeout(CONNECT_TIMEOUT_MS); // 接続のタイムアウト
            con.setReadTimeout(READ_TIMEOUT_MS);  // 読み込みのタイムアウト
            con.setInstanceFollowRedirects(false); // Redirect 許可なし
            con.setRequestProperty("Accept-Language", "jp");
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");


            PrintStream ps = new PrintStream(con.getOutputStream());
            ps.print( m_PostData );

            status = con.getResponseCode();

            ps.close();
            Log.i(TAG,"status:"+status);

            BufferedReader reader =
                    new BufferedReader
                            (
                                    new InputStreamReader(con.getInputStream(), "UTF-8")
                            );
            buffer = reader.readLine();
            Log.i(TAG,"buffer:"+buffer);
            con.disconnect();
            return buffer;
        }
        catch ( MalformedURLException e)
        {
            e.printStackTrace();
            Log.i(TAG,e.getMessage());
        }
        catch ( FileNotFoundException e )
        { // IOException をキャッチするより先に FileNotFoundException をキャッチしないと IOException のキャッチブロックに行くのでこうする
            System.err.println(e);
            InputStream is_err = null;
            try
            {
                is_err = con.getErrorStream();
                // 4xx または 5xx なレスポンスのボディーを読み取る
                // ...
                BufferedReader reader =
                        new BufferedReader
                                (
                                        new InputStreamReader(is_err, "UTF-8")
                                );
                String buffer = reader.readLine();
                Log.i(TAG,"buffer:"+buffer.toString());

            }
            catch (IOException ef)
            {
                System.err.println(ef);
            }
            finally
            {
                if (is_err != null)
                {
                    try
                    {
                        is_err.close();
                    }
                    catch (IOException ef)
                    {
                        System.err.println(ef);
                    }
                }
                return null;
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }
    /******************************************************************
     onProgressUpdate : on Progress update in this class
     :Parameters:
     :params     : values .
     :Returns Type: void
     ******************************************************************/
    @Override
    protected void onProgressUpdate
            (
                    Integer... values
            )
    {
        Log.i(TAG, values[0].toString());
    }
}
