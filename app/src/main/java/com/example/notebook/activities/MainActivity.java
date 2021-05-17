package com.example.notebook.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

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
    public static final int REQUEST_CODE_SELECT_IMAGE  = 4;
    public static final int REQUEST_CODE_STORAGE_PERMISSION  = 5;

    private RecyclerView notesRecyclerView;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;

    private int noteClickedPosition = -1;

    private AlertDialog dialogAddUrl;

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

        findViewById(R.id.imageAddNote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(
                        new Intent(getApplicationContext(), CreateNoteActivity.class),
                        REQUEST_CODE_ADD_NOTE
                );
            }
        });

        findViewById(R.id.imageAddImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(
                        getApplicationContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_CODE_STORAGE_PERMISSION
                    );
                } else {
                    Log.d("aa", "onClick: else part");
                    selectImage();
                }
            }
        });

        findViewById(R.id.imageAddWeb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddURLDialog();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage();
            } else {
                Toast.makeText(this, "Permission Denied!!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String getPathFromUri(Uri contentUri) {

        String filePath;
        Cursor cursor = getContentResolver()
                .query(contentUri,null, null, null, null);

        if(cursor == null) {
            filePath = contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }

        return filePath;
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
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

    @RequiresApi(api = Build.VERSION_CODES.O)
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
        } else if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if(data != null) {
                Uri selectedImageUri = data.getData();
                if(selectedImageUri != null) {
                    try {
                        String selectedImagePath = getPathFromUri(selectedImageUri);
                        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                        intent.putExtra("isFromQuickActions", true);
                        intent.putExtra("quickActionType", "image");
                        intent.putExtra("imagePath", selectedImagePath);
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                    } catch (Exception exception) {
                        Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
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

    /** This function is used to make a custom alert dialog */
    private void showAddURLDialog() {
        if(dialogAddUrl == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            getLayoutInflater();
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_add_url,
                    (ViewGroup) findViewById(R.id.layoutAddUrlContainer)
            );
            builder.setView(view);

            dialogAddUrl = builder.create();
            if(dialogAddUrl.getWindow() != null) {
                dialogAddUrl.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputURL = view.findViewById(R.id.inputUrl);
            inputURL.requestFocus();

            view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(inputURL.getText().toString().trim().isEmpty()) {
                        Toast.makeText(MainActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                    }

                    else if (!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()) {
                        Toast.makeText(MainActivity.this, "Enter Valid URL", Toast.LENGTH_SHORT).show();
                    }

                    else {
                        dialogAddUrl.dismiss();
                        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                        intent.putExtra("isFromQuickActions", true);
                        intent.putExtra("quickActionType", "URL");
                        intent.putExtra("URL", inputURL.getText().toString());
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                    }
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    dialogAddUrl.dismiss();
                }
            });
        }
        dialogAddUrl.show();
    }
}