package hu.zeletrik.wear.glanceface;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MainMenuAdapter extends RecyclerView.Adapter<MainMenuAdapter.RecyclerViewHolder> {

    private ArrayList<MenuItem> dataSource;
    public interface AdapterCallback{
        void onItemClicked(Integer menuPosition);
    }
    private AdapterCallback callback;

    private String drawableIcon;
    private Context context;


    public MainMenuAdapter(Context context, ArrayList<MenuItem> dataArgs, AdapterCallback callback){
        this.context = context;
        this.dataSource = dataArgs;
        this.callback = callback;
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.main_menu_item,parent,false);

        return new RecyclerViewHolder(view);
    }

    public static class RecyclerViewHolder extends RecyclerView.ViewHolder
    {
        RelativeLayout menuContainer;
        TextView menuItem;
        ImageView menuIcon;

        public RecyclerViewHolder(View view) {
            super(view);
            menuContainer = view.findViewById(R.id.menu_container);
            menuItem = view.findViewById(R.id.menu_item);
            menuIcon = view.findViewById(R.id.menu_icon);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, final int position) {
        MenuItem data_provider = dataSource.get(position);

        holder.menuItem.setText(data_provider.getText());
        holder.menuIcon.setImageResource(data_provider.getImage());
        holder.menuContainer.setOnClickListener(v -> {

            if(callback != null) {
                callback.onItemClicked(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataSource.size();
    }
}

class MenuItem {
    private String text;
    private int image;

    public MenuItem(int image, String text) {
        this.image = image;
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public int getImage() {
        return image;
    }
}
