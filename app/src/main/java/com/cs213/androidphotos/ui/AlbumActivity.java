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

public class AlbumActivity extends AppCompatActivity {
    private static final int PICK_PHOTO_REQUEST = 1;

    private AppDataManager dataManager;
    private Album album;
    private GridView photosGridView;
    private TextView albumNameTextView;
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

        albumNameTextView = findViewById(R.id.albumNameTextView);
        photosGridView = findViewById(R.id.photosGridView);
        addPhotoButton = findViewById(R.id.addPhotoButton);
        albumMenuButton = findViewById(R.id.albumMenuButton);
        backButton = findViewById(R.id.backButton);

        albumNameTextView.setText(album.getName());

        setupPhotoAdapter();

        addPhotoButton.setOnClickListener(v -> openPhotoSelector());
        albumMenuButton.setOnClickListener(v -> showAlbumOptionsDialog());
        backButton.setOnClickListener(v -> finish());

        photosGridView.setOnItemClickListener((parent, view, position, id) -> {
            Photo photo = photoAdapter.getItem(position);
            if (photo != null) {
                openPhotoView(photo);
            }
        });

        photosGridView.setOnItemLongClickListener((parent, view, position, id) -> {
            Photo photo = photoAdapter.getItem(position);
            if (photo != null) {
                showPhotoOptionsDialog(photo);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (photoAdapter != null) {
            photoAdapter.notifyDataSetChanged();
        }
    }

    private void setupPhotoAdapter() {
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
        String[] options = {"View Photo", "Delete Photo"};

        new AlertDialog.Builder(this)
                .setTitle("Photo Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // View
                            openPhotoView(photo);
                            break;
                        case 1: // Delete
                            confirmDeletePhoto(photo);
                            break;
                    }
                })
                .show();
    }

    private void confirmDeletePhoto(Photo photo) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Photo")
                .setMessage("Are you sure you want to delete this photo?")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    if (dataManager.removePhotoFromAlbum(album, photo)) {
                        photoAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "Photo deleted successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to delete photo", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void openPhotoSelector() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_PHOTO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

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
                    photoAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Photo added successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to add photo", Toast.LENGTH_SHORT).show();
                }
            }
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
        Intent intent = new Intent(AlbumActivity.this, PhotoActivity.class);
        intent.putExtra("albumName", album.getName());
        intent.putExtra("photoPath", photo.getFilePath());
        startActivity(intent);
    }

    private void showAlbumOptionsDialog() {
        String[] options = {"Rename Album", "Delete Album"};

        new AlertDialog.Builder(this)
                .setTitle(R.string.album_actions)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Rename
                            showRenameAlbumDialog();
                            break;
                        case 1: // Delete
                            confirmDeleteAlbum();
                            break;
                    }
                })
                .show();
    }

    private void showRenameAlbumDialog() {
        EditText input = new EditText(this);
        input.setText(album.getName());

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_rename_album_title)
                .setView(input)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        if (dataManager.renameAlbum(album, newName)) {
                            albumNameTextView.setText(newName);
                        } else {
                            Toast.makeText(this, "An album with this name already exists", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmDeleteAlbum() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_album_title)
                .setMessage(R.string.dialog_delete_album_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    dataManager.deleteAlbum(album);
                    finish(); // Return to main activity
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }
}