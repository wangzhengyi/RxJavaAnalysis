# RxJava入门系列三,响应式编程

在RxJava入门系列一,我向你介绍了RxJava的基础架构.RxJava入门系列二,我向你展示了RxJava提供的多种牛逼操作符.但是你可能仍然没能劝服自己使用RxJava,这一篇博客里我将向你展示RxJava提供的其他优势,没准了解了这些优势,你就真的想去使用RxJava了.

------
# 异常处理

直到目前为止,我都没有去介绍onComplete()和onError()方法.这两个方法是用来停止Observable继续发出事件并告知观察者为什么停止(是正常的停止还是因为发生了错误而停止).

Subscriber对象具备监听onComplete()和onError()方法被调用的能力,让我们通过部分代码来使用一下这两个函数:

```java
Observable.just("Hello, world!")
    .map(s -> potentialException(s))
    .map(s -> anotherPotentialException(s))
    .subscribe(new Subscriber<String>() {
        @Override
        public void onNext(String s) { System.out.println(s); }

        @Override
        public void onCompleted() { System.out.println("Completed!"); }

        @Override
        public void onError(Throwable e) { System.out.println("Ouch!"); }
    })
```

代码中potentialException()和anotherPotentialException()方法都有可能在运行时抛出错误异常,而每个Observable都会在停止时回调onCompleted()或者onError()方法.因此,这里程序要么输出"Completed"(正常停止),要么输出"Ouch"(因为有异常抛出).

这种异常处理模式有几处优势:

1. 无论何时发生异常,onError()方法都会被回调.
   这使得异常处理变得非常简单,我只需要在onError()方法中做异常处理即可.
2. 操作符不需要去处理异常.
   将异常交给Subscriber去处理,这样Observable的链式调用一旦出了异常,就会直接执行Subscriber的onError()方法.
3. 你可以知道Subscriber什么时候已经接收完全部的信息.
   知道什么时候事件发出结束可对你代码的流程调用有很大的帮助.
   
我发现RxJava这种异常处理模式比传统的异常处理模式简单.在传统的异常处理模式中,你通过需要设置回调函数来处理错误异常.这样做不仅会导致大量重复代码的存在,而且还意味着每个回调函数都需要清楚自己需要处理哪种错误异常,意味着异常回调处理函数和调用者紧耦合.

使用RxJava,你的Observable对象根本不需要知道如何处理错误和异常,你的链式调用中的操作符也不需要知道如何处理异常,一旦发生运行时错误,就会跳过操作符,将错误交给Subscriber去处理.

-------
# 调度器

你编写的Android应用肯定会有发送网络请求的代码.发送网络请求是一个非常耗时操作,所以你希望在子线程中建立连接拉取数据.这个时候,问题就来了.

编写Android多线程程序是很难的,因为你必须明确哪部分代码应该运行在哪个线程上,否则你的应用很有可能会崩溃.一个典型的崩溃案例就是:当你试图从非主线程去修改View时Android系统就会报错.

在RxJava中,你可以使用subscribeOn()方法告知Observable对象代码运行在哪个线程上,同时可以使用observeOn()方法告知Subscriber对象代码运行在哪个线程上:

```java
myObservableServices.retrieveImage(url)
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(bitmap -> myImageView.setImageBitmap(bitmap));
```

是不是很简单?在subscriber之前的代码都是运行在io线程中,subscriber代码运行在主线程中,所以最后myImageView能够在主线程中更新自己的UI.

最屌的就是我可以把subscribeOn()和observeOn()添加到任意Observable对象上!它俩也是操作符!我不需要关心Observable和它之前的操作符,仅仅通过这两个操作符就能让我轻松的实现线程调度.

使用AsyncTask或者类似的异步框架,我必须设计我的代码,找出哪部分代码是需要并发执行的.但是使用RxJava,我的代码可以保持不变,我只需要在线程调度的地方添加那两个操作符即可.

------
# 订阅

在之前的博客里我向你隐瞒了一件事情.当你调用Observable.subscribe()方法时,返回的其实是Subscription对象.它代表了Observable和Subscriber之间的订阅关系.

```java
Subscription subscription = Observable.just("Hello, World!")
    .subscribe(s -> System.out.println(s));
```

你可以使用Subscription对象来取消Subscriber对Observable的订阅关系:

```java
subscription.unsubscribe();
System.out.println("Unsubscribed=" + subscription.isUnsubscribed());
// Outputs "Unsubscribed=true"
```

这是RxJava又一个非常赞的功能,当你使用Subscription取消订阅时,会停止整个Observable的链式调用.如果你有一系列非常复杂的链式操作符调用,使用unsubscribe方法会立刻停止正在执行的链式调用代码,你不需要在做其他额外的工作.

------
# 总结

记住这个仅仅是RxJava的入门系列.RxJava有太多你需要去学习的功能和实现细节(例如背压).我也不是使用RxJava来实现全部的代码,我仅仅是当代码复杂到一定程度需要将其简化时才会使用RxJava.

原本我计划这篇文章作为RxJava入门系列的终结篇,但是我收到很多留言希望我能介绍一下RxJava如何在Android中具体使用.因此,我继续写了RxJava入门篇四.我希望通过这一系列教程的讲解能够打动你开始使用RxJava这个有趣的框架.

------
# 原文链接

[Grokking RxJava, Part 3: Reactive with Benefits](http://blog.danlew.net/2014/09/15/grokking-rxjava-part-3/)
