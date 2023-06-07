package com.jormun.likeshadow.exception

class ShadowException :RuntimeException(){
}

class HostContextNullException:RuntimeException("Host Context is null, please invoke setHostContext() at first!")

class ApkPathNullException:RuntimeException("Apk path is null, please check!")

class ResourcesNullException:RuntimeException("Resources is null, please check!")