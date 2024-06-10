package com.example.myapplication;
import java.time.LocalDateTime;

class Product {
    String name;
    String image;
    boolean box;
    LocalDateTime date;
    int imageResId;

    Product(String _describe, String _image, boolean _box, LocalDateTime _date){
        name = _describe;
        image = _image;
        box = _box;
        date = _date;
        this.imageResId = 0;
    }
    Product(String _describe, String _image, boolean _box){
        name = _describe;
        image = _image;
        box = _box;
    }

    public void setImageResId(int resId) {
        this.imageResId = resId;
    }
    public int getImageResId() {
        return imageResId;
    }

    public String getImage() {
        return image;
    }
}
