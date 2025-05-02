package com.cs213.androidphotos.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cs213.androidphotos.R;
import com.cs213.androidphotos.data.Album;
import com.cs213.androidphotos.data.AppDataManager;
import com.cs213.androidphotos.data.Photo;
import com.cs213.androidphotos.data.Tag;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class PhotoActivity extends AppCompatActivity {
    private ImageView photoImageView;
    private TextView captionTextView;
    private TextView dateTextView;
    private ListView tagsListView;
    private EditText tagNameEditText;
    private EditText tagValueEditText;
    
    private String albumName;
    private Photo currentPhoto;
    private ArrayAdapter<String> tagsAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        
        photoImageView = findViewById(R.id.photoImageView);
        captionTextView = findViewById(R.id.captionTextView);
        dateTextView = findViewById(R.id.dateTextView);
        tagsListView = findViewById(R.id.tagsListView);
        tagNameEditText = findViewById(R.id.tagNameEditText);
        tagValueEditText = findViewById(R.id.tagValueEditText);
        
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
        
        tagsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        tagsListView.setAdapter(tagsAdapter);
        
        loadPhotoDetails();
    }
    
    private void loadPhotoDetails() {
        Glide.with(this)
            .load(Uri.parse(currentPhoto.getUri()))
            .into(photoImageView);
        
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
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.photo_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_edit_caption) {
            showEditCaptionDialog();
            return true;
        } else if (id == R.id.action_delete_photo) {
            confirmDeletePhoto();
            return true;
        } else if (id == R.id.action_move_photo) {
            showMovePhotoDialog();
            return true;
        } else if (id == R.id.action_copy_photo) {
            showCopyPhotoDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
    
    public void onAddTagClick(android.view.View view) {
        String tagName = tagNameEditText.getText().toString().trim();
        String tagValue = tagValueEditText.getText().toString().trim();
        
        if (tagName.isEmpty() || tagValue.isEmpty()) {
            Toast.makeText(this, "Tag name and value cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!tagName.equalsIgnoreCase("person") && !tagName.equalsIgnoreCase("location")) {
            Toast.makeText(this, "Only 'person' and 'location' tags are allowed", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Tag newTag = new Tag(tagName.toLowerCase(), tagValue);
        if (currentPhoto.addTag(newTag)) {
            AppDataManager.getInstance().updatePhoto(albumName, currentPhoto);
            tagNameEditText.setText("");
            tagValueEditText.setText("");
            updateTagsList();
        } else {
            Toast.makeText(this, "Tag already exists on this photo", Toast.LENGTH_SHORT).show();
        }
    }
    
    public void onDeleteTagClick(android.view.View view) {
        int position = tagsListView.getCheckedItemPosition();
        if (position == ListView.INVALID_POSITION) {
            Toast.makeText(this, "No tag selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String selectedTag = tagsAdapter.getItem(position);
        if (selectedTag != null) {
            String[] parts = selectedTag.split("=", 2);
            if (parts.length == 2) {
                Tag tagToRemove = new Tag(parts[0], parts[1]);
                if (currentPhoto.removeTag(tagToRemove)) {
                    AppDataManager.getInstance().updatePhoto(albumName, currentPhoto);
                    updateTagsList();
                }
            }
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