package com.cs213.androidphotos.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cs213.androidphotos.model.Album;
import com.cs213.androidphotos.model.Photo;
import com.cs213.androidphotos.model.Tag;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Central data manager for the Photos application.
 * Handles data persistence, album management, and search functionality.
 */
public class AppDataManager {
    private static final String TAG = "AppDataManager";
    private static final String DATA_FILE = "photos_app_data.ser";

    private static AppDataManager instance;

    private List<Album> albums;
    private Context context;

    /**
     * Private constructor for singleton pattern.
     *
     * @param context application context for file operations
     */
    private AppDataManager(Context context) {
        this.context = context.getApplicationContext();
        this.albums = new ArrayList<>();
        loadData();
    }

    /**
     * Gets the singleton instance of the data manager.
     *
     * @param context application context
     * @return the singleton instance
     */
    public static synchronized AppDataManager getInstance(Context context) {
        if (instance == null) {
            instance = new AppDataManager(context);
        }
        return instance;
    }

    /**
     * Gets all albums.
     *
     * @return list of albums
     */
    public List<Album> getAlbums() {
        return albums;
    }

    /**
     * Gets an album by name.
     *
     * @param name the album name to find
     * @return the album, or null if not found
     */
    public Album getAlbum(String name) {
        for (Album album : albums) {
            if (album.getName().equals(name)) {
                return album;
            }
        }
        return null;
    }

    /**
     * Creates a new album.
     *
     * @param name the name for the new album
     * @return the new album, or null if an album with this name already exists
     */
    public Album createAlbum(String name) {
        // Check if album already exists
        if (getAlbum(name) != null) {
            return null;
        }

        Album newAlbum = new Album(name);
        albums.add(newAlbum);
        saveData();
        return newAlbum;
    }

    /**
     * Deletes an album.
     *
     * @param album the album to delete
     * @return true if the album was deleted, false if it wasn't found
     */
    public boolean deleteAlbum(Album album) {
        boolean removed = albums.remove(album);
        if (removed) {
            saveData();
        }
        return removed;
    }

    /**
     * Renames an album.
     *
     * @param album the album to rename
     * @param newName the new name for the album
     * @return true if renamed successfully, false if another album already has this name
     */
    public boolean renameAlbum(Album album, String newName) {
        // Check if an album with newName already exists
        if (getAlbum(newName) != null) {
            return false;
        }

        album.setName(newName);
        saveData();
        return true;
    }

    /**
     * Adds a photo to an album.
     *
     * @param album the album to add the photo to
     * @param filePath the file path of the photo
     * @return the added photo, or null if the photo already exists in the album
     */
    public Photo addPhotoToAlbum(Album album, String filePath) {
        Photo photo = new Photo(filePath);
        if (album.addPhoto(photo)) {
            saveData();
            return photo;
        }
        return null;
    }

    /**
     * Removes a photo from an album.
     *
     * @param album the album to remove the photo from
     * @param photo the photo to remove
     * @return true if removed, false otherwise
     */
    public boolean removePhotoFromAlbum(Album album, Photo photo) {
        if (album.removePhoto(photo)) {
            saveData();
            return true;
        }
        return false;
    }

    /**
     * Moves a photo from one album to another.
     *
     * @param fromAlbum the source album
     * @param toAlbum the destination album
     * @param photo the photo to move
     * @return true if moved successfully, false otherwise
     */
    public boolean movePhoto(Album fromAlbum, Album toAlbum, Photo photo) {
        if (fromAlbum.equals(toAlbum)) {
            return false;
        }

        if (fromAlbum.removePhoto(photo) && toAlbum.addPhoto(photo)) {
            saveData();
            return true;
        }

        // If the move failed but the photo was removed from the source album,
        // add it back to maintain consistency
        if (!fromAlbum.getPhotos().contains(photo)) {
            fromAlbum.addPhoto(photo);
        }

        return false;
    }

    /**
     * Adds a tag to a photo.
     *
     * @param photo the photo to add the tag to
     * @param type the tag type ("person" or "location")
     * @param value the tag value
     * @return true if the tag was added, false if it already exists
     */
    public boolean addTagToPhoto(Photo photo, String type, String value) {
        if (!Tag.isValidType(type)) {
            return false;
        }

        Tag tag = new Tag(type, value);
        if (photo.addTag(tag)) {
            saveData();
            return true;
        }
        return false;
    }

    /**
     * Removes a tag from a photo.
     *
     * @param photo the photo to remove the tag from
     * @param tag the tag to remove
     * @return true if removed, false otherwise
     */
    public boolean removeTagFromPhoto(Photo photo, Tag tag) {
        if (photo.removeTag(tag)) {
            saveData();
            return true;
        }
        return false;
    }

    // SEARCH FUNCTIONALITY

    /**
     * Searches for photos with tags matching a prefix.
     * Provides auto-completion functionality for tag values.
     *
     * @param type the tag type to search for
     * @param valuePrefix the prefix of the tag value to match
     * @return list of matching photos
     */
    public List<Photo> searchByTagPrefix(String type, String valuePrefix) {
        List<Photo> results = new ArrayList<>();
        Set<Photo> uniqueResults = new HashSet<>();

        if (!Tag.isValidType(type)) {
            return results;
        }

        for (Album album : albums) {
            for (Photo photo : album.getPhotos()) {
                for (Tag tag : photo.getTags()) {
                    if (tag.getType().equalsIgnoreCase(type) &&
                            tag.getValue().toLowerCase().startsWith(valuePrefix.toLowerCase())) {
                        uniqueResults.add(photo);
                    }
                }
            }
        }

        results.addAll(uniqueResults);
        return results;
    }

    /**
     * Searches for photos that have both tag1 AND tag2.
     *
     * @param type1 the first tag type
     * @param value1 the first tag value
     * @param type2 the second tag type
     * @param value2 the second tag value
     * @return list of photos that match both tag conditions
     */
    public List<Photo> searchByTagConjunction(String type1, String value1, String type2, String value2) {
        List<Photo> results = new ArrayList<>();

        if (!Tag.isValidType(type1) || !Tag.isValidType(type2)) {
            return results;
        }

        for (Album album : albums) {
            for (Photo photo : album.getPhotos()) {
                if (matchesTagPrefix(photo, type1, value1) && matchesTagPrefix(photo, type2, value2)) {
                    if (!results.contains(photo)) {
                        results.add(photo);
                    }
                }
            }
        }

        return results;
    }

    /**
     * Searches for photos that have either tag1 OR tag2.
     *
     * @param type1 the first tag type
     * @param value1 the first tag value
     * @param type2 the second tag type
     * @param value2 the second tag value
     * @return list of photos that match either tag condition
     */
    public List<Photo> searchByTagDisjunction(String type1, String value1, String type2, String value2) {
        List<Photo> results = new ArrayList<>();
        Set<Photo> uniqueResults = new HashSet<>();

        if (!Tag.isValidType(type1) || !Tag.isValidType(type2)) {
            return results;
        }

        for (Album album : albums) {
            for (Photo photo : album.getPhotos()) {
                if (matchesTagPrefix(photo, type1, value1) || matchesTagPrefix(photo, type2, value2)) {
                    uniqueResults.add(photo);
                }
            }
        }

        results.addAll(uniqueResults);
        return results;
    }

    /**
     * Checks if a photo has a tag matching the given type and value prefix.
     *
     * @param photo the photo to check
     * @param type the tag type
     * @param valuePrefix the tag value prefix
     * @return true if the photo has a matching tag
     */
    private boolean matchesTagPrefix(Photo photo, String type, String valuePrefix) {
        for (Tag tag : photo.getTags()) {
            if (tag.getType().equalsIgnoreCase(type) &&
                    tag.getValue().toLowerCase().startsWith(valuePrefix.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all possible values for a specific tag type that match a prefix.
     * Used for auto-completion suggestions.
     *
     * @param type the tag type
     * @param prefix the prefix to match
     * @return list of matching tag values
     */
    public List<String> getTagValueSuggestions(String type, String prefix) {
        Set<String> uniqueValues = new HashSet<>();

        if (!Tag.isValidType(type)) {
            return new ArrayList<>();
        }

        for (Album album : albums) {
            for (Photo photo : album.getPhotos()) {
                for (Tag tag : photo.getTags()) {
                    if (tag.getType().equalsIgnoreCase(type) &&
                            tag.getValue().toLowerCase().startsWith(prefix.toLowerCase())) {
                        uniqueValues.add(tag.getValue());
                    }
                }
            }
        }

        return new ArrayList<>(uniqueValues);
    }

    /**
     * Creates a new album from search results.
     *
     * @param name the name for the new album
     * @param photos the list of photos to include
     * @return the new album, or null if an album with this name already exists
     */
    public Album createAlbumFromSearchResults(String name, List<Photo> photos) {
        Album album = createAlbum(name);
        if (album == null) {
            return null;
        }

        for (Photo photo : photos) {
            album.addPhoto(photo);
        }

        saveData();
        return album;
    }

    // DATA PERSISTENCE

    /**
     * Saves all application data to a file.
     */
    public void saveData() {
        try {
            FileOutputStream fileOut = context.openFileOutput(DATA_FILE, Context.MODE_PRIVATE);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(albums);
            out.close();
            fileOut.close();
            Log.d(TAG, "Data saved successfully");
        } catch (IOException e) {
            Log.e(TAG, "Error saving data: " + e.getMessage());
        }
    }

    /**
     * Loads application data from a file.
     */
    @SuppressWarnings("unchecked")
    private void loadData() {
        try {
            FileInputStream fileIn = context.openFileInput(DATA_FILE);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            albums = (List<Album>) in.readObject();
            in.close();
            fileIn.close();
            Log.d(TAG, "Data loaded successfully");
        } catch (IOException | ClassNotFoundException e) {
            Log.d(TAG, "No saved data found or error loading data: " + e.getMessage());
            albums = new ArrayList<>();
        }
    }
}