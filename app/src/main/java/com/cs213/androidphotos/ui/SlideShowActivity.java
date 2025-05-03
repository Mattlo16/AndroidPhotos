package com.cs213.androidphotos.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.cs213.androidphotos.R;
import com.cs213.androidphotos.model.Album;
import com.cs213.androidphotos.model.Photo;
import com.cs213.androidphotos.util.AppDataManager;

import java.util.List;

public class SlideShowActivity extends AppCompatActivity {
    private AppDataManager dataManager;
    private Album album;
    private List<Photo> photos;

    private ViewPager2 slideshowViewPager;
    private Button previousButton;
    private Button exitButton;
    private Button nextButton;
    private TextView slideshowTitleTextView;

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
                    break;
                }
            }
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

        // Update button states based on position
        slideshowViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                previousButton.setEnabled(position > 0);
                nextButton.setEnabled(position < photos.size() - 1);
            }
        });
    }

    private class PhotoPagerAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder> {

        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_slideshow, parent, false);
            return new PhotoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
            Photo photo = photos.get(position);

            // Load image
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(photo.getFilePath());
                holder.imageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // Set caption
            holder.captionTextView.setText(photo.getFileName());
        }

        @Override
        public int getItemCount() {
            return photos.size();
        }

        class PhotoViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ImageView imageView;
            TextView captionTextView;

            PhotoViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.slideshowImageView);
                captionTextView = itemView.findViewById(R.id.slideshowCaptionTextView);
            }
        }
    }
}