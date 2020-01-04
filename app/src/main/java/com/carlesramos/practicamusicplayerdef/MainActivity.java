package com.carlesramos.practicamusicplayerdef;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

import com.carlesramos.practicamusicplayerdef.adapters.SongAdapter;
import com.carlesramos.practicamusicplayerdef.interficies.ISongListener;
import com.carlesramos.practicamusicplayerdef.model.Song;
import com.carlesramos.practicamusicplayerdef.services.MusicPlayerService;
import com.carlesramos.practicamusicplayerdef.viewmodel.MainActiViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ISongListener {

    private static final int ACCES_MUSIC_DIRECTORY = 1;
    private RecyclerView rvSongs;
    private ImageButton btPlay;
    private ImageButton btNext;
    private ImageButton btPrev;
    private boolean isPlaying;
    private MusicPlayerService musicPlayerService;
    private ArrayList<Song> songs;
    private int arrayPosition;
    private SeekBar mySeekBar;
    private boolean isPaused;
    private Handler mHandler = new Handler();
    private MainActiViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        viewModel = new MainActiViewModel();
        songs = new ArrayList<>();
        getMusic();
        Collections.sort(songs);
        isPlaying = false;
        btPlay = findViewById(R.id.ibPlayPause);
        btNext = findViewById(R.id.ibNext);
        btPrev = findViewById(R.id.ibPrev);
        btPrev.setOnClickListener(this);
        btPlay.setOnClickListener(this);
        btNext.setOnClickListener(this);
        habilitarBotons(false);
        rvSongs = findViewById(R.id.rvSong);
        mySeekBar = findViewById(R.id.sbProgress);
        isPaused = false;
        final Intent intent = new Intent(this, MusicPlayerService.class);

        if (!isMyServiceRunning(MusicPlayerService.class)) {
            startService(intent);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
        else {
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }

        //Registre els recivers
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicPlayerService.PLAY_THIS);
        final PlaySongReciver playSongReciver = new PlaySongReciver();
        LocalBroadcastManager.getInstance(this).registerReceiver(playSongReciver, filter);
        //adapte les dades al recyclerView
        DividerItemDecoration itemDecorator = new DividerItemDecoration(MainActivity.this, DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.divider));
        rvSongs.setAdapter(new SongAdapter(MainActivity.this, songs,
                MainActivity.this));
        rvSongs.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        rvSongs.addItemDecoration(itemDecorator);

        //cree el executor per a la seekbar
        ScheduledExecutorService executors = Executors.newSingleThreadScheduledExecutor();
        executors.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if(musicPlayerService != null){
                    mySeekBar.setProgress(musicPlayerService.getPosition());
                }
                mHandler.postDelayed(this, 200);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        mySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser){
                    musicPlayerService.seekToPosition(progress);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(musicPlayerService.getPosition());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(musicPlayerService.getPosition());
            }
        });

    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.LocalBinder binder = (MusicPlayerService.LocalBinder)service;
            musicPlayerService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

    };

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ibPlayPause : {
                if (!isPlaying){
                    btPlay.setImageResource(R.drawable.pause);
                    musicPlayerService.play(this, songs.get(arrayPosition).getPath(), false);
                    if (!isPaused) {
                        mySeekBar.setMax(songs.get(arrayPosition).getDuration());
                    }
                    isPlaying = true;
                    habilitarBotons(true);
                }
                else{
                    btPlay.setImageResource(R.drawable.baseline_play_arrow_black_18dp);
                    musicPlayerService.pause();
                    isPlaying = false;
                    isPaused = true;
                    habilitarBotons(false);
                }
                break;
            }
            case R.id.ibNext : {
                arrayPosition++;
                if (arrayPosition >= songs.size() - 1){
                    arrayPosition = 0;
                }
                musicPlayerService.nextPrev(this, songs.get(arrayPosition).getPath());
                initSeekBar(songs.get(arrayPosition).getDuration());
                Toast.makeText(MainActivity.this, songs.get(arrayPosition).getTitle(), Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.ibPrev : {
                arrayPosition--;
                if (arrayPosition < 0){
                    arrayPosition = songs.size() - 1;
                }
                musicPlayerService.nextPrev(this, songs.get(arrayPosition).getPath());
                initSeekBar(songs.get(arrayPosition).getDuration());
                Toast.makeText(MainActivity.this, songs.get(arrayPosition).getTitle(), Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    @Override
    public void onSelectedSong(int position) {
        Toast.makeText(musicPlayerService, "Song: "+
        songs.get(position).
         getTitle(), Toast.LENGTH_SHORT).show();
        btPlay.setImageResource(R.drawable.pause);
        musicPlayerService.play(this, songs.get(position).getPath(),true);
        initSeekBar(songs.get(position).getDuration());
        arrayPosition = position;
        isPlaying = true;
        habilitarBotons(true);
    }

    public void habilitarBotons(boolean habilite){
        if (habilite){
            btPrev.setClickable(true);
            btNext.setClickable(true);
        }
        else{
            btNext.setClickable(false);
            btPrev.setClickable(false);
        }
    }

    public void getMusic(){
        String title;
        String artist;
        String album;
        int duration;
        String path;
        String imagePath;

        ContentResolver contentResolver = getContentResolver();
        Uri uri = EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor songCursor = contentResolver.query(uri, null, selection, null, sortOrder);
        if (songCursor != null && songCursor.moveToFirst()){
            int songTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int songArtist = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int songDuration = songCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int songLocation = songCursor.getColumnIndex(MediaStore.Audio.Media.DATA);

            while (songCursor.moveToNext()){
                title = songCursor.getString(songTitle);
                artist= songCursor.getString(songArtist);
                album = songCursor.getString(albumTitle);
                duration = songCursor.getInt(songDuration);
                path = songCursor.getString(songLocation);
                songs.add(new Song(title,artist,album,duration,path));
            }
            songCursor.close();
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void checkPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)){
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, ACCES_MUSIC_DIRECTORY);
            }
            else{
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, ACCES_MUSIC_DIRECTORY);
            }
        }
    }

    public void initSeekBar(int duration){
        mySeekBar.setProgress(0);
        mySeekBar.setMax(duration);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.setSeeckBarPosition(mySeekBar.getProgress());
        viewModel.setSongPosition(arrayPosition);
        unbindService(connection);
    }

    public class PlaySongReciver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (arrayPosition < songs.size()) {
                arrayPosition++;
            }
            else{
                arrayPosition = 0;
            }
            isPlaying = true;
            initSeekBar(songs.get(arrayPosition).getDuration());
            musicPlayerService.play(MainActivity.this, songs.get(arrayPosition).getPath(), false);
        }
    }

}
