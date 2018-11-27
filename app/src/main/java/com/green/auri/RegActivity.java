package com.green.auri;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegActivity extends AppCompatActivity{

    private FirebaseAuth mAuth;
    private Context context;
    private EditText password;
    private EditText email;
    private Button button_register;
    private TextView backTosign;
    private static final String TAG = "EmailPassword";
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg);

        email = (EditText) findViewById(R.id.textInputEditTextEmail);
        password = (EditText) findViewById(R.id.textInputEditTextPassword);
        button_register = (Button) findViewById(R.id.appCompatButtonRegister);
        backTosign = (TextView) findViewById(R.id.appCompatTextViewLoginLink);
        mAuth = FirebaseAuth.getInstance();

        // Share preference to save the login mode info
        sp = getSharedPreferences("login",MODE_PRIVATE);

        if(sp.getBoolean("logged",false)){
            Intent intent2 = new Intent(RegActivity.this, MainActivity.class);
            intent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP| Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent2);
            finish();
        }

        button_register.setOnClickListener(new View.OnClickListener() {
            // Click the Register button to register user
            @Override
            public void onClick(View v) {
                if (v == button_register) {
                    RegisterUser(); // class for the register action
                }
            }
        });
    }

    public void back(View v)
    { // Class that goes back to the login page if user has an account already
        if (v == backTosign) {
            Intent intent1 = new Intent(RegActivity.this, LoginActivity.class);
            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent1);
            finish();
//            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        }
    }


    // Register User Class
    public void RegisterUser(){
        String Email = email.getText().toString();
        String Password = password.getText().toString();

        if (TextUtils.isEmpty(Email)){
            Toast.makeText(this, "A Field is Empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(Password)){
            Toast.makeText(this, "A Field is Empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create User with Firebase
        mAuth.createUserWithEmailAndPassword(Email, Password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        try {
//                            Log.e("hi", "here");
                            //check if successful
                            if (task.isSuccessful()) {
                                Log.d(TAG, "createUserWithEmail:success");
                                //User is successfully registered and logged in
                                //Go to Main Activity Page here
                                Toast.makeText(RegActivity.this, "Registration Successful",
                                        Toast.LENGTH_SHORT).show();
//                                finish();
                                Intent intent2 = new Intent(RegActivity.this, MainActivity.class);
                                intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent2);
                                sp.edit().putBoolean("logged",true).apply();
                                finish();
//                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                            }else{
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                Toast.makeText(RegActivity.this, "Couldn't Register, Try Again",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                });
    }



}
