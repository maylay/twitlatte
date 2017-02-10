package com.github.moko256.twicalico;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import twitter4j.PagableResponseList;
import twitter4j.TwitterException;
import twitter4j.User;

/**
 * Created by moko256 on 2016/03/29.
 *
 * @author moko256
 */
public abstract class BaseUsersFragment extends BaseListFragment {

    UsersAdapter adapter;
    ArrayList<User> list;
    long next_cursor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        list=new ArrayList<>();
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view=super.onCreateView(inflater, container, savedInstanceState);

        adapter=new UsersAdapter(getContext(), list);
        setAdapter(adapter);
        if(!isInitializedList()){
            adapter.notifyDataSetChanged();
        }

        return view;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            ArrayList<User> l=(ArrayList<User>) savedInstanceState.getSerializable("list");
            if(l!=null){
                list.addAll(l);
            }
            next_cursor=savedInstanceState.getLong("next_cursor",-1);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putSerializable("list", (ArrayList) list);
        outState.putLong("next_cursor",next_cursor);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        adapter=null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        list=null;
    }

    @Override
    protected void onInitializeList() {
        getResponseObservable(-1)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result-> {
                            list.addAll(result);
                            adapter.notifyDataSetChanged();
                        },
                        e -> {
                            e.printStackTrace();
                            Snackbar.make(getView(), "Error", Snackbar.LENGTH_INDEFINITE)
                                    .setAction("Try", v -> onInitializeList())
                                    .show();
                        },
                        ()->getSwipeRefreshLayout().setRefreshing(false)
                );
    }

    @Override
    protected void onUpdateList() {
        getSwipeRefreshLayout().setRefreshing(false);
    }

    @Override
    protected void onLoadMoreList() {
        getResponseObservable(next_cursor)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            int size = result.size();
                            if (size > 0) {
                                int l = list.size();
                                list.addAll(result);
                                adapter.notifyItemRangeInserted(l, size);
                            }
                        },
                        e -> {
                            e.printStackTrace();
                            Snackbar.make(getView(), "Error", Snackbar.LENGTH_INDEFINITE)
                                    .setAction("Try", v -> onLoadMoreList())
                                    .show();
                        },
                        () -> {}
                );
    }

    @Override
    protected boolean isInitializedList() {
        return !list.isEmpty();
    }

    public Observable<PagableResponseList<User>> getResponseObservable(long cursor) {
        return Observable.create(
                subscriber->{
                    try {
                        PagableResponseList<User> pagableResponseList=getResponseList(cursor);
                        next_cursor=pagableResponseList.getNextCursor();
                        subscriber.onNext(pagableResponseList);
                        subscriber.onCompleted();
                    } catch (TwitterException e) {
                        subscriber.onError(e);
                    }
                }
        );
    }

    public abstract PagableResponseList<User> getResponseList(long cursor) throws TwitterException;

}