package com.lglab.ivan.lgxeducontroller.activities.manager.adapters;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.lglab.ivan.lgxeducontroller.R;
import com.lglab.ivan.lgxeducontroller.activities.manager.EditGameActivity;
import com.lglab.ivan.lgxeducontroller.activities.manager.IGamesAdapterActivity;
import com.lglab.ivan.lgxeducontroller.activities.manager.fragments.AddGameFragment;
import com.lglab.ivan.lgxeducontroller.drive.GoogleDriveManager;
import com.lglab.ivan.lgxeducontroller.games.Category;
import com.lglab.ivan.lgxeducontroller.games.Game;
import com.lglab.ivan.lgxeducontroller.games.GameManager;
import com.lglab.ivan.lgxeducontroller.legacy.data.POIsProvider;
import com.thoughtbot.expandablerecyclerview.ExpandableRecyclerViewAdapter;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;
import com.thoughtbot.expandablerecyclerview.models.ExpandableListPosition;
import com.thoughtbot.expandablerecyclerview.viewholders.ChildViewHolder;
import com.thoughtbot.expandablerecyclerview.viewholders.GroupViewHolder;

import org.json.JSONException;

import java.util.List;

public class CategoryManagerAdapter extends ExpandableRecyclerViewAdapter<CategoryManagerAdapter.CategoryViewHolder, CategoryManagerAdapter.GameViewHolder> {

    private IGamesAdapterActivity activity;

    public CategoryManagerAdapter(List<? extends ExpandableGroup> groups, IGamesAdapterActivity activity) {
        super(groups);
        this.activity = activity;
    }

    @Override
    public CategoryViewHolder onCreateGroupViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public GameViewHolder onCreateChildViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_quiz, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindChildViewHolder(GameViewHolder holder, int flatPosition, ExpandableGroup group,
                                      int childIndex) {
        final Game game = ((Category) group).getItems().get(childIndex);
        holder.onBind(game, this, flatPosition);

    }

    @Override
    public void onBindGroupViewHolder(CategoryViewHolder holder, int flatPosition,
                                      ExpandableGroup group) {
        holder.setCategoryTitle(group);
        holder.arrow.setOnClickListener(holder);
    }

    private void removeItem(int position) {
        ExpandableListPosition listPos = expandableList.getUnflattenedPosition(position);
        ExpandableGroup group = expandableList.getExpandableGroup(listPos);
        switch (listPos.type) {
            case ExpandableListPosition.CHILD:
                group.remove(listPos.childPos);
                if (group.getItemCount() == 0) {
                    expandableList.remove(listPos.groupPos);
                    notifyItemRangeRemoved(position - 1, 2);
                    activity.onGamesChanged(false);
                } else {
                    notifyItemRemoved(position);
                    activity.onGamesChanged(false);
                }
                break;
            case ExpandableListPosition.GROUP:
                expandableList.remove(listPos.groupPos);
                notifyItemRangeRemoved(position, group.getItemCount() + 1);
                activity.onGamesChanged(false);
                break;
        }
    }

    static class CategoryViewHolder extends GroupViewHolder {

        private TextView categoryTitle;
        private ImageButton arrow;

        CategoryViewHolder(View itemView) {
            super(itemView);
            categoryTitle = itemView.findViewById(R.id.list_item_category_name);
            arrow = itemView.findViewById(R.id.list_item_category_arrow);

        }

        void setCategoryTitle(ExpandableGroup group) {
            categoryTitle.setText(group.getTitle());
        }
    }

    public static class GameViewHolder extends ChildViewHolder {

        public Game game;
        private TextView quizName;
        private ImageButton editButton;
        private ImageButton deleteButton;
        private ImageButton shareButton;

        GameViewHolder(View itemView) {
            super(itemView);

            quizName = itemView.findViewById(R.id.list_item_quiz_name);
            editButton = itemView.findViewById(R.id.list_edit_game);
            deleteButton = itemView.findViewById(R.id.list_delete_game);
            shareButton = itemView.findViewById(R.id.list_share_game);
        }

        void onBind(Game game, final CategoryManagerAdapter adapter, final int flatPosition) {
            this.game = game;
            quizName.setText(game.getName());

            this.quizName.setOnClickListener(view -> AddGameFragment.newInstance(game, adapter, flatPosition).show(((FragmentActivity) itemView.getContext()).getSupportFragmentManager(), "fragment_modify_game"));

            if(!game.getFileId().equals("")) {
                this.shareButton.setVisibility(View.GONE);
            }

            this.shareButton.setOnClickListener(view -> {
                new AlertDialog.Builder(itemView.getContext())
                        .setTitle("Do you want to upload \"" + game.getName() + "\" to your drive?")
                        //.setMessage("")
                        .setPositiveButton("Upload", (dialog, id) -> {
                            //upload to GoogleDrive
                            if(GoogleDriveManager.DriveServiceHelper == null) {
                                Toast.makeText(view.getContext(), "unable to login to google drive", Toast.LENGTH_LONG).show();
                                return;
                            }

                            GoogleDriveManager.DriveServiceHelper.createFile()
                                    .addOnSuccessListener((result) -> {

                                        try {
                                            GoogleDriveManager.DriveServiceHelper.saveFile(result, game.getNameForExporting(), game.pack_external(view.getContext()).toString())
                                                .addOnFailureListener(exception -> Log.d("drive", exception.toString()))
                                                .addOnSuccessListener(result2 -> {
                                                    view.setVisibility(View.GONE);
                                                    Log.d("drive", result);
                                                    game.setFileId(result);
                                                    POIsProvider.updateGameFileIdById(game.getId(), result);
                                                });
                                        }
                                        catch (JSONException ignored) {

                                        }
                                    })
                                    .addOnFailureListener(exception -> Log.d("drive", exception.toString()));
                        })
                        .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel())
                        .create()
                        .show();
            });

            this.editButton.setOnClickListener(view -> {

                GameManager.editGame(this.game);

                Intent intent = new Intent(itemView.getContext(), EditGameActivity.class);
                intent.putExtra("is_new", false);
                itemView.getContext().startActivity(intent);
            });

            this.deleteButton.setOnClickListener(view ->
                    new AlertDialog.Builder(itemView.getContext())
                            .setTitle("Are you sure you want to delete \"" + game.getName() + "\"?")
                            .setMessage("The game will be deleted immediately. You can't undo this action.")
                            .setPositiveButton("Delete", (dialog, id) -> {
                                POIsProvider.removeGameById((int) game.getId());
                                adapter.removeItem(flatPosition);
                            })
                            .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel())
                            .create()
                            .show());
        }
    }
}