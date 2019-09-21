package exer.task;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class Common {
//    public static String URL_SERVER = "http://192.168.196.189:8080/Spot_MySQL_Web/";
    public static String URL_SERVER = "http://10.0.2.2:8080/User_MySQL_Web/";

    // 確認裝置是否連線至網路
    public static boolean networkConnected(Activity activity) {
        ConnectivityManager conManager =
                (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conManager != null ? conManager.getActiveNetworkInfo() : null;
        return networkInfo != null && networkInfo.isConnected();
    }

    /* 把 Toast 方法寫在 Common 內省去重複撰寫的不便 */
    public static void showToast(Context context, int messageResId) {
        Toast.makeText(context, messageResId, Toast.LENGTH_SHORT).show();
    }

}