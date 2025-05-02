package com.cs213.androidphotos.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cs213.androidphotos.R;
import com.cs213.androidphotos.data.Album;
import com.cs213.androidphotos.data.AppDataManager;
import com.cs213.androidphotos.data.Photo;
import com.cs213.androidphotos.data.Tag;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SearchActivity extends AppCompatActivity {

    private Spinner searchTypeSpinner;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private AutoCompleteTextView tagName1Field;
    private AutoCompleteTextView tagValue1Field;
    private AutoCompleteTextView tagName2Field;
    private AutoCompleteTextView tagValue2Field;
    private RadioGroup tagSearchTypeGroup;
    private RadioButton singleTagRadio;
    private RadioButton andRadio;
    private RadioButton orRadio;
    private RecyclerView searchResultsRecyclerView;
    private Button searchButton;
    private Button clearButton;
    private LinearLayout dateSearchContainer;
    private LinearLayout tagSearchContainer;

    private List<Photo> searchResults = new ArrayList<>();
    private PhotoAdapter photoAdapter;
    private ArrayAdapter<String> tagNameAdapter;
    private ArrayAdapter<String> tagValueAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initializeViews();
        setupAdapters();
        setupListeners();
    }

    private void initializeViews() {
        searchTypeSpinner = findViewById(R.id.searchTypeSpinner);
        fromDatePicker = findViewById(R.id.fromDatePicker);
        toDatePicker = findViewById(R.id.toDatePicker);
        tagName1Field = findViewById(R.id.tagName1Field);
        tagValue1Field = findViewById(R.id.tagValue1Field);
        tagName2Field = findViewById(R.id.tagName2Field);
        tagValue2Field = findViewById(R.id.tagValue2Field);
        tagSearchTypeGroup = findViewById(R.id.tagSearchTypeGroup);
        singleTagRadio = findViewById(R.id.singleTagRadio);
        andRadio = findViewById(R.id.andRadio);
        orRadio = findViewById(R.id.orRadio);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        searchButton = findViewById(R.id.searchButton);
        clearButton = findViewById(R.id.clearButton);
        dateSearchContainer = findViewById(R.id.dateSearchContainer);
        tagSearchContainer = findViewById(R.id.tagSearchContainer);

        searchResultsRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        photoAdapter = new PhotoAdapter(searchResults, null); 
        searchResultsRecyclerView.setAdapter(photoAdapter);
    }

    private void setupAdapters() {
        List<String> tagNames = new ArrayList<>();
        tagNames.add("person");
        tagNames.add("location");
        tagNameAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, tagNames);
        
        tagName1Field.setAdapter(tagNameAdapter);
        tagName2Field.setAdapter(tagNameAdapter);

        tagValueAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        
        tagValue1Field.setAdapter(tagValueAdapter);
        tagValue2Field.setAdapter(tagValueAdapter);

        tagName1Field.addTextChangedListener(new TagNameWatcher(tagValue1Field));
        tagName2Field.addTextChangedListener(new TagNameWatcher(tagValue2Field));
    }

    private void setupListeners() {
        searchButton.setOnClickListener(v -> handleSearch());
        clearButton.setOnClickListener(v -> handleClear());

        singleTagRadio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tagName2Field.setEnabled(!isChecked);
            tagValue2Field.setEnabled(!isChecked);
        });

        tagName2Field.setEnabled(false);
        tagValue2Field.setEnabled(false);
    }

    private void handleSearch() {
        searchResults.clear();
        photoAdapter.notifyDataSetChanged();

        String searchType = searchTypeSpinner.getSelectedItem().toString();
        
        if (searchType.equals("Date Range")) {
            searchByDateRange();
        } else if (searchType.equals("Tag Search")) {
            searchByTags();
        }

        if (searchResults.isEmpty()) {
            Toast.makeText(this, "No photos match your search criteria", Toast.LENGTH_SHORT).show();
        } else {
            photoAdapter.notifyDataSetChanged();
        }
    }

    private void searchByDateRange() {
        Calendar fromDate = Calendar.getInstance();
        fromDate.set(fromDatePicker.getYear(), 
                   fromDatePicker.getMonth(), 
                   fromDatePicker.getDayOfMonth(), 
                   0, 0, 0);
        
        Calendar toDate = Calendar.getInstance();
        toDate.set(toDatePicker.getYear(), 
                 toDatePicker.getMonth(), 
                 toDatePicker.getDayOfMonth(), 
                 23, 59, 59);

        for (Album album : AppDataManager.getInstance().getAllAlbums()) {
            for (Photo photo : album.getPhotos()) {
                Calendar photoDate = photo.getDate();
                if ((photoDate.equals(fromDate) || photoDate.after(fromDate)) && 
                    (photoDate.equals(toDate) || photoDate.before(toDate))) {
                    if (!searchResults.contains(photo)) {
                        searchResults.add(photo);
                    }
                }
            }
        }
    }

    private void searchByTags() {
        String tagName1 = tagName1Field.getText().toString().trim();
        String tagValue1 = tagValue1Field.getText().toString().trim();

        if (tagName1.isEmpty()) {
            Toast.makeText(this, "First tag name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<Photo> allPhotos = new HashSet<>();
        for (Album album : AppDataManager.getInstance().getAllAlbums()) {
            allPhotos.addAll(album.getPhotos());
        }

        if (singleTagRadio.isChecked()) {
            for (Photo photo : allPhotos) {
                if (tagValue1.isEmpty()) {
                    if (photo.hasTagWithName(tagName1) && !searchResults.contains(photo)) {
                        searchResults.add(photo);
                    }
                } else {
                    if (photo.hasTag(tagName1, tagValue1) && !searchResults.contains(photo)) {
                        searchResults.add(photo);
                    }
                }
            }
        } else {
            String tagName2 = tagName2Field.getText().toString().trim();
            String tagValue2 = tagValue2Field.getText().toString().trim();

            if (tagName2.isEmpty()) {
                Toast.makeText(this, "Second tag name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            for (Photo photo : allPhotos) {
                boolean matchesTag1 = tagValue1.isEmpty() ? 
                    photo.hasTagWithName(tagName1) : 
                    photo.hasTag(tagName1, tagValue1);
                
                boolean matchesTag2 = tagValue2.isEmpty() ? 
                    photo.hasTagWithName(tagName2) : 
                    photo.hasTag(tagName2, tagValue2);

                if ((andRadio.isChecked() && matchesTag1 && matchesTag2) || 
                    (orRadio.isChecked() && (matchesTag1 || matchesTag2))) {
                    if (!searchResults.contains(photo)) {
                        searchResults.add(photo);
                    }
                }
            }
        }
    }

    private void handleClear() {
        fromDatePicker.updateDate(
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH),
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        );
        toDatePicker.updateDate(
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH),
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        );
        
        tagName1Field.setText("");
        tagValue1Field.setText("");
        tagName2Field.setText("");
        tagValue2Field.setText("");
        singleTagRadio.setChecked(true);
        
        searchResults.clear();
        photoAdapter.notifyDataSetChanged();
    }

    private class TagNameWatcher implements TextWatcher {
        private AutoCompleteTextView valueField;

        public TagNameWatcher(AutoCompleteTextView valueField) {
            this.valueField = valueField;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            String tagName = s.toString().trim().toLowerCase();
            if (tagName.equals("person") || tagName.equals("location")) {
                List<String> values = AppDataManager.getInstance()
                    .getTagValuesForType(tagName);
                tagValueAdapter.clear();
                tagValueAdapter.addAll(values);
                tagValueAdapter.notifyDataSetChanged();
            }
        }
    }
}
