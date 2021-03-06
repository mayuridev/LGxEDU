package com.lglab.ivan.lgxeducontroller.activities.manager.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lglab.ivan.lgxeducontroller.R;
import com.lglab.ivan.lgxeducontroller.activities.manager.EditGameActivity;
import com.lglab.ivan.lgxeducontroller.activities.manager.adapters.CategoryManagerAdapter;
import com.lglab.ivan.lgxeducontroller.games.Category;
import com.lglab.ivan.lgxeducontroller.games.Game;
import com.lglab.ivan.lgxeducontroller.games.GameEnum;
import com.lglab.ivan.lgxeducontroller.games.GameManager;
import com.lglab.ivan.lgxeducontroller.legacy.data.POIsProvider;

import org.json.JSONException;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class AddGameFragment extends DialogFragment {

    private static int RESULT_LOAD_IMG = 1;

    private Game game;
    private CategoryManagerAdapter adapter;
    private int flatPosition;

    private EditText gameTitleText;
    private Spinner gameTypeSpinner;
    private AppCompatImageView gameImage;
    private Button selectImageButton;
    private AutoCompleteTextView categoryAutoComplete;
    private ArrayAdapter<Category> categoryStringList;
    private ArrayAdapter<String> gameTypeStringList;

    private Category selectedCategory = null;
    private GameEnum selectedGameType = null;

    public AddGameFragment(Game game, CategoryManagerAdapter adapter, final int flatPosition) {
        super();
        this.game = game;
        this.adapter = adapter;
        this.flatPosition = flatPosition;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());

        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.fragment_add_game, null);
        builder.setView(dialogView);

        gameTitleText = dialogView.findViewById(R.id.game_title_text);
        gameTypeSpinner = dialogView.findViewById(R.id.game_type_spinner);
        gameImage = dialogView.findViewById(R.id.image_create_game);
        selectImageButton = dialogView.findViewById(R.id.select_image_button);
        categoryAutoComplete = dialogView.findViewById(R.id.game_category_autocomplete);

        selectImageButton.setOnClickListener((view) -> {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);
        });

        if (game == null) {

            builder.setTitle("Create a game");


            List<Category> categories = new ArrayList<>();
            Cursor category_cursor = POIsProvider.getAllGameCategories();
            while (category_cursor.moveToNext()) {
                long categoryId = category_cursor.getLong(category_cursor.getColumnIndexOrThrow("_id"));
                String categoryName = category_cursor.getString(category_cursor.getColumnIndexOrThrow("Name"));
                Category cat = new Category(categoryId, categoryName, new ArrayList<>());
                categories.add(cat);
            }
            category_cursor.close();
            selectedCategory = categories.get(0);

            categoryStringList = new ArrayAdapter<>(this.getContext(), android.R.layout.select_dialog_item);
            categoryStringList.addAll(categories);
            categoryAutoComplete.setAdapter(categoryStringList);
            categoryAutoComplete.setOnItemClickListener((parent, view1, position, id) -> selectedCategory = categoryStringList.getItem(position));


            List<String> list = new ArrayList<>();
            for (GameEnum gameType : GameEnum.values()) {
                list.add(gameType.name());
            }

            gameTypeStringList = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, list);
            gameTypeStringList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            gameTypeSpinner.setAdapter(gameTypeStringList);
            gameTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long selectedItemId) {
                    selectedGameType = GameEnum.findByName(gameTypeStringList.getItem(pos));
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            selectedGameType = GameEnum.findByName(list.get(0));

            // Show soft keyboard automatically and request focus to field
            gameTitleText.requestFocus();

            builder.setPositiveButton("Create", (dialog, id) -> {
                dialog.cancel();

                BitmapDrawable drawable = (BitmapDrawable) gameImage.getDrawable();

                Game newGame = GameManager.createGame(gameTitleText.getText().toString(), selectedGameType, drawable != null ? drawable.getBitmap() : null, categoryAutoComplete.getText().toString(), getContext());
                newGame.getQuestions().add(newGame.createQuestion());
                GameManager.editGame(newGame);

                Intent intent = new Intent(getContext(), EditGameActivity.class);
                intent.putExtra("is_new", true);
                getContext().startActivity(intent);
            })
                    .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel());

            return builder.create();
        }

        builder.setTitle("Modify a game");
        gameTitleText.setText(game.getName());

        categoryAutoComplete.setVisibility(View.GONE);
        gameTypeSpinner.setVisibility(View.GONE);

        gameImage.setImageBitmap(game.getImage(getContext()));
        gameImage.setVisibility(View.VISIBLE);

        gameTitleText.requestFocus();

        builder.setPositiveButton("Modify", (dialog, id) -> {
            dialog.cancel();
            game.setName(gameTitleText.getText().toString());
            if (hasChanged) {
                BitmapDrawable drawable = (BitmapDrawable) gameImage.getDrawable();
                game.setNewImage(drawable != null ? drawable.getBitmap() : null, getContext());
            }
            try {
                POIsProvider.updateGameById((int) game.getId(), game.pack().toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            adapter.notifyItemChanged(flatPosition);
        }).setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel());

        return builder.create();
    }

    private boolean hasChanged = false;

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        if (reqCode == RESULT_LOAD_IMG && resultCode == RESULT_OK) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getActivity().getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                gameImage.setImageBitmap(selectedImage);
                hasChanged = true;
                gameImage.setVisibility(View.VISIBLE);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_LONG).show();
            }

        } else {
            Toast.makeText(getContext(), "You haven't picked Image", Toast.LENGTH_LONG).show();
        }
    }

    public static AddGameFragment newInstance(Game game, CategoryManagerAdapter adapter, final int flatPosition) {
        AddGameFragment frag = new AddGameFragment(game, adapter, flatPosition);
        return frag;
    }
}
