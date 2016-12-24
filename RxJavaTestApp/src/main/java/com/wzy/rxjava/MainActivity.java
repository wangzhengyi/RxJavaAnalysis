package com.wzy.rxjava;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rxJavaHelloWorld();
        rxJavaSimpleHelloWorld();
        rxJavaMapExample();
    }

    private void rxJavaMapExample() {
        Observable.just("Hello World!")
                .map(new Func1<String, Integer>() {
                    @Override
                    public Integer call(String s) {
                        return s.hashCode();
                    }
                })
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        Log.d("RxJava", Integer.toString(integer));
                    }
                });
    }

    private void rxJavaSimpleHelloWorld() {
        Observable.just("Hello, World! -Dan")
                .map(new Func1<String, String>() {
                    @Override
                    public String call(String s) {
                        return s + "-Dan";
                    }
                })
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        Log.d("RxJava", s);
                    }
                });
    }

    private void rxJavaHelloWorld() {
        Observable<String> myObservable = Observable.create(
                new Observable.OnSubscribe<String>() {

                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        subscriber.onNext("Hello, World");
                        subscriber.onCompleted();
                    }
                }
        );
        Subscriber<String> mySubscriber = new Subscriber<String>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String s) {
                Log.d("RxJava", s);
            }
        };
        myObservable.subscribe(mySubscriber); // Log outputs "Hello, World"
    }
}
