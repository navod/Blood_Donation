package com.example.blooddontation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.core.Tag;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class DonnorRegistrationActivity extends AppCompatActivity {

    private TextView backButton;

    private ImageView profile_image;

    private TextInputEditText registerFullName, registerIdNumber,
            registerPhoneNumber, registerEmail, registerPassword;

    private Spinner bloodGroupSpinner;

    private Button registerButton;

    private Uri resultUri;

    private ProgressDialog loader;

    private FirebaseAuth mAuth;

    private DatabaseReference userDatabaseRef;

    private static final int IMAGE_PICK_CODE = 1000;

    private static final int PERMISSION_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donnor_registration);

        backButton = findViewById(R.id.backButton);


        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DonnorRegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        profile_image = findViewById(R.id.profile_image);
        registerFullName = findViewById(R.id.registerFullName);
        registerIdNumber = findViewById(R.id.registerIdNumber);
        registerPhoneNumber = findViewById(R.id.registerPhoneNumber);
        registerEmail = findViewById(R.id.registerEmail);
        registerPassword = findViewById(R.id.registerPassword);
        bloodGroupSpinner = findViewById(R.id.bloodGroupSpinner);
        registerButton = findViewById(R.id.registerButton);

        mAuth = FirebaseAuth.getInstance();

        profile_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
                        requestPermissions(permissions, PERMISSION_CODE);
                    } else {
                        pickUpImageFromGallery();
                    }
                } else {
                    pickUpImageFromGallery();
                }
//                System.out.println("Hello");
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String email = registerEmail.getText().toString().trim();
                final String password = registerPassword.getText().toString().trim();
                final String fullName = registerFullName.getText().toString().trim();
                final String idNumber = registerIdNumber.getText().toString().trim();
                final String phoneNumber = registerPhoneNumber.getText().toString().trim();
                final String bloodGroup = bloodGroupSpinner.getSelectedItem().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    registerEmail.setError("Email is required!");
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    registerPassword.setError("Password is required!");
                    return;
                }

                if (TextUtils.isEmpty(fullName)) {
                    registerFullName.setError("Full name is required!");
                    return;
                }

                if (TextUtils.isEmpty(idNumber)) {
                    registerIdNumber.setError("Id number is required!");
                    return;
                }

                if (TextUtils.isEmpty(phoneNumber)) {
                    registerPhoneNumber.setError("Phone number is required!");
                    return;
                }

                if (bloodGroup.equals("Select your blood group")) {
                    Toast.makeText(DonnorRegistrationActivity.this, "Select blood group", Toast.LENGTH_SHORT).show();
                    return;
                }

                else {
                    loader = new ProgressDialog(DonnorRegistrationActivity.this);
                    loader.setMessage("Registering you..");
                    loader.setCanceledOnTouchOutside(false);
                    loader.show();
                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        String currentUserId = mAuth.getCurrentUser().getUid();
                                        userDatabaseRef = FirebaseDatabase.getInstance().getReference()
                                                .child("users").child(currentUserId);

                                        HashMap userInfo = new HashMap();
                                        userInfo.put("id", currentUserId);
                                        userInfo.put("name", fullName);
                                        userInfo.put("email", email);
                                        userInfo.put("idNumber", idNumber);
                                        userInfo.put("phoneNumber", phoneNumber);
                                        userInfo.put("bloodGroup", bloodGroup);
                                        userInfo.put("type", "donor");
                                        userInfo.put("search", "donor" + bloodGroup);

                                        userDatabaseRef.updateChildren(userInfo).addOnCompleteListener(new OnCompleteListener() {
                                            @Override
                                            public void onComplete(@NonNull Task task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(DonnorRegistrationActivity.this, "Data set successfully", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(DonnorRegistrationActivity.this, task.getException().toString(), Toast.LENGTH_SHORT).show();

                                                }

                                                finish();
//                                                loader.dismiss();
                                            }
                                        });

                                        if (resultUri != null) {
                                            final StorageReference filePatch = FirebaseStorage.getInstance().getReference()
                                                    .child("Profile image").child(currentUserId);

                                            Bitmap bitmap = null;

                                            try {
                                                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);

                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }

                                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, byteArrayOutputStream);
                                            byte[] data = byteArrayOutputStream.toByteArray();
                                            UploadTask uploadTask = filePatch.putBytes(data);
                                            uploadTask.addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(DonnorRegistrationActivity.this, "Image Upload failed!", Toast.LENGTH_SHORT).show();
                                                }
                                            });

                                            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                @Override
                                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                    if (taskSnapshot.getMetadata() != null && taskSnapshot.getMetadata().getReference() != null) {
                                                        Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                                                        result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                            @Override
                                                            public void onSuccess(Uri uri) {
                                                                String imageUrl = uri.toString();
                                                                Map newImageMap = new HashMap();
                                                                newImageMap.put("profilePictureUrl", imageUrl);
                                                                userDatabaseRef.updateChildren(newImageMap).addOnCompleteListener(new OnCompleteListener() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task task) {
                                                                        if (task.isSuccessful()) {
                                                                            Toast.makeText(DonnorRegistrationActivity.this, "Image url added to database successfully!", Toast.LENGTH_SHORT).show();
                                                                        } else {
                                                                            Toast.makeText(DonnorRegistrationActivity.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    }
                                                                });

                                                                finish();
                                                            }
                                                        });
                                                    }
                                                }
                                            });


                                            Intent intent = new Intent(DonnorRegistrationActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                            loader.dismiss();
                                        }
                                    } else {
                                        String error = task.getException().toString();
                                        Toast.makeText(DonnorRegistrationActivity.this, "Error" + error,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });
    }

    private void pickUpImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICK_CODE){
            resultUri = data.getData();
            profile_image.setImageURI(resultUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickUpImageFromGallery();
                } else {
                    Toast.makeText(this, "Permission denied..!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
