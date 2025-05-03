package com.cs213.androidphotos.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cs213.androidphotos.R;
import com.cs213.androidphotos.model.Album;
import com.cs213.androidphotos.model.Photo;
import com.cs213.androidphotos.model.Tag;
import com.cs213.androidphotos.util.AppDataManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PhotoActivity extends AppCompatActivity {
    private AppDataManager dataManager;
    private Album album;
    private Photo photo;

    private ImageView photoImageView;
    private TextView captionTextView;
    private RecyclerView tagsRecyclerView;
    private Spinner tagTypeSpinner;
    private EditText tagValueEditText;
    private Button addTagButton;
    private Button slideshowButton;
    private Button moveToAlbumButton;
    private Button deletePhotoButton;
    private Button backToAlbumButton;

    private TagAdapter tagAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        String albumName = getIntent().getStringExtra("albumName");
        String photoPath = getIntent().getStringExtra("photoPath");

        if (albumName == null || photoPath == null) {
            Toast.makeText(this, "Error: Missing album or photo information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dataManager = AppDataManager.getInstance(this);
        album = dataManager.getAlbum(albumName);

        if (album == null) {
            Toast.makeText(this, "Error: Album not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        photo = null;
        for (Photo p : album.getPhotos()) {
            if (p.getFilePath().equals(photoPath)) {
                photo = p;
                break;
            }
        }

        if (photo == null) {
            Toast.makeText(this, "Error: Photo not found in album", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();

        loadPhotoDetails();

        setupTagAdapter();

        setupButtonListeners();
    }

    private void initializeViews() {
        photoImageView = findViewById(R.id.photoImageView);
        captionTextView = findViewById(R.id.captionTextView);
        tagsRecyclerView = findViewById(R.id.tagsRecyclerView);
        tagTypeSpinner = findViewById(R.id.tagTypeSpinner);
        tagValueEditText = findViewById(R.id.tagValueEditText);
        addTagButton = findViewById(R.id.addTagButton);
        slideshowButton = findViewById(R.id.slideshowButton);
        moveToAlbumButton = findViewById(R.id.moveToAlbumButton);
        backToAlbumButton = findViewById(R.id.backToAlbumButton);

        // Initialize deletePhotoButton - this assumes you have a button with ID 'deletePhotoButton' in your layout
        deletePhotoButton = findViewById(R.id.deletePhotoButton);
        
        // If the button doesn't exist in the layout yet, you'll need to add it to activity_photo.xml
        // If it doesn't exist in the XML, you can handle the null case or create it programmatically
        if (deletePhotoButton == null) {
            // For demonstration purposes only - you should add this button to your XML layout instead
            // This code will not work without proper layout parameters and positioning
            deletePhotoButton = new Button(this);
            deletePhotoButton.setText("Delete Photo");
            // Add to parent layout
            // This is just a placeholder - you should modify the XML layout instead
        }

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this, R.array.tag_types, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tagTypeSpinner.setAdapter(spinnerAdapter);
    }

    private void loadPhotoDetails() {
        try {
            String filePath = photo.getFilePath();
            if (filePath.startsWith("content://")) {
                Uri photoUri = Uri.parse(filePath);
                photoImageView.setImageBitmap(getBitmapFromUri(photoUri));
            } else {
                Bitmap bitmap = BitmapFactory.decodeFile(photo.getFilePath());
                photoImageView.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
            photoImageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        captionTextView.setText(photo.getFileName());
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ContentResolver resolver = getContentResolver();
        InputStream inputStream = resolver.openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        if (inputStream != null) {
            inputStream.close();
        }
        return bitmap;
    }

    private void setupTagAdapter() {
        tagAdapter = new TagAdapter(photo.getTags());
        tagsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tagsRecyclerView.setAdapter(tagAdapter);
    }

    private void setupButtonListeners() {
        addTagButton.setOnClickListener(v -> addTagToPhoto());

        slideshowButton.setOnClickListener(v -> {
            Intent intent = new Intent(PhotoActivity.this, SlideShowActivity.class);
            intent.putExtra("albumName", album.getName());
            intent.putExtra("currentPhotoPath", photo.getFilePath());
            startActivity(intent);
        });

        moveToAlbumButton.setOnClickListener(v -> showMovePhotoDialog());
        
        deletePhotoButton.setOnClickListener(v -> confirmDeletePhoto());

        backToAlbumButton.setOnClickListener(v -> finish());
    }

    private void confirmDeletePhoto() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Photo")
                .setMessage("Are you sure you want to delete this photo from the album?")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    if (dataManager.removePhotoFromAlbum(album, photo)) {
                        Toast.makeText(this, "Photo deleted successfully", Toast.LENGTH_SHORT).show();
                        finish(); // Return to album view
                    } else {
                        Toast.makeText(this, "Failed to delete photo", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void addTagToPhoto() {
        String tagType = tagTypeSpinner.getSelectedItem().toString();
        String tagValue = tagValueEditText.getText().toString().trim();

        if (tagValue.isEmpty()) {
            Toast.makeText(this, "Tag value cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dataManager.addTagToPhoto(photo, tagType, tagValue)) {
            tagAdapter.notifyDataSetChanged();
            tagValueEditText.setText("");
            Toast.makeText(this, "Tag added successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to add tag or tag already exists", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMovePhotoDialog() {
        List<String> albumNames = new ArrayList<>();
        for (Album a : dataManager.getAlbums()) {
            if (!a.getName().equals(album.getName())) {
                albumNames.add(a.getName());
            }
        }

        if (albumNames.isEmpty()) {
            Toast.makeText(this, "No other albums available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = albumNames.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Move Photo To...")
                .setItems(items, (dialog, which) -> {
                    String targetAlbumName = items[which];
                    Album targetAlbum = dataManager.getAlbum(targetAlbumName);

                    if (targetAlbum != null) {
                        if (dataManager.movePhoto(album, targetAlbum, photo)) {
                            Toast.makeText(this, "Photo moved successfully", Toast.LENGTH_SHORT).show();
                            finish(); // Return to album view
                        } else {
                            Toast.makeText(this, "Failed to move photo", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    private class TagAdapter extends RecyclerView.Adapter<TagAdapter.TagViewHolder> {
        private List<Tag> tags;

        public TagAdapter(List<Tag> tags) {
            this.tags = tags;
        }

        @Override
        public TagViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new TagViewHolder(view);
        }

        @Override
        public void onBindViewHolder(TagViewHolder holder, int position) {
            Tag tag = tags.get(position);
            holder.tagTextView.setText(tag.getType() + ": " + tag.getValue());

            holder.itemView.setOnLongClickListener(v -> {
                showDeleteTagDialog(position);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return tags.size();
        }

        class TagViewHolder extends RecyclerView.ViewHolder {
            TextView tagTextView;

            TagViewHolder(View itemView) {
                super(itemView);
                tagTextView = (TextView) itemView.findViewById(android.R.id.text1);
            }
        }
    }

    private void showDeleteTagDialog(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Tag")
                .setMessage("Are you sure you want to delete this tag?")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    Tag tagToDelete = photo.getTags().get(position);
                    if (dataManager.removeTagFromPhoto(photo, tagToDelete)) {
                        tagAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "Tag deleted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to delete tag", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }
}