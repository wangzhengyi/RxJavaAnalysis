# RxJava入门系列二,操作符篇

在RxJava入门系列一基础篇中,我介绍了RxJava的基础框架,同时也介绍了map()操作符的使用.然而我知道你现在对RxJava的了解还不够深入,所以你不能让自己去在实际代码中使用RxJava.但是通过这篇文章,通过更多操作符的介绍,你将见识到RxJava的强大,没准你会开始愿意使用RxJava.
让我们先通过一个例子来介绍一下RxJava的操作符.

------
# 准备工作

假设我有这样一个方法:

```java
// 通过搜索关键字返回一组网站URL列表.
Observable<List<String>> query(String text); 
```

我想构建一个鲁棒性很强的系统,可以提供根据字符串来搜索并输出匹配的URL.根据上一篇博客的内容,我们可以很快写出如下代码:

```java
query("Hello, world!")
    .subscribe(urls -> {
        for (String url : urls) {
            System.out.println(url);
        }
    });
```

上面的代码鲁棒性是非常差的,因为我们不能通过上面的代码去改变原始的数据流.如果我想改变每个URL,我只能在Subscriber的代码中完成这项工作.而且在上面代码中我们竟然没使用map()操作符!

当然,我可以在上面的代码中增加map()操作符,输入是一组url集合,输出也是一组url集合.但是每次map()调用还是需要内嵌for-each循环来一个个的处理url.

------
# 一丝希望

RxJava还提供了一个方法:Observable.from(),它可以接受一组事件然后一个一个的发送这些事件:

```java
Observable.from("url1", "url2", "url3")
    .subscribe(url -> System.out.println(url));
```
看起来似乎有用,让我们使用这个方法来优化一下上面处理urls的例子:
```java
query("Hello, world!")
    .subscribe(urls -> {
        Observable.from(urls)
            .subscribe(url -> System.out.println(url));
    });
```
使用这个方法可以让我们避免内嵌的for-each循环代码,但是却让代码看起来更糟糕.因为代码中出现了多个内嵌且并列Subscription.这样实现代码不仅丑陋还难以修改,而且这样做打破了RxJava的undiscovered特性.

-------
# 更好的方法

屏住呼吸,让我们来看一个更屌的操作符:flatMap()
Observable.flatMap()接收一个Observable的输出作为输入,同时输出另外一个Observable.让我们看一下如何通过Observable.flatMap()来解决内嵌for-each循环的问题:
```java
query("Hello, world!")
    .flatMap(new Func1<List<String>, Observable<String>>() {
        @Override
        public Observable<String> call(List<String> urls) {
            return Observable.from(urls);
        }
    })
    .subscribe(url -> System.out.println(url));
```
我展示出完整的代码便于你了解flatMap()内部发生了什么,但是上面的代码是可以使用lambda表达式进行简化的:
```java
query("Hello, world!")
    .flatMap(urls -> Observable.from(urls))
    .subscribe(url -> System.out.println(url));
```
flatMap()看起来很难理解是不是?为什么它要输出另一个Observable?理解flatMap()的关键在于:flatMap()输出的Observable所发出的事件正是Subscriber想要接收的事件.现在Subscriber不再收到List<String>,而是收到由Observable.from()生成的一系列单个的String.

这部分也是我学习RxJava中最难理解的一部分,但是一旦我想通了这块原理,许多RxJava的难题也就迎刃而解了.

------
# 继续优化

我必须强调一个观点:flatMap()可以返回任意你想要的Observable.

假设我又有一个已经实现了一个方法,用法如下:

```java
// 通过URL返回网站的标题,如果URL对应的网站不存在则返回404
Observable<String> getTitle(String URL);
```

接着上面打印一组URLS的例子,这里我想打印出每个URL对应网站的标题.但是有个问题:我的方法每次只能传入一个URL,并且返回值不是一个String,而是一个输出String的Observable.

使用flatMap(),我们可以很轻松的解决这个问题.在分解URLS之后,我们在每个url到达Subscriber之前再次使用flatMap()方法对每个url使用getTitle方法获取标题:

```java
query("Hello, world!")
    .flatMap(urls -> Observable.from(urls))
    .flatMap(new Func1<String, Observable<String>>() {
        @Override
        public Observable<String> call(String url) {
            return getTitle(url);
        }
    })
    .subscribe(title -> System.out.println(title));
```

使用lambda表达式进一步优化代码:

```java
query("Hello, world!")
    .flatMap(urls -> Observable.from(urls))
    .flatMap(url -> getTitle(url))
    .subscribe(title -> System.out.println(title));
```

是不是很屌?我竟然能将多个独立返回的Observable对象组合在一起使用,简直屌爆了!
屌爆了还不止这些,通过上面的代码,你应该注意到我通过链式调用将两个API串联了起来,我甚至可以将任意多的API通过链式调用串联起来.你应该知道在没有RxJava之前将所有API回调的接口串联起来后显示是多么的蛋疼!现在有了RxJava,所有的回调逻辑都被包装成简短的链式调用了.

-------
# 大量的操作符

到目前位置我们只介绍了RxJava的两个操作符,但是RxJava还提供了其他大量的操作符,那么我们如何使用这些操作符来优化我们的代码呢?

上面的getTitle()方法在URL不存在的情况下会返回null.我们不想输出null,我们可以在subscriber输出null之前将它过滤掉.

```java
query("Hello, world!")
    .flatMap(urls -> Observable.from(urls))
    .flatMap(url -> getTitle(url))
    .filter(title -> title != null)
    .subscribe(title -> System.out.println(title));
```

filter()输入和输出都是相同的元素,但是不满足过滤条件的元素将不会被输出.

然后我们只想显示5条结果:

```java
query("Hello, world!")
    .flatMap(urls -> Observable.from(urls))
    .flatMap(url -> getTitle(url))
    .filter(title -> title != null)
    .take(5)
    .subscribe(title -> System.out.println(title));
```

take()方法帮我们指定输出的最大数量.

现在我们又想在打印前,把每个标题保存在硬盘上:

```java
query("Hello, world!")
    .flatMap(urls -> Observable.from(urls))
    .flatMap(url -> getTitle(url))
    .filter(title -> title != null)
    .take(5)
    .doOnNext(title -> saveTitle(title))
    .subscribe(title -> System.out.println(title));
```

doNext()方法允许我们在输出每个事件之前做一些额外的操作,这里我们的额外操作就是存储操作.

可以看到,我们可以很轻松的操控数据流.你可以再添加更多的操作符,并且这些操作符不会扰乱你原来的代码逻辑.

RxJava有大量的操作符.虽然操作符的数量大的惊人,但是它们仍然值得你去花时间了解一下,这样你就能更好的操控RxJava.

在使用操作符的基础上,你甚至可以编写操作符.这已经超出本篇博客的讲解范围,但是你可以自行去研究,去实践.

------
# 感觉如何?

好吧,你还是不想使用RxJava,你觉得为什么你要关心这么多的操作符?

那是因为:操作符可以帮助你对数据流做任意的改动.

你可以使用一系列操作符来完成复杂的逻辑.代码将会被解耦成一块一块可以组合的片段,这就是响应式编程牛逼的地方.你越多的使用RxJava,你就会更深的了解响应式编程的思想.

另外,RxJava使我们处理数据变得非常简单.在最后一个例子里,我们调用了两个API,就改变了数据流,并且将它存储在了硬盘上,但是对Subscriber却是无感知的,Subscriber只是认为自己是在订阅一个简单的Observable<String>对象.良好的封装性使代码变得简洁!

在第三部分中,我们将介绍RxJava中另一些很屌的特性,比如错误处理和并发.

------
# 原文链接

[Grokking RxJava, Part 2: Operator, Operator](http://blog.danlew.net/2014/09/15/grokking-rxjava-part-2/)