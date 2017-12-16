package com.boisneyphilippe.rxapp;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;


/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends Fragment {

    // FOR DESIGN
    @BindView(R.id.fragment_main_textview) TextView textView;

    // FOR DATA
    private Disposable disposable;
    private static int LOADER_ID = 10; //or whatever you want...

    public MainFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, view);
        this.resumeObservableIfPossible();
        return view;
    }

    @Override
    public void onDestroy() {
        if (this.disposable != null) this.disposable.dispose();
        super.onDestroy();
    }

    // -----------------
    // ACTIONS
    // -----------------

    @OnClick(R.id.fragment_main_button)
    public void submit(View view) {
        this.executeObservable();
    }

    // ------------------
    //  RX JAVA
    // ------------------

    private Observable<String> getObservable(){
        return Observable.just("Long process is ended !")
                .delay(10, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private DisposableObserver<String> getSubscriber(){
        return new DisposableObserver<String>() {
            @Override
            public void onNext(String result) {
                Log.e("TAG","On Next");
                updateUIWhenStoppingOperation(result);
            }

            @Override
            public void onError(Throwable e) {
                Log.e("TAG","On Error");
            }

            @Override
            public void onComplete() {
                Log.e("TAG","On Complete !!");
            }
        };
    }

    // -----

    private void executeObservable(){
        this.updateUIWhenStartingOperation();
        ConnectableObservable connectableObservable = getObservable()
                .compose(RxJavaLoader.<String>compose(this, LOADER_ID))
                .publish();
        connectableObservable.subscribeWith(getSubscriber());
        this.disposable = connectableObservable.connect();
    }

    private void resumeObservableIfPossible(){
        Observable<String> observable = RxJavaLoader.<String>initializeLoader(this, LOADER_ID, getObservable());
        if (observable != null){
            this.updateUIWhenStartingOperation();
            observable.publish();
            this.disposable = observable.subscribeWith(getSubscriber());
        }
    }

    // ------------------
    //  UPDATE UI
    // ------------------

    private void updateUIWhenStartingOperation(){
        this.textView.setText("Starting long process...");
    }

    private void updateUIWhenStoppingOperation(String result){
        this.textView.setText(result);
    }
}
