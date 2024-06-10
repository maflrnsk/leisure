package com.example.myapplication;
import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.myapplication.databinding.FragmentProfileBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;

public class Profile extends Fragment {
    private FragmentProfileBinding binding;
    private StorageReference reference;
    private static final int GALLERY_REQUEST_CODE = 1001;
    private static final int CAMERA_REQUEST_CODE = 1002;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1003;
    private static final String IMAGE_STORAGE_KEY = "profile_image";
    private Uri photoUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        binding.exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                NotificationHelper.cancelNotification(getActivity());
                startActivity(new Intent(getActivity(), LogIn.class));
                getActivity().finish();
            }
        });

        binding.change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPassword = binding.editpassword.getText().toString();
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    user.updatePassword(newPassword)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful())
                                        Toast.makeText(getActivity(), "Пароль успешно изменен", Toast.LENGTH_SHORT).show();
                                    else
                                        Toast.makeText(getActivity(), "Ошибка при изменении пароля", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
        });

        binding.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickerOptions();
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        loadUserData();
        loadImageData();
    }

    private void showImagePickerOptions() {
        CharSequence[] options = {"Выбрать из галереи", "Сделать фото"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Выберите вариант");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
                } else if (which == 1) {
                    if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                    } else {
                        startCamera();
                    }
                }
            }
        });
        builder.show();
    }

    private void startCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        photoUri = FileProvider.getUriForFile(getActivity(), "your.package.name.fileprovider", createImageFile());
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
    }

    private File createImageFile() {
        String imageFileName = "profile_image_" + System.currentTimeMillis();
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(storageDir, imageFileName + ".jpg");
    }

    private void loadImageData() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(IMAGE_STORAGE_KEY).child(userId);
        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(getActivity()).load(uri).into(binding.imageView);
                binding.imageView.setBackground(null);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("TAG", "Failed to load image", e);
            }
        });
    }

    private void loadUserData() {
        FirebaseFirestore store = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DocumentReference docref = store.collection("Users").document(userId);
        docref.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    String email = documentSnapshot.getString("Email");
                    String name = documentSnapshot.getString("Name");
                    if (name != null) {
                        binding.textView.setText(name);
                    }
                    if (email != null) {
                        binding.textView2.setText(email);
                    }
                } else {
                    Log.d("TAG", "No such document");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("TAG", "get failed with ", e);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Uri selectedImageUri = null;
            if (requestCode == GALLERY_REQUEST_CODE && data != null) {
                selectedImageUri = data.getData();
            } else if (requestCode == CAMERA_REQUEST_CODE) {
                selectedImageUri = photoUri;
            }
            if (selectedImageUri != null) {
                binding.imageView.setImageURI(selectedImageUri); // Отображаем новое изображение сразу
                binding.imageView.setBackground(null);
                uploadImageToFirebase(selectedImageUri);
            }
        }
    }

    private void uploadImageToFirebase(Uri selectedImageUri) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(IMAGE_STORAGE_KEY).child(userId);
        imageRef.putFile(selectedImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(getActivity(), "Аватарка успешно загружена", Toast.LENGTH_SHORT).show();
                loadImageData();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getActivity(), "Ошибка загрузки аватарки", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(getActivity(), "Не получилось сделать фото", Toast.LENGTH_SHORT).show();
            }
        }
    }
}




