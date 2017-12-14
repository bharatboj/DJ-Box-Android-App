package edu.illinois.finalproject;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.ListView;
import android.widget.ToggleButton;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.Track;

import static edu.illinois.finalproject.DJBoxUtils.getSimpleTrack;
import static edu.illinois.finalproject.DJBoxUtils.getSpotifyService;
import static edu.illinois.finalproject.MainSignInActivity.getAccessToken;
import static edu.illinois.finalproject.SpotifyClient.CLIENT_ID;

public class DJHomeActivity extends AppCompatActivity implements
        Player.NotificationCallback, ConnectionStateCallback {

    private ListView songsList;

    private String roomID;
    private List<Map.Entry<String, SimpleTrack>> tracks;
    private Player mPlayer;
    private int currentTrackPosMs;

    /**
     * This function sets up the activity
     *
     * @param savedInstanceState    a Bundle object containing the activity's previously saved state
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dj_home);

        songsList = (ListView) findViewById(R.id.lv_playlist_songs);
        ToggleButton playButton = (ToggleButton) findViewById(R.id.play_button);

        // get room, roomID, and playlist that was passed through the intents
        Intent intent = getIntent();
        roomID = intent.getStringExtra("roomID");
        Room room = intent.getParcelableExtra("room");
        PlaylistSimple playlist = intent.getParcelableExtra("playlist");

        // Sets the title of the ActionBar for this activity to the name of the room (Party)
        setTitle(room.getName());

        initializePartyPlaylist(room, playlist);
        addPlaylistTracksToDatabase(roomID, room);

        displaySongs(roomID);
        setPlayer(room.getCurrPlayingTrackID());
    }

    /**
     * Sets Set of PlaylistTrack objects to tracks that are contained within playlist
     *
     * @param playlist      PlaylistSimple object to obtain PlaylistTrack objects from
     */
    private void initializePartyPlaylist(Room room, PlaylistSimple playlist) {
        tracks = new ArrayList<>();

        // get SpotifyService object to allow to make API calls to Spotify
        SpotifyService spotify = getSpotifyService();

        // adds 100 Track objects within PlaylistSimple object to a List of Track objects
        // possible to add more, but decided that 100 is a reasonable limit for a playlist
        // that is going to be continuously edited during the party
        String playlistOwnerID = playlist.owner.id;
        String playlistID = playlist.id;
        List<PlaylistTrack> playlistTracks = spotify
                .getPlaylistTracks(playlistOwnerID, playlistID).items;

        for (int index = 0; index < playlistTracks.size(); index++) {
            // set the current Playing track and next in queue track
            Track track = playlistTracks.get(index).track;
            if (index == 0) {
                room.setCurrPlayingTrackID(track.id);
            } else if (index == 1) {
                room.setNextToPlayTrackID(track.id);
            }
            // add only the necessary parts of the track to use
            tracks.add(new AbstractMap.SimpleEntry<>(track.id, getSimpleTrack(track)));
        }
    }

    /**
     * Adds all tracks information in playlist to FireBase Database
     *
     * @param roomID    String representing roomID of room to add playlist tracks to
     */
    private void addPlaylistTracksToDatabase(String roomID, Room room) {
        // add a playlist containing the songIDs in the
        // respective room and updates FireBase room values
        room.updatePlaylist(tracks);
        FirebaseDatabase.getInstance().getReference("Rooms")
                .child(roomID).setValue(room);
    }

    /**
     * Initialize tracks to the tracks that are within the playlist of the room
     *
     * @param roomID                String representing the id of the room
     */
     private void displaySongs(String roomID) {
        DatabaseReference roomRef = FirebaseDatabase.getInstance()
                .getReference("Rooms").child(roomID);

        // updates the current playlist every time there's a change in the playlist list of tracks
        roomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                updateQueue(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    /**
     * Updates all current tracks and populates ListView with new set of tracks
     *
     * @param roomSnapshot          DataSnapshot object representing the playlist of a particular room
     */
    private void updateQueue(DataSnapshot roomSnapshot) {
        // sorts all tracks except 1st two by number of likes
        // the first two don't get sorted because they contain currently
        // playing track and queued track
        Room updatedRoom = roomSnapshot.getValue(Room.class);
        // updatedRoom is never null from the audience side when using app
        List<Map.Entry<String, SimpleTrack>> sortedPlayOrder = updatedRoom.getUpdatedPlaylist(true);

        // Uses an Adapter to populate the ListView DJ Home with each PlaylistTrack
        DJSongAdapter playlistAdapter = new DJSongAdapter(this, sortedPlayOrder);
        songsList.setAdapter(playlistAdapter);
    }

    /**
     * This function allows the user to log out from the DJ account when the "Log Out" button is
     * clicked. This will "end" the party by removing the room from the Firebase database
     *
     * @param view      View object that has actions performed when clicked on
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void onLogOutButtonClicked(final View view) {
        // remove room from Firebase to indicate party has ended once dj logs out
        FirebaseDatabase.getInstance().getReference("Rooms").child(roomID).removeValue();

        Intent logoutIntent = new Intent(this, MainSignInActivity.class);

        // makes sure user cannot navigate backwards anymore
        logoutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        startActivity(logoutIntent);

        finishAffinity();

        // code from: https://stackoverflow.com/questions/28998241/how-to-clear-cookies
        //      -and-cache-of-webview-on-android-when-not-in-webview
        //
        // Since a WebView is used, information about the user from previous use is always retined.
        // This allows to remove all cookies so the user is able to logout completely from the
        // application
        CookieManager.getInstance().removeAllCookie();
    }

    private void usePlayButtonToPlaySong(String trackID) {
        ToggleButton playButton = (ToggleButton) findViewById(R.id.play_button);
        playButton.setOnCheckedChangeListener((compoundButton, isPaused) -> {
            if (!isPaused) {
                mPlayer.playUri(null, "spotify:track:" + trackID, 0, currentTrackPosMs);
                currentTrackPosMs = (int) mPlayer.getPlaybackState().positionMs;
            } else {
                mPlayer.pause(null);
            }
        });
    }

    private void setPlayer(String trackID) {
        Config playerConfig = new Config(this, getAccessToken(), CLIENT_ID);

        Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
            @Override
            public void onInitialized(SpotifyPlayer spotifyPlayer) {
                mPlayer = spotifyPlayer;
                mPlayer.addConnectionStateCallback(DJHomeActivity.this);
                mPlayer.addNotificationCallback(DJHomeActivity.this);
                usePlayButtonToPlaySong(trackID);
            }

            @Override
            public void onError(Throwable throwable) {
            }
        });
    }

    /**
     * Makes sure that the user cannot navigate back anymore once user reaches this activity
     */
    @Override
    public void onBackPressed() {
    }

    @Override
    protected void onDestroy() {
        // VERY IMPORTANT! This must always be called or else you will leak resources
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    public void onLoggedIn() {
    }

    @Override
    public void onLoggedOut() {
    }

    @Override
    public void onLoginFailed(Error error) {
    }

    @Override
    public void onTemporaryError() {
    }

    @Override
    public void onConnectionMessage(String s) {
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
    }

    @Override
    public void onPlaybackError(Error error) {
    }
}
