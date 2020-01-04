package com.carlesramos.practicamusicplayerdef.viewmodel;

import androidx.lifecycle.ViewModel;

public class MainActiViewModel extends ViewModel {
    private int seeckBarPosition;
    private int songPosition;

    public int getSeeckBarPosition() {
        return seeckBarPosition;
    }

    public void setSeeckBarPosition(int seeckBarPosition) {
        this.seeckBarPosition = seeckBarPosition;
    }

    public int getSongPosition() {
        return songPosition;
    }

    public void setSongPosition(int songPosition) {
        this.songPosition = songPosition;
    }

}
