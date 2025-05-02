package com.cs213.androidphotos.model;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a photo in the Photos application.
 * A photo has a file path and tags.
 */
public class Photo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String filePath;
    private List<Tag> tags;

    /**
     * Creates a new Photo with the specified file path.
     *
     * @param filePath the path to the photo file
     */
    public Photo(String filePath) {
        this.filePath = filePath;
        this.tags = new ArrayList<>();
    }

    /**
     * Gets the file path of this photo.
     *
     * @return the file path
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Gets the filename to use as caption.
     *
     * @return the filename
     */
    public String getFileName() {
        return new File(filePath).getName();
    }

    /**
     * Gets all tags for this photo.
     *
     * @return list of tags
     */
    public List<Tag> getTags() {
        return tags;
    }

    /**
     * Adds a tag to this photo.
     * Will not add if a duplicate tag (same name and value) already exists.
     *
     * @param tag the tag to add
     * @return true if tag was added, false if a duplicate exists
     */
    public boolean addTag(Tag tag) {
        // Check for duplicate tags
        for (Tag existingTag : tags) {
            if (existingTag.getType().equalsIgnoreCase(tag.getType()) &&
                    existingTag.getValue().equalsIgnoreCase(tag.getValue())) {
                return false; // Duplicate tag found
            }
        }

        tags.add(tag);
        return true;
    }

    /**
     * Removes a tag from this photo.
     *
     * @param tag the tag to remove
     * @return true if the tag was removed, false if it wasn't found
     */
    public boolean removeTag(Tag tag) {
        return tags.remove(tag);
    }

    /**
     * Finds tags with the specified type.
     *
     * @param type the tag type to search for
     * @return list of matching tags
     */
    public List<Tag> getTagsByType(String type) {
        List<Tag> matchingTags = new ArrayList<>();

        for (Tag tag : tags) {
            if (tag.getType().equalsIgnoreCase(type)) {
                matchingTags.add(tag);
            }
        }

        return matchingTags;
    }

    /**
     * Checks if this photo has a tag with the given type and value.
     *
     * @param type the tag type
     * @param value the tag value
     * @return true if the photo has the specified tag
     */
    public boolean hasTag(String type, String value) {
        for (Tag tag : tags) {
            if (tag.getType().equalsIgnoreCase(type) &&
                    tag.getValue().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a string representation of this photo.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "Photo: " + getFileName() + " (Tags: " + tags.size() + ")";
    }
}