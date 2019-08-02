package com.lglab.ivan.lgxeducontroller.activities.manager;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lglab.ivan.lgxeducontroller.R;
import com.lglab.ivan.lgxeducontroller.activities.manager.adapters.CategoryManagerAdapter;
import com.lglab.ivan.lgxeducontroller.activities.manager.fragments.AddGameFragment;
import com.lglab.ivan.lgxeducontroller.drive.GoogleDriveManager;
import com.lglab.ivan.lgxeducontroller.games.Category;
import com.lglab.ivan.lgxeducontroller.games.Game;
import com.lglab.ivan.lgxeducontroller.games.GameManager;
import com.lglab.ivan.lgxeducontroller.legacy.data.POIsProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ManageGamesFragment extends Fragment implements IGamesAdapterActivity {

    public static ManageGamesFragment newInstance() {
        return new ManageGamesFragment();
    }

    private CategoryManagerAdapter adapter;
    private RecyclerView recyclerView;
    private TextView textView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_games_manager, null, false);

        recyclerView = rootView.findViewById(R.id.recycler_view);

        LinearLayoutManager layoutManager = new LinearLayoutManager(rootView.getContext());

        textView = rootView.findViewById(R.id.no_games_found);
        textView.setVisibility(View.GONE);
        recyclerView.setLayoutManager(layoutManager);

        rootView.findViewById(R.id.add_game).setOnClickListener(view -> AddGameFragment.newInstance(null, null, 0).show(getFragmentManager(), "fragment_add_game"));

        rootView.findViewById(R.id.manage_drive).setOnClickListener(view -> {
            if(GoogleDriveManager.DriveServiceHelper != null) {

                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
                builder.setView(R.layout.progress);
                Dialog loading_dialog = builder.create();
                loading_dialog.setCancelable(false);
                loading_dialog.setCanceledOnTouchOutside(false);
                loading_dialog.show();

                GoogleDriveManager.DriveServiceHelper.searchForAppFolderID(() -> {
                    for(String filename : GoogleDriveManager.DriveServiceHelper.files.values()) {
                        Log.d("drive", filename);
                    }
                    loading_dialog.dismiss();
                });
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        reloadAdapter();
    }

    private List<Category> makeCategories() {
        HashMap<String, Category> categories = new HashMap<>();

        Cursor category_cursor = POIsProvider.getAllGameCategories();
        while (category_cursor.moveToNext()) {
            long categoryId = category_cursor.getLong(category_cursor.getColumnIndexOrThrow("_id"));
            String categoryName = category_cursor.getString(category_cursor.getColumnIndexOrThrow("Name"));
            categories.put(categoryName.toLowerCase(), new Category(categoryId, categoryName, new ArrayList<>()));
        }
        category_cursor.close();

        Cursor game_cursor = POIsProvider.getAllGames();
        while (game_cursor.moveToNext()) {
            long gameId = game_cursor.getLong(game_cursor.getColumnIndexOrThrow("_id"));
            String questData = game_cursor.getString(game_cursor.getColumnIndexOrThrow("Data"));
            String fileId = game_cursor.getString(game_cursor.getColumnIndexOrThrow("google_drive_file_id"));

            try {
                Game newGame = GameManager.unpackGame(new JSONObject(questData));
                newGame.setId(gameId);
                newGame.setFileId(fileId);
                Category category = categories.get(newGame.getCategory().toLowerCase());
                if (category == null) {
                    long id = POIsProvider.insertCategoryGame(newGame.getCategory());
                    categories.put(newGame.getCategory().toLowerCase(), new Category(id, newGame.getCategory(), Collections.singletonList(newGame)));
                } else {
                    category.getItems().add(newGame);
                }
            } catch (JSONException e) {
                Log.e("TAG", e.toString());
            }
        }
        game_cursor.close();

        //REMOVE EMPTY CATEGORIES
        Iterator<Map.Entry<String, Category>> iter = categories.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Category> entry = iter.next();
            if (entry.getValue().getItems().size() == 0) {
                iter.remove();
            } else {
                Collections.sort(entry.getValue().getItems(), (p1, p2) -> p1.getName().compareTo(p2.getName()));
            }
        }

        //ORDER CATEGORIES BY ID
        ArrayList<Category> orderedCategories = new ArrayList<>(categories.values());
        Collections.sort(orderedCategories, (f1, f2) -> f1.getTitle().compareTo(f2.getTitle()));

        return orderedCategories;
    }

    private void reloadAdapter() {

        List<Category> categories = makeCategories();
        adapter = new CategoryManagerAdapter(categories, this);
        recyclerView.setAdapter(adapter);

        for (int i = adapter.getGroups().size() - 1; i >= 0; i--) {
            if (adapter.isGroupExpanded(i)) {
                continue;
            }
            adapter.toggleGroup(i);
        }

        onGamesChanged(false);
    }

    @Override
    public void onGamesChanged(boolean reloadAdapter) {
        if (reloadAdapter) {
            reloadAdapter();
            return;
        }

        if (adapter.getGroups().size() == 0) {
            recyclerView.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() { super.onDestroy();

        try {
            trimCache(getContext());
            // Toast.makeText(this,"onDestroy " ,Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void trimCache(Context context) {
        try {
            File[] dirs = ContextCompat.getExternalCacheDirs(context);
            for (File dir : dirs) {
                if (dir != null && dir.isDirectory()) {
                    deleteDir(dir);
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }
}
