pluginManagement {
    repositories {
        // 清华大学开源软件镜像站
        maven { url = uri("https://mirrors.tuna.tsinghua.edu.cn/maven2/") }
        // 阿里巴巴开源软件镜像站
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 清华大学开源软件镜像站
        maven { url = uri("https://mirrors.tuna.tsinghua.edu.cn/maven2/") }
        // 阿里巴巴开源软件镜像站
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        mavenCentral()
    }
}

rootProject.name = "accountRecord"
include(":app")
