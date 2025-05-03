package com.cs213.androidphotos.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cs213.androidphotos.R;
import com.cs213.androidphotos.model.Album;
import com.cs213.androidphotos.util.AppDataManager;
import com.cs213.androidphotos.model.Photo;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class AlbumActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final int PICK_PHOTO_REQUEST = 1;
    
    private GridView photosGridView;
    private ArrayAdapter<Photo> photoAdapter;
    private ArrayList<Photo> photosList;
    private String albumName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        
        albumName = getIntent().getStringExtra("albumName");
        if (albumName == null || albumName.isEmpty()) {
            showErrorAndFinish("Invalid album");
            return;
        }
        
        TextView albumNameTextView = findViewById(R.id.albumNameTextView);
        albumNameTextView.setText(albumName);
        
        initializeViews();
        loadAlbumPhotos();
    }
    
    private void initializeViews() {
        photosGridView = findViewById(R.id.photosGridView);
        photosGridView.setOnItemClickListener(this);
        
        Button addPhotoButton = findViewById(R.id.addPhotoButton);
        addPhotoButton.setOnClickListener(v -> openPhotoPicker());

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        Button menuButton = findViewById(R.id.albumMenuButton);
        menuButton.setOnClickListener(v -> showAlbumActionsMenu());

    }

    private void showAlbumActionsMenu() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Album Actions")
            .setItems(new String[]{
                    "Rename Album", 
                    "Delete Album", 
                    "Search Photos"
            }, (dialog, which) -> {
                switch (which) {
                    case 0:
                        showRenameAlbumDialog();
                        break;
                    case 1:
                        confirmDeleteAlbum();
                        break;
                    case 2:
                        startActivity(new Intent(this, SearchActivity.class));
                        break;
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void loadAlbumPhotos() {
        Album album = AppDataManager.getInstance(this).getAlbum(albumName);
        if (album == null) {
            showErrorAndFinish("Album not found");
            return;
        }
        
        photosList = new ArrayList<>(album.getPhotos());
        photoAdapter = new ArrayAdapter<Photo>(this, 0, photosList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ImageView imageView;
                if (convertView == null) {
                    imageView = new ImageView(AlbumActivity.this);
                    int imageSize = getResources().getDisplayMetrics().widthPixels / 3 - 16;
                    imageView.setLayoutParams(new GridView.LayoutParams(imageSize, imageSize));
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageView.setPadding(8, 8, 8, 8);
                } else {
                    imageView = (ImageView) convertView;
                }
                
                Photo photo = getItem(position);
                if (photo != null) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeFile(photo.getFilePath());
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap);
                        } else {
                            imageView.setImageBitmap(null);
                        }
                    } catch (Exception e) {
                        imageView.setImageBitmap(null);
                    }
                }
                
                return imageView;
            }
        };
        
        photosGridView.setAdapter(photoAdapter);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Photo photo = photosList.get(position);
        Intent intent = new Intent(this, PhotoActivity.class);
        intent.putExtra("albumName", albumName);
        intent.putExtra("photoPath", photo.getFilePath());
        startActivity(intent);
    }
    
    private void showRenameAlbumDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setTitle("Rename Album")
            .setView(R.layout.dialog_rename_album)
            .setPositiveButton("Rename", (d, which) -> {
                TextInputEditText etNewName = ((AlertDialog)d).findViewById(R.id.etNewAlbumName);
                String newName = Objects.requireNonNull(etNewName).getText().toString().trim();
                renameAlbum(newName);
            })
            .setNegativeButton("Cancel", null)
            .create();
            
        dialog.show();
        
        TextInputEditText etNewName = dialog.findViewById(R.id.etNewAlbumName);
        if (etNewName != null) {
            etNewName.setText(albumName);
        }
    }
    
    private void renameAlbum(String newName) {
        if (newName.isEmpty()) {
            showToast("Album name cannot be empty");
            return;
        }
        
        if (AppDataManager.getInstance(this).getAlbum(newName) != null && !newName.equals(albumName)) {
            showToast("An album with this name already exists");
            return;
        }

        Album albumToRename = AppDataManager.getInstance(this).getAlbum(albumName);
        if (albumToRename == null) {
            showToast("Album not found");
            return;
        }
        
        boolean success = AppDataManager.getInstance(this).renameAlbum(albumToRename, newName);
        if (success) {
            albumName = newName;
            TextView albumNameTextView = findViewById(R.id.albumNameTextView);
            albumNameTextView.setText(albumName);
            showToast("Album renamed");
        } else {
            showToast("Failed to rename album");
        }
    }
    
    private void confirmDeleteAlbum() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Album")
            .setMessage("Are you sure you want to delete this album?")
            .setPositiveButton("Delete", (dialog, which) -> deleteAlbum())
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void deleteAlbum() {
        Album albumToDelete = AppDataManager.getInstance(this).getAlbum(albumName);
        if (albumToDelete == null) {
            showToast("Album not found");
            return;
        }
        
        boolean success = AppDataManager.getInstance(this).deleteAlbum(albumToDelete);
        if (success) {
            showToast("Album deleted");
            finish();
        } else {
            showToast("Failed to delete album");
        }
    }
    
    private void openPhotoPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_PHOTO_REQUEST);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_PHOTO_REQUEST && resultCode == RESULT_OK && data != null) {
            String filePath = data.getData().getPath();
            if (filePath != null) {
                Album album = AppDataManager.getInstance(this).getAlbum(albumName);
                if (album == null) {
                    showToast("Album not found");
                    return;
                }
                
                Photo addedPhoto = AppDataManager.getInstance(this).addPhotoToAlbum(album, filePath);
                
                if (addedPhoto != null) {
                    photosList.add(addedPhoto);
                    photoAdapter.notifyDataSetChanged();
                    showToast("Photo added successfully");
                } else {
                    showToast("Photo already exists in album");
                }
            }
        }
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void showErrorAndFinish(String message) {
        showToast(message);
        finish();
    }
}
    
    