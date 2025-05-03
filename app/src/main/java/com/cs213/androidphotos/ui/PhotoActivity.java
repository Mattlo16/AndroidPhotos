package com.cs213.androidphotos.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cs213.androidphotos.R;
import com.cs213.androidphotos.model.Album;
import com.cs213.androidphotos.model.Photo;
import com.cs213.androidphotos.model.Tag;
import com.cs213.androidphotos.util.AppDataManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class PhotoActivity extends AppCompatActivity {
    private ImageView photoImageView;
    private TextView captionTextView;
    private TextView dateTextView;
    private RecyclerView tagsRecyclerView;
    private Spinner tagTypeSpinner;
    private EditText tagValueEditText;
    private Button addTagButton;
    private Button slideshowButton;
    private Button moveToAlbumButton;
    private Button backToAlbumButton;
    
    private String albumName;
    private Photo currentPhoto;
    private ArrayAdapter<String> tagsAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        
        // Initialize views
        photoImageView = findViewById(R.id.photoImageView);
        captionTextView = findViewById(R.id.captionTextView);
        dateTextView = findViewById(R.id.dateTextView);
        tagsRecyclerView = findViewById(R.id.tagsRecyclerView);
        tagTypeSpinner = findViewById(R.id.tagTypeSpinner);
        tagValueEditText = findViewById(R.id.tagValueEditText);
        addTagButton = findViewById(R.id.addTagButton);
        slideshowButton = findViewById(R.id.slideshowButton);
        moveToAlbumButton = findViewById(R.id.moveToAlbumButton);
        backToAlbumButton = findViewById(R.id.backToAlbumButton);
        
        // Setup RecyclerView
        tagsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tagsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        tagsRecyclerView.setAdapter(tagsAdapter);
        
        // Setup Spinner
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.tag_types, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tagTypeSpinner.setAdapter(spinnerAdapter);
        
        // Get intent data
        albumName = getIntent().getStringExtra("albumName");
        String photoUri = getIntent().getStringExtra("photoUri");
        
        if (albumName == null || photoUri == null) {
            showErrorAndFinish("Invalid photo data");
            return;
        }
        
        currentPhoto = AppDataManager.getInstance().getPhoto(albumName, photoUri);
        if (currentPhoto == null) {
            showErrorAndFinish("Photo not found");
            return;
        }
        
        setTitle(currentPhoto.getCaption().isEmpty() ? "Photo" : currentPhoto.getCaption());
        
        // Set click listeners
        addTagButton.setOnClickListener(v -> onAddTagClick());
        slideshowButton.setOnClickListener(v -> startSlideshow());
        moveToAlbumButton.setOnClickListener(v -> showMovePhotoDialog());
        backToAlbumButton.setOnClickListener(v -> finish());
        
        loadPhotoDetails();
    }
    
    private void loadPhotoDetails() {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                getContentResolver(), 
                Uri.parse(currentPhoto.getUri()));
            photoImageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            photoImageView.setImageResource(R.drawable.ic_broken_image);
        }
        
        captionTextView.setText(currentPhoto.getCaption().isEmpty() ? 
            "(No caption)" : currentPhoto.getCaption());
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault());
        dateTextView.setText(dateFormat.format(currentPhoto.getDate()));
        
        updateTagsList();
    }
    
    private void updateTagsList() {
        tagsAdapter.clear();
        for (Tag tag : currentPhoto.getTags()) {
            tagsAdapter.add(tag.toString());
        }
        tagsAdapter.notifyDataSetChanged();
    }
    
    private void onAddTagClick() {
        String tagName = tagTypeSpinner.getSelectedItem().toString();
        String tagValue = tagValueEditText.getText().toString().trim();
        
        if (tagValue.isEmpty()) {
            Toast.makeText(this, "Tag value cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Tag newTag = new Tag(tagName, tagValue);
        if (currentPhoto.addTag(newTag)) {
            AppDataManager.getInstance().updatePhoto(albumName, currentPhoto);
            tagValueEditText.setText("");
            updateTagsList();
        } else {
            Toast.makeText(this, "Tag already exists on this photo", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startSlideshow() {
        Intent intent = new Intent(this, SlideShowActivity.class);
        intent.putExtra("albumName", albumName);
        intent.putExtra("photoUri", currentPhoto.getUri());
        startActivity(intent);
    }
    
    private void showEditCaptionDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setTitle("Edit Caption")
            .setView(R.layout.dialog_edit_caption)
            .setPositiveButton("Save", (d, which) -> {
                EditText captionEditText = ((AlertDialog)d).findViewById(R.id.captionEditText);
                if (captionEditText != null) {
                    String newCaption = captionEditText.getText().toString().trim();
                    editCaption(newCaption);
                }
            })
            .setNegativeButton("Cancel", null)
            .create();
            
        dialog.show();
        
        EditText captionEditText = dialog.findViewById(R.id.captionEditText);
        if (captionEditText != null) {
            captionEditText.setText(currentPhoto.getCaption());
        }
    }
    
    private void editCaption(String newCaption) {
        currentPhoto.setCaption(newCaption);
        AppDataManager.getInstance().updatePhoto(albumName, currentPhoto);
        setTitle(newCaption.isEmpty() ? "Photo" : newCaption);
        captionTextView.setText(newCaption.isEmpty() ? "(No caption)" : newCaption);
    }
    
    private void confirmDeletePhoto() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Photo")
            .setMessage("Are you sure you want to delete this photo?")
            .setPositiveButton("Delete", (dialog, which) -> deletePhoto())
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void deletePhoto() {
        boolean success = AppDataManager.getInstance().removePhotoFromAlbum(albumName, currentPhoto);
        if (success) {
            Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to delete photo", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showMovePhotoDialog() {
        ArrayList<String> otherAlbums = new ArrayList<>(
            AppDataManager.getInstance().getAlbumNames()
        );
        otherAlbums.remove(albumName);
        
        if (otherAlbums.isEmpty()) {
            Toast.makeText(this, "No other albums available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] albumNames = otherAlbums.toArray(new String[0]);
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("Move Photo To...")
            .setItems(albumNames, (dialog, which) -> {
                String targetAlbum = albumNames[which];
                movePhotoToAlbum(targetAlbum);
            })
            .show();
    }
    
    private void movePhotoToAlbum(String targetAlbum) {
        boolean success = AppDataManager.getInstance().movePhotoBetweenAlbums(
            albumName, targetAlbum, currentPhoto
        );
        
        if (success) {
            Toast.makeText(this, "Photo moved to " + targetAlbum, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Photo already exists in target album", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showCopyPhotoDialog() {
        ArrayList<String> otherAlbums = new ArrayList<>(
            AppDataManager.getInstance().getAlbumNames()
        );
        otherAlbums.remove(albumName);
        
        if (otherAlbums.isEmpty()) {
            Toast.makeText(this, "No other albums available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] albumNames = otherAlbums.toArray(new String[0]);
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("Copy Photo To...")
            .setItems(albumNames, (dialog, which) -> {
                String targetAlbum = albumNames[which];
                copyPhotoToAlbum(targetAlbum);
            })
            .show();
    }
    
    private void copyPhotoToAlbum(String targetAlbum) {
        boolean success = AppDataManager.getInstance().addPhotoToAlbum(targetAlbum, currentPhoto);
        
        if (success) {
            Toast.makeText(this, "Photo copied to " + targetAlbum, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Photo already exists in target album", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }
}