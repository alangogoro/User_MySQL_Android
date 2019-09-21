package exer.task;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.google.gson.JsonObject;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import exer.main.R;

/* 抓取圖片的新執行緒工作 */
public class ImageTask extends AsyncTask<Object, Integer, Bitmap> {
    private final static String TAG = "TAG_ImageTask";
    private String url;
    private int id, imageSize;

    /* WeakReference 不會阻止資源回收機制
     * 所以需注意參照對象可能會是空值（已被系統回收掉） */
    /* ImageTask 的屬性 strong 參照到 UserListFragment 內的 imageView 不好，
        會導致 UserListFragment 進入背景時 imageView 被參照而無法被釋放，
        而且 imageView 會參照到 Context，也會導致 Activity 無法被回收。
        改採 weak 參照，就不會阻止 imageView 被回收 */
    private WeakReference<ImageView> imageViewWeakReference;

    // 取單張圖片
    public ImageTask(String url, int id, int imageSize) {
        this(url, id, imageSize, null);
    }

    // 取完圖片後使用傳入的 ImageView 顯示，適用於 顯示多張圖片  ImageView
    public ImageTask(String url, int id, int imageSize, ImageView imageView) {
        this.url = url;
        this.id = id;
        this.imageSize = imageSize;
        /* 弱參照到 imageView */
        this.imageViewWeakReference = new WeakReference<>(imageView);
    }

    @Override
    protected Bitmap doInBackground(Object... params) {
        JsonObject jo = new JsonObject();
        jo.addProperty("action", "getImage");
        jo.addProperty("id", id);
        jo.addProperty("imageSize", imageSize);
        return getRemoteImage(url, jo.toString());
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        ImageView imageView = imageViewWeakReference.get();

        /* 由於是弱參照，imageView 可能是空值 */
        if (isCancelled() || imageView == null) {
            return;
        }
        if (bitmap != null) {/* 不是空值才貼上圖片 */
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(R.drawable.no_image);
        }
    }

    private Bitmap getRemoteImage(String url, String jsonOut) {
        HttpURLConnection connection = null;
        Bitmap bitmap = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setDoInput(true);    // allow inputs
            connection.setDoOutput(true);   // allow outputs
            connection.setUseCaches(false); // do not use a cached copy
            connection.setRequestMethod("POST");
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            bw.write(jsonOut);
            Log.d(TAG, "Output: " + jsonOut);
            bw.close();

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {

                /* 取得縮圖結果並放入 bitmap */
                bitmap = BitmapFactory.decodeStream(
                        new BufferedInputStream(connection.getInputStream()));

            } else {
                Log.d(TAG, "Response code: " + responseCode);
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return bitmap;
    }
}