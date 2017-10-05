package com.bignerdranch.android.twittersearches;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    private static final String SEARCHES = "searches";
    private EditText queryEditText;
    private EditText tagEditText;
    private SharedPreferences savedSearches;
    private ArrayList<String> tags;
    private ArrayAdapter<String> adapter;
    private RecyclerView mTagRecyclerView;
    private TagAdapter mTagAdapter;
    private ImageButton mSaveTagButton;


    private class TagAdapter extends RecyclerView.Adapter<TagHolder> {
        private ArrayList<String> theTags;
        private LayoutInflater mInflater;

        public TagAdapter(Context context, ArrayList<String> theTags) {
            this.mInflater = LayoutInflater.from(context);
            this.theTags = theTags;
        }

        @Override
        public TagHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.list_item, parent, false);
            return new TagHolder(view);
        }

        @Override
        public void onBindViewHolder(TagHolder holder, int position) {
            String tag = tags.get(position);
            TextView curTagHolder = holder.tagHolderView;
            curTagHolder.setText(tag);
        }

        @Override
        public int getItemCount() {
            return tags.size();
        }

    }

    private class TagHolder extends RecyclerView.ViewHolder  implements View.OnClickListener, View.OnLongClickListener {
        public TextView tagHolderView;


        public TagHolder (View itemView)
        {
            super(itemView);
            tagHolderView = (TextView) itemView.findViewById(R.id.tagSearchList);
            tagHolderView.setOnClickListener(this);
            tagHolderView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            //get query string and create a URL representing the search
            String tagToSend = ((TextView) view).getText().toString();
            String urlString = getString(R.string.searchURL) +
                    Uri.encode(savedSearches.getString(tagToSend, ""), "UTF-8");

            // create an Intent to launch a web browser
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));

            // launches web browser to view results
            startActivity(webIntent);
        }

        @Override
        public boolean onLongClick(View view) {
            //get the tag that the user long touched
            final String tag = ((TextView) view).getText().toString();

            // create a enw AlertDialog
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

            // set the AlertDialog's title
            builder.setTitle(getString(R.string.shareEditDeleteTitle, tag));

            // set list of items to display in dialog
            builder.setItems(R.array.dialog_items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    switch(i) {
                        case 0: // share
                            shareSearch(tag);
                            break;
                        case 1: // edit
                            // set EditTexts to match chosen tag and query
                            tagEditText.setText(tag);
                            queryEditText.setText(savedSearches.getString(tag,""));
                            break;
                        case 2: //dete
                            deleteSearch(tag);
                            break;
                    }
                }
            });
            builder.create().show();
            return false;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // wire up widgets
        queryEditText = (EditText) findViewById(R.id.queryEditText);
        tagEditText = (EditText) findViewById(R.id.tagEditText);

        // get the ShredPreferences containging the user's save searches
        savedSearches = getSharedPreferences(SEARCHES, MODE_PRIVATE);

        // store the saved tags in an ArrayList then sort them
        tags = new ArrayList<String>(savedSearches.getAll().keySet());
        Collections.sort(tags, String.CASE_INSENSITIVE_ORDER);

        mTagRecyclerView = (RecyclerView) findViewById(R.id.list_of_tags);
        mTagRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mTagAdapter = new TagAdapter(this, tags);
        mTagRecyclerView.setAdapter(mTagAdapter);

        mSaveTagButton = (ImageButton) findViewById(R.id.saveButton);
        mSaveTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // create tag if neither queryEditText nor TagEditText is empty
                if ((queryEditText.getText().length() > 0) &&
                    (tagEditText.getText().length() > 0)) {
                    addTaggedSearch(queryEditText.getText().toString(),
                                    tagEditText.getText().toString());

                    // clear out entry fields once search is added to persistent storage
                    queryEditText.setText("");
                    tagEditText.setText("");

                    ((InputMethodManager)getSystemService(
                            Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
                                    tagEditText.getWindowToken(), 0);
                }
                else { // display message asking user to provide query and a tag

                    //create a new AlertDialog Builder
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                    // set dialog's title and message to dislay
                    builder.setMessage(R.string.missingMessage);

                    // provide an OK button that simply dismisses the dialog
                    builder.setPositiveButton(getString(R.string.OK), null);

                    // create AlertDialog from the AlertDialog.Builder
                    AlertDialog errorDialog = builder.create();
                    errorDialog.show();
                }
            }
        });
    }

    private void addTaggedSearch(String query, String tag) {
        // get a SharedPreferences Editor to store enw tag/query pair
        SharedPreferences.Editor preferencesEditor = savedSearches.edit();
        preferencesEditor.putString(tag, query);
        preferencesEditor.apply();

        if (!tags.contains(tag)) {
            tags.add(tag);
            Collections.sort(tags, String.CASE_INSENSITIVE_ORDER);
            mTagAdapter.notifyDataSetChanged();
        }
    }

    // allows user to choose an app for searching a saved search's URL
    private void shareSearch(String tag) {
        // create the URL representing the search
        String urlString = getString(R.string.searchURL);
        Uri.encode(savedSearches.getString(tag, ""), "UTF-8");

        /* Create Intent to share urlString, let activities capable of handling send actions
           work with this intent */
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);

        // Note: Not all activities supporting ACTION_SEND will support EXTRA_SUBJECT
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.shareSubject));

        // insert urlString into string placeholder
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.shareMessage, urlString));

        // let any activity capable of sending plain text message handle it
        shareIntent.setType("text/plain");

        //display apps that can share intent
        startActivity(Intent.createChooser(shareIntent, getString(R.string.shareSearch)));
    }

    // deletes a search after the user confirms the delete operation
    private void deleteSearch(final String tag) {
        // create a new AlertDialog
        AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(this);

        // set the AlertDialog's message
        confirmBuilder.setMessage(getString(R.string.confirmMessage, tag));

        // set the AlerDialog's negative button
        confirmBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel(); //dimiss dialog
            }
        });

        // set the AlertDialog's positive button
        confirmBuilder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
            @Override

            // called when "Cancel" Button is clicked
            public void onClick(DialogInterface dialogInterface, int i) {
                tags.remove(tag); //remove tag from tags

                //get SharedPreferences Editor to remove saved search
                SharedPreferences.Editor preferencesEditor = savedSearches.edit();
                preferencesEditor.remove(tag); //remove search
                preferencesEditor.apply(); //saves the searches

                // update adapter
                mTagAdapter.notifyDataSetChanged();
            }
        });

        // dispaly alertdialog
        confirmBuilder.create().show();
    }
}
