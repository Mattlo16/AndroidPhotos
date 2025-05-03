package com.cs213.androidphotos.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cs213.androidphotos.R;
import com.cs213.androidphotos.model.Album;
import com.cs213.androidphotos.util.AppDataManager;

public class MainActivity extends AppCompatActivity {

    private AppDataManager dataManager;
    private GridLayout albumsGrid;
    private EditText newAlbumNameEditText;
    private Button createAlbumButton;
    private Button searchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    
        // Initialize AppDataManager
        dataManager = AppDataManager.getInstance(this);
    
        // Initialize UI components
        albumsGrid = findViewById(R.id.albumsGrid);
        newAlbumNameEditText = findViewById(R.id.newAlbumNameEditText);
        createAlbumButton = findViewById(R.id.createAlbumButton);
        searchButton = findViewById(R.id.searchButton);
    
        createAlbumButton.setOnClickListener(v -> createNewAlbum());
        searchButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            startActivity(intent);
        });
    
        // Display albums
        displayAlbums();
    }

    private void createNewAlbumLayout() {
        // Create layout for adding new albums
        newAlbumNameEditText = new EditText(this);
        newAlbumNameEditText.setHint(R.string.new_album_name);

        createAlbumButton = new Button(this);
        createAlbumButton.setText(R.string.create_album);
        createAlbumButton.setOnClickListener(v -> createNewAlbum());

        searchButton = new Button(this);
        searchButton.setText(R.string.search_photos);
        searchButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            startActivity(intent);
        });
    }

    private void createNewAlbum() {
        String albumName = newAlbumNameEditText.getText().toString().trim();
        if (albumName.isEmpty()) {
            Toast.makeText(this, "Album name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Album album = dataManager.createAlbum(albumName);
        if (album != null) {
            newAlbumNameEditText.setText("");
            displayAlbums();
            Toast.makeText(this, "Album created successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "An album with this name already exists", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayAlbums() {
        albumsGrid.removeAllViews();

        for (Album album : dataManager.getAlbums()) {
            View albumView = getLayoutInflater().inflate(R.layout.item_album, albumsGrid, false);
            TextView albumNameTextView = albumView.findViewById(R.id.albumNameTextView);
            Button openButton = albumView.findViewById(R.id.openAlbumButton);
            Button deleteButton = albumView.findViewById(R.id.deleteAlbumButton);
            Button renameButton = albumView.findViewById(R.id.renameAlbumButton);

            albumNameTextView.setText(album.getName());

            openButton.setOnClickListener(v -> openAlbum(album));
            deleteButton.setOnClickListener(v -> confirmDeleteAlbum(album));
            renameButton.setOnClickListener(v -> showRenameDialog(album));

            albumsGrid.addView(albumView);
        }
    }

    private void openAlbum(Album album) {
        Intent intent = new Intent(MainActivity.this, AlbumActivity.class);
        intent.putExtra("albumName", album.getName());
        startActivity(intent);
    }

    private void confirmDeleteAlbum(Album album) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_album_title)
                .setMessage(R.string.dialog_delete_album_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    dataManager.deleteAlbum(album);
                    displayAlbums();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showRenameDialog(Album album) {
        EditText input = new EditText(this);
        input.setText(album.getName());

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_rename_album_title)
                .setView(input)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        if (dataManager.renameAlbum(album, newName)) {
                            displayAlbums();
                        } else {
                            Toast.makeText(this, "An album with this name already exists", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}