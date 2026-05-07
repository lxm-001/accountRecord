# 《Android开发艺术探索》面试知识点梳理

> 基于任玉刚《Android开发艺术探索》各章核心内容，聚焦高频面试考点。

---

## 第1章 Activity的生命周期和启动模式

### 1.1 生命周期

#### 典型情况下的生命周期

```
onCreate → onStart → onResume → [运行中] → onPause → onStop → onDestroy
```

- `onCreate`：Activity 正在被创建，可做初始化工作（setContentView、初始化数据）
- `onStart`：Activity 正在被启动，已可见但不在前台，无法交互
- `onResume`：Activity 已在前台，可交互
- `onPause`：Activity 正在停止，可做轻量级存储操作，不能太耗时（新 Activity 的 onResume 要等当前 onPause 执行完）
- `onStop`：Activity 即将停止，可做稍重的回收工作
- `onDestroy`：Activity 即将被销毁，做最终的资源释放

#### 面试高频问题

**Q: A 启动 B，回调顺序是什么？**
```
A.onPause → B.onCreate → B.onStart → B.onResume → A.onStop
```
关键点：A 的 onPause 先执行，B 才能启动。所以 onPause 中不能做耗时操作。

**Q: onStart/onStop 与 onResume/onPause 的区别？**
- `onStart/onStop`：从是否可见的角度回调
- `onResume/onPause`：从是否在前台（可交互）的角度回调

#### 异常情况下的生命周期

**资源相关的系统配置发生改变（如旋转屏幕）：**
- Activity 会被销毁并重建
- 调用 `onSaveInstanceState`（在 onStop 之前）保存状态
- 重建后在 `onRestoreInstanceState`（在 onStart 之后）恢复状态
- Bundle 会同时传递给 onCreate，但推荐在 onRestoreInstanceState 中恢复（不用判空）

**资源内存不足导致低优先级 Activity 被杀死：**
- 优先级：前台 Activity > 可见但非前台 Activity > 后台 Activity
- 同样会触发 onSaveInstanceState/onRestoreInstanceState

### 1.2 启动模式（launchMode）

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| standard | 默认模式，每次启动都创建新实例 | 普通页面 |
| singleTop | 栈顶复用，回调 onNewIntent | 通知跳转、搜索页 |
| singleTask | 栈内复用，会清除其上的 Activity，回调 onNewIntent | 主页面、浏览器主界面 |
| singleInstance | 独占一个任务栈 | 来电界面、系统级页面 |

**面试重点：singleTask 的工作流程**
1. 先寻找目标 Activity 所需的任务栈（由 taskAffinity 决定）
2. 如果任务栈不存在，创建任务栈并创建 Activity 实例
3. 如果任务栈存在但 Activity 不在栈中，创建 Activity 实例入栈
4. 如果任务栈存在且 Activity 在栈中，将其上方的 Activity 全部出栈（clearTop），并回调 onNewIntent

**taskAffinity：**
- 默认值为应用包名
- 与 singleTask 或 allowTaskReparenting 配合使用才有意义
- 可通过 `android:taskAffinity` 指定

### 1.3 IntentFilter 匹配规则

- action：必须存在且必须匹配其中一个
- category：Intent 中的每个 category 都必须在 filter 中存在（系统会自动添加 DEFAULT category）
- data：与 action 类似，必须匹配其中一个

---

## 第2章 IPC机制

### 2.1 Android中的多进程模式

**开启多进程的方式：** 在 AndroidManifest 中给四大组件指定 `android:process` 属性

```xml
<!-- 私有进程（以:开头） -->
<activity android:process=":remote" />
<!-- 全局进程 -->
<activity android:process="com.example.remote" />
```

**多进程导致的问题：**
1. 静态成员和单例模式完全失效
2. 线程同步机制完全失效
3. SharedPreferences 可靠性下降
4. Application 会多次创建

### 2.2 IPC基础概念

#### Serializable vs Parcelable

| 对比项 | Serializable | Parcelable |
|--------|-------------|------------|
| 所属 | Java 接口 | Android 接口 |
| 实现复杂度 | 简单（声明即可） | 较复杂（需实现读写方法） |
| 性能 | 较低（大量 I/O 和反射） | 高（内存序列化） |
| 适用场景 | 存储到磁盘、网络传输 | 内存中的 IPC（Intent、Binder） |

#### Binder

- Android 中最具特色的 IPC 方式，是 ServiceManager 连接各种 Manager 和对应 ManagerService 的桥梁
- 从 Framework 层看，Binder 是 ServiceManager 连接各种 Manager 的桥梁
- 从应用层看，Binder 是客户端和服务端通信的媒介

**AIDL 生成的 Binder 类核心方法：**
- `asInterface`：将服务端 Binder 对象转换为客户端所需的 AIDL 接口类型（同进程返回 Stub 本身，跨进程返回 Stub.Proxy）
- `onTransact`：运行在服务端 Binder 线程池中，处理客户端请求
- `transact`：运行在客户端，发起远程调用（会挂起当前线程直到返回）

### 2.3 Android中的IPC方式

| 方式 | 特点 | 适用场景 |
|------|------|----------|
| Bundle | 简单，支持 Parcelable 数据 | 四大组件间传递数据 |
| 文件共享 | 简单，不适合高并发 | 无并发的简单数据交换 |
| Messenger | 基于 AIDL，串行处理，简单 | 低并发的一对多通信 |
| AIDL | 功能强大，支持并发 | 一对多且有并发需求 |
| ContentProvider | 天然支持 CRUD，数据源访问 | 一对多的数据共享 |
| Socket | 功能强大，通过网络传输字节流 | 网络数据交换 |

### 2.4 Binder连接池

当有多个 AIDL 接口时，不需要为每个接口创建一个 Service，可以使用 Binder 连接池统一管理：
- 每个业务模块创建自己的 AIDL 接口
- 服务端提供一个 queryBinder 接口，根据业务模块的标识返回对应的 Binder 对象
- 减少 Service 数量，避免资源浪费

---

## 第3章 View的事件体系

### 3.1 View基础知识

**坐标系：**
- `getLeft/getTop/getRight/getBottom`：相对于父容器的位置（不随平移改变）
- `getX/getY`：相对于父容器的实际位置（= left/top + translationX/Y）
- `getTranslationX/getTranslationY`：相对于 left/top 的偏移量

**MotionEvent：**
- `getX/getY`：相对于当前 View 左上角的坐标
- `getRawX/getRawY`：相对于屏幕左上角的坐标

**TouchSlop：** 系统能识别的最小滑动距离，通过 `ViewConfiguration.get(context).getScaledTouchSlop()` 获取

### 3.2 View的滑动

三种实现方式：

| 方式 | 原理 | 特点 |
|------|------|------|
| scrollTo/scrollBy | 改变 View 内容的位置 | 只能移动内容，不能移动 View 本身 |
| 动画 | 操作 View 的 translationX/Y | 属性动画能真正改变位置，View 动画只改变影像 |
| 改变布局参数 | 修改 LayoutParams | 适合有交互的场景 |

**弹性滑动：**
- `Scroller`：配合 `computeScroll()` 实现，本质是将大的滑动分成小段在一定时间内完成
- `Handler.postDelayed`：通过延时消息分段滑动
- 属性动画：天然支持弹性效果

### 3.3 事件分发机制（最高频面试题）

**三个核心方法：**

```java
// 分发事件
public boolean dispatchTouchEvent(MotionEvent ev)
// 拦截事件（只有 ViewGroup 有）
public boolean onInterceptTouchEvent(MotionEvent ev)
// 消费事件
public boolean onTouchEvent(MotionEvent ev)
```

**伪代码表示分发逻辑：**

```java
public boolean dispatchTouchEvent(MotionEvent ev) {
    boolean consume = false;
    if (onInterceptTouchEvent(ev)) {
        consume = onTouchEvent(ev);
    } else {
        consume = child.dispatchTouchEvent(ev);
    }
    return consume;
}
```

**核心结论：**
1. 事件传递顺序：Activity → Window → DecorView → ViewGroup → View
2. 某个 View 一旦开始处理事件（onTouchEvent 返回 true），后续事件都会交给它处理
3. 某个 View 的 onTouchEvent 返回 false，其父 View 的 onTouchEvent 会被调用
4. ViewGroup 默认不拦截任何事件（onInterceptTouchEvent 默认返回 false）
5. View 没有 onInterceptTouchEvent 方法，事件传递到 View 就直接调用 onTouchEvent
6. View 的 onTouchEvent 默认返回 true（除非它是不可点击的）
7. onClick 发生的前提是 View 可点击，且收到了 DOWN 和 UP 事件
8. 事件传递过程是由外向内的，即先传递给父元素，再由父元素分发给子 View
9. 子 View 可以通过 `requestDisallowInterceptTouchEvent` 干预父元素的事件分发（ACTION_DOWN 除外）

**OnTouchListener vs onTouchEvent vs OnClickListener 优先级：**
```
OnTouchListener.onTouch > onTouchEvent > OnClickListener.onClick
```

### 3.4 滑动冲突

**常见场景：**
1. 外部滑动方向与内部滑动方向不一致（如 ViewPager + ListView）
2. 外部滑动方向与内部滑动方向一致（如 ScrollView 嵌套 ListView）
3. 以上两种情况的嵌套

**解决方式：**

**外部拦截法（推荐）：** 重写父容器的 `onInterceptTouchEvent`
```java
public boolean onInterceptTouchEvent(MotionEvent event) {
    boolean intercepted = false;
    switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            intercepted = false; // 必须不拦截，否则后续事件都无法传递给子 View
            break;
        case MotionEvent.ACTION_MOVE:
            if (父容器需要当前事件) {
                intercepted = true;
            } else {
                intercepted = false;
            }
            break;
        case MotionEvent.ACTION_UP:
            intercepted = false;
            break;
    }
    return intercepted;
}
```

**内部拦截法：** 重写子 View 的 `dispatchTouchEvent`，配合 `requestDisallowInterceptTouchEvent`


---

## 第4章 View的工作原理

### 4.1 ViewRoot和DecorView

- `ViewRoot` 对应 `ViewRootImpl` 类，是连接 WindowManager 和 DecorView 的纽带
- View 的三大流程（measure、layout、draw）均通过 ViewRoot 完成
- `DecorView` 是顶级 View，一般包含一个竖直方向的 LinearLayout（标题栏 + 内容栏）
- `setContentView` 设置的布局被添加到内容栏（id 为 `android.R.id.content`）

### 4.2 MeasureSpec

MeasureSpec 是一个 32 位 int 值：高 2 位是 SpecMode，低 30 位是 SpecSize。

| SpecMode | 含义 |
|----------|------|
| UNSPECIFIED | 父容器不限制，一般用于系统内部（如 ScrollView） |
| EXACTLY | 精确大小，对应 match_parent 和具体数值 |
| AT_MOST | 不超过 SpecSize，对应 wrap_content |

**MeasureSpec 的确定规则（子 View 的 MeasureSpec = 父容器的 MeasureSpec + 子 View 的 LayoutParams）：**

| 子 View \ 父容器 | EXACTLY | AT_MOST | UNSPECIFIED |
|-------------------|---------|---------|-------------|
| dp/px（精确值） | EXACTLY, childSize | EXACTLY, childSize | EXACTLY, childSize |
| match_parent | EXACTLY, parentSize | AT_MOST, parentSize | UNSPECIFIED, 0 |
| wrap_content | AT_MOST, parentSize | AT_MOST, parentSize | UNSPECIFIED, 0 |

### 4.3 View的measure过程

**View 的 measure：**
- `onMeasure` 中调用 `setMeasuredDimension` 设置测量宽高
- `getDefaultSize`：对于 AT_MOST 和 EXACTLY 都返回 specSize
- 这就是为什么自定义 View 需要重写 onMeasure 处理 wrap_content（否则效果等同于 match_parent）

**ViewGroup 的 measure：**
- ViewGroup 是抽象类，没有重写 onMeasure（由子类如 LinearLayout 实现）
- 提供了 `measureChildren` 方法遍历测量子 View

**面试题：如何在 Activity 中获取 View 的宽高？**
1. `Activity.onWindowFocusChanged`：View 已初始化完毕（会多次调用）
2. `view.post(runnable)`：投递到消息队列尾部，等 View 初始化好后执行
3. `ViewTreeObserver.addOnGlobalLayoutListener`：View 树状态改变时回调
4. 手动调用 `view.measure`：需要根据 LayoutParams 分情况处理

### 4.4 View的layout过程

- `layout` 方法确定 View 本身的位置（通过 setFrame 设置四个顶点）
- `onLayout` 确定子 View 的位置（ViewGroup 需要实现）
- 测量宽高（getMeasuredWidth/Height）在 measure 后确定
- 最终宽高（getWidth/Height）在 layout 后确定
- 一般情况下两者相等，但可以在 layout 中人为修改

### 4.5 View的draw过程

绘制顺序：
1. 绘制背景（`drawBackground`）
2. 绘制自己（`onDraw`）
3. 绘制子 View（`dispatchDraw`）
4. 绘制装饰（如前景、滚动条等，`onDrawForeground`）

**setWillNotDraw：** 如果一个 View 不需要绘制任何内容，设置此标记后系统会进行优化。ViewGroup 默认开启，自定义 ViewGroup 需要绘制内容时要关闭。

---

## 第5章 理解RemoteViews

### 5.1 RemoteViews的应用

- 主要用于通知栏（Notification）和桌面小部件（AppWidget）
- 通知栏通过 `NotificationManager` 管理
- 桌面小部件通过 `AppWidgetProvider`（本质是 BroadcastReceiver）管理

### 5.2 RemoteViews的内部机制

- RemoteViews 不支持所有 View 类型，仅支持有限的布局和控件
- 支持的布局：LinearLayout、RelativeLayout、FrameLayout、GridLayout
- 支持的控件：Button、TextView、ImageView、ProgressBar 等
- 不支持自定义 View 和上述之外的 View
- 通过 Action 对象（如 ReflectionAction）跨进程更新 View

---

## 第7章 Android动画深入分析

### 7.1 View动画

- 四种变换：平移（Translate）、缩放（Scale）、旋转（Rotate）、透明度（Alpha）
- 可通过 XML 或代码定义
- View 动画只改变 View 的影像，不改变 View 的实际属性和位置

### 7.2 帧动画

- 通过 `AnimationDrawable` 实现
- 注意避免使用大量大图导致 OOM

### 7.3 属性动画

**核心类：**
- `ValueAnimator`：对值进行动画，通过监听值的变化来更新属性
- `ObjectAnimator`：继承自 ValueAnimator，直接对对象的属性进行动画
- `AnimatorSet`：组合多个动画

**属性动画的工作原理：**
- 通过反射调用 get/set 方法来读取和设置属性值
- 如果没有 set 方法，可以用 `ValueAnimator` + 监听器手动设置
- 也可以用包装类提供 get/set 方法

**插值器（Interpolator）和估值器（TypeEvaluator）：**
- 插值器：决定动画的变化速率（如加速、减速、弹性）
- 估值器：决定属性值的具体计算方式（如 IntEvaluator、FloatEvaluator）

**面试题：属性动画 vs View 动画**

| 对比项 | View 动画 | 属性动画 |
|--------|----------|----------|
| 作用对象 | 仅 View | 任意对象 |
| 是否改变属性 | 否（只改变影像） | 是 |
| 点击事件位置 | 停留在原位置 | 跟随动画移动 |
| 动画类型 | 4种固定变换 | 可自定义任意属性 |

### 7.4 使用动画的注意事项

1. OOM：帧动画图片过多过大
2. 内存泄漏：属性动画中的无限循环动画需要在 Activity 销毁时停止
3. View 动画的点击问题：动画后 View 的实际位置不变
4. 硬件加速：某些动画在开启硬件加速后效果更好

---

## 第8章 理解Window和WindowManager

### 8.1 Window和WindowManager

- Window 是抽象类，唯一实现是 `PhoneWindow`
- Window 的创建通过 `WindowManager` 完成
- WindowManager 是接口，继承自 `ViewManager`，实现类是 `WindowManagerImpl`

**Window 的类型（层级）：**
- 应用 Window（1-99）：Activity
- 子 Window（1000-1999）：Dialog
- 系统 Window（2000-2999）：Toast、状态栏（需要权限）

**WindowManager 的三个核心操作：**
- `addView`：添加 Window
- `updateViewLayout`：更新 Window
- `removeView`：删除 Window

### 8.2 Window的内部机制

**Window 的添加过程：**
```
WindowManager.addView → WindowManagerImpl.addView → WindowManagerGlobal.addView
→ 创建 ViewRootImpl → ViewRootImpl.setView → WindowSession.addToDisplay（IPC）
→ WindowManagerService.addWindow
```

**Window 的删除和更新过程类似，最终都通过 IPC 调用 WindowManagerService。**

### 8.3 Window的创建过程

**Activity 的 Window 创建：**
1. `Activity.attach` 中创建 PhoneWindow
2. `setContentView` 时：
   - 如果没有 DecorView 则创建
   - 将 View 添加到 DecorView 的 content 中
   - 回调 `onContentChanged`
3. `ActivityThread.handleResumeActivity` 中将 DecorView 添加到 WindowManager

**Dialog 的 Window 创建：** 与 Activity 类似，但 Dialog 必须使用 Activity 的 Context（因为需要 Activity 的 token）

**Toast 的 Window 创建：** 通过 NMS（NotificationManagerService）管理，内部使用 Handler

---

## 第9章 四大组件的工作过程

### 9.1 Activity的工作过程

启动流程（简化）：
```
startActivity → Instrumentation.execStartActivity → AMS.startActivity
→ ActivityThread.scheduleLaunchActivity → handleLaunchActivity
→ performLaunchActivity（创建 Activity、调用 attach 和 onCreate）
```

### 9.2 Service的工作过程

**启动方式对比：**

| 方式 | 生命周期 | 特点 |
|------|----------|------|
| startService | onCreate → onStartCommand → onDestroy | 独立运行，调用者退出不影响 |
| bindService | onCreate → onBind → onUnbind → onDestroy | 与调用者绑定，调用者退出则停止 |

### 9.3 BroadcastReceiver的工作过程

**注册方式：**
- 静态注册：在 AndroidManifest 中声明，常驻型
- 动态注册：通过 `registerReceiver`，非常驻型，跟随组件生命周期

**广播类型：**
- 普通广播：异步，无序
- 有序广播：同步，按优先级传递，可拦截
- 本地广播：`LocalBroadcastManager`，仅应用内，更高效安全

### 9.4 ContentProvider的工作过程

- `onCreate` 在 `Application.onCreate` 之前调用
- 增删改查四个方法运行在 Binder 线程池中，需要注意线程安全
- `query` 方法可以并发执行，其他方法（insert/delete/update）需要同步

---

## 第10章 Android的消息机制

### 10.1 消息机制概述

Android 的消息机制主要指 Handler 的运行机制，包含：
- `Handler`：发送和处理消息
- `MessageQueue`：消息队列（单链表结构）
- `Looper`：消息循环，不断从 MessageQueue 中取出消息交给 Handler 处理

### 10.2 MessageQueue的工作原理

- `enqueueMessage`：插入消息（按时间排序的单链表插入）
- `next`：取出消息（无限循环，无消息时阻塞在 nativePollOnce）

### 10.3 Looper的工作原理

```java
Looper.prepare();    // 创建 Looper 并存入 ThreadLocal
Looper.loop();       // 开启消息循环
```

- `loop()` 是一个死循环，唯一退出方式是 `MessageQueue.next` 返回 null（调用 quit/quitSafely）
- 主线程的 Looper 在 `ActivityThread.main` 中创建，不能退出

### 10.4 Handler的工作原理

**消息发送：** `sendMessage → sendMessageDelayed → sendMessageAtTime → enqueueMessage`

**消息处理优先级：**
1. `Message.callback`（即 Runnable，通过 post 方式发送）
2. `Handler.mCallback`（构造时传入的 Callback）
3. `Handler.handleMessage`（重写的方法）

### 10.5 ThreadLocal的工作原理

- 线程内部的数据存储类，不同线程访问同一个 ThreadLocal 对象，获取的值不同
- 每个 Thread 内部有一个 `ThreadLocalMap`，以 ThreadLocal 为 key 存储数据
- Looper 就是通过 ThreadLocal 实现每个线程独立的 Looper 实例

**面试题：子线程中如何使用 Handler？**
```kotlin
val thread = Thread {
    Looper.prepare()
    val handler = Handler(Looper.myLooper()!!) { msg ->
        // 处理消息
        true
    }
    Looper.loop()
}
thread.start()
```
或者使用 `HandlerThread`（已封装好 Looper 的创建和启动）。


---

## 第11章 Android的线程和线程池

### 11.1 主线程和子线程

- 主线程（UI 线程）：处理界面交互，不能执行耗时操作（否则 ANR）
- 子线程（工作线程）：执行耗时操作（网络请求、数据库操作等）
- Android 3.0 开始，网络请求必须在子线程中执行

### 11.2 Android中的线程形态

**AsyncTask（已废弃，但面试常问）：**
- 封装了 Thread 和 Handler
- 核心方法：`onPreExecute`（主线程）→ `doInBackground`（子线程）→ `onPostExecute`（主线程）
- 默认串行执行（3.0 之后），可通过 `executeOnExecutor` 并行执行
- 注意事项：内存泄漏（持有 Activity 引用）、生命周期不跟随 Activity

**HandlerThread：**
- 继承自 Thread，内部创建了 Looper
- 适合需要在子线程中使用 Handler 的场景（如 IntentService）

**IntentService（已废弃）：**
- 继承自 Service，内部使用 HandlerThread
- 适合执行后台耗时任务，执行完自动停止
- 优先级比普通线程高（不容易被系统杀死）

### 11.3 线程池

**使用线程池的好处：**
1. 重用线程，减少创建和销毁的开销
2. 控制最大并发数，避免资源竞争
3. 提供定时执行和间隔执行等功能

**ThreadPoolExecutor 核心参数：**

| 参数 | 含义 |
|------|------|
| corePoolSize | 核心线程数（默认一直存活） |
| maximumPoolSize | 最大线程数 |
| keepAliveTime | 非核心线程闲置超时时间 |
| unit | keepAliveTime 的时间单位 |
| workQueue | 任务队列（BlockingQueue） |
| threadFactory | 线程工厂 |
| handler | 拒绝策略（队列满且线程数达到最大时） |

**线程池执行任务的规则：**
1. 线程数 < corePoolSize → 启动核心线程执行
2. 线程数 >= corePoolSize → 任务加入队列
3. 队列已满且线程数 < maximumPoolSize → 启动非核心线程执行
4. 队列已满且线程数 >= maximumPoolSize → 执行拒绝策略

**四种常见线程池：**

| 线程池 | 核心线程 | 最大线程 | 队列 | 特点 |
|--------|----------|----------|------|------|
| FixedThreadPool | n | n | LinkedBlockingQueue（无界） | 固定线程数，适合负载较重的服务器 |
| CachedThreadPool | 0 | Integer.MAX_VALUE | SynchronousQueue | 按需创建，适合大量短任务 |
| ScheduledThreadPool | n | Integer.MAX_VALUE | DelayedWorkQueue | 定时和周期性任务 |
| SingleThreadExecutor | 1 | 1 | LinkedBlockingQueue（无界） | 单线程串行执行，保证顺序 |

---

## 第12章 Bitmap的加载和Cache

### 12.1 Bitmap的高效加载

**BitmapFactory 的四种加载方式：**
- `decodeFile`：从文件加载
- `decodeResource`：从资源加载
- `decodeStream`：从输入流加载
- `decodeByteArray`：从字节数组加载

**核心优化：通过 BitmapFactory.Options 的 inSampleSize 参数进行采样压缩**

```kotlin
// 高效加载 Bitmap 的步骤
fun decodeSampledBitmap(res: Resources, resId: Int, reqWidth: Int, reqHeight: Int): Bitmap {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true  // 1. 只解析尺寸，不加载到内存
    }
    BitmapFactory.decodeResource(res, resId, options)
    
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)  // 2. 计算采样率
    options.inJustDecodeBounds = false  // 3. 加载缩放后的 Bitmap
    
    return BitmapFactory.decodeResource(res, resId, options)
}
```

- `inSampleSize`：采样率，值为 2 时宽高各缩小一半，像素数变为 1/4
- 官方建议 inSampleSize 取 2 的幂次

### 12.2 Android中的缓存策略

**LruCache（内存缓存）：**
- 基于 `LinkedHashMap`（accessOrder = true，按访问顺序排列）
- 当缓存满时，移除最近最少使用的对象
- 线程安全（内部使用 synchronized）

**DiskLruCache（磁盘缓存）：**
- 通过 `open` 创建，指定缓存目录和大小
- 写入：通过 `Editor` 获取输出流写入
- 读取：通过 `Snapshot` 获取输入流读取
- 需要手动 `flush` 和 `close`

**典型的三级缓存策略：**
```
内存缓存（LruCache）→ 磁盘缓存（DiskLruCache）→ 网络
```

### 12.3 ImageLoader的实现

一个优秀的图片加载框架应具备：
1. 图片压缩（inSampleSize）
2. 内存缓存和磁盘缓存
3. 同步/异步加载
4. 网络拉取
5. 图片加载时的列表错位处理（通过 tag 对比 URL）

---

## 第13章 综合技术

### 13.1 CrashHandler

- 实现 `Thread.UncaughtExceptionHandler` 接口
- 在 Application 中设置为默认的异常处理器
- 可以收集设备信息、异常信息并上传到服务器

### 13.2 Multidex

- 单个 dex 文件方法数上限为 65536（64K）
- 超过时需要使用 multidex 方案
- Android 5.0+ 原生支持（ART 运行时）
- 5.0 以下需要引入 `multidex` 支持库

### 13.3 反编译

- `dex2jar`：将 dex 转为 jar
- `jd-gui`：查看 jar 中的 Java 代码
- `apktool`：反编译资源文件

---

## 第15章 Android性能优化

### 15.1 布局优化

- 减少布局层级：使用 `ConstraintLayout` 替代多层嵌套
- `<include>`：复用布局
- `<merge>`：减少多余的层级（配合 include 使用）
- `<ViewStub>`：延迟加载，需要时才 inflate（适合不常用的布局）

### 15.2 绘制优化

- `onDraw` 中不要创建新的局部对象（频繁 GC）
- `onDraw` 中不要做耗时操作
- 避免过度绘制（Overdraw）

### 15.3 内存泄漏

**常见场景：**
1. 静态变量持有 Activity 引用
2. 单例模式持有 Activity 引用
3. 属性动画未在 onDestroy 中停止
4. Handler 导致的泄漏（非静态内部类持有外部类引用）
5. 资源未关闭（Cursor、Stream、Bitmap）
6. WebView 泄漏

**Handler 内存泄漏的解决方案：**
```kotlin
class MyActivity : AppCompatActivity() {
    // 使用静态内部类 + 弱引用
    private class MyHandler(activity: MyActivity) : Handler(Looper.getMainLooper()) {
        private val activityRef = WeakReference(activity)
        override fun handleMessage(msg: Message) {
            val activity = activityRef.get() ?: return
            // 处理消息
        }
    }
    
    private val handler = MyHandler(this)
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // 移除所有消息
    }
}
```

### 15.4 响应速度优化（ANR）

**ANR 超时时间：**
- Activity：5 秒
- BroadcastReceiver：10 秒
- Service：20 秒（前台）/ 200 秒（后台）

**避免 ANR：**
- 不在主线程做耗时操作
- 使用子线程处理耗时逻辑
- ANR 日志位于 `/data/anr/traces.txt`

### 15.5 ListView/RecyclerView 优化

1. 使用 ViewHolder 模式复用 View
2. 滑动时不加载图片
3. 分页加载
4. 避免在 getView/onBindViewHolder 中做耗时操作
5. 开启硬件加速

### 15.6 线程优化

- 使用线程池代替直接创建 Thread
- 避免创建大量线程

### 15.7 其他优化建议

- 避免创建过多对象
- 不要过多使用枚举（内存开销比整型大）
- 常量使用 `const val`
- 使用 `SparseArray` 替代 `HashMap<Integer, Object>`
- 适当使用软引用和弱引用
- 使用内存缓存和磁盘缓存

---

## 附录：高频面试题速查

| 主题 | 核心考点 |
|------|----------|
| Activity 生命周期 | 正常/异常生命周期、A启动B的回调顺序、onSaveInstanceState |
| 启动模式 | 四种模式区别、singleTask 工作流程、taskAffinity |
| IPC | Binder 原理、AIDL、Messenger、Serializable vs Parcelable |
| 事件分发 | 三个核心方法、传递顺序、滑动冲突解决 |
| View 绘制 | measure/layout/draw 流程、MeasureSpec、自定义 View 注意事项 |
| 消息机制 | Handler/Looper/MessageQueue 关系、ThreadLocal、子线程使用 Handler |
| 线程池 | 核心参数、执行规则、四种常见线程池 |
| Bitmap | 高效加载、inSampleSize、三级缓存 |
| 性能优化 | 内存泄漏、ANR、布局优化、绘制优化 |
| Window | Window 类型、添加过程、Activity/Dialog/Toast 的 Window 创建 |
