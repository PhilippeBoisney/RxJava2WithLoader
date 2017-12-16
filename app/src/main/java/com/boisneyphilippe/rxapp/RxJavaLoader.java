package com.boisneyphilippe.rxapp;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.subjects.BehaviorSubject;

/**
 * Created by Philippe on 16/12/2017.
 */

public class RxJavaLoader<T> extends Loader<T> {

    private final Observable<T> observable;
    private final BehaviorSubject<T> cache = BehaviorSubject.create();

    private Disposable disposable;

    private RxJavaLoader(Context context, Observable<T> observable) {
        super(context);
        this.observable = observable;
    }

    public static <T> ObservableTransformer<T, T> compose(final Fragment fragment, final int id) {
        return new ObservableTransformer<T, T>() {
            @Override
            public ObservableSource<T> apply(Observable<T> upstream) {
                return create(fragment, id, upstream);
            }
        };
    }


    public static <T> Observable<T> create(Fragment fragment, int id, Observable<T> observable) {
        LoaderManager loaderManager = fragment.getLoaderManager();

        CreateLoaderCallback<T> createLoaderCallback = new CreateLoaderCallback<>(fragment.getContext(), observable);

        loaderManager.restartLoader(id, null, createLoaderCallback);

        RxJavaLoader<T> rxLoader = (RxJavaLoader<T>) loaderManager.getLoader(id);
        return rxLoader.cache.hide();
    }

    @Nullable
    public static <T> Observable<T> initializeLoader(Fragment fragment, int id, Observable<T> observable){
        LoaderManager loaderManager = fragment.getLoaderManager();
        CreateLoaderCallback<T> createLoaderCallback = new CreateLoaderCallback<>(fragment.getContext(), observable);

        if (loaderManager.getLoader(id) != null) {
            loaderManager.initLoader(id, null, createLoaderCallback);
        }

        RxJavaLoader<T> rxLoader = (RxJavaLoader<T>) loaderManager.getLoader(id);
        if (rxLoader != null && !rxLoader.cache.hasComplete()){
            return rxLoader.cache.hide();
        } else {
            return null;
        }
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        disposable = observable.subscribeWith(cache).subscribeWith(new DisposableObserver<T>() {
            @Override
            public void onNext(T t) { }

            @Override
            public void onError(Throwable e) { }

            @Override
            public void onComplete() { }
        });
    }

    @Override
    protected void onReset() {
        super.onReset();
        disposable.dispose();
    }

    private static class CreateLoaderCallback<T> implements LoaderManager.LoaderCallbacks<T> {

        private final Context context;
        private final Observable<T> observable;

        public CreateLoaderCallback(Context context, Observable<T> observable) {
            this.context = context;
            this.observable = observable;
        }

        @Override
        public Loader<T> onCreateLoader(int id, Bundle args) {
            return new RxJavaLoader<>(context, observable);
        }

        @Override
        public void onLoadFinished(Loader<T> loader, T data) { /* nothing */ }

        @Override
        public void onLoaderReset(Loader<T> loader) { /* nothing */ }
    }
}

