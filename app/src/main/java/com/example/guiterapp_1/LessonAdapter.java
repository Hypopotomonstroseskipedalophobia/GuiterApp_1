package com.example.guiterapp_1;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.example.guiterapp_1.data.Lesson;
import com.example.guiterapp_1.data.LessonWithExercise;
import com.example.guiterapp_1.databinding.ItemMediaRowBinding;
import com.example.guiterapp_1.databinding.ItemVideoBinding;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LessonAdapter extends RecyclerView.Adapter<LessonAdapter.LessonViewHolder> {

    private List<LessonWithExercise> lessons = new ArrayList<>();
    private final OnLessonClickListener listener;
    private final Set<Integer> expandedLessonIds = new HashSet<>();
    private int selectedLessonId = -1;

    public interface OnLessonClickListener {
        void onLessonClicked(Lesson lesson);
        void onEditLesson(Lesson lesson);
        void onDeleteLesson(Lesson lesson);
    }

    public LessonAdapter(OnLessonClickListener listener) {
        this.listener = listener;
    }

    public void setLessons(List<LessonWithExercise> lessons) {
        this.lessons = lessons;
        notifyDataSetChanged();
    }

    public void setSelectedLessonId(int id) {
        this.selectedLessonId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LessonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVideoBinding binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new LessonViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LessonViewHolder holder, int position) {
        LessonWithExercise item = lessons.get(position);
        Lesson lesson = item.lesson;
        boolean isExpanded = expandedLessonIds.contains(lesson.id);
        boolean isSelected = lesson.id == selectedLessonId;
        
        holder.bind(item, isExpanded, isSelected, listener, () -> {
            if (expandedLessonIds.contains(lesson.id)) {
                expandedLessonIds.remove(lesson.id);
            } else {
                expandedLessonIds.add(lesson.id);
            }
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return lessons.size();
    }

    static class LessonViewHolder extends RecyclerView.ViewHolder {
        private final ItemVideoBinding binding;

        public LessonViewHolder(ItemVideoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(LessonWithExercise item, boolean isExpanded, boolean isSelected, OnLessonClickListener listener, Runnable toggleExpand) {
            Lesson lesson = item.lesson;
            
            String bpmInfo = "";
            if (item.exercise != null && item.exercise.current_bpm > 0) {
                bpmInfo = String.format(Locale.getDefault(), " | %d BPM", item.exercise.current_bpm);
            }

            String info = String.format(Locale.getDefault(),
                    "Practice at %s (%d mins)%s\nNotes: %s",
                    lesson.startTime, (lesson.lessonLength / 60), bpmInfo,
                    (lesson.notes != null ? lesson.notes : "No notes"));
            binding.videoName.setText(info);

            // Show star indicator if there's an exercise linked to this lesson
            binding.exerciseIndicator.setVisibility(item.exercise != null ? View.VISIBLE : View.GONE);

            // Highlight if selected
            binding.getRoot().setCardElevation(isSelected ? 8f : 2f);
            binding.getRoot().setStrokeWidth(isSelected ? 4 : 0);
            binding.getRoot().setStrokeColor(0xFF00E676); // Light green
            
            // Show delete button only when selected
            binding.btnDeleteLesson.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            binding.btnDeleteLesson.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteLesson(lesson);
            });

            String[] videoPaths = lesson.videoPath != null && !lesson.videoPath.isEmpty() ? lesson.videoPath.split("\\|") : new String[0];
            String[] audioPaths = lesson.audioPath != null && !lesson.audioPath.isEmpty() ? lesson.audioPath.split("\\|") : new String[0];
            
            boolean hasMedia = videoPaths.length > 0 || audioPaths.length > 0;

            if (hasMedia) {
                binding.videoThumbnail.setAlpha(1.0f);
                if (videoPaths.length > 0) {
                    binding.videoThumbnail.setImageResource(android.R.drawable.ic_menu_slideshow);
                } else {
                    binding.videoThumbnail.setImageResource(android.R.drawable.ic_btn_speak_now);
                }
                binding.expandArrow.setVisibility(View.VISIBLE);
                binding.expandArrow.setRotation(isExpanded ? 0 : -90);
                binding.mediaDropdown.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                
                if (isExpanded) {
                    populateMediaList(binding.videoListContainer, videoPaths, true, lesson.notes);
                    populateMediaList(binding.audioListContainer, audioPaths, false, lesson.notes);
                }
            } else {
                binding.videoThumbnail.setAlpha(0.3f);
                binding.videoThumbnail.setImageResource(android.R.drawable.ic_menu_slideshow);
                binding.expandArrow.setVisibility(View.GONE);
                binding.mediaDropdown.setVisibility(View.GONE);
            }

            binding.mainItemContainer.setOnClickListener(v -> {
                listener.onLessonClicked(lesson);
                if (hasMedia) {
                    toggleExpand.run();
                }
            });
            
            binding.mainItemContainer.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onEditLesson(lesson);
                    return true;
                }
                return false;
            });
        }

        private void populateMediaList(ViewGroup container, String[] paths, boolean isVideo, String lessonNotes) {
            container.removeAllViews();
            if (paths.length == 0) {
                container.setVisibility(View.GONE);
                return;
            }
            container.setVisibility(View.VISIBLE);
            
            LayoutInflater inflater = LayoutInflater.from(container.getContext());
            for (int i = 0; i < paths.length; i++) {
                String path = paths[i];
                ItemMediaRowBinding rowBinding = ItemMediaRowBinding.inflate(inflater, container, true);
                
                rowBinding.mediaIcon.setImageResource(isVideo ? android.R.drawable.ic_menu_slideshow : android.R.drawable.ic_btn_speak_now);
                rowBinding.mediaName.setText((isVideo ? "Video " : "Audio ") + (i + 1));
                
                rowBinding.playMediaButton.setOnClickListener(v -> playMedia(v, path, isVideo ? "video/*" : "audio/*", lessonNotes));
            }
        }

        private void playMedia(View v, String path, String type, String title) {
            try {
                Context context = v.getContext();

                if (type.startsWith("audio") && context instanceof MainActivity) {
                    ((MainActivity) context).playAudio(path, title);
                    return;
                }

                Uri contentUri;
                if (path.startsWith("content://") || path.startsWith("http")) {
                    contentUri = Uri.parse(path);
                } else {
                    File file = new File(path);
                    if (!file.exists()) {
                        Toast.makeText(context, "File not found at: " + path, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    contentUri = FileProvider.getUriForFile(context, 
                        context.getPackageName() + ".fileprovider", file);
                }

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(contentUri, type);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(intent);
                } else {
                    Intent chooser = Intent.createChooser(intent, "Open with");
                    context.startActivity(chooser);
                }
            } catch (Exception e) {
                Toast.makeText(v.getContext(), "Cannot play this file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }
}
