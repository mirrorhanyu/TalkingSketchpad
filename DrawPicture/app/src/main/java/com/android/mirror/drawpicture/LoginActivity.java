package com.android.mirror.drawpicture;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import static android.text.TextUtils.*;

public class LoginActivity extends Activity{

    private EditText mUsernameView;

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mUsernameView = (EditText)findViewById(R.id.username_input);
        mUsernameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == R.id.login || actionId == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
        Button signInButton = (Button)findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });
    }

    private void attemptLogin(){
        mUsernameView.setError(null);
        String username = mUsernameView.getText().toString().trim();
        if(isEmpty(username)){
            mUsernameView.setError("Username required");
            mUsernameView.requestFocus();
            return;
        }
        Intent intent = new Intent();
        intent.putExtra("username", username);
        setResult(RESULT_OK, intent);
        finish();
    }
}
