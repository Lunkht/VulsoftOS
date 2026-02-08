package com.vulsoft.vulsoftos;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class ScreenRecorderTileService extends TileService {

    @Override
    public void onClick() {
        super.onClick();
        
        // Start the Screen Recorder Service to show floating controls
        Intent intent = new Intent(this, ScreenRecorderService.class);
        intent.setAction(ScreenRecorderService.ACTION_SHOW_CONTROLS);
        
        // Use startService directly. 
        // The service will show a floating view (TYPE_APPLICATION_OVERLAY).
        // To ensure the user sees it, we should collapse the status bar.
        // startActivityAndCollapse is the standard way to collapse, but it requires an Activity.
        // We can just start the service and assume the user will swipe up, 
        // or we can launch a dummy transparent activity to collapse.
        // For now, simple startService.
        try {
            startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if (tile != null) {
            // We don't track recording state here yet, so just show as inactive (clickable)
            // or active if we want it to look "on". 
            // Since this tile launches the controls (which might be separate from recording state),
            // let's keep it inactive but with the correct label/icon.
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel(getString(R.string.screen_recorder_title));
            tile.updateTile();
        }
    }
}
