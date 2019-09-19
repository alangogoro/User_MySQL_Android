package exer.user_mysql_android_new;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import exer.task.CommonTask;
import exer.user.User;

import static exer.task.Common.URL_SERVER;

public class MainFragment extends Fragment {
    private static final String TAG = "TAG_MainFragment";
    private Activity activity;
    private EditText etName, etPassword;
    private Button btSignIn, btSignUp;
    private TextView textView;

    private CommonTask loginTask;

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
        etName = view.findViewById(R.id.etName);
        etPassword = view.findViewById(R.id.etPassword);
        btSignIn = view.findViewById(R.id.btSignIn);
        btSignUp = view.findViewById(R.id.btSignUp);
        textView = view.findViewById(R.id.textView);

        btSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etName.getText().toString();
                String password = etPassword.getText().toString();
                User user = new User(name, password);

                JsonObject jo = new JsonObject();
                jo.addProperty("action", "findByUser");
                jo.addProperty("user", new Gson.toJson(user));

                loginTask = new CommonTask(URL_SERVER, jo.toString());
                //textView.setText(user.getName()+"\n"+user.getPassword());

            }
        });

        btSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etName.getText().toString();
                String password = etPassword.getText().toString();
                User user = new User(name, password);

                Navigation.findNavController(btSignUp)
                        .navigate(R.id.action_mainFragment_to_resultFragment);
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (loginTask != null){
            loginTask.cancel(true);
            loginTask = null;
        }
    }
}
