package com.cs213.androidphotos.ui;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.cs213.androidphotos.R;
import com.cs213.androidphotos.model.Album;
import com.cs213.androidphotos.model.Photo;
import com.cs213.androidphotos.model.Tag;
import com.cs213.androidphotos.util.AppDataManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SlideShowActivity extends AppCompatActivity {
    private AppDataManager dataManager;
    private Album album;
    private List<Photo> photos;

    private ViewPager2 slideshowViewPager;
    private Button previousButton;
    private Button exitButton;
    private Button nextButton;
    private Button deleteButton;
    private ToggleButton autoplayToggle;
    private TextView slideshowTitleTextView;
    private TextView photoInfoTextView;

    private Handler autoplayHandler = new Handler();
    private Runnable autoplayRunnable;
    private boolean isAutoplayActive = false;
    private static final int AUTOPLAY_DELAY = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slideshow);

        // Get data from intent
        String albumName = getIntent().getStringExtra("albumName");
        String currentPhotoPath = getIntent().getStringExtra("currentPhotoPath");

        if (albumName == null) {
            finish();
            return;
        }

        // Initialize data
        dataManager = AppDataManager.getInstance(this);
        album = dataManager.getAlbum(albumName);

        if (album == null) {
            finish();
            return;
        }

        photos = album.getPhotos();
        if (photos.isEmpty()) {
            finish();
            return;
        }

        // Initialize views
        slideshowViewPager = findViewById(R.id.slideshowViewPager);
        previousButton = findViewById(R.id.previousButton);
        exitButton = findViewById(R.id.exitSlideshowButton);
        nextButton = findViewById(R.id.nextButton);
        slideshowTitleTextView = findViewById(R.id.slideshowTitleTextView);
        
        // These views might need to be added to your layout
        photoInfoTextView = findViewById(R.id.photoInfoTextView);
        autoplayToggle = findViewById(R.id.autoplayToggle);
        deleteButton = findViewById(R.id.deletePhotoButton);
        
        // Handle null views if they don't exist in the layout yet
        if (photoInfoTextView == null) {
            photoInfoTextView = new TextView(this);
            // Note: This is not a proper solution. You should add this to your XML layout.
        }
        
        if (autoplayToggle == null) {
            autoplayToggle = new ToggleButton(this);
            autoplayToggle.setTextOn("Autoplay ON");
            autoplayToggle.setTextOff("Autoplay OFF");
            // Note: This is not a proper solution. You should add this to your XML layout.
        }
        
        if (deleteButton == null) {
            deleteButton = new Button(this);
            deleteButton.setText("Delete");
            // Note: This is not a proper solution. You should add this to your XML layout.
        }

        // Set title
        slideshowTitleTextView.setText(getString(R.string.slideshow_title) + " - " + albumName);

        // Set up ViewPager
        PhotoPagerAdapter adapter = new PhotoPagerAdapter();
        slideshowViewPager.setAdapter(adapter);

        // Start at the selected photo if provided
        if (currentPhotoPath != null) {
            for (int i = 0; i < photos.size(); i++) {
                if (photos.get(i).getFilePath().equals(currentPhotoPath)) {
                    slideshowViewPager.setCurrentItem(i, false);
                    updatePhotoInfo(i);
                    break;
                }
            }
        } else {
            updatePhotoInfo(0);
        }

        // Set up button listeners
        previousButton.setOnClickListener(v -> {
            int currentPosition = slideshowViewPager.getCurrentItem();
            if (currentPosition > 0) {
                slideshowViewPager.setCurrentItem(currentPosition - 1);
            }
        });

        nextButton.setOnClickListener(v -> {
            int currentPosition = slideshowViewPager.getCurrentItem();
            if (currentPosition < photos.size() - 1) {
                slideshowViewPager.setCurrentItem(currentPosition + 1);
            }
        });

        exitButton.setOnClickListener(v -> finish());
        
        // Set up autoplay toggle
        if (autoplayToggle != null) {
            autoplayToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    startAutoplay();
                } else {
                    stopAutoplay();
                }
            });
        }
        
        // Set up delete button
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                int position = slideshowViewPager.getCurrentItem();
                confirmDeletePhoto(position);
            });
        }

        // Update button states based on position
        slideshowViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                previousButton.setEnabled(position > 0);
                nextButton.setEnabled(position < photos.size() - 1);
                updatePhotoInfo(position);
            }
        });
        
        // Initialize autoplay runnable
        autoplayRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAutoplayActive) {
                    int currentPosition = slideshowViewPager.getCurrentItem();
                    if (currentPosition < photos.size() - 1) {
                        slideshowViewPager.setCurrentItem(currentPosition + 1);
                    } else {
                        // Loop back to the beginning
                        slideshowViewPager.setCurrentItem(0);
                    }
                    autoplayHandler.postDelayed(this, AUTOPLAY_DELAY);
                }
            }
        };
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        stopAutoplay();
    }

    private void updatePhotoInfo(int position) {
        if (photoInfoTextView != null && position >= 0 && position < photos.size()) {
            Photo photo = photos.get(position);
            StringBuilder infoBuilder = new StringBuilder();
            infoBuilder.append("Photo: ").append(photo.getFileName());
            
            List<Tag> tags = photo.getTags();
            if (!tags.isEmpty()) {
                infoBuilder.append("\nTags: ");
                for (int i = 0; i < tags.size(); i++) {
                    Tag tag = tags.get(i);
                    infoBuilder.append(tag.getType()).append(": ").append(tag.getValue());
                    if (i < tags.size() - 1) {
                        infoBuilder.append(", ");
                    }
                }
            }
            
            photoInfoTextView.setText(infoBuilder.toString());
        }