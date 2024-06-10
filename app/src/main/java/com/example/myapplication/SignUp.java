package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivitySignUpBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUp extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore store;
    private ActivitySignUpBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        auth = FirebaseAuth.getInstance();
        store = FirebaseFirestore.getInstance();

        binding.switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    binding.switch1.setText("Администратор");
                } else {
                    binding.switch1.setText("Пользователь");
                }
            }
        });

        binding.buttonsignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = binding.editname.getText().toString().trim();
                String email = binding.editemail.getText().toString().trim();
                String password = binding.editpassword.getText().toString().trim();

                if (name.isEmpty()) {
                    binding.editname.setError("Имя не может быть пустым");
                    return;
                }
                if (email.isEmpty()) {
                    binding.editemail.setError("E-mail не может быть пустым");
                    return;
                }
                if (password.isEmpty()) {
                    binding.editpassword.setError("Пароль не может быть пустым");
                    return;
                }

                auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                Toast.makeText(SignUp.this, "Успешная регистрация", Toast.LENGTH_SHORT).show();
                                FirebaseUser user = auth.getCurrentUser();
                                DocumentReference docRef = store.collection("Users").document(user.getUid());
                                Map<String, Object> userInfo = new HashMap<>();
                                userInfo.put("Name", name);
                                userInfo.put("Email", email);

                                if (binding.switch1.isChecked()) {
                                    userInfo.put("Role", "Admin");
                                    docRef.set(userInfo);
                                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                    intent.putExtra("userRole", "Admin");
                                    startActivity(intent);
                                } else {
                                    userInfo.put("Role", "User");
                                    docRef.set(userInfo);
                                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                    intent.putExtra("userRole", "User");
                                    startActivity(intent);
                                }
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(SignUp.this, "Ошибка регистрации: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        binding.login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), LogIn.class));
            }
        });
    }
}

