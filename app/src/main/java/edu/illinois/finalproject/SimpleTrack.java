package edu.illinois.finalproject;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.HashMap;

class SimpleTrack implements Parcelable, Comparable {
    private String name;
    private String artists;
    private String duration;
    private int durationMs;
    private int likesCount;
    private HashMap<String, Boolean> likedBy;
    private String imageUrl;

    SimpleTrack() {
    }

    private SimpleTrack(Parcel in) {
        this.name = in.readString();
        this.artists = in.readString();
        this.duration = in.readString();
        this.durationMs = in.readInt();
        this.likesCount = in.readInt();
        this.likedBy = (HashMap<String, Boolean>) in.readSerializable();
        this.imageUrl = in.readString();
    }

    SimpleTrack(String name, String artists, String duration, int durationMs
            , int likesCount, HashMap<String, Boolean> likes, String imageUrl) {
        this.name = name;
        this.artists = artists;
        this.duration = duration;
        this.durationMs = durationMs;
        this.likesCount = likesCount;
        this.likedBy = likes;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getArtists() {
        return artists;
    }

    public String getDuration() {
        return duration;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public HashMap<String, Boolean> getLikes() {
        return likedBy;
    }

    public String getImageUrl() {
        return imageUrl;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.artists);
        dest.writeString(this.duration);
        dest.writeInt(this.durationMs);
        dest.writeInt(this.likesCount);
        dest.writeSerializable(this.likedBy);
        dest.writeString(this.imageUrl);
    }

    public static final Creator<SimpleTrack> CREATOR = new Creator<SimpleTrack>() {
        @Override
        public SimpleTrack createFromParcel(Parcel source) {
            return new SimpleTrack(source);
        }

        @Override
        public SimpleTrack[] newArray(int size) {
            return new SimpleTrack[size];
        }
    };

    @Override
    public int compareTo(@NonNull Object anotherTrack) {
        if (!(anotherTrack instanceof SimpleTrack))
            throw new ClassCastException("A SimpleTrack object expected.");
        int anotherPersonAge = ((SimpleTrack) anotherTrack).getLikesCount();
        return anotherPersonAge - this.likesCount;
    }
}
