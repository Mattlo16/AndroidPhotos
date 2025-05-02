package com.cs213.androidphotos.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a tag for a photo in the Photos application.
 * A tag consists of a type-value pair (e.g., "location" and "New York").
 * Only "person" and "location" are valid tag types.
 */
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_PERSON = "person";
    public static final String TYPE_LOCATION = "location";

    private String type;
    private String value;

    /**
     * Creates a new tag with the specified type and value.
     * Only "person" and "location" are valid tag types.
     *
     * @param type the tag type (must be "person" or "location")
     * @param value the tag value (e.g., "New York", "Alice")
     * @throws IllegalArgumentException if type is not "person" or "location"
     */
    public Tag(String type, String value) {
        if (!isValidType(type)) {
            throw new IllegalArgumentException("Invalid tag type. Only 'person' or 'location' allowed.");
        }
        this.type = type.toLowerCase();
        this.value = value;
    }

    /**
     * Gets the type of this tag.
     *
     * @return the tag type
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the value of this tag.
     *
     * @return the tag value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of this tag.
     *
     * @param value the new value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Checks if the given string is a valid tag type.
     *
     * @param type the tag type to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidType(String type) {
        return TYPE_PERSON.equalsIgnoreCase(type) ||
                TYPE_LOCATION.equalsIgnoreCase(type);
    }

    /**
     * Checks if this tag equals another object.
     * Two tags are equal if they have the same type and value.
     *
     * @param obj the object to compare with
     * @return true if the objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Tag other = (Tag) obj;
        return type.equalsIgnoreCase(other.type) &&
                value.equalsIgnoreCase(other.value);
    }

    /**
     * Returns a hash code for this tag.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(type.toLowerCase(), value.toLowerCase());
    }

    /**
     * Returns a string representation of this tag.
     *
     * @return string representation in the format "type=value"
     */
    @Override
    public String toString() {
        return type + "=" + value;
    }
}