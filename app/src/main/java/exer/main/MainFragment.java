package exer.main;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;

import exer.task.Common;
import exer.task.CommonTask;
import exer.user.User;

import static android.app.Activity.RESULT_OK;
import static exer.task.Common.URL_SERVER;

public class MainFragment extends Fragment {
    private static final String TAG = "TAG_MainFragment";
    private Activity activity;
    private EditText etName, etPassword;
    private Button btSignIn, btSignUp;
    private TextView textView;

    /* 連線使用資料庫變數 */
    private CommonTask loginTask, signUpTask;

    /* 拍照方法使用變數 */
//    private byte[] image;
    private ImageView ivUser;
    private static final int REQ_TAKE_PICTURE = 0;
    private static final int REQ_PICK_PICTURE = 1;
    private static final int REQ_CROP_PICTURE = 2;
    private byte[] image;
    private Uri contentUri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        activity.setTitle(R.string.app_name);
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivUser = view.findViewById(R.id.ivUser);

        etName = view.findViewById(R.id.etName);
        etPassword = view.findViewById(R.id.etPassword);
        btSignIn = view.findViewById(R.id.btSignIn);
        btSignUp = view.findViewById(R.id.btSignUp);
        textView = view.findViewById(R.id.textView);

        /* 登入按鈕 */
        btSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etName.getText().toString();
                String password = etPassword.getText().toString();
                User user = new User(name, password);
                boolean isUserValid = false;


                if (Common.networkConnected(activity)){

                    JsonObject jo = new JsonObject();
                    jo.addProperty("action", "findByUser");
                    jo.addProperty("user", new Gson().toJson(user));

                    try{
                        loginTask = new CommonTask(URL_SERVER, jo.toString());
                        String loginResult = loginTask.execute().get();
                        isUserValid = Boolean.valueOf(loginResult);

                    } catch (Exception e){
                        Log.e(TAG, e.toString());
                    }
                }
                if(isUserValid){
                    Navigation.findNavController(textView)
                            .navigate(R.id.action_mainFragment_to_resultFragment);
                } else{
                    textView.setText(R.string.textSignInFail);
                }
            }
        });

        /* 註冊按鈕 */
        btSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etName.getText().toString();
                String password = etPassword.getText().toString();
                if (name.length() <= 0 || password.length() <= 0){
                    Toast.makeText(activity, R.string.textInvalidInput, Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                if (Common.networkConnected(activity)){

                    User user = new User(name, password);

                    JsonObject jo = new JsonObject();
                    jo.addProperty("action", "userInsert");
                    jo.addProperty("user", new Gson().toJson(user));

                    if (image != null){
                        jo.addProperty("imageBase64", Base64.encodeToString(image, Base64.DEFAULT));
                    }
                    int count = 0;
                    try{
                        signUpTask = new CommonTask(URL_SERVER, jo.toString());
                        String signUpResult = signUpTask.execute().get();
                        /* signUpResult 新增成功的資料筆數
                         * 因為是字串，需要轉回成 int 型態 */
                        count = Integer.valueOf(signUpResult);
                    } catch (Exception e){
                        Log.d(TAG, e.toString());
                    }
                    if (count == 0){
                        textView.setText(R.string.textSignUpFail);
                    } else {
                        textView.setText(R.string.textSignUpSuccess);
                    }
                } else{
                    Toast.makeText(activity, R.string.textNoNetwork, Toast.LENGTH_SHORT);
                }
            }
        });

        /* 拍照按鈕 */
        Button btTakePicture = view.findViewById(R.id.btTakePicture);
        btTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // 指定存檔路徑
                File file = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                file = new File(file, "picture.jpg");
                contentUri = FileProvider.getUriForFile(
                        activity, activity.getPackageName() + ".provider", file);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);

                if (intent.resolveActivity(activity.getPackageManager()) != null) {
                    startActivityForResult(intent, REQ_TAKE_PICTURE);
                } else {
                    Toast.makeText(activity, R.string.textNoCameraApp, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /* App 畫面停止時被執行 */
    @Override
    public void onStop() {
        super.onStop();
        if (loginTask != null){
            loginTask.cancel(true);
            loginTask = null;
        }
    }

    /* 當 startActivityForResult(Intent, int) 呼叫後 */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQ_TAKE_PICTURE:
                    crop(contentUri);
                    break;
                case REQ_PICK_PICTURE:
                    crop(intent.getData());
                    break;
                case REQ_CROP_PICTURE:
                    Uri uri = intent.getData();
                    Bitmap bitmap = null;
                    if (uri != null) {
                        try {
                            bitmap = BitmapFactory.decodeStream(
                                    activity.getContentResolver().openInputStream(uri));
                            ivUser.setImageBitmap(bitmap);

                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                            /* image 存著拍照完畢的結果 */
                            image = out.toByteArray();

                        } catch (FileNotFoundException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                    if (bitmap != null) {
                        ivUser.setImageBitmap(bitmap);
                    } else {
                        ivUser.setImageResource(R.drawable.no_image);
                    }
                    break;
            }
        }
    }

    /* 截圖方法 */
    private void crop(Uri sourceImageUri) {
        File file = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        file = new File(file, "picture_cropped.jpg");
        Uri uri = Uri.fromFile(file);
        // 開啟截圖功能
        Intent intent = new Intent("com.android.camera.action.CROP");
        // 授權讓截圖程式可以讀取資料
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // 設定圖片來源與類型
        intent.setDataAndType(sourceImageUri, "image/*");
        // 設定要截圖
        intent.putExtra("crop", "true");
        // 設定截圖框大小，0代表user任意調整大小
        intent.putExtra("aspectX", 0);
        intent.putExtra("aspectY", 0);
        // 設定圖片輸出寬高，0代表維持原尺寸
        intent.putExtra("outputX", 0);
        intent.putExtra("outputY", 0);
        // 是否保持原圖比例
        intent.putExtra("scale", true);
        // 設定截圖後圖片位置
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        // 設定是否要回傳值
        intent.putExtra("return-data", true);
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            // 開啟截圖 activity
            startActivityForResult(intent, REQ_CROP_PICTURE);
        } else {
             Toast.makeText(activity, R.string.textNoImageCropAppFound,
                    Toast.LENGTH_SHORT).show();
        }
    }
}