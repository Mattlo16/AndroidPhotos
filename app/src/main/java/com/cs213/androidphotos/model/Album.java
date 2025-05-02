package com.cs213.androidphotos.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a photo album in the Photos application.
 * An album has a name and contains multiple photos.
 */
public class Album implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private List<Photo> photos;

    /**
     * Creates a new album with the specified name.
     *
     * @param name the name of the album
     */
    public Album(String name) {
        this.name = name;
        this.photos = new ArrayList<>();
    }

    /**
     * Gets the name of this album.
     *
     * @return the album name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets a new name for this album.
     *
     * @param name the new album name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the list of photos in this album.
     *
     * @return list of photos
     */
    public List<Photo> getPhotos() {
        return photos;
    }

    /**
     * Gets the number of photos in this album.
     *
     * @return the photo count
     */
    public int getPhotoCount() {
        return photos.size();
    }

    /**
     * Adds a photo to this album.
     * Will not add if a photo with the same file path already exists in the album.
     *
     * @param photo the photo to add
     * @return true if the photo was added, false if a duplicate exists
     */
    public boolean addPhoto(Photo photo) {
        // Check for duplicates (same file path)
        for (Photo existingPhoto : photos) {
            if (existingPhoto.getFilePath().equals(photo.getFilePath())) {
                return false; // Duplicate found
            }
        }

        photos.add(photo);
        return true;
    }

    /**
     * Removes a photo from this album.
     *
     * @param photo the photo to remove
     * @return true if the photo was removed, false if it wasn't found
     */
    public boolean removePhoto(Photo photo) {
        return photos.remove(photo);
    }

    /**
     * Returns a string representation of this album.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "Album: " + name + " (Photos: " + photos.size() + ")";
    }
}