/*
 * Copyright 2015-2019 The twitlatte authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.moko256.twitlatte;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.bumptech.glide.RequestManager;
import com.github.moko256.latte.client.base.entity.Post;
import com.github.moko256.latte.client.base.entity.Repeat;
import com.github.moko256.latte.client.base.entity.Status;
import com.github.moko256.latte.client.base.entity.User;
import com.github.moko256.twitlatte.entity.Client;
import com.github.moko256.twitlatte.intent.AppCustomTabsKt;
import com.github.moko256.twitlatte.model.base.StatusActionModel;
import com.github.moko256.twitlatte.repository.PreferenceRepository;
import com.github.moko256.twitlatte.text.TwitterStringUtils;
import com.github.moko256.twitlatte.widget.ImagesTableView;

import java.util.List;

import kotlin.Unit;

import static com.github.moko256.twitlatte.GlobalApplicationKt.preferenceRepository;
import static com.github.moko256.twitlatte.repository.PreferenceRepositoryKt.KEY_HIDE_SENSITIVE_MEDIA;
import static com.github.moko256.twitlatte.repository.PreferenceRepositoryKt.KEY_IS_PATTERN_TWEET_MUTE;
import static com.github.moko256.twitlatte.repository.PreferenceRepositoryKt.KEY_IS_PATTERN_TWEET_MUTE_SHOW_ONLY_IMAGE;
import static com.github.moko256.twitlatte.repository.PreferenceRepositoryKt.KEY_IS_PATTERN_TWEET_SOURCE_MUTE;
import static com.github.moko256.twitlatte.repository.PreferenceRepositoryKt.KEY_IS_PATTERN_USER_NAME_MUTE;
import static com.github.moko256.twitlatte.repository.PreferenceRepositoryKt.KEY_IS_PATTERN_USER_SCREEN_NAME_MUTE;
import static com.github.moko256.twitlatte.repository.PreferenceRepositoryKt.KEY_TIMELINE_IMAGE_LOAD_MODE;
import static com.github.moko256.twitlatte.repository.PreferenceRepositoryKt.KEY_TWEET_MUTE_PATTERN;
import static com.github.moko256.twitlatte.repository.PreferenceRepositoryKt.KEY_TWEET_MUTE_SHOW_ONLY_IMAGE_PATTERN;
import static com.github.moko256.twitlatte.repository.PreferenceRepositoryKt.KEY_TWEET_SOURCE_MUTE_PATTERN;
import static com.github.moko256.twitlatte.repository.PreferenceRepositoryKt.KEY_USER_NAME_MUTE_PATTERN;
import static com.github.moko256.twitlatte.repository.PreferenceRepositoryKt.KEY_USER_SCREEN_NAME_MUTE_PATTERN;

/**
 * Created by moko256 on 2016/02/11.
 *
 * @author moko256
 */
public class StatusesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Long> data;
    private final Context context;
    private final StatusActionModel statusActionModel;
    private final Client client;
    private final PreferenceRepository conf;
    private final RequestManager glideRequests;

    public OnLoadMoreClickListener onLoadMoreClick;

    public boolean shouldShowMediaOnly = false;

    StatusesAdapter(Client client, StatusActionModel statusActionModel, PreferenceRepository preferenceRepository, Context context, List<Long> data, RequestManager glideRequests) {
        this.client = client;
        this.statusActionModel = statusActionModel;
        this.conf = preferenceRepository;
        this.context = context;
        this.data = data;
        this.glideRequests = glideRequests;
        setHasStableIds(true);
    }


    public void setOnLoadMoreClick(OnLoadMoreClickListener onLoadMoreClick) {
        this.onLoadMoreClick = onLoadMoreClick;
    }

    @Override
    public long getItemId(int position) {
        return data.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        if (data.get(position) == -1L) {
            return R.layout.layout_list_load_more_text;
        }
        Post post = client.getPostCache().getPost(data.get(position));

        if (!(post != null && post.getStatus() != null)) {
            return R.layout.layout_list_load_more_text;
        }

        Status item = post.getStatus();
        User user = post.getUser();

        if ((conf.getBoolean(KEY_IS_PATTERN_TWEET_MUTE, false) && conf.getPattern(KEY_TWEET_MUTE_PATTERN).matcher(item.getText()).find()) ||
                (conf.getBoolean(KEY_IS_PATTERN_USER_SCREEN_NAME_MUTE, false) && conf.getPattern(KEY_USER_SCREEN_NAME_MUTE_PATTERN).matcher(user.getScreenName()).find()) ||
                (conf.getBoolean(KEY_IS_PATTERN_USER_NAME_MUTE, false) && conf.getPattern(KEY_USER_NAME_MUTE_PATTERN).matcher(user.getName()).find()) ||
                (conf.getBoolean(KEY_IS_PATTERN_TWEET_SOURCE_MUTE, false) && conf.getPattern(KEY_TWEET_SOURCE_MUTE_PATTERN).matcher((item.getSourceName() != null) ? item.getSourceName() : "").find())
        ) {
            return R.layout.layout_list_muted_text;
        } else if (shouldShowMediaOnly || (conf.getBoolean(KEY_IS_PATTERN_TWEET_MUTE_SHOW_ONLY_IMAGE, false)
                && item.getMedias() != null
                && conf.getPattern(KEY_TWEET_MUTE_SHOW_ONLY_IMAGE_PATTERN).matcher(item.getText()).find())) {
            return R.layout.layout_list_tweet_only_image;
        } else {
            return R.layout.layout_post_card;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View child = LayoutInflater
                .from(context)
                .inflate(
                        i,
                        viewGroup,
                        false
                );
        switch (i) {
            case R.layout.layout_list_load_more_text:
                return new MoreLoadViewHolder(child);
            case R.layout.layout_list_muted_text:
                return new MutedTweetViewHolder(child);
            case R.layout.layout_list_tweet_only_image:
                return new ImagesOnlyTweetViewHolder(child, context);
            case R.layout.layout_post_card:
                return new StatusViewHolder(child, context);
            default:
                throw new RuntimeException("Invalid id");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, final int i) {
        if (viewHolder instanceof MoreLoadViewHolder) {
            ViewGroup.LayoutParams layoutParams = viewHolder.itemView.getLayoutParams();
            if (layoutParams instanceof StaggeredGridLayoutManager.LayoutParams) {
                ((StaggeredGridLayoutManager.LayoutParams) layoutParams).setFullSpan(true);
            }

            ((MoreLoadViewHolder) viewHolder).setIsLoading(false);
            viewHolder.itemView.setOnClickListener(v -> {
                ((MoreLoadViewHolder) viewHolder).setIsLoading(true);
                onLoadMoreClick.onClick(i);
            });
        } else {
            Post post = client.getPostCache().getPost(data.get(i));
            if (post != null) {
                if (viewHolder instanceof StatusViewHolder) {
                    ((StatusViewHolder) viewHolder).setStatus(
                            client,
                            statusActionModel,
                            glideRequests,
                            post.getRepeatedUser(),
                            post.getRepeat(),
                            post.getUser(),
                            post.getStatus(),
                            post.getQuotedRepeatingUser(),
                            post.getQuotedRepeatingStatus()
                    );
                } else if (viewHolder instanceof ImagesOnlyTweetViewHolder) {
                    ((ImagesOnlyTweetViewHolder) viewHolder).setStatus(
                            client,
                            post.getStatus(),
                            glideRequests
                    );
                }
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof StatusViewHolder) {
            ((StatusViewHolder) holder).clear();
        } else if (holder instanceof ImagesOnlyTweetViewHolder) {
            ((ImagesOnlyTweetViewHolder) holder).close();
        }
    }

    @Override
    public int getItemCount() {
        if (data != null) {
            return data.size();
        } else {
            return 0;
        }
    }

    private static class StatusViewHolder extends RecyclerView.ViewHolder {
        final StatusViewBinder statusViewBinder;
        final Context context;

        StatusViewHolder(View itemView, Context context) {
            super(itemView);
            statusViewBinder = new StatusViewBinder(itemView);
            this.context = context;
        }

        void setStatus(
                Client client,
                StatusActionModel statusActionModel,
                RequestManager glideRequests,
                User repeatedUser,
                Repeat repeat,
                User user,
                Status status,
                User quotedStatusUser,
                Status quotedStatus
        ) {
            View.OnClickListener onContentClick = v -> {
                ActivityOptionsCompat animation = ActivityOptionsCompat
                        .makeSceneTransitionAnimation(
                                ((Activity) context),
                                statusViewBinder.getUserImage(),
                                "icon_image"
                        );
                context.startActivity(
                        GlobalApplicationKt.setAccountKeyForActivity(ShowTweetActivity.getIntent(context, status.getId()), ((Activity) context)),
                        animation.toBundle()
                );
            };

            itemView.setOnClickListener(onContentClick);
            statusViewBinder.getTweetContext().setOnClickListener(onContentClick);

            statusViewBinder.getUserImage().setOnClickListener(v -> {
                ActivityOptionsCompat animation = ActivityOptionsCompat
                        .makeSceneTransitionAnimation(
                                (Activity) context,
                                v,
                                "icon_image"
                        );
                context.startActivity(
                        GlobalApplicationKt.setAccountKeyForActivity(
                                ShowUserActivity.getIntent(context, user.getId()),
                                ((Activity) context)
                        ),
                        animation.toBundle()
                );
            });

            statusViewBinder.setOnQuotedStatusClicked(v -> context.startActivity(
                    GlobalApplicationKt.setAccountKeyForActivity(
                            ShowTweetActivity.getIntent(context, quotedStatus.getId()),
                            ((Activity) context)
                    )
            ));

            statusViewBinder.setOnCardClicked(
                    v -> AppCustomTabsKt.launchChromeCustomTabs(
                            context,
                            status.getCard().getUrl(),
                            false
                    )
            );

            statusViewBinder.getLikeButton().setOnCheckedChangeListener((compoundButton, b) -> {
                if (b) {
                    statusActionModel.createFavorite(status.getId());
                } else {
                    statusActionModel.removeFavorite(status.getId());
                }
                return Unit.INSTANCE;
            });

            statusViewBinder.getRepeatButton().setOnCheckedChangeListener((compoundButton, b) -> {
                if (b) {
                    statusActionModel.createRepeat(status.getId());
                } else {
                    statusActionModel.removeRepeat(status.getId());
                }
                return Unit.INSTANCE;
            });

            statusViewBinder.getReplyButton().setOnClickListener(
                    v -> context.startActivity(PostActivity.getIntent(
                            context,
                            status.getId(),
                            TwitterStringUtils.convertToReplyTopString(
                                    client.getUserCache().get(client.getAccessToken().getUserId()).getScreenName(),
                                    user.getScreenName(),
                                    status.getMentions()
                            ).toString()
                    ))
            );

            statusViewBinder.getSendVote().setOnClickListener(v ->
                    statusActionModel
                            .sendVote(
                                    status.getId(),
                                    status.getPoll().getId(),
                                    statusViewBinder.getPollAdapter().getSelections()
                            )
            );

            statusViewBinder.setStatus(
                    client,
                    glideRequests,
                    repeatedUser,
                    repeat,
                    user,
                    status,
                    quotedStatusUser,
                    quotedStatus
            );
        }

        void clear() {
            statusViewBinder.clear();
        }
    }

    private static class MoreLoadViewHolder extends RecyclerView.ViewHolder {
        final TextView text;
        final ProgressBar progressBar;

        MoreLoadViewHolder(View view) {
            super(view);
            text = itemView.findViewById(R.id.layout_list_load_more_text_view);
            progressBar = itemView.findViewById(R.id.layout_list_load_more_text_progress);
        }

        void setIsLoading(boolean isLoading) {
            itemView.setClickable(!isLoading);
            text.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private static class MutedTweetViewHolder extends RecyclerView.ViewHolder {
        MutedTweetViewHolder(View view) {
            super(view);
        }
    }

    private static class ImagesOnlyTweetViewHolder extends RecyclerView.ViewHolder {
        final Context context;
        final ImagesTableView imagesTableView;
        final String imageLoadMode = preferenceRepository.getString(KEY_TIMELINE_IMAGE_LOAD_MODE, "normal");
        final boolean isHideSensitiveMedia = preferenceRepository.getBoolean(KEY_HIDE_SENSITIVE_MEDIA, true);

        ImagesOnlyTweetViewHolder(View view, Context context) {
            super(view);
            imagesTableView = itemView.findViewById(R.id.list_tweet_image_container);
            this.context = context;
        }

        void setStatus(Client client, Status status, RequestManager glideRequests) {
            imagesTableView.setMedias(
                    glideRequests,
                    status.getMedias(),
                    client.getAccessToken().getClientType(),
                    status.isSensitive(),
                    imageLoadMode,
                    isHideSensitiveMedia
            );
            imagesTableView.setOnLongClickListener(v -> {
                context.startActivity(
                        GlobalApplicationKt.setAccountKeyForActivity(
                                ShowTweetActivity.getIntent(context, status.getId()),
                                ((Activity) context)
                        )
                );
                return true;
            });
        }

        void close() {
            imagesTableView.clearImages();
        }
    }

    interface OnLoadMoreClickListener {
        void onClick(int position);
    }

}
