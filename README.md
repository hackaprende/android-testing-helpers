# android-testing-helpers
Classes to make Unit and Espresso testing easier when using MVVM and Dependency Injection with Hilt.

## Setup
This project divides in two folders:
- unit: For unit tests
- instrumentation: For instrumentation tests with Espresso

1.- Add the dependencies.txt to your build.gradle(app) file and sync. They could need to be updated if new versions available.
2.- Add the classes in the unit folder to your (test) folder in Android.
3.- Add the classes in the instrumentation folder to your (androidTest) folder in Android.

## Classes for unit tests

### getOrAwaitValue.kt
It helps you to test LiveData, for example if you want to test a LiveData has a value when did something you can use it like this

        @Test 
        fun whenDownloadData_checkMyLiveDataHasSomeValue() {
          myViewModel.downloadData()

          Assert.assertThat(
              myViewModel.myLiveData.getOrAwaitValue(), CoreMatchers.`is`(someValueHere)
          )
        }
        
### CoroutineRule.kt
Boilerplate class to support coroutines in your tests.

## Classes for instrumentation tests

### CustomTestRunner.kt
Boilerplate class to support Hilt in your instrumentation tests. To setup this TestRunner, in your Build.gradle(app) file you need to change this:
        
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"

for this:

        testInstrumentationRunner "com.hackaprende.todogs.CustomTestRunner"
        
### EspressoTestUtil.kt
A class with custom matchers for Espresso that I've been developing and collecting along my career, 
very useful when you are testing things like Menus, BottomNavMenu, RecyclerViews, etc.
