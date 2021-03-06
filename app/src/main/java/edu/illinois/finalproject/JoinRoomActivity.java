package edu.illinois.finalproject;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class JoinRoomActivity extends AppCompatActivity {

    private ListView roomList;
    private Map<String, Room> rooms;

    /**
     * This function sets up the activity
     *
     * @param savedInstanceState a Bundle object containing the activity's previously saved state
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.join_room);

        // initialize fields
        roomList = (ListView) findViewById(R.id.lv_join_room_list);
        rooms = new HashMap<>();

        displayListOfRooms();
    }

    /**
     * Displays all the Rooms that are listed in Firebase
     */
    private void displayListOfRooms() {
        DatabaseReference roomsRef = FirebaseDatabase.getInstance().getReference("Rooms");

        roomsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // update rooms every time there's a change
                updateRooms(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    /**
     * Updates all the Rooms based on the DataSnapshot of the list of rooms provided
     *
     * @param roomsSnap     DataSnapshot object that represents all the room subtrees
     */
    private void updateRooms(DataSnapshot roomsSnap) {
        // assuming there will not be many rooms that show up on the user's page setting to
        // an empty HashTable each time there's a change will not disrupt performance much.
        rooms.clear();
        for (DataSnapshot roomSnap : roomsSnap.getChildren()) {
            // adds the roomID and room to
            String roomID = roomSnap.getKey();
            Room room = roomSnap.getValue(Room.class);
            rooms.put(roomID, room);
        }

        if (rooms.size() == 0) {
            String noActiveRoomsText = "No active rooms in this area. Please check again later.";
            Toast.makeText(this, noActiveRoomsText, Toast.LENGTH_SHORT).show();
        }

        // Populates the join room ListView with all the appropriate room informatoin
        RoomAdapter roomAdapter = new RoomAdapter(this, rooms);
        roomList.setAdapter(roomAdapter);
    }
}