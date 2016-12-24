# RxJava入门系列一,基础篇

RxJava在Android开发者中变得越来越流行(ps:2016年底才用的我表示自己太out了).使用RxJava唯一的问题就是当你第一次接触RxJava时,你会觉得RxJava特别难上手,特别是当你之前只使用同步式编程语言,没有接触过响应式编程理念.但是一旦你真正了解了RxJava,你就会知道它是多么好用.

# 基础
RxJava最基础的模块是Observable(被观察者)和Subscriber(观察者).Observable发出一系列事件,Subscriber来消费这些事件.

RxJava中事件发送是有固定模式的.一个Observable可能会发出零个或者多个事件,每个事件要不就是成功的被处理,要不就是遇到了错误被终止.对于每个发出事件,Observable都会调用监听该事件的Subscriber的onNext()方法,最后调用Subscriber.onComplete()方法或者Subscriber.onError()方法来结束事件的处理.

RxJava框架看起来很像观察者模式,但是和观察者模式有一点明显的不同,那就是Observable当没有任何Subscriber来监听时,Observable是不会发送任何事件的.

# Hello, World!

让我们通过一个具体的例子来看一下RxJava的框架实现.我们先来创建一个基本的Observable对象:

```java
Observable<String> myObservable = Observable.create(
        new Observable.OnSubscribe<String>() {

            @Override
            public void call(Subscriber<? super String> subscriber) {
                subscriber.onNext("Hello, World");
                subscriber.onCompleted();
            }
        }
);
```

这里我们定义了一个Observable对象,它发出“Hello,World”字符串就结束了.
接下来,我们来创建一个Subscriber对象来消费Observable发出的字符串.

```java
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
```

Subscriber对象的作用就是打印每一个Observable发出的字符串.
现在我们已经有了Observable对象和Subscriber对象,我们可以通过subscriber()将myObservable和mySubscriber关联起来:

```java
myObservable.subscribe(mySubscriber); // Log outputs "Hello, World"
```

一旦订阅模式被建立起来,myObservable就会调用mySubscriber的onNext()方法.因此,mySubscriber会打印出"Hello, World"字符串.

------
# 更简洁的代码

在上面的例子中,仅仅为了打印“Hello,World”使用了大量的RxJava模板代码,之所以这样做是为了让读者更好的了解RxJava背后的原理.那同时,RxJava提供了很多便捷的函数来帮助我们简化代码.
我们先来简化一下Observable的构建代码.RxJava中内置了很多简化Observable创建的函数.例如,Observable.just()是用来创建只发出一个事件就结束的Observable对象.上个例子中创建的Observable对象可以简化为一行代码:

```java
Observable<String> myObservable = Observable.just("Hello, world");
```

接下来,我们再来简化一下Subscriber的构建.在上面的例子中,我们不会关心onComplete()和onError(),我们只需要定义在onNext()里做什么处理.这时候就可以使用Action1类.

```java
Action1<String> onNextAction = new Action1<String>() {
    @Override
    public void call(String s) {
        Log.d("RxJava", s);
    }
};
```

Action类可以用来定义Subscriber的onNext(),onComplete()和onError().Observable.subscribe()方法通过重载可以接受一个参数的Action,两个参数的Action和三个参数的Action,分别相当于onNext(),onError()和onComplete()的占位符.用Actions来替换subscribe()的Subscriber对象的代码如下:

```java
myObservable.subscribe(onNextAction, onErrorAction, onCompleteAction);
```

由于这里我们不关心Subscriber的onError()和onComplete()实现,所以我们只需要传入一个Action参数:

```java
myObservable.subscribe(onNextAction);
```

现在,让我们去掉上面的变量声明,直接采用链式调用的写法来实现一个"Hello, World"的输出:

```java
Observable.just("Hello, World")
        .subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                Log.d("RxJava", s);
            }
        });
```

最后,让我用Java8的lambda表达式来避免多余的Action对象声明.

```java
Observable.just("Lambda")
        .subscribe(s -> Log.d("RxJava", s));
```

如果你使用的Android版本的SDK不支持Java8,我强烈推荐你使用[retrolambda](https://github.com/evant/gradle-retrolambda)这个gradle插件.它可以极大的避免代码中冗长的代码.

------
# 转换

让我们来研究一些更高端的RxJava使用方法.

假设我想在"Hello, World"输出之前加上我的签名一起输出.你可能想到的一种方法是直接修改Observable:

```java
Observable.just("Hello, World! -Dan")
        .subscribe(s -> Log.d("RxJava", s));
```

如果你可以控制你的Observable对象,这种方法当然是可行的.但是如果你不能控制Observable对象呢？例如Observable对象是由第三方库生成的？另一个潜在的问题是:如果我的Observable对象呗多个订阅者订阅,而我只想让其中的几个订阅者在输出前增加我的签名?
如果我们试图按照下面的代码修改我们的Subscriber对象:

```java
Observable.just("Hello, World!")
        .subscribe(s -> Log.d("RxJava", s + " -Dan"));
```

这种做法依然无法让人满意,因为我希望我的Subscriber对象越轻量级越好,因为我很有可能在主线程里使用Subscriber对象.另外,根据响应式函数编程概念,Subscriber做的更多的应该是去响应事件,而不是去转换事件.
如果我能通过一些中间步骤对Observable的发出事件进行转换,那将会非常酷!

------
# 操作符

我们使用操作符来解决发出事件的转换问题.操作符可以在最初的Observable和最终的Subscriber之间去转换Observable发出的事件.RxJava中提供了大量的操作符可供使用,但是初学者还是先学会典型的操作符使用.
为了实现上面例子中的添加签名,我们可以使用map()操作符:

```java
Observable.just("Hello, World! -Dan")
        .map(new Func1<String, String>() {
            @Override
            public String call(String s) {
                return s + "-Dan";
            }
        })
        .subscribe(s -> Log.d("RxJava", s + " -Dan"));
```

再一次,我们可以使用lambda表达式来简化代码:

```java
Observable.just("Hello, World! -Dan")
        .map(s -> s + "-Dan");
        .subscribe(s -> Log.d("RxJava", s + " -Dan"));
```

是不是很酷!map()操作符的作用就是转换Observable对象的.我们可以通过链式调用来串联多个你想转换的map()操作符,最终达到将发出事件转换成Subscriber想要的事件的目的.

------
# map()进阶

关于map()操作符更有趣的一点是:它转换的Observable对象发出的事件类型不需要和之前的Observable发出的事件类型保持一致.
假设我的Subscriber对象对Observable发出的原始字符串不感兴趣,但是却对原始字符串的hash值敢兴趣,我们可以使用如下代码做转换:

```java
Observable.just("Hello World!")
        .map(new Func1<String, Integer>() {
            @Override
            public Integer call(String s) {
                return s.hashCode();
            }
        })
        .subscribe(s -> Log.d("RxJava", s + " -Dan"));
```

是不是很有意思哈!开始我们使用Observable发出一个字符串事件,但是我们的Subscriber最终接收到的却是一个Integer事件.我们再次使用Lambda来简化代码:

```java
Observable.just("Hello World!")
        .map(s->s.hashCode())
        .subscribe(i -> Log.d(Integer.toString(i)));
```

就像我前面说的,在响应式编程中,我们希望Subscriber尽可能的做最少的工作.让我们再增加一个map,来将整形的Hash值转换为对应的字符串:

```java
Observable.just("Hello World!")
        .map(s->s.hashCode())
        .map(i->Integer.toString(i))
        .subscribe(s -> Log.d("RxJava", s));
```

到这里你会发现,我们的Observable和Subscriber又回到了最初的代码实现,我们仅仅是在中间增加了几个转换步骤.我可以把签名实现也加入到map转换中去:

```java
Observable.just("Hello World!")
        .map(s->s + "-Dan")
        .map(s->s.hashCode())
        .map(i->Integer.toString(i))
        .subscribe(s -> Log.d("RxJava", s));
```

------
# 感觉不够牛逼？

看到这里,你可能会觉得上面的这些例子太简单,根本没体现出使用RxJava的价值!确实是这样,上面讲的都是一些简单的例子.但是通过上面的例子,你应该能感受到以下两点:

1. Observable和Subscriber可以做任何事情.
    你的Observable可以是一个数据库查询,Subscriber用来将结果展示在屏幕上.你的Observable可以是一个屏幕上的点击事件,Subscriber是对它的响应.你的Observable可以是网络上返回的字节流,Subscriber是将它写到本地磁盘上.
    通过这几个举例,你可以看出Observable和Subscriber可以是一种通用的异步框架.
2. Observable和Subscriber与之间的转换过程是完全解耦的.
   你可以在Observable和Subscriber之间添加任意数量的map()操作符.整个框架是高度解耦的,操作Observable发出的事件变得非常简单,只要有正确的输入和输出,你可以使用链式结构无限转换.

结合以上两点,你可以发现这个框架有着无限的可能.由于第一部分只介绍RxJava基础的关系,我们这里只介绍了map()操作符,接下来,我们会在第二部分深入探讨RxJava中提供的更多更牛逼的操作符使用.