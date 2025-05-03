package com.cs213.androidphotos.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
import android.widget.EditText;
import android.database.Cursor;
import android.provider.DocumentsContract;
import android.content.ContentResolver;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cs213.androidphotos.R;
import com.cs213.androidphotos.model.Album;
import com.cs213.androidphotos.model.Photo;
import com.cs213.androidphotos.util.AppDataManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class AlbumActivity extends AppCompatActivity {
    private static final int PICK_PHOTO_REQUEST = 1;

    private AppDataManager dataManager;
    private Album album;
    private GridView photosGridView;
    private TextView albumNameTextView;
    private TextView emptyAlbumTextView; // Add this if you want to show a message for empty albums
    private Button addPhotoButton, albumMenuButton, backButton;
    private ArrayAdapter<Photo> photoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        String albumName = getIntent().getStringExtra("albumName");
        if (albumName == null) {
            Toast.makeText(this, "Error: Album name not provided", Toast.LENGTH_SHORT).show();
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

        // Initialize views
        albumNameTextView = findViewById(R.id.albumNameTextView);
        photosGridView = findViewById(R.id.photosGridView);
        addPhotoButton = findViewById(R.id.addPhotoButton);
        albumMenuButton = findViewById(R.id.albumMenuButton);
        backButton = findViewById(R.id.backButton);
        
        // Optional empty album text view (add this to your layout if needed)
        // emptyAlbumTextView = findViewById(R.id.emptyAlbumTextView);

        albumNameTextView.setText(album.getName());

        setupPhotoAdapter();

        // Set click listeners
        addPhotoButton.setOnClickListener(v -> openPhotoSelector());
        albumMenuButton.setOnClickListener(v -> showAlbumOptionsDialog());
        backButton.setOnClickListener(v -> finish());

        // Handle grid item click
        photosGridView.setOnItemClickListener((parent, view, position, id) -> {
            try {
                Photo photo = photoAdapter.getItem(position);
                if (photo != null) {
                    openPhotoView(photo);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error opening photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Handle grid item long click
        photosGridView.setOnItemLongClickListener((parent, view, position, id) -> {
            try {
                Photo photo = photoAdapter.getItem(position);
                if (photo != null) {
                    showPhotoOptionsDialog(photo);
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error showing options: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            return false;
        });
        
        // Update empty state
        updateEmptyState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the data when returning to this activity
        if (album != null) {
            album = dataManager.getAlbum(album.getName()); // Get fresh data
            if (photoAdapter != null) {
                photoAdapter.clear();
                photoAdapter.addAll(album.getPhotos());
                photoAdapter.notifyDataSetChanged();
                updateEmptyState();
            }
        }
    }
    
    private void updateEmptyState() {
        // If you have an empty state text view, update its visibility
        // if (emptyAlbumTextView != null) {
        //     if (album.getPhotos().isEmpty()) {
        //         emptyAlbumTextView.setVisibility(View.VISIBLE);
        //         photosGridView.setVisibility(View.GONE);
        //     } else {
        //         emptyAlbumTextView.setVisibility(View.GONE);
        //         photosGridView.setVisibility(View.VISIBLE);
        //     }
        // }
    }

    private void setupPhotoAdapter() {
        // Make sure we're working with a valid list
        if (album.getPhotos() == null) {
            album.setPhotos(new ArrayList<>());
        }
        
        photoAdapter = new ArrayAdapter<Photo>(this, android.R.layout.simple_list_item_1, album.getPhotos()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ImageView imageView;

                if (convertView == null) {
                    imageView = new ImageView(AlbumActivity.this);
                    imageView.setLayoutParams(new GridView.LayoutParams(200, 200));
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageView.setPadding(8, 8, 8, 8);
                } else {
                    imageView = (ImageView) convertView;
                }

                Photo photo = getItem(position);
                if (photo != null) {
                    try {
                        String filePath = photo.getFilePath();
                        if (filePath.startsWith("content://")) {
                            Uri photoUri = Uri.parse(filePath);
                            imageView.setImageBitmap(getBitmapFromUri(photoUri));
                        } else {
                            Bitmap bitmap = BitmapFactory.decodeFile(photo.getFilePath());
                            imageView.setImageBitmap(bitmap);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                }

                return imageView;
            }
        };

        photosGridView.setAdapter(photoAdapter);
    }

    private void showPhotoOptionsDialog(Photo photo) {
        try {
            String[] options = {"View Photo", "Start Slideshow", "Delete Photo"};

            new AlertDialog.Builder(this)
                    .setTitle("Photo Options")
                    .setItems(options, (dialog, which) -> {
                        try {
                            switch (which) {
                                case 0: // View
                                    openPhotoView(photo);
                                    break;
                                case 1: // Slideshow
                                    startSlideshow(photo);
                                    break;
                                case 2: // Delete
                                    confirmDeletePhoto(photo);
                                    break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error showing options: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startSlideshow(Photo startPhoto) {
        try {
            if (album.getPhotos().isEmpty()) {
                Toast.makeText(this, "Album is empty. Add photos first.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent intent = new Intent(AlbumActivity.this, SlideShowActivity.class);
            intent.putExtra("albumName", album.getName());
            
            // Make sure we have a valid photo to start with
            if (startPhoto != null && startPhoto.getFilePath() != null) {
                intent.putExtra("currentPhotoPath", startPhoto.getFilePath());
            } else if (!album.getPhotos().isEmpty()) {
                // Fallback to first photo if specific one is invalid
                intent.putExtra("currentPhotoPath", album.getPhotos().get(0).getFilePath());
            }
            
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error starting slideshow: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeletePhoto(Photo photo) {
        try {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Photo")
                    .setMessage("Are you sure you want to delete this photo?")
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        try {
                            if (dataManager.removePhotoFromAlbum(album, photo)) {
                                photoAdapter.remove(photo);
                                photoAdapter.notifyDataSetChanged();
                                updateEmptyState();
                                Toast.makeText(this, "Photo deleted successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Failed to delete photo", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error deleting photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error showing delete dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openPhotoSelector() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_PHOTO_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening photo selector: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            if (requestCode == PICK_PHOTO_REQUEST && resultCode == RESULT_OK && data != null) {
                Uri photoUri = data.getData();
                if (photoUri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(
                                photoUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }

                    String uriString = photoUri.toString();
                    Photo newPhoto = dataManager.addPhotoToAlbum(album, uriString);

                    if (newPhoto != null) {
                        photoAdapter.clear();
                        photoAdapter.addAll(album.getPhotos());
                        photoAdapter.notifyDataSetChanged();
                        updateEmptyState();
                        Toast.makeText(this, "Photo added successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to add photo", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error adding photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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

    private void openPhotoView(Photo photo) {
        try {
            Intent intent = new Intent(AlbumActivity.this, PhotoActivity.class);
            intent.putExtra("albumName", album.getName());
            intent.putExtra("photoPath", photo.getFilePath());
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showAlbumOptionsDialog() {
        try {
            String[] options = {"Start Slideshow", "Rename Album", "Delete Album"};

            new AlertDialog.Builder(this)
                    .setTitle(R.string.album_actions)
                    .setItems(options, (dialog, which) -> {
                        try {
                            switch (which) {
                                case 0: // Slideshow
                                    if (album.getPhotos().isEmpty()) {
                                        Toast.makeText(this, "Album is empty. Add photos first.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        startSlideshow(album.getPhotos().get(0));
                                    }
                                    break;
                                case 1: // Rename
                                    showRenameAlbumDialog();
                                    break;
                                case 2: // Delete
                                    confirmDeleteAlbum();
                                    break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error showing album options: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showRenameAlbumDialog() {
        try {
            EditText input = new EditText(this);
            input.setText(album.getName());

            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_rename_album_title)
                    .setView(input)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        try {
                            String newName = input.getText().toString().trim();
                            if (!newName.isEmpty()) {
                                if (dataManager.renameAlbum(album, newName)) {
                                    albumNameTextView.setText(newName);
                                } else {
                                    Toast.makeText(this, "An album with this name already exists", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error renaming album: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error showing rename dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteAlbum() {
        try {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_delete_album_title)
                    .setMessage(R.string.dialog_delete_album_message)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        try {
                            dataManager.deleteAlbum(album);
                            finish(); // Return to main activity
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error deleting album: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error showing delete dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}