package com.example.notebook.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.notebook.R;
import com.example.notebook.adapters.NotesAdapter;
import com.example.notebook.database.NotesDatabase;
import com.example.notebook.entities.Note;
import com.example.notebook.listeners.NoteListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NoteListener {

    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_SHOW_NOTES = 3;

    private RecyclerView notesRecyclerView;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;

    private int noteClickedPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageAddNoteMain = findViewById(R.id.imageAddNoteMain);
        imageAddNoteMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(
                        new Intent(getApplicationContext(), CreateNoteActivity.class),
                        REQUEST_CODE_ADD_NOTE
                );
            }
        });

        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        notesRecyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        );

        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, this);
        notesRecyclerView.setAdapter(notesAdapter);

        /**
         * This getNotes() method is called from onCreate() method of an activity. It means
         * the application is just started and we need to display all notes from database
         * and that's why we are passing REQUEST_CODE_SHOW_NOTES to that method..
         * */
        getNotes(REQUEST_CODE_SHOW_NOTES, false);

        /**
         * For searching Note*/
        EditText inputSearch = findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                notesAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (noteList.size() != 0) {
                    notesAdapter.searchNotes(s.toString());
                }
            }
        });
    }

    /**
     * This code is used to view or update the note.
     * */
    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("note", note);
        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {
            /**
             * This getNotes() method is called from the onActivityResult() method of activity and
             * we checked the current request code is for add note and the result is RESULT_OK. It
             * means a new note is added from CreateNote activity and its result is sent back to this
             * activity that's why we are passing REQUEST_CODE_ADD_NOTE to that method.
             * */
            getNotes(REQUEST_CODE_ADD_NOTE, false);

        } else if (requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK) {
            if(data != null) {
                /**
                 * This getNotes() method is called from the onActivityResult() method of activity
                 * and we checked the current request code is for update note and the result is
                 * RESULT_OK. It means already available note is updated from CreateNote activity
                 * and its result is sent back to this activity that's why we are passing
                 * REQUEST_CODE_UPDATE_NOTE to that method.
                 * */
                getNotes(REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDeleted", false));
            }
        }
    }

    private void getNotes(final int requestCode, final boolean isNoteDeleted) {

        /** Android AyncTask going to do background operation on background thread
         *   and update on main thread. In android we cant directly touch background
         *   thread to main thread in android development. Asynctask help us to make
         *   communication between background thread to main thread.
         */
        @SuppressLint("StaticFieldLeak")
        class GetNodesTask extends AsyncTask <Void, Void, List<Note>> {

            @Override
            protected List<Note> doInBackground(Void... voids) {
                return NotesDatabase
                        .getDatabase(getApplicationContext())
                        .noteDao().getAllNotes();
            }

            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);

                if(requestCode == REQUEST_CODE_SHOW_NOTES) {
                    /**
                     * Here, request code is REQUEST_CODE_SHOW_NOTES, so we are adding all notes
                     * from database to noteList and notify adapter about the new data set.
                     * */
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                }

                else if(requestCode == REQUEST_CODE_ADD_NOTE) {
                    /**
                     * Here, request code is REQUEST_CODE_ADD_NOTE, so we are adding an only first
                     * note (newly added note) from database to noteList and notify the adapter
                     * for the newly inserted item and scrolling recycler view to the top.
                     * */
                    noteList.add(0, notes.get(0));
                    notesAdapter.notifyItemInserted(0);
                    notesRecyclerView.smoothScrollToPosition(0);
                }

                else if (requestCode == REQUEST_CODE_UPDATE_NOTE) {
                    /**
                     * Here, request code is REQUEST_CODE_UPDATE_NOTE, so we are removing note
                     * from the clicked position and adding the latest updated note from same
                     * position from the database and notify the adapter for item changed at the
                     * position.
                     * */
                    noteList.remove(noteClickedPosition);
                    if(isNoteDeleted) {
                        notesAdapter.notifyItemRemoved(noteClickedPosition);
                    } else {
                        noteList.add(noteClickedPosition, notes.get(noteClickedPosition));
                        notesAdapter.notifyItemChanged(noteClickedPosition);
                    }
                }
            }
        }

        new GetNodesTask().execute();
    }
}