package exer.main;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
/* 使用到 UI 畫面元件 */
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.widget.Button;
/* 使用到除錯 Debug 功能 */
import android.util.Log;
/* 使用到 RecyclerCardView */
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
/* 使用到 Gson */
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
/* 使用到 Java */
import java.util.List;
import java.lang.reflect.Type;
/* 使用到連線（新執行緒） */
import exer.task.Common;
import exer.task.CommonTask;
import exer.task.ImageTask;
import exer.user.User;


public class ResultFragment extends Fragment {
    private static final String TAG = "TAG_ResultFragment";
    private Activity activity;

    private RecyclerView rvUser;
    private List<User> users;

    /* 取得全部 User 的新執行緒工作（不含圖片） */
    private CommonTask userGetAllTask;
    /* 去取圖的 新執行緒工作 */
    private ImageTask userImageTask;

    private Button btSignOut;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        activity.setTitle(R.string.textResult);
        return inflater.inflate(R.layout.fragment_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvUser = view.findViewById(R.id.rvUser);

        rvUser.setLayoutManager(new LinearLayoutManager(activity));
        users = getUsers();
        showUsers(users);

        FloatingActionButton btAdd = view.findViewById(R.id.btBack);
        btAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Navigation.findNavController(btSignOut).popBackStack();
            }
        });
    }

    private List<User> getUsers() {
        List<User> users = null;
        if (Common.networkConnected(activity)) {
            // 指定網址
            String url = Common.URL_SERVER;
            JsonObject jo = new JsonObject();
            jo.addProperty("action", "getAll");

            String jsonOut = jo.toString();
            userGetAllTask = new CommonTask(url, jsonOut);

            try {

                String jsonIn = userGetAllTask.execute().get();

                Type listType = new TypeToken<List<User>>() {
                }.getType();
                /* 解析回 List<User> 型別 */
                users = new Gson().fromJson(jsonIn, listType);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Common.showToast(activity, R.string.textNoNetwork);
        }
        return users;
    }

    private void showUsers(List<User> users) {
        if (users == null || users.isEmpty()) {
            Common.showToast(activity, R.string.textNoUsersFound);
            return;
        }
        UserAdapter userAdapter = (UserAdapter) rvUser.getAdapter();
        // 如果userAdapter不存在就建立新的，否則續用舊有的
        /* 第一次進入是沒有 Adapter 的 */
        if (userAdapter == null) {
            rvUser.setAdapter(new UserAdapter(activity, users));
        } else {
            userAdapter.setUsers(users);
            userAdapter.notifyDataSetChanged();
        }
    }

    @Override   /* App 畫面停止時被執行 */
    public void onStop() {
        super.onStop();
        if (userGetAllTask != null) {
            userGetAllTask.cancel(true);
            userGetAllTask = null;
        }

        if (userImageTask != null) {
            userImageTask.cancel(true);
            userImageTask = null;
        }
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.MyViewHolder> {
        private LayoutInflater layoutInflater;
        private List<User> users;

        /* 宣告 imageSize 整數 */
        private int imageSize;

        /* 取得使用者螢幕的大小
         * 來決定列表縮圖的大小 */
        UserAdapter(Context context, List<User> users) {
            layoutInflater = LayoutInflater.from(context);
            this.users = users;

            /* 縮圖大小＝螢幕寬度 ÷ 4
             * 還要依照原圖等比例的鎖小 */
            imageSize = getResources().getDisplayMetrics().widthPixels / 4;
        }

        void setUsers(List<User> users) {
            this.users = users;
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView tvName, tvPassword;

            MyViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.ivUser);
                tvName = itemView.findViewById(R.id.tvName);
                tvPassword = itemView.findViewById(R.id.tvPassword);
            }
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = layoutInflater.inflate(R.layout.itemview_user, parent, false);
            return new MyViewHolder(itemView);
        }

        /* 在 onBindViewHolder 內才會去查出圖片的資料
         * 先載入文字內容
         * 因為圖片體積太大所以選擇直到這裡才做載入 */
        @Override
        public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int position) {
            final User user = users.get(position);
            String url = Common.URL_SERVER;

            int id = user.getId();
            userImageTask = new ImageTask(url, id, imageSize, myViewHolder.imageView);

            /* 不寫 execute().get()
             * 執行緒就不會等待執行結果，圖片會漸次載入 */
            userImageTask.execute();

            myViewHolder.tvName.setText(user.getName());
            myViewHolder.tvPassword.setText(user.getPassword());

            /* User id Problem */
        }

    }
}