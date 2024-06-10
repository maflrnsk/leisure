package com.example.myapplication;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class BoxAdapter extends BaseAdapter {
    Context ctx;
    LayoutInflater lInflater;
    ArrayList<Product> objects;
    boolean createCheckbox;
    boolean isAdmin;

    BoxAdapter(Context context, ArrayList<Product> products, boolean createCheckbox, boolean isAdmin){
        ctx = context;
        objects = products;
        lInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.createCheckbox = createCheckbox;
        this.isAdmin = isAdmin;
    }
    @Override
    public int getCount(){
        return objects.size();
    }
    @Override
    public Object getItem(int position){
        return objects.get(position);
    }
    @Override
    public long getItemId(int position){
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = lInflater.inflate(createCheckbox ? R.layout.item_event : R.layout.item, parent, false);
        }
        Product p = getProduct(position);
        ((TextView) view.findViewById(R.id.tvDescr)).setText(p.name);
        ImageView imageView = view.findViewById(R.id.ivImage);

        if (p.imageResId != 0) {
            // Если imageResId не равен 0, значит изображение загружено из ресурсов (R.drawable)
            imageView.setImageResource(p.imageResId);
        } else {
            // Если imageResId равен 0, значит изображение загружено из файла
            imageView.setImageURI(Uri.parse(p.image));
        }

        if (isAdmin) {
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((Event) ctx).onImageClick(position);
                }
            });
        }

        if (createCheckbox) {
            LocalDateTime date = p.date;
            DateTimeFormatter form = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String formatteddate = date != null ? date.format(form) : "";
            ((TextView) view.findViewById(R.id.tvData)).setText(formatteddate);
            if (!isAdmin) {
                CheckBox cbBuy = view.findViewById(R.id.cbBox);
                if (cbBuy != null) {
                    cbBuy.setChecked(p.box);
                    cbBuy.setTag(position);
                    cbBuy.setOnCheckedChangeListener(myCheckChangeList);
                }
            } else {
                CheckBox cbBuy = view.findViewById(R.id.cbBox);
                if (cbBuy != null) {
                    cbBuy.setVisibility(View.GONE);
                }
            }
        }
        return view;
    }



    Product getProduct(int position){
        return ((Product) getItem(position));
    }
    ArrayList<Product> getBox(){
        ArrayList<Product> box = new ArrayList<Product>();
        for (Product p : objects){
            if (p.box)
                box.add(p);
        }
        return box;
    }
    CompoundButton.OnCheckedChangeListener myCheckChangeList = new CompoundButton.OnCheckedChangeListener(){
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
            getProduct((Integer) buttonView.getTag()).box = isChecked;
        }
    };
}