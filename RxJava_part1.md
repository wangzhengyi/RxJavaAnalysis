# RxJava系列一,基础篇

RxJava在Android开发者中变得越来越流行(ps:2016年底才用的我表示自己太out了).使用RxJava唯一的问题就是当你第一次接触RxJava时,你会觉得RxJava特别难上手,特别是当你之前都是使用imperative编程语言.但是一旦你真正了解了RxJava,你就会知道它是多么好用.

## 基础
Reactive编程最基础的模块是Observables(被观察者)和Subscribers(观察者).Observables发出一系列事件,Subscribers来消费这些事件.

Reactive编程中事件发送是有固定模式的.一个Observable可能会发出零个或者多个事件,每个事件要不就是成功的被处理,要不就是遇到了错误被终止.对于每个发出事件,Observable都会调用监听该事件的Subscriber的onNext(),最后调用Subscriber.onComplete()或者Subscriber.onError()来结束事件的处理.

Reactive编程看起来很像观察者模式,但是有一点明显的不同,那就是Observable当没有任何Subscriber来监听时,Observable是不会发送任何事件的.

## Hello, World!

让我们通过一个具体的例子来看一下Reactive编程的框架实现.我们先来创建一个基本的Observable对象:
```java

```