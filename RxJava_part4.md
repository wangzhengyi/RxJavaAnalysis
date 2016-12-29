# RxJava入门系列四,Android中的响应式编程

在入门系列1,2,3中,我基本介绍了RxJava是如何使用的.但是作为一名Android开发人员,你怎么让RxJava能为你所用呢?这篇博客我将针对Android开发来介绍一下RxJava的使用场景.

------
# RxAndroid

RxAndroid是为Android打造的RxJava扩展.通过RxAndroid可以让你的Android开发变得更轻松.

首先,RxAndroid中提供了AndroidSchedulers,你可以用它来切换Android线程.你想要将代码运行在UI线程?没问题,使用AndroidSchedulers.mainThread()即可:

```java
retrofitService.getImage(url)
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(bitmap -> myImageView.setImageBitmap(bitmap));
```

如果你已经创建了一个Handler,你还可以使用HandlerThreadScheduler创建一个调度器关联到你的Handler上.

接下来,RxAndroid中提供了AndroidObservable,它提供很多可以配合Android应用生命周期一起使用的功能.例如bindActivity()和bindFragment()两个方法,默认使用AndroidSchedulers.mainThread()线程来执行观察者代码,同时当Activity或者Fragment退出时,Observable自动停止发送新事件.

```java
AndroidObservable.bindActivity(this, retrofitService.getImage(url))
    .subscribeOn(Schedulers.io())
    .subscribe(bitmap -> myImageView.setImageBitmap(bitmap));
```

我同样也很喜欢其中的AndroidObservable.fromBroadcast()方法,它可以帮助你创建一个和BroadcastReceiver配合使用的Observable对象.这里展示如下如何使用Observable来监听网络状态变化：

```java
IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
AndroidObservable.fromBroadcast(context, filter)
    .subscribe(intent -> handleConnectivityChange(intent));
```

最后,再介绍一下ViewObservable对象,它可以用来绑定View.例如,可以使用ViewObservable.clicks()方法来监听View的点击事件,使用ViewObservable.text()方法来监听TextView的内容变化事件.

```java
ViewObservable.clicks(mCardNameEditText, false)
    .subscribe(view -> handleClick(view));
```

--------
# Retrofit

有一个非常出名的支持RxJava的项目：Retrofit,它是一个为Android设计的简化HTTP请求的库.通常情况下,你可以添加一个Callback对象来定义一个异步方法调用：

```java
@GET("/user/{id}/photo")
void getUserPhoto(@Path("id") int id, Callback<Photo> cb);
```

但是使用RxJava,你可以通过返回Observable对象来代替传入Callback对象:

```java
@GET("/user/{id}/photo")
Observable<Photo> getUserPhoto(@Path("id") int id);
```

拿到了Observable对象后,你就可以任意操作了,不光是可以获取之前的数据流,你甚至可以再次改变数据流.

Retrofit对Observable的支持使得Retrofit可以很简单的将多个REST请求合并起来.例如,我们有一个请求获取照片,还有一个请求是获取元数据.我们通过Observable将两个请求合并起来：

```java
Observable.zip(
    service.getUserPhoto(id),
    service.getPhotoMetadata(id),
    (photo, metadata) -> createPhotoWithData(photo, metadata))
    .subscribe(photoWithData -> showPhoto(photoWithData));
```

在RxJava入门系统二中,我使用flatMap做过类似的例子.这里我是想展示通过Retrofit可以多么轻松的将多个Retrofit请求合并起来.

------
# 历史代码

虽然Retrofit支持RxJava,但是你项目中使用的其他库不支持RxJava怎么办?你想把一些历史遗留代码转成RxJava的形式该怎么做?能不能通过不修改代码的方式就将历史代码转成RxJava的形式?

Observable.just()和Observable.from()一般来说可以帮助你解决这些问题：

```java
private Object oldMethod() { ... }

public Observable<Object> newMethod() {
    return Observable.just(oldMethod());
}
```

如果oldMethod()方法的执行速度很快,这样做通常不会出什么问题.但是如果oldMethod()执行很慢呢?当你将oldMethod()传入Observable.just()方法时,由于oldMethod()执行太慢可能会导致主线程的阻塞.

为了避免这种问题,我经常使用defer()来包装运行缓慢的代码:

```java
private Object slowBlockingMethod() { ... }

public Observable<Object> newMethod() {
    return Observable.defer(() -> Observable.just(slowBlockingMethod()));
}
```

现在,newMethod()的调用就不会阻塞了,除非你订阅了返回的Observable对象.

-------
# 生命周期

我把最难的部分留到了最后.使用RxJava如何处理Activity的生命周期?这里主要会遇到两个难题：

1. 如何在环境配置发生变化时(例如屏幕旋转)依然保持订阅关系?
    假设你想通过Retrofit发送网络请求,并将结果显示到ListView时.如果过程中用户旋转屏幕了,你是否还想继续保持这种操作,如何保持?

2. 如果解决Observable持有Context对象可能导致的内存泄露?
    这个问题是由于创建Subscription时保持了Context对象,这个当遇到操控View时很容易被触发.如果Observable没有及时结束,你可能会因此浪费越来越多的内存.
    
不幸的是,目前没有明确的方法来解决上述两个问题,但是你可以依据下面的指导来尽量规避Android中使用RxJava会遇到的坑.

第一个问题可以使用RxJava内置的缓存机制来解决,你可以对同一个Observable对象执行unsubscribe和resubscribe.Cache机制会继续执行之前的请求,即使你已经取消订阅了.这意味着你可以在Activity重建时再次建立订阅关系:

```java
Observable<Photo> request = service.getUserPhoto(id).cache();
Subscription sub = request.subscribe(photo -> handleUserPhoto(photo));

// ...When the Activity is being recreated...
sub.unsubscribe();

// ...Once the Activity is recreated...
request.subscribe(photo -> handleUserPhoto(photo));
```

第二个问题可以通过在Activity的具体生命周期函数中解除订阅关系来解决.一种很常见的模式就是使用CompositeSubscription来持有所有的Subscription,然后在onDestroy()或者onDestroyView()方法中取消订阅.

```java
private CompositeSubscription mCompositeSubscription
    = new CompositeSubscription();

private void doSomething() {
    mCompositeSubscription.add(
        AndroidObservable.bindActivity(this, Observable.just("Hello, World!"))
        .subscribe(s -> System.out.println(s)));
}

@Override
protected void onDestroy() {
    super.onDestroy();
    mCompositeSubscription.unsubscribe();
}
```

你还是可以实现的更通用一点,你可以在Activity/Fragment基类里创建一个CompositeSubscription对象,在基类的onDestory等相关生命周期函数中使用unsubscribe方法.

注意!一旦你调用了CompositeSubscription.unsubscribe()方法,这个CompositeSubscription对象就不可用了,如果你还想使用CompositeSubscription,就必须重新创建一个新的对象了.

解决上面的两个问题都需要新增部分代码.我希望以后能有开发者告诉我一个更好的不需要新增这么多模板代码的解决方案.

-------
# 总结

RxAndroid还没有完全的适配好Android平台,毕竟RxJava都是一个很新的项目,RxAndroid就更是需要一段时间的完善了.RxAndroid还处于开发阶段,目前也没有太好的示例可供展示.我打赌一年之后,这篇博客中提到的几点建议可能就不再适用了.

同时,我发现RxJava不仅会简化你的代码逻辑,还会让你的代码变得有趣,变得优雅.

------
# 原文链接

[Grokking RxJava, Part 4: Reactive Android](http://blog.danlew.net/2014/09/15/grokking-rxjava-part-4/)



    