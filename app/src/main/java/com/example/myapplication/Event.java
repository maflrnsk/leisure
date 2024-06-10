package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivityEventBinding;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Event extends AppCompatActivity {
    private ActivityEventBinding binding;
    private ArrayList<Product> products;
    private BoxAdapter boxAdapter;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private ArrayList<LocalDate> savedDates;
    private ArrayList<String> savedNames;
    private String userRole;
    private int position;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEventBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();

        userRole = getIntent().getStringExtra("userRole");
        position = getIntent().getIntExtra("position", 0);
        products = new ArrayList<>();

        copyAssetsToInternalStorage();
        loadEventData(position);

        boxAdapter = new BoxAdapter(this, products, true, "Admin".equals(userRole));

        setButtonListeners();
        loadDatesAndNamesFromFirestore();

        ListView lvMain = binding.lvMain;
        lvMain.setAdapter(boxAdapter);
        lvMain.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if ("Admin".equals(userRole)) {
                    showDeleteEventDialog(position);
                }
            }
        });

        if ("Admin".equals(userRole)) {
            binding.add.setText("Добавить событие");
        }
        /*File file = new File(getFilesDir(), "events.json");
        if (file.exists()) {
            if (file.delete()) {
                Toast.makeText(this, "Файл events.json успешно удалён", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Не удалось удалить файл events.json", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Файл events.json не найден", Toast.LENGTH_SHORT).show();
        }*/
    }
    private void showDeleteEventDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Хотите удалить это событие?");
        builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteEvent(position);
            }
        });
        builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void deleteEvent(int position) {
        Product deletedProduct = products.remove(position);
        updateJsonFileAfterDelete(deletedProduct);
        boxAdapter.notifyDataSetChanged();
        Toast.makeText(this, "Событие успешно удалено", Toast.LENGTH_SHORT).show();
    }

    private void updateJsonFileAfterDelete(Product deletedProduct) {
        try {
            File file = new File(getFilesDir(), "events.json");
            InputStream is = new FileInputStream(file);
            Scanner scanner = new Scanner(is).useDelimiter("\\A");
            String jsonString = scanner.hasNext() ? scanner.next() : "";

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
            JsonArray eventArray = jsonObject.getAsJsonArray("event");
            for (Iterator<JsonElement> iterator = eventArray.iterator(); iterator.hasNext(); ) {
                JsonObject eventObject = iterator.next().getAsJsonObject();
                JsonArray namesArray = eventObject.getAsJsonArray("namesArray");
                JsonArray drawableArray = eventObject.getAsJsonArray("drawableArray");
                JsonArray dateArray = eventObject.getAsJsonArray("dateArray");
                int indexToRemove = -1;
                for (int i = 0; i < namesArray.size(); i++) {
                    if (namesArray.get(i).getAsString().equals(deletedProduct.name)) {
                        indexToRemove = i;
                        break;
                    }
                }
                if (indexToRemove != -1) {
                    namesArray.remove(indexToRemove);
                    drawableArray.remove(indexToRemove);
                    dateArray.remove(indexToRemove);
                }
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(gson.toJson(jsonObject).getBytes());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    private void loadEventData(int position) {
        try {
            File file = new File(getFilesDir(), "events.json");
            InputStream is = new FileInputStream(file);
            Scanner scanner = new Scanner(is).useDelimiter("\\A");
            String jsonString = scanner.hasNext() ? scanner.next() : "";

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
            JsonArray eventArray = jsonObject.getAsJsonArray("event");

            for (JsonElement element : eventArray) {
                JsonObject eventObject = element.getAsJsonObject();
                int eventPosition = eventObject.get("position").getAsInt();
                if (eventPosition == position) {
                    JsonArray namesArray = eventObject.getAsJsonArray("namesArray");
                    JsonArray drawableArray = eventObject.getAsJsonArray("drawableArray");
                    JsonArray dateArray = eventObject.getAsJsonArray("dateArray");

                    for (int i = 0; i < namesArray.size(); i++) {
                        String name = namesArray.get(i).getAsString();
                        String image = drawableArray.get(i).getAsString();
                        String date = dateArray.get(i).getAsString();

                        String imageName = image.substring(image.lastIndexOf(".") + 1);
                        int drawableId = getResources().getIdentifier(imageName, "drawable", getPackageName());

                        LocalDate localDate = LocalDate.parse(date);

                        Product product = new Product(name, image, false, localDate.atStartOfDay());
                        product.setImageResId(drawableId);
                        products.add(product);
                    }
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    private void loadDatesAndNamesFromFirestore() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference datesRef = firestore.collection("Users").document(userId);
            datesRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        savedDates = new ArrayList<>();
                        savedNames = new ArrayList<>();
                        if (document.exists()) {
                            ArrayList<String> dateStrings = (ArrayList<String>) document.get("dates");
                            ArrayList<String> nameStrings = (ArrayList<String>) document.get("names");
                            if (dateStrings != null) {
                                for (String dateString : dateStrings) {
                                    savedDates.add(LocalDate.parse(dateString));
                                }
                            }
                            if (nameStrings != null) {
                                savedNames.addAll(nameStrings);
                            }
                        }
                    } else {
                        savedDates = new ArrayList<>();
                        savedNames = new ArrayList<>();
                    }
                }
            });
        }
    }
    private void copyAssetsToInternalStorage() {
        File file = new File(getFilesDir(), "events.json");
        if (!file.exists()) {
            try (InputStream is = getAssets().open("events.json");
                 OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void setButtonListeners() {
        ListView lvMain = binding.lvMain;
        lvMain.setAdapter(boxAdapter);
        binding.exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        binding.add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("Admin".equals(userRole)) {
                    showAddEventDialog();
                } else {
                    ArrayList<LocalDate> newDates = new ArrayList<>();
                    ArrayList<String> newNames = new ArrayList<>();
                    for (int i = 0; i < lvMain.getChildCount(); i++) {
                        View item = lvMain.getChildAt(i);
                        CheckBox checkBox = item.findViewById(R.id.cbBox);
                        if (checkBox.isChecked()) {
                            Product selectedProduct = (Product) boxAdapter.getItem(i);
                            LocalDate selectedDate = LocalDate.from(selectedProduct.date);
                            String selectedName = selectedProduct.name;
                            if (!savedDates.contains(selectedDate)) {
                                newDates.add(selectedDate);
                                newNames.add(selectedName);
                            }
                        }
                    }
                    saveDatesAndNamesToFirestore(newDates, newNames);
                }
            }
        });
    }

    private void showAddEventDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_event, null);
        builder.setView(dialogView);

        EditText eventNameInput = dialogView.findViewById(R.id.eventNameInput);
        DatePicker eventDatePicker = dialogView.findViewById(R.id.eventDatePicker);

        builder.setPositiveButton("Добавить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                String eventName = eventNameInput.getText().toString().trim();
                int day = eventDatePicker.getDayOfMonth();
                int month = eventDatePicker.getMonth();
                int year = eventDatePicker.getYear();
                LocalDate eventDate = LocalDate.of(year, month + 1, day);

                if (!eventName.isEmpty()) {
                    addEventToJsonFile(eventName, eventDate);
                } else {
                    Toast.makeText(Event.this, "Название события не может быть пустым", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Назад", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void addEventToJsonFile(String eventName, LocalDate eventDate) {
        try {
            File file = new File(getFilesDir(), "events.json");
            InputStream is = new FileInputStream(file);
            Scanner scanner = new Scanner(is).useDelimiter("\\A");
            String jsonString = scanner.hasNext() ? scanner.next() : "";

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
            JsonArray eventArray = jsonObject.getAsJsonArray("event");
            for (JsonElement element : eventArray) {
                JsonObject eventObject = element.getAsJsonObject();
                JsonArray namesArray = eventObject.getAsJsonArray("namesArray");
                for (JsonElement nameElement : namesArray) {
                    if (nameElement.getAsString().equalsIgnoreCase(eventName)) {
                        Toast.makeText(this, "Событие с таким названием уже существует", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }

            JsonObject targetEventObject = null;
            for (JsonElement element : eventArray) {
                JsonObject eventObject = element.getAsJsonObject();
                int eventPosition = eventObject.get("position").getAsInt();
                if (eventPosition == position) {
                    targetEventObject = eventObject;
                    break;
                }
            }

            if (targetEventObject != null) {
                JsonArray namesArray = targetEventObject.getAsJsonArray("namesArray");
                JsonArray drawableArray = targetEventObject.getAsJsonArray("drawableArray");
                JsonArray dateArray = targetEventObject.getAsJsonArray("dateArray");

                namesArray.add(eventName);
                drawableArray.add("R.drawable.photo");
                dateArray.add(eventDate.toString());
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(jsonObject.toString().getBytes());
                }
                products.clear();
                loadEventData(position);
                boxAdapter.notifyDataSetChanged();

                Toast.makeText(this, "Событие добавлено", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Не удалось найти событие", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(this, "Ошибка при добавлении события", Toast.LENGTH_SHORT).show();
        }
    }


    private void saveDatesAndNamesToFirestore(ArrayList<LocalDate> newDates, ArrayList<String> newNames) {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userDocRef = firestore.collection("Users").document(userId);
            userDocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Map<String, Object> existingData = (Map<String, Object>) document.get("datanames");
                            if (existingData != null) {
                                savedDates = new ArrayList<>();
                                savedNames = new ArrayList<>();
                                for (Map.Entry<String, Object> entry : existingData.entrySet()) {
                                    String name = entry.getKey();
                                    Object value = entry.getValue();
                                    if (value instanceof String) {
                                        try {
                                            LocalDate date = LocalDate.parse((String) value);
                                            savedNames.add(name);
                                            savedDates.add(date);
                                        } catch (DateTimeParseException e) {
                                            Log.e("DateParsingError", "Failed to parse date for name: " + name, e);
                                        }
                                    }
                                }
                            } else {
                                existingData = new HashMap<>();
                            }
                            for (int i = 0; i < newDates.size(); i++) {
                                LocalDate date = newDates.get(i);
                                String name = newNames.get(i);
                                if (!savedNames.contains(name)) {
                                    savedNames.add(name);
                                    savedDates.add(date);
                                    existingData.put(name, date.toString());
                                }
                            }
                            userDocRef.update("datanames", existingData).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(Event.this, "Новое событие успешно добавлено", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(Event.this, MainActivity.class);
                                        intent.putExtra("dates", savedDates);
                                        startActivity(intent);
                                    } else {
                                        Toast.makeText(Event.this, "Ошибка при добавлении события", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                        } else {
                            Toast.makeText(Event.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(Event.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private static final int PICK_IMAGE_REQUEST = 1;
    public void onImageClick(int position) {
        this.position = position; // Save the position to update later
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra("position", position);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                String imagePath = saveImageToInternalStorage(selectedImage, position);

                // Determine selectedImageIndex
                int selectedImageIndex = determineSelectedImageIndex(imagePath);

                updateJsonFileWithNewImage(position, selectedImageIndex, imagePath);
                Product product = products.get(position);
                product.image = imagePath;
                product.setImageResId(0); // Если изображение из файла, resId будет 0
                boxAdapter.notifyDataSetChanged();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private int determineSelectedImageIndex(String imagePath) {
        int selectedImageIndex = -1;
        try {
            File file = new File(getFilesDir(), "events.json");
            InputStream is = new FileInputStream(file);
            Scanner scanner = new Scanner(is).useDelimiter("\\A");
            String jsonString = scanner.hasNext() ? scanner.next() : "";

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
            JsonArray eventArray = jsonObject.getAsJsonArray("event");

            // Iterate through eventArray to find the index of imagePath
            for (JsonElement element : eventArray) {
                JsonObject eventObject = element.getAsJsonObject();
                JsonArray drawableArray = eventObject.getAsJsonArray("drawableArray");
                for (int i = 0; i < drawableArray.size(); i++) {
                    String drawableImagePath = drawableArray.get(i).getAsString();
                    if (drawableImagePath.equals(imagePath)) {
                        selectedImageIndex = i;
                        break;
                    }
                }
                if (selectedImageIndex != -1) {
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return selectedImageIndex;
    }



    private String saveImageToInternalStorage(Bitmap bitmap, int position) {
        String fileName = "event_image_" + position + ".png";
        File file = new File(getFilesDir(), fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void updateJsonFileWithNewImage(int position, int imageIndex, String imagePath) {
        try {
            File file = new File(getFilesDir(), "events.json");
            InputStream is = new FileInputStream(file);
            Scanner scanner = new Scanner(is).useDelimiter("\\A");
            String jsonString = scanner.hasNext() ? scanner.next() : "";

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
            JsonArray eventArray = jsonObject.getAsJsonArray("event");
            Log.d("UpdatedJsonObject", "Updated JSON object: " + jsonObject.toString());

            for (JsonElement element : eventArray) {
                JsonObject eventObject = element.getAsJsonObject();
                int eventPosition = eventObject.get("position").getAsInt();
                if (eventPosition == position) {
                    JsonArray drawableArray = eventObject.getAsJsonArray("drawableArray");
                    if (imageIndex < drawableArray.size()) {
                        drawableArray.set(imageIndex, new JsonPrimitive(imagePath));

                        // Update the Product object in the list
                        Product product = products.get(position);
                        product.image = imagePath;
                        Log.d("ImagePath", "New image path: " + imagePath);
                        product.setImageResId(0); // Если изображение из файла, resId будет 0
                    }
                    break;
                }
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(gson.toJson(jsonObject).getBytes());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // Notify the adapter about the data change
        boxAdapter.notifyDataSetChanged();
    }

}
