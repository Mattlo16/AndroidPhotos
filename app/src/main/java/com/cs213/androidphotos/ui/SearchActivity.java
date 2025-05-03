package com.cs213.androidphotos.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cs213.androidphotos.R;
import com.cs213.androidphotos.model.Album;
import com.cs213.androidphotos.model.Photo;
import com.cs213.androidphotos.util.AppDataManager;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {
    private AppDataManager dataManager;

    private RadioGroup searchTypeRadioGroup;
    private RadioButton singleTagRadioButton;
    private RadioButton andRadioButton;
    private RadioButton orRadioButton;

    private Spinner tag1TypeSpinner;
    private Spinner tag2TypeSpinner;
    private AutoCompleteTextView tag1ValueAutoComplete;
    private AutoCompleteTextView tag2ValueAutoComplete;

    private Button searchButton;
    private Button clearButton;
    private Button backButton;

    private RecyclerView searchResultsRecyclerView;
    private SearchResultAdapter searchResultAdapter;

    private List<Photo> searchResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Initialize data manager
        dataManager = AppDataManager.getInstance(this);

        // Initialize views
        initializeViews();

        // Set up adapters
        setupAdapters();

        // Set up listeners
        setupListeners();
    }

    private void initializeViews() {
        searchTypeRadioGroup = findViewById(R.id.searchTypeRadioGroup);
        singleTagRadioButton = findViewById(R.id.singleTagRadioButton);
        andRadioButton = findViewById(R.id.andRadioButton);
        orRadioButton = findViewById(R.id.orRadioButton);

        tag1TypeSpinner = findViewById(R.id.tag1TypeSpinner);
        tag2TypeSpinner = findViewById(R.id.tag2TypeSpinner);
        tag1ValueAutoComplete = findViewById(R.id.tag1ValueAutoComplete);
        tag2ValueAutoComplete = findViewById(R.id.tag2ValueAutoComplete);

        searchButton = findViewById(R.id.searchButton);
        clearButton = findViewById(R.id.clearButton);
        backButton = findViewById(R.id.backButton);

        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        searchResultsRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
    }

    private void setupAdapters() {
        // Set up spinners
        ArrayAdapter<CharSequence> tagTypeAdapter = ArrayAdapter.createFromResource(
                this, R.array.tag_types, android.R.layout.simple_spinner_item);
        tagTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        tag1TypeSpinner.setAdapter(tagTypeAdapter);
        tag2TypeSpinner.setAdapter(tagTypeAdapter);

        // Set up search results adapter
        searchResultAdapter = new SearchResultAdapter(searchResults);
        searchResultsRecyclerView.setAdapter(searchResultAdapter);
    }

    private void setupListeners() {
        // Radio buttons
        searchTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.singleTagRadioButton) {
                tag2TypeSpinner.setEnabled(false);
                tag2ValueAutoComplete.setEnabled(false);
            } else {
                tag2TypeSpinner.setEnabled(true);
                tag2ValueAutoComplete.setEnabled(true);
            }
        });

        // Auto-complete
        tag1TypeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateTagValueSuggestions(tag1TypeSpinner, tag1ValueAutoComplete);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        tag2TypeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateTagValueSuggestions(tag2TypeSpinner, tag2ValueAutoComplete);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Buttons
        searchButton.setOnClickListener(v -> performSearch());
        clearButton.setOnClickListener(v -> clearSearchFields());
        backButton.setOnClickListener(v -> finish());
    }

    private void updateTagValueSuggestions(Spinner tagTypeSpinner, AutoCompleteTextView tagValueAutoComplete) {
        String tagType = tagTypeSpinner.getSelectedItem().toString();
        List<String> suggestions = dataManager.getTagValueSuggestions(tagType, "");

        ArrayAdapter<String> suggestionAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, suggestions);
        tagValueAutoComplete.setAdapter(suggestionAdapter);

        // Add listener for auto-complete based on partial input
        tagValueAutoComplete.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String prefix = s.toString();
                List<String> filteredSuggestions = dataManager.getTagValueSuggestions(tagType, prefix);
                suggestionAdapter.clear();
                suggestionAdapter.addAll(filteredSuggestions);
                suggestionAdapter.notifyDataSetChanged();
            }
        });
    }

    private void performSearch() {
        searchResults.clear();

        // Get search parameters
        String tagType1 = tag1TypeSpinner.getSelectedItem().toString();
        String tagValue1 = tag1ValueAutoComplete.getText().toString().trim();

        if (tagValue1.isEmpty()) {
            Toast.makeText(this, "Please enter a tag value", Toast.LENGTH_SHORT).show();
            return;
        }

        // Perform search based on selection
        if (singleTagRadioButton.isChecked()) {
            searchResults.addAll(dataManager.searchByTagPrefix(tagType1, tagValue1));
        } else {
            String tagType2 = tag2TypeSpinner.getSelectedItem().toString();
            String tagValue2 = tag2ValueAutoComplete.getText().toString().trim();

            if (tagValue2.isEmpty()) {
                Toast.makeText(this, "Please enter a value for the second tag", Toast.LENGTH_SHORT).show();
                return;
            }

            if (andRadioButton.isChecked()) {
                searchResults.addAll(dataManager.searchByTagConjunction(tagType1, tagValue1, tagType2, tagValue2));
            } else if (orRadioButton.isChecked()) {
                searchResults.addAll(dataManager.searchByTagDisjunction(tagType1, tagValue1, tagType2, tagValue2));
            }
        }

        searchResultAdapter.notifyDataSetChanged();

        if (searchResults.isEmpty()) {
            Toast.makeText(this, "No photos found matching your search criteria", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearSearchFields() {
        singleTagRadioButton.setChecked(true);
        tag1ValueAutoComplete.setText("");
        tag2ValueAutoComplete.setText("");
        tag2TypeSpinner.setEnabled(false);
        tag2ValueAutoComplete.setEnabled(false);

        searchResults.clear();
        searchResultAdapter.notifyDataSetChanged();
    }

    // Create an album from search results
    private void createAlbumFromResults() {
        if (searchResults.isEmpty()) {
            Toast.makeText(this, "No search results to create an album from", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Enter album name");

        new AlertDialog.Builder(this)
                .setTitle("Create Album from Search Results")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String albumName = input.getText().toString().trim();
                    if (!albumName.isEmpty()) {
                        Album newAlbum = dataManager.createAlbumFromSearchResults(albumName, searchResults);
                        if (newAlbum != null) {
                            Toast.makeText(this, "Album created successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to create album. Name might already exist.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Album name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Adapter for search results
    private class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder> {
        private List<Photo> photos;

        public SearchResultAdapter(List<Photo> photos) {
            this.photos = photos;
        }

        @NonNull
        @Override
        public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_photo, parent, false);
            return new SearchResultViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
            Photo photo = photos.get(position);

            try {
                Bitmap bitmap = BitmapFactory.decodeFile(photo.getFilePath());
                holder.imageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        @Override
        public int getItemCount() {
            return photos.size();
        }

        class SearchResultViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            SearchResultViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.photoImageView);
            }
        }
    }
}