package com.cs213.androidphotos.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cs213.androidphotos.R;
import com.cs213.androidphotos.model.Photo;
import com.cs213.androidphotos.model.Tag;
import com.cs213.androidphotos.util.AppDataManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class PhotoActivity extends AppCompatActivity {

    private ImageView photoImageView;
    private TextView captionTextView;
    private RecyclerView tagsRecyclerView;
    private Spinner tagTypeSpinner;
    private EditText tagValueEditText;
    private Button addTagButton;
    private Button slideshowButton;
    private Button moveToAlbumButton;
    private Button backToAlbumButton;
    
    private String albumName;
    private Photo currentPhoto;
    private TagsAdapter tagsAdapter;

    private class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.TagViewHolder> {
        private List<Tag> tags;

        public TagsAdapter(List<Tag> tags) {
            this.tags = tags;
        }

        public void updateTags(List<Tag> newTags) {
            this.tags = newTags;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new TagViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
            holder.bind(tags.get(position));
        }

        @Override
        public int getItemCount() {
            return tags.size();
        }

        class TagViewHolder extends RecyclerView.ViewHolder {
            private final TextView textView;

            public TagViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }

            public void bind(Tag tag) {
                textView.setText(tag.toString());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        
        photoImageView = findViewById(R.id.photoImageView);
        captionTextView = findViewById(R.id.captionTextView);
        tagsRecyclerView = findViewById(R.id.tagsRecyclerView);
        tagTypeSpinner = findViewById(R.id.tagTypeSpinner);
        tagValueEditText = findViewById(R.id.tagValueEditText);
        addTagButton = findViewById(R.id.addTagButton);
        slideshowButton = findViewById(R.id.slideshowButton);
        moveToAlbumButton = findViewById(R.id.moveToAlbumButton);
        backToAlbumButton = findViewById(R.id.backToAlbumButton);
        
        tagsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tagsAdapter = new TagsAdapter(new ArrayList<>());
        tagsRecyclerView.setAdapter(tagsAdapter);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Person", "Location"});
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tagTypeSpinner.setAdapter(spinnerAdapter);

        albumName = getIntent().getStringExtra("albumName");
        String photoPath = getIntent().getStringExtra("photoPath");
        
        if (albumName == null || photoPath == null) {
            showErrorAndFinish("Invalid photo data");
            return;
        }
        
        currentPhoto = AppDataManager.getInstance(this).getPhotoByPath(photoPath);
        if (currentPhoto == null) {
            showErrorAndFinish("Photo not found");
            return;
        }
        
        setTitle(currentPhoto.getCaption().isEmpty() ? "Photo" : currentPhoto.getCaption());
        
        addTagButton.setOnClickListener(v -> onAddTagClick());
        slideshowButton.setOnClickListener(v -> startSlideshow());
        moveToAlbumButton.setOnClickListener(v -> showMovePhotoDialog());
        backToAlbumButton.setOnClickListener(v -> finish());
        
        loadPhotoDetails();
    }
    
    private void loadPhotoDetails() {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(currentPhoto.getFilePath());
            if (bitmap != null) {
                photoImageView.setImageBitmap(bitmap);
            } else {
                photoImageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } catch (Exception e) {
            photoImageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        
        captionTextView.setText(currentPhoto.getCaption().isEmpty() ? 
                "(No caption)" : currentPhoto.getCaption());
        
        updateTagsList();
    }
    
    private void updateTagsList() {
        tagsAdapter.updateTags(currentPhoto.getTags());
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
            AppDataManager.getInstance(this).updatePhoto(albumName, currentPhoto);
            tagValueEditText.setText("");
            updateTagsList();
        } else {
            Toast.makeText(this, "Tag already exists on this photo", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startSlideshow() {
        Intent intent = new Intent(this, SlideShowActivity.class);
        intent.putExtra("albumName", albumName);
        intent.putExtra("photoPath", currentPhoto.getFilePath());
        startActivity(intent);
    }
    
    private void showMovePhotoDialog() {
        ArrayList<String> otherAlbums = new ArrayList<>(
                AppDataManager.getInstance(this).getAlbumNames()
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
        boolean success = AppDataManager.getInstance(this).movePhotoBetweenAlbums(
                albumName, targetAlbum, currentPhoto
        );
        
        if (success) {
            Toast.makeText(this, "Photo moved to " + targetAlbum, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Photo already exists in target album", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }
}