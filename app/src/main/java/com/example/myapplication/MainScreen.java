package com.example.myapplication;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.example.myapplication.databinding.FragmentMainScreenBinding;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

public class MainScreen extends Fragment {
    private FragmentMainScreenBinding binding;
    private ArrayList<Product> products;
    private BoxAdapter boxAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        products = loadProductsFromJson();
        boxAdapter = new BoxAdapter(requireContext(), products, false, false); // для MainScreen
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMainScreenBinding.inflate(inflater, container, false);
        setButtonListeners();
        return binding.getRoot();
    }

    private ArrayList<Product> loadProductsFromJson() {
        ArrayList<Product> products = new ArrayList<>();
        try {
            // Открываем файл в папке assets
            InputStream inputStream = requireContext().getAssets().open("products.json");
            InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");

            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Product>>() {}.getType();
            products = gson.fromJson(reader, listType);

            for (Product product : products) {
                int resId = requireContext().getResources().getIdentifier(product.image, "drawable", requireContext().getPackageName());
                product.setImageResId(resId);
                Log.d("MainScreen", "Product: " + product.name + ", ImageResId: " + resId);
            }
            reader.close();
        } catch (IOException e) {
            Log.e("MainScreen", "Error reading JSON file", e);
        }

        return products;
    }



    protected void setButtonListeners() {
        ListView lvMain = binding.lvMain;
        lvMain.setAdapter(boxAdapter);
        binding.lvMain.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(view.getContext(), Event.class);
                intent.putExtra("userRole", "User");
                intent.putExtra("position", position);
                startActivity(intent);
            }
        });
    }
}