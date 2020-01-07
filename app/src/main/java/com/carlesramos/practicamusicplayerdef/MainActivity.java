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
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;
import com.carlesramos.practicamusicplayerdef.adapters.SongAdapter;
import com.carlesramos.practicamusicplayerdef.interficies.ISongListener;
import com.carlesramos.practicamusicplayerdef.services.MusicPlayerService;
import com.carlesramos.practicamusicplayerdef.viewmodel.MainActiViewModel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ISongListener {

    private static final int ACCES_MUSIC_DIRECTORY = 1;
    private RecyclerView rvSongs;
    private ImageButton btPlay;
    private ImageButton btNext;
    private ImageButton btPrev;
    private Button btNewPlaylist;
    private Button btSelectPlaylist;
    private boolean isPlaying;
    private MusicPlayerService musicPlayerService;
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
        isPlaying = false;
        btPlay = findViewById(R.id.ibPlayPause);
        btNext = findViewById(R.id.ibNext);
        btPrev = findViewById(R.id.ibPrev);
        btNewPlaylist = findViewById(R.id.btNewPlayList);
        btSelectPlaylist = findViewById(R.id.btSelectPlaylist);
        btPrev.setOnClickListener(this);
        btPlay.setOnClickListener(this);
        btNext.setOnClickListener(this);
        btNewPlaylist.setOnClickListener(this);
        btSelectPlaylist.setOnClickListener(this);

        rvSongs = findViewById(R.id.rvSong);
        mySeekBar = findViewById(R.id.sbProgress);
        habilitarBotons(false);

        //referencie el service i l'inicie
        final Intent intent = new Intent(this, MusicPlayerService.class);
        if (!isMyServiceRunning(MusicPlayerService.class)) {
            startService(intent);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
        else {
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }

        //Registre els recivers
        IntentFilter intitSeekbarFilter = new IntentFilter();
        intitSeekbarFilter.addAction(MusicPlayerService.INIT_SEEKBAR);
        final InitSeekReciver initSeekReciver = new InitSeekReciver();
        LocalBroadcastManager.getInstance(this).registerReceiver(initSeekReciver, intitSeekbarFilter);

        IntentFilter alertFilter = new IntentFilter();
        alertFilter.addAction(MusicPlayerService.ALERT);
        final AlertReciver alertReciver = new AlertReciver();
        LocalBroadcastManager.getInstance(this).registerReceiver(alertReciver, alertFilter);

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

    /**
     * Cree la conexió amb el service
     */
    private ServiceConnection connection = new ServiceConnection() {
        //Quan el service esta conectat, adapte el recyclerView.
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.LocalBinder binder = (MusicPlayerService.LocalBinder)service;
            musicPlayerService = binder.getService();
            //adapte les dades al recyclerView
            DividerItemDecoration itemDecorator = new DividerItemDecoration(MainActivity.this, DividerItemDecoration.VERTICAL);
            itemDecorator.setDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.divider));
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
            linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
            rvSongs.setLayoutManager(linearLayoutManager);
            rvSongs.setHasFixedSize(true);
            rvSongs.addItemDecoration(itemDecorator);
            rvSongs.setAdapter(new SongAdapter(MainActivity.this, musicPlayerService.getSongs(),
                    MainActivity.this));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(MainActivity.this, getText(R.string.serviceDisconnected), Toast.LENGTH_LONG).show();
        }

    };

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ibPlayPause : {
                if (!isPlaying){
                    playing();
                    habilitarBotons(true);
                    musicPlayerService.play();
                }
                else{
                    paused();
                    habilitarBotons(false);
                    musicPlayerService.pause();
                }
                break;
            }
            case R.id.ibNext : {
                musicPlayerService.next();
                break;
            }
            case R.id.ibPrev : {
                musicPlayerService.prev();
                break;
            }
            case R.id.btNewPlayList :{
                Toast.makeText(MainActivity.this, getText(R.string.fullVersion), Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.btSelectPlaylist : {
                Toast.makeText(MainActivity.this, getText(R.string.fullVersion), Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    @Override
    public void onSelectedSong(int position) {
        playing();
        habilitarBotons(true);
        musicPlayerService.play(position);
    }

    /**
     * Metode per saber si el servei esta en funcionament
     * la idea era crear una notificació i al tornar saber si el service esta corrent
     * @param serviceClass
     * @return si esta o no en funcionament
     */
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Métode per demanar els permisos, nomes serán demanats una vegada
     * si son acceptats
     */
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

    /**
     * Metode per iniciar  la seekbar
     * @param duration duració de la canço que es reprodueix.
     */
    public void initSeekBar(int duration){
        mySeekBar.setProgress(0);
        mySeekBar.setMax(duration);
    }

    /**
     * Intentava guardar per a quan es tornara a inciar la activity
     * que la seeckbar estara igual, es bona idea?
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.setSeeckBarPosition(mySeekBar.getProgress());
        viewModel.setSongDuration(musicPlayerService.getSongDuration());
        unbindService(connection);
    }

    /**
     * Metode per cambiar la icona
     */
    public void playing(){
        btPlay.setImageResource(R.drawable.pause);
        isPlaying = true;
    }
    /**
     * Metode per cambiar la icona
     */
    public void paused(){
        btPlay.setImageResource(R.drawable.baseline_play_arrow_black_18dp);
        isPlaying = false;
    }

    /**
     * Métode per habilitar o deshabilitar els botons next-prev
     * @param habilite pasem true per habilitar i al contrari
     */
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

    /**
     * BroadcastReciver per a iniciar la seeckbar
     * llançat per el servei
     */
    public class InitSeekReciver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            viewModel.setSeeckBarPosition(mySeekBar.getProgress());
            viewModel.setSongDuration(musicPlayerService.getSongDuration());
            initSeekBar(musicPlayerService.getSongDuration());
            Toast.makeText(musicPlayerService, getText(R.string.title)+
                            musicPlayerService.getSongTitle()
                    , Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * BroadcastReciver per a llançar la alerta, si no es troven arxius musicals
     * llançat per el servei
     */
    public class AlertReciver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle(getString(R.string.alertTitle));
            alertDialog.setMessage(getString(R.string.alertMessage));
            alertDialog.setIcon(R.drawable.jo);
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "TEST MODE",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
    }
}
