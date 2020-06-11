# Movie Tag

A JavaFX application that write tags and cover art images in mp4 and mkv video files.
It uses the [JSON API](http://api.themoviedb.org/) provided by [TMDb](https://www.themoviedb.org/).

## Screenshots

![main](https://user-images.githubusercontent.com/10371312/50688641-0979df00-102f-11e9-8ce0-e5eec0fca3e6.PNG)
![search](https://user-images.githubusercontent.com/10371312/50688660-1ac2eb80-102f-11e9-8f30-293cbab2380c.PNG)
![write](https://user-images.githubusercontent.com/10371312/50688666-1e567280-102f-11e9-8f8a-3d490d67e43c.PNG)

## Build

In order to build the project you need [IntelliJ IDEA](https://www.jetbrains.com/idea/).
Clone the repository and open the project with IntelliJ.

Then in `MovieTag -> src -> main -> resources` folder create a `tmdb.properties` file with the following content

```
KEY=YOUR_KEY_FOR_TMDB_API
```

and replace `YOUR_KEY_FOR_TMDB_API` with your key from [TMDb API](http://api.themoviedb.org/).

Then you can run the project or generate native bundle and JAR files.

## Libraries Used

-   [JCodec](http://jcodec.org/docs/working_with_mp4_metadata.html) used for editing metadata of mp4 files.
-   [themoviedbapi](https://github.com/holgerbrandl/themoviedbapi) used to access the TMDb API.
-   [TrayNotification](https://github.com/PlusHaze/TrayNotification) used to show notifications.
