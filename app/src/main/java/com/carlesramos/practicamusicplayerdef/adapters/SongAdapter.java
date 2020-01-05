package com.carlesramos.practicamusicplayerdef.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.carlesramos.practicamusicplayerdef.R;
import com.carlesramos.practicamusicplayerdef.interficies.ISongListener;
import com.carlesramos.practicamusicplayerdef.model.Song;

import java.util.ArrayList;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder>{

    private ArrayList<Song> songs;
    private Context context;
    private ISongListener listener;
    private int selectedPos = 0;
    public SongAdapter(Context context, ArrayList<Song> songs, ISongListener listener) {
        this.songs = songs;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.song_item_new, parent, false);
        SongViewHolder viewHolder = new SongViewHolder(itemView, context, listener);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.itemView.setBackgroundColor(selectedPos == position ? Color.GRAY : Color.TRANSPARENT);
        holder.bindSong(song);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }


    public class SongViewHolder extends RecyclerView.ViewHolder implements View

            .OnClickListener{

        private TextView tvTitle;
        private TextView tvArtist;
        private TextView tvAlbum;
        private TextView tvMinutos;
        private TextView tvSegundos;
        private Context context;
        private ISongListener listener;

        public SongViewHolder(@NonNull View itemView, Context context, ISongListener listener) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitleNew);
            tvArtist = itemView.findViewById(R.id.tvArtistNew);
            tvAlbum = itemView.findViewById(R.id.tvAlbumNew);
            tvMinutos = itemView.findViewById(R.id.tvMinutos);
            tvSegundos = itemView.findViewById(R.id.tvSegundos);
            this.context = context;
            this.listener = listener;
            itemView.setOnClickListener(this);
        }

        public void bindSong(Song s){
            tvTitle.setText(s.getTitle());
            tvArtist.setText(s.getArtist());
            tvAlbum.setText(s.getAlbum());
            tvMinutos.setText(String.valueOf(s.getMinuts()));
            tvSegundos.setText(String.valueOf(s.getSeconds()));
        }

        @Override
        public void onClick(View v) {
            notifyItemChanged(selectedPos);
            selectedPos = getAdapterPosition();
            notifyItemChanged(selectedPos);
            listener.onSelectedSong(getAdapterPosition());
        }
    }
}
