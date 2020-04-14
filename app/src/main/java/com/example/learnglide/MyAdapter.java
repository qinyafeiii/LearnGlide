package com.example.learnglide;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;


public class MyAdapter extends BaseAdapter {
    private Context context;

    public MyAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return 999;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (null == convertView) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }


        Bitmap bitmap = ImageLoadUtil.loadImage(context, position + "", 70, 70, 1);

        holder.iv.setImageBitmap(bitmap);
        return convertView;
    }

    class ViewHolder {
        ImageView iv;

        ViewHolder(View view) {
            iv = view.findViewById(R.id.iv);
        }
    }
}
