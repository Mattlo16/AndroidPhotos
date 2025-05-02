package com.cs213.androidphotos.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cs213.androidphotos.R;
import com.cs213.androidphotos.data.Album;
import com.cs213.androidphotos.data.AppDataManager;
import com.cs213.androidphotos.data.Photo;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SlideShowActivity extends AppCompatActivity {

    private ImageView photoImageView;
    private TextView captionTextView;
    private TextView dateTextView;
    private TextView positionTextView;
    private Button prevButton;
    private Button nextButton;

    private List<Photo> photos;
    private int currentPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slideshow);

        photoImageView = findViewById(R.id.slideshow_image);
        captionTextView = findViewById(R.id.slideshow_caption);
        dateTextView = findViewById(R.id.slideshow_date);
        positionTextView = findViewById(R.id.slideshow_position);
        prevButton = findViewById(R.id.prev_button);
        nextButton = findViewById(R.id.next_button);

        String albumName = getIntent().getStringExtra("albumName");
        if (albumName == null) {
            finish();
            return;
        }

        Album album = AppDataManager.getInstance().getAlbum(albumName);
        if (album == null) {
            finish();
            return;
        }

        photos = album.getPhotos();
        if (photos.isEmpty()) {
            finish();
            return;
        }

        prevButton.setOnClickListener(v -> showPreviousPhoto());
        nextButton.setOnClickListener(v -> showNextPhoto());

        showPhoto(currentPosition);
    }

    private void showPhoto(int position) {
        if (position < 0 || position >= photos.size()) {
            return;
        }

        Photo photo = photos.get(position);
        
        Glide.with(this)
            .load(Uri.parse(photo.getUri()))
            .into(photoImageView);

        captionTextView.setText(photo.getCaption().isEmpty() ? 
            "(No caption)" : photo.getCaption());

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        dateTextView.setText(dateFormat.format(photo.getDate()));

        positionTextView.setText(String.format(Locale.getDefault(), 
            "%d of %d", position + 1, photos.size()));

        prevButton.setEnabled(position > 0);
        nextButton.setEnabled(position < photos.size() - 1);
    }

    private void showPreviousPhoto() {
        if (currentPosition > 0) {
            currentPosition--;
            showPhoto(currentPosition);
        }
    }

    private void showNextPhoto() {
        if (currentPosition < photos.size() - 1) {
            currentPosition++;
            showPhoto(currentPosition);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}