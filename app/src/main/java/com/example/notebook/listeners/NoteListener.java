package com.example.notebook.listeners;

import com.example.notebook.entities.Note;

public interface NoteListener {
    void onNoteClicked(Note note, int position);
}
