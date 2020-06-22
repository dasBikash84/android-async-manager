# android-async-manager

Library to run back-ground tasks sequentially in allocated managed thread pool for android platform.

[![](https://jitpack.io/v/dasBikash84/android-async-manager.svg)](https://jitpack.io/#dasBikash84/android-async-manager)

## Dependency

Add this in your root `build.gradle` file (**not** your module `build.gradle` file):

```gradle
allprojects {
	repositories {
        maven { url "https://jitpack.io" }
    }
}
```

Then, add the library to your module `build.gradle`
```gradle
dependencies {
    implementation 'com.github.dasBikash84:android-async-manager:latest.release.here'
}
```

## Features/Notes
- [`Launching and running`](https://github.com/dasBikash84/android-async-manager/blob/master/async_manager/src/main/java/com/dasbikash/async_manager/AsyncTaskManager.kt) back-ground tasks made very easy and safe(with launcher component lifecycle-owner hook). i.e. if launched with component life-cycle hook, then auto cancellation of task will occur `onDestroy` of component.
- Optional callback method for task success/failure `(Will run on main thread)`.
- Very useful [`extension`](https://github.com/dasBikash84/android-async-manager/blob/master/async_manager/src/main/java/com/dasbikash/async_manager/Extensions.kt) function provided for **running any code block with suspension**, inside a suspend function.
- Also extension function provided to access current **CoroutineContext** inside any `suspend` function.

## Usage example

##### Initialization (optional):
```
    AsyncTaskManager.init()
    
    // or
    
    AsyncTaskManager.init(maxParallelTasks:Int)
```
##### Cancellation of entire task queue:
```
    AsyncTaskManager.clear()
```
##### Add back-ground task on pending task queue:
```
    AsyncTaskManager.addTask(task: AsyncTask<T>)
    
    //or
    
    AsyncTaskManager.addTask(task:()->T?) //Without caller lifecycle-owner hook
```
##### Add/cancellation of back-ground task from AppCompatActivity/Fragment body with lifecycle-owner hook:
```
    val task:AsyncTask<T> = addAsyncTask(task:()->T?) // Adding back-ground task
    .....
    .....
    task.cancel() //Cancellation of back-ground task
```
License
--------

    Copyright 2020 Bikash Das(das.bikash.dev@gmail.com)

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
